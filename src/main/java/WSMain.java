import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import io.javalin.Context;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

public class WSMain{
    static JsonParser jp;
    static Gson gson;
    static UrlJwkProvider provider;
    static AWSCognitoIdentityProvider idp;
    static DynamoDBMapper db;
    String awsClientId = "2kssdfqe3oirdasavb7og6bh76";
    static String awsUserPool = "us-east-1_4mPbywTgw";

    static Map<String, User> userMap = new HashMap<>();

    static public String doRequest(User u, String path, String body){
        System.out.println("User: " + u.username);
        System.out.println("Group: " + u.groupID);
        System.out.println("Path: " + path);

        switch(path){
            case "/keys/verify": return verifyKeys(u);
            case "/group/lookup/auth": return authGroupLookup(u);
            case "/report/new": return createNewReport(u, body);
            case "/report/pull": return pullReports(u);
            case "/report/update": return updateReport(u, body);
        }
        return "404";
    }

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(6969);
        app.before(ctx->System.out.println(ctx.path()));
        jp = new JsonParser();
        gson = new Gson();

        AWSCredentials creds = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return "AKIAJMDHSUATRNN2IAVQ";
            }

            @Override
            public String getAWSSecretKey() {
                return "EjcyBjI8SwDjaw2VWAiahnm2/ucZxY3iLEQ78ZZQ";
            }
        };
        AWSStaticCredentialsProvider acp = new AWSStaticCredentialsProvider(creds);

        try {
            provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_4mPbywTgw/.well-known/jwks.json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        idp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(acp).withRegion(Regions.US_EAST_1).build();

        AmazonDynamoDBClientBuilder bdb = AmazonDynamoDBClientBuilder.standard();
        bdb.withRegion(Regions.US_EAST_1);
        bdb.withCredentials(acp);
        db = new DynamoDBMapper(bdb.build());

        //WARNING DO NOT UNCOMMENT OR YOU WILL LITERALLY DELETE EVERYONE.  FOR TESTING ONLY
        //List<UserType> l = idp.listUsers(new ListUsersRequest().withUserPoolId(awsUserPool)).getUsers();
        //for(UserType u : l){
        //    idp.adminDeleteUser(new AdminDeleteUserRequest().withUserPoolId(awsUserPool).withUsername(u.getUsername()));
        //}

        //everything is called by doRequest and is secured.  or passed on through security if not logged in
        app.put("/*", ctx->secureRequest(ctx));

//        Report r = new Report();
//        r.setReportID("100");
//        r.setGroupID("6969");
//        r.setDescription("TESTING ONE TWO");
//        db.save(r);
    }

    public static DecodedJWT decodeJWT(Context ctx){
        DecodedJWT verified = null;
        try{
            String encoded = ctx.header("Authentication");
            DecodedJWT decoded = JWT.decode(new String(Util.fromHexString(encoded)));
            Jwk jwk = provider.get(decoded.getKeyId());
            Algorithm al = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier j = JWT.require(al).acceptLeeway(3).build();
            verified = j.verify(decoded.getToken());
        } catch (JwkException e) {
            e.printStackTrace();
            ctx.status(401);
        }
        return verified;
    }

    static public void secureRequest(Context ctx){
        //non-authenticated functions
        switch(ctx.path()){
        case "/keys/issue":
            issueKeys(ctx);
            return;
        case "/group/new":
            ctx.result(createNewGroup(ctx.body()));
            return;
        case "/group/lookup/unauth":
            ctx.result(unauthGroupLookup(ctx.body()));
            return;
        case "/group/join/student":
            ctx.result(addStudentToGroup(ctx.body()));
            return;
        }
        
        DecodedJWT verified = decodeJWT(ctx);
        String afterRequest = "";
        String sub = verified.getClaim("sub").asString();
        User thisUser = userMap.get(sub);
        if(thisUser != null){ //user already has an AES key stored in the userMap
            try {
                String decrypted = new String(thisUser.decrypter.doFinal(Util.fromHexString(ctx.body())));
                System.out.println(decrypted);
                afterRequest = doRequest(thisUser, ctx.path(), decrypted);
                System.out.println(afterRequest);
                String encrypted = Util.asHex(thisUser.encrypter.doFinal(afterRequest.getBytes()));
                ctx.result(encrypted);
            } catch (IllegalBlockSizeException e) {
                ctx.status(404);
            } catch (BadPaddingException e) {
                ctx.status(401);
            }
        }
    }

    static public void issueKeys(Context ctx){
        DecodedJWT verified = decodeJWT(ctx);
        String sub = verified.getClaim("sub").asString();
        String username = verified.getClaim("cognito:username").asString();
        String groupID = verified.getClaim("custom:groupID").asString();
        List<String> l = verified.getClaim("cognito:groups").asList(String.class);
        boolean isAdmin = l.contains("Administrators");
        User u = new User(sub, username, groupID, isAdmin);
        userMap.put(sub, u);

        JsonObject jay = new JsonObject();
        jay.addProperty("key", u.rsaEncryptedAESKey);
        jay.addProperty("iv", u.rsaEncryptedIvParams);
        System.out.println(jay.toString());
        ctx.result(jay.toString());
    }

    public static String createNewCognitoUser(String groupID, String username, boolean isAdmin){
        List<AttributeType> attribs = new ArrayList<>();
        attribs.add(new AttributeType().withName("custom:groupID").withValue(groupID));
        attribs.add(new AttributeType().withName("custom:rsaPublicKey").withValue("none"));
        String group;
        String newPassword = "BeRThAfirsttimestudent";
        if(isAdmin){
            group = "Administrators";
            attribs.add(new AttributeType().withName("email_verified").withValue("true"));
            attribs.add(new AttributeType().withName("email").withValue(username));
            newPassword = String.format("%09d", new SecureRandom().nextInt(1000000000));
        }
        else group = "Students";
        AdminCreateUserRequest req = new AdminCreateUserRequest()
                .withUserPoolId(awsUserPool)
                .withUsername(username)
                .withTemporaryPassword(newPassword)
                .withUserAttributes(attribs);
        if(isAdmin) req=req.withDesiredDeliveryMediums(DeliveryMediumType.EMAIL);
        try{
            idp.adminCreateUser(req);
        }
        catch(AWSCognitoIdentityProviderException e){
            return("EXISTING USER");
        }
        idp.adminAddUserToGroup(new AdminAddUserToGroupRequest()
                .withUserPoolId(awsUserPool)
                .withUsername(username)
                .withGroupName(group)
        );
        return username;
    }

    // =============================================
    // ==============  UNAUTHENTICATED =============
    // =============================================

    public static String addAdminToGroup(String groupID, String username){
        //only unauthed when creating new group.  otherwise called from authed function
        System.out.println(groupID);
        Group g = db.load(Group.class, groupID);
        g.admins.add(username);
        db.save(g);
        return "OK";
    }

    public static String addStudentToGroup(String groupID){
        Group g = db.load(Group.class, groupID);
        String newStudentUsername = ("student-" + groupID + "-" + (Integer) (g.baseStudentID));
        g.baseStudentID++;
        db.save(g);
        return createNewCognitoUser(groupID, newStudentUsername, false);
    }

    public static String createNewGroup(String body){
        JsonObject jay = jp.parse(body).getAsJsonObject();
        String newGroupCode = ((Integer) new Random().nextInt(1000000)).toString();
        Group g = new Group(newGroupCode, jay.get("groupName").getAsString(), jay.get("newAdmin").getAsString());
        db.save(g);
        return createNewCognitoUser(g.groupID, jay.get("newAdmin").getAsString(), true);
    }

    public static String unauthGroupLookup(String groupID){
        Group g = db.load(Group.class, groupID);
        if(g == null) return "NONE";
        g.setAlerts(new ArrayList<>()); // hide alerts from students
        return gson.toJson(g);
    }

    // =============================================
    // ==============  AUTHENTICATED ===============
    // =============================================

    public static String verifyKeys(User u){
        //send alert if admin logs in
        return "SECURE";
    }

    public static String authGroupLookup(User u){
        Group g = db.load(Group.class, u.groupID);
        if(!u.isAdmin) g.setAlerts(new ArrayList<>());
        return gson.toJson(g);
    }

    public static String createNewReport(User u, String body){
        Report r = gson.fromJson(body, Report.class);
        r.setCreationTimestamp(System.currentTimeMillis());
        Group g = db.load(Group.class, u.groupID);
        r.setReportID(((Integer) g.baseReportID).toString());
        r.setStudentID(u.username);
        r.setGroupID(u.groupID);
        r.setStatus("New");
        r.addLog(new Log(u, "Report created"));
        db.save(r);

        g.addAlert(new Alert(r, "Unopened report"));
        db.save(g);

        return gson.toJson(r);
    }

    public static String pullReports(User u){
        Map<String, String> attribNames = new HashMap<>();
        Map<String, AttributeValue> attribVals = new HashMap<>();
        if(u.isAdmin) {
            attribNames.put("#id", "groupID");
            attribVals.put(":val", new AttributeValue().withS(u.groupID)); //this is fuckin dumb but guess it's easier than what we had to do before
        }
        else {
            attribNames.put("#id", "studentID");
            attribVals.put(":val", new AttributeValue().withS(u.username));
        }

        DynamoDBScanExpression q = new DynamoDBScanExpression()
                .withFilterExpression("#id = :val")
                .withExpressionAttributeNames(attribNames).withExpressionAttributeValues(attribVals);
        List<Report> reports = db.scan(Report.class, q);
        JsonArray jarray = new JsonArray();
        for(Report r : reports){
            if (u.isAdmin)
                r.setStudentID("Hidden");
            else if (r.status.equals("Closed")) continue;
            jarray.add(gson.toJson(r));
        }
        return jarray.toString();
    }

    public static String updateReport(User u, String body){
        Report rNew = gson.fromJson(body, Report.class);
        Report rOld = db.load(Report.class, rNew.getReportID(), rNew.getGroupID());

        List<Field> changes = new ArrayList<>();
        try {
            for (Field f : Report.class.getDeclaredFields()) {
                if(f.getName().equals("logs")) continue;
                if (!f.get(rNew).equals(f.get(rOld)))
                    changes.add(f);
            }
        }catch (Exception e){e.printStackTrace();}
        if(changes.size() == 0) return body;

        Group g = db.load(Group.class, u.groupID);
        while(changes.size() > 0){
            Field changedField = changes.remove(0);
            String removed = null;
            String added = null;
            switch (changedField.getName()){
                case "status":
                    String s = rNew.getStatus();
                    rNew.addLog(new Log(u, "Status changed to " + s));
                    if(s.equals("Open")) g.addAlert(new Alert(rNew, "Reopened report").addAdmins(g.getAdmins()));
                    break;
                case "assignedTo":
                    removed = Util.findRemoved(rOld.getAssignedTo(), rNew.getAssignedTo());
                    added = Util.findAdded(rOld.getAssignedTo(), rNew.getAssignedTo());
                    if(removed != null) rNew.addLog(new Log(u, "Removed assignees " + removed));
                    if(added != null) rNew.addLog(new Log(u, "Added assignees " + added));
                    g.addAlert(new Alert(rNew, "Assigned to me").addAdmins(Util.addedAdmins(rOld.getAssignedTo(), rNew.getAssignedTo())));
                    break;
                case "tags":
                    removed = Util.findRemoved(rOld.getTags(), rNew.getTags());
                    added = Util.findAdded(rOld.getTags(), rNew.getTags());
                    if(removed != null) rNew.addLog(new Log(u, "Removed tags " + removed));
                    if(added != null) rNew.addLog(new Log(u, "Added tags " + added));
                    break;
                case "categories":
                    removed = Util.findRemoved(rOld.getCategories(), rNew.getCategories());
                    added = Util.findAdded(rOld.getCategories(), rNew.getCategories());
                    if(removed != null) rNew.addLog(new Log(u, "Removed category " + removed));
                    if(added != null) rNew.addLog(new Log(u, "Added category " + added));
                    break;
                case "notes":
                    rNew.addLog(new Log(u, "Added administrator note"));
                    break;
                case "messages":
                    rNew.addLog(new Log(u, "New message"));
                    Alert a = new Alert(rNew, "Student message");
                    if(rNew.getAssignedTo().size() == 0) g.addAlert(a.addAdmins(g.getAdmins()));
                    else g.addAlert(a.addAdmins(rNew.getAssignedTo()));
                    break;
            }
        }
        db.save(rNew);
        db.save(g);
        return gson.toJson(rNew, Report.class);
    }
}
