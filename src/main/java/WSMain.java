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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.org.apache.bcel.internal.util.ClassPath;
import io.javalin.Javalin;
import io.javalin.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.*;

public class WSMain{
    static String resourcePath = "";

    static JsonParser jp;
    static Gson gson;
    static AWSCognitoIdentityProvider idp;
    static DynamoDBMapper db;
    static String awsUserPool = "us-east-1_4mPbywTgw";

    static AuthManager auth;
    static Map<Integer, Map<Integer, Long>> groupLastUpdated;

    public static void initAWSCrap(){
        AWSCredentials creds = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return System.getenv("BERTHA_AWS_KEY");
            }

            @Override
            public String getAWSSecretKey() {
                return System.getenv("BERTHA_AWS_SECRET");
            }
        };

        AWSStaticCredentialsProvider acp = new AWSStaticCredentialsProvider(creds);

        idp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(acp).withRegion(Regions.US_EAST_1).build();

        AmazonDynamoDBClientBuilder bdb = AmazonDynamoDBClientBuilder.standard();
        bdb.withRegion(Regions.US_EAST_1);
        bdb.withCredentials(acp);
        db = new DynamoDBMapper(bdb.build());
    }

    public static void main(String[] args){
        initAWSCrap();

        Integer port = Integer.valueOf(System.getenv("BERTHA_PORT"));

        Javalin app = Javalin.create();
        app.enableStaticFiles("/userfiles");
        app.enableStaticFiles("/website");
        app.start(port);

        jp = new JsonParser();
        gson = new Gson();
        auth = new AuthManager();

        groupLastUpdated = new HashMap<>();
        List<Group> groups = db.scan(Group.class, new DynamoDBScanExpression());
        List<Report> reports = db.scan(Report.class, new DynamoDBScanExpression());
        for(Group g : groups)
            groupLastUpdated.put(g.getGroupID(), new LinkedHashMap<>());
        for(Report r : reports)
            groupLastUpdated.get(r.getGroupID()).put(r.getReportID(), System.currentTimeMillis());

        //WARNING DO NOT UNCOMMENT OR YOU WILL LITERALLY DELETE EVERYONE.  FOR TESTING ONLY
//        List<UserType> l = idp.listUsers(new ListUsersRequest().withUserPoolId(awsUserPool)).getUsers();
//        for(UserType u : l){
//            idp.adminDeleteUser(new AdminDeleteUserRequest().withUserPoolId(awsUserPool).withUsername(u.getUsername()));
//        }

//        createGroupInDatabase("Test Unit", 999999);
//        addAdminToGroup(999999, "ssinischo@gmail.com");
//        createNewCognitoUser(999999, "ssinischo@gmail.com", true);


        //To send to my 2 phones
        //TODO get the proper ID's
        List<String> fireBaseTokens = new ArrayList<>();
        //fireBaseTokens.add("ewZzB-Vjezo:APA91bHk1GojULsjZtBXRo6BFGt09clMqAVHOHvD_7ygwntXbVYk1bk7PHFcXe1Av6csZoZczw5iiHZHjB94Px2e8vfPZQ6mPSf-TM_drzzWZJYgEtFs1DuGTXHXLNDGgLgIoh5nU3Cm");
        //fireBaseTokens.add("dZIzD2vZRWc:APA91bHdshKURPO7iyDmRbCwYtcFxcv4zR6zpEBXU5qv04gwZek2NctZezd1X71sDOEFfIBqxq0hPn1v8wvmc7mhegfRSnNCkqCtvhmb3LZ_epmY17VxdaId1Q9hH1D_KIXzEYjpRfFO");
//
//        try {
//            new FireMessage("lol", "jake is gay").sendToToken(fireBaseTokens);
//        } catch (Exception e) {
//            e.printStackTrace();
//       }
        File dir = new File(WSMain.class.getClassLoader().getResource("userfiles").getFile()); //maybe not necessary
        resourcePath = dir.getAbsolutePath();

        app.before(ctx->{
            System.out.println(ctx.path());
        });

        app.put("/group/create", ctx->{
            JsonObject jay = jp.parse(ctx.body()).getAsJsonObject();
            String firstAdmin = jay.get("firstAdmin").getAsString();
            Integer newGroupID = createGroupInDatabase(jay.get("groupName").getAsString(), null);
            addAdminToGroup(newGroupID, firstAdmin);
            ctx.result(createNewCognitoUser(newGroupID, firstAdmin, true));
        });

        app.put("/group/join/student", ctx->{
            Integer groupID = new Integer(ctx.body());
            String newStudentUsername = addStudentToGroup(groupID);
            ctx.result(createNewCognitoUser(groupID, newStudentUsername, false));
        });

        app.put("/group/info", WSMain::sendBasicGroupInfo);

        app.put("/keys/issue", ctx->db.save(auth.issueKeys(ctx)));

        //All other functions require login

        app.put("/keys/verify", ctx->auth.doSecure(ctx, (u, b)->{
            db.save(u);
            return "SECURE";
        }));

        app.put("/report/pull", ctx->auth.doSecure(ctx, WSMain::sendSingleReport));

        app.put("/report/pull/all", ctx->auth.doSecure(ctx, WSMain::sendAllReports));

        app.put("/report/create", ctx->auth.doSecure(ctx, WSMain::createNewReport));

        app.put("/report/update", ctx->auth.doSecure(ctx, WSMain::updateReport));

        app.put("/group/admins", ctx->auth.doSecure(ctx, WSMain::sendAdminList));

        app.put("/group/alert/pull", ctx->auth.doSecure(ctx, WSMain::sendAlerts));

        app.put("/group/alert/dismiss", ctx->auth.doSecure(ctx, WSMain::dismissAlert));

        app.put("/group/reports", ctx->auth.doSecure(ctx, WSMain::sendReportList));

        app.put("/group/togglestatus", ctx->auth.doSecure(ctx, WSMain::toggleRegistration));

        app.put("/group/emblem", ctx->auth.doSecure(ctx, WSMain::downloadEmblem));

        app.put("/refresh", ctx->auth.doSecure(ctx, WSMain::checkForUpdates));

    }

    private static String downloadEmblem(User u, String body) {
        byte[] imgData = Util.fromHexString(body);
        try {
            FileOutputStream stream = new FileOutputStream(resourcePath + "/emblem/" + u.getGroupID() + ".png");
            stream.write(imgData);
        } catch(Exception e ){e.printStackTrace();}
        return "OK";
    }

    private static String dismissAlert(User u, String body) {
        Group g = db.load(Group.class, u.getGroupID());
        List<Integer> li = g.getGroupAdminAlerts().get(u.getUsername());
        li.removeIf((x)->x.equals(Integer.valueOf(body)));
        db.save(g);
        return "OK";
    }

    private static String checkForUpdates(User u, String body) {
        JsonArray idsWithUpdate = new JsonArray();
        for(Map.Entry<Integer, Long> e : groupLastUpdated.get(u.getGroupID()).entrySet()) {
            System.out.println(String.format("report %d was updated on %d - client updated on %d", e.getKey(), e.getValue(), u.getLastUpdated()));
            if (e.getValue() > u.getLastUpdated()) idsWithUpdate.add(e.getKey());
            //else break;
        }
        u.setLastUpdated(System.currentTimeMillis());
        if(idsWithUpdate.size() == 0) return "nope";
        return idsWithUpdate.toString();
    }

    private static String sendAlerts(User u, String body) {
        FireMessage f = new FireMessage("MY TITLE", "TEST MESSAGE");

/*        //To Individual Devices
        List<String> fireBaseTokens = new ArrayList<>();
        fireBaseTokens.add("fm2zarYa8c8:APA91bHO8RbRd3J0mRWjPNa1-CGxN_Z3AFVN18aDOH5zo7lKv3iVZH8cOxTl4O9qsP5flR76I7pJoWvW5BpCKikeJS51H-OqU882XghLVQiEuSxJ0fP2w1rxCkbCsAPvJV3h3Bip8uRG");
        fireBaseTokens.add("dZIzD2vZRWc:APA91bHdshKURPO7iyDmRbCwYtcFxcv4zR6zpEBXU5qv04gwZek2NctZezd1X71sDOEFfIBqxq0hPn1v8wvmc7mhegfRSnNCkqCtvhmb3LZ_epmY17VxdaId1Q9hH1D_KIXzEYjpRfFO");
        try {
            f.sendToToken(fireBaseTokens.toArray(new String[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }*/


        Group g = db.load(Group.class, u.getGroupID());
        Map<Integer, Message> groupAlerts = g.getGroupAlerts();
        JsonArray myAlerts = new JsonArray();
        List<Integer> alertIDs = g.getGroupAdminAlerts().get(u.getUsername());

        if(alertIDs == null) return myAlerts.toString();
        for(Integer i : alertIDs) //get this admin's unread alerts
            myAlerts.add(gson.toJson(groupAlerts.get(i))); //add unread alert to list
        return myAlerts.toString();
    }

    private static String sendSingleReport(User u, String body) {
        Report r = db.load(Report.class, new Integer(body), u.getGroupID());
        //if ((!r.getGroupID().equals(u.getGroupID())) || (!u.getIsAdmin() && !r.getStudentID().equals(u.getUsername())))
        //    return null;
        if(u.getIsAdmin()) r.setStudentID("Hidden");
        return gson.toJson(r);
    }
    private static String sendAllReports(User u, String body) {
        JsonArray jarray = new JsonArray();

        Map<String, AttributeValue> attribs = new HashMap<>();
        attribs.put(":id", new AttributeValue().withN("999999"));
        List<Report> query = db.scan(Report.class, new DynamoDBScanExpression().withFilterExpression("groupID = :id").withExpressionAttributeValues(attribs));

        for(Report r : query){
            if(u.getIsAdmin()) r.setStudentID("Hidden");
            else if(!r.getStudentID().equals(u.getUsername())) continue;
            jarray.add(gson.toJson(r));
        }
        return jarray.toString();
    }

    private static String sendReportList(User u, String body) {
        Group g = db.load(Group.class, u.getGroupID());
        JsonArray ids = new JsonArray();
        if(u.getIsAdmin())
            for(Integer i : g.getGroupReports().keySet())
                ids.add(i);
        else
            for(Integer i : g.getGroupStudentReports().get(u.getUsername()))
                ids.add(i);

        return ids.toString();
    }

    private static String sendAdminList(User u, String body) {
        Group g = db.load(Group.class, u.getGroupID());
        JsonArray adminList = new JsonArray();
        for(String s : g.getGroupAdminAlerts().keySet()) adminList.add(s);
        return adminList.toString();
    }

    private static void sendBasicGroupInfo(Context ctx){
        Integer groupID = new Integer(ctx.body());
        Group g = db.load(Group.class, groupID);
        JsonObject jay = new JsonObject();
        if(g != null) {
            jay.addProperty("groupName", g.getGroupName());
            jay.addProperty("groupStatus", g.getGroupStatus());
        }
        else jay.addProperty("groupName", "NONE");
        ctx.result(jay.toString());
    }

    private static String toggleRegistration(User u, String body){
        Group g = db.load(Group.class, u.getGroupID());
        if(g.getGroupStatus().equals("Open")) g.setGroupStatus("Closed");
        else g.setGroupStatus("Open");
        //todo group log
        db.save(g);
        return g.getGroupStatus();
    }

    public static String createNewReport(User u, String body){
        Report r = gson.fromJson(body, Report.class);
        r.setCreationDate(System.currentTimeMillis());
        r.setStudentID(u.getUsername());
        r.setGroupID(u.getGroupID());
        r.setStatus("New");

        Group g = db.load(Group.class, u.getGroupID());
        r.setReportID(g.getGroupReports().size() + 1000);
        g.getGroupReports().put(r.getReportID(), System.currentTimeMillis());

        g.addAlertForAll(auth.getUserMap(), new Message(u, "New report", r.getReportID()), null);
        db.save(r);
        db.save(g);

        Map<Integer, Long> balls = groupLastUpdated.get(r.getGroupID());
        if(balls == null)
            groupLastUpdated.put(u.getGroupID(), new HashMap<>());
        groupLastUpdated.get(u.getGroupID()).put(r.getReportID(), System.currentTimeMillis());
        return gson.toJson(r);
    }

    public static String updateReport(User u, String body){
        Report rNew = gson.fromJson(body, Report.class);
        Report rOld = db.load(Report.class, rNew.getReportID(), rNew.getGroupID());

        List<Field> changes = new ArrayList<>();
        try {
            for (Field f : Report.class.getDeclaredFields()) {
                if (!f.get(rNew).equals(f.get(rOld)))
                    changes.add(f);
            }
        }catch (Exception e){e.printStackTrace();}
        if(changes.size() == 0) return body;

        Group g = db.load(Group.class, u.getGroupID());

        boolean notifyAssignees = false;
        List<String> assigneeAlertList = rNew.getAssignedTo();
        assigneeAlertList.removeIf( s -> s.equals(u.getUsername()));

        while(changes.size() > 0){
            Field changedField = changes.remove(0);
            String removed = null;
            String added = null;

            switch (changedField.getName()){
                case "status":
                    String status = rNew.getStatus();
                    rNew.addLog(new Message(u, "Status changed to " + status));
                    if(status.equals("Open"))
                        g.addAlertForAll(auth.getUserMap(), new Message(u, "Re-opened report", rNew.getReportID()), null);
                    else notifyAssignees = true;
                    break;
                case "assignedTo":
                    removed = Util.findRemoved(rOld.getAssignedTo(), rNew.getAssignedTo());
                    added = Util.findAdded(rOld.getAssignedTo(), rNew.getAssignedTo());
                    if(removed != null) rNew.addLog(new Message(u,"Removed assignees " + removed));
                    if(added != null) rNew.addLog(new Message(u,"Added assignees " + added));
                    g.addAlertForAdmins(auth.getUserMap(), new Message(u, "Assignees updated", rNew.getReportID()), rOld.getAssignedTo());
                    g.addAlertForAdmins(auth.getUserMap(), new Message(u, "Assigned to me", rNew.getReportID()), Util.addedAdmins(rOld.getAssignedTo(), rNew.getAssignedTo()));
                    break;
                case "tags":
                    removed = Util.findRemoved(rOld.getTags(), rNew.getTags());
                    added = Util.findAdded(rOld.getTags(), rNew.getTags());
                    if(removed != null) rNew.addLog(new Message(u, "Removed tags " + removed));
                    if(added != null) rNew.addLog(new Message(u,"Added tags " + added));
                    notifyAssignees = true;
                    break;
                case "categories":
                    removed = Util.findRemoved(rOld.getCategories(), rNew.getCategories());
                    added = Util.findAdded(rOld.getCategories(), rNew.getCategories());
                    if(removed != null) rNew.addLog(new Message(u, "Removed category " + removed));
                    if(added != null) rNew.addLog(new Message(u, "Added category " + added));
                    notifyAssignees = true;
                    break;
                case "notes":
                    if(rNew.getNotes().size() == rOld.getNotes().size()) break;
                    rNew.addLog(new Message(u, "Added administrator note #" + rNew.getNotes().size()));
                    Message newNote = rNew.getNotes().get(rNew.getNotes().size()-1);
                    newNote.setMessageID(rNew.getNotes().size());
                    newNote.setReportID(rNew.getReportID());
                    newNote.setMessageSubject(u.getUsername());
                    newNote.setMessageTimestamp(System.currentTimeMillis());
                    notifyAssignees = true;
                    break;
                case "messages":
                    if(rNew.getMessages().size() == rOld.getMessages().size()) break;
                    rNew.addLog(new Message(u, "Sent message #" + rNew.getMessages().size()));
                    Message newMessage = rNew.getMessages().get(rNew.getMessages().size()-1);
                    newMessage.setMessageID(rNew.getMessages().size());
                    newMessage.setReportID(rNew.getReportID());
                    newMessage.setMessageSubject(u.getUsername());
                    newMessage.setMessageTimestamp(System.currentTimeMillis());

                    if(u.getIsAdmin()){
                        g.addAlertForAdmins(auth.getUserMap(), new Message(u, "Admin message", rNew.getReportID()), assigneeAlertList);
                    }
                    else if (rNew.getAssignedTo().size() > 0) g.addAlertForAdmins(auth.getUserMap(), new Message(u, "Student message", rNew.getReportID()), rNew.getAssignedTo());
                    else g.addAlertForAll(auth.getUserMap(), new Message(u, "Student message", rNew.getReportID()), null);
                    break;
            }
            if(notifyAssignees) g.addAlertForAdmins(auth.getUserMap(), new Message(u, "Details updated", rNew.getReportID()), assigneeAlertList);
        }
        g.notifyUpdate(rNew.getReportID());
        db.save(rNew);
        db.save(g);

        groupLastUpdated.get(rNew.getGroupID()).remove(rNew.getReportID());
        groupLastUpdated.get(rNew.getGroupID()).put(rNew.getReportID(), System.currentTimeMillis());
        return gson.toJson(rNew, Report.class);
    }

    private static Integer createGroupInDatabase(String groupName, Integer newGroupID){
        if(newGroupID == null) newGroupID = new Random().nextInt(1000000);
        if(newGroupID < 100000 || db.load(Group.class, newGroupID) != null) return createGroupInDatabase(groupName, null);
        Group g = new Group();
        g.setGroupName(groupName);
        g.setGroupID(newGroupID);
        db.save(g);
        return newGroupID;
    }

    public static void addAdminToGroup(Integer groupID, String username){
        Group g = db.load(Group.class, groupID);
        g.getGroupAdminAlerts().put(username, new ArrayList<>());
        db.save(g);
    }

    public static String addStudentToGroup(Integer groupID){
        Group g = db.load(Group.class, groupID);
        String newStudentUsername = ("student-" + groupID + "-" + (1000 + g.getGroupStudentReports().size()));
        g.getGroupStudentReports().put(newStudentUsername, new ArrayList<>());
        db.save(g);
        return newStudentUsername;
    }

    public static String createNewCognitoUser(Integer groupID, String username, boolean isAdmin){
        List<AttributeType> attribs = new ArrayList<>();
        attribs.add(new AttributeType().withName("custom:groupID").withValue(groupID.toString()));
        attribs.add(new AttributeType().withName("custom:rsaPublicKey").withValue("none"));

        String group = "Students";
        String newPassword = "BeRThAfirsttimestudent";

        if(isAdmin){
            group = "Administrators";
            attribs.add(new AttributeType().withName("email_verified").withValue("true"));
            attribs.add(new AttributeType().withName("email").withValue(username));
            newPassword = String.format("%09d", new SecureRandom().nextInt(1000000000));
        }

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
            e.printStackTrace();
            return("EXISTING USER");
        }

        idp.adminAddUserToGroup(new AdminAddUserToGroupRequest()
                .withUserPoolId(awsUserPool)
                .withUsername(username)
                .withGroupName(group)
        );

        return username;
    }
}
