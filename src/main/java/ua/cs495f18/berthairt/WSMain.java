package ua.cs495f18.berthairt;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.javalin.Handler;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WSMain extends AWSManager{

    static final boolean ENCRYPTION_ENABLED = true;

    static Map<String, User> userMap = new HashMap<>();
    static Map<Integer, Map<Integer, Report>> reportMap = new HashMap<>();
    static Map<Integer, Group> groupMap = new HashMap<>();
    static Gson gson = new Gson();
    static JsonParser jp = new JsonParser();

    interface AuthCookieHandler {
        String withIdentifiedRequest(User u, String body);
    }

    static Handler requestIdentifier(AuthCookieHandler authCookieRequest){
        return ctx -> {
            String user = ctx.cookieStore("user");
            log(user, "COOKIE USER");
            User u = userMap.get(user);
            //log(gson.toJson(u), "USERMAP");
            String body = ctx.cookieStore("body");
            ctx.result(authCookieRequest.withIdentifiedRequest(u, body));
        };
    }

    public static void addPaths(Javalin app){
        app.put("*/keyexchange", ctx->{
            if(ctx.path().endsWith("test")) ctx.result("SECURE");
        });
        app.put("*/group/info", GroupManager::sendBasicGroupInfo);
        app.put("*/group/create", GroupManager::createNewGroup);
        app.put("*/group/join", GroupManager::addStudentToGroup);

        app.put("*/report/create", requestIdentifier(ReportManager::createNewReport));
        app.put("*/report/update", requestIdentifier(ReportManager::updateReport));
        app.put("*/report/pull/all", requestIdentifier(ReportManager::sendAll));
        app.put("*/report/pull", requestIdentifier(ReportManager::sendSingle));

        app.put("*/alerts", requestIdentifier((u, b)-> gson.toJson(u.getAlerts())));
        app.put("*/group/addadmin", requestIdentifier(GroupManager::addAdminToGroup));
        app.put("*/group/removeadmin", requestIdentifier(GroupManager::removeAdminFromGroup));
        app.put("*/group/togglestatus", requestIdentifier(GroupManager::toggleStatus));
        app.put("*/group/changename", requestIdentifier(GroupManager::newInstitutionName));

        app.put("*/group/emblem", (ctx)->{
            try {
                DecodedJWT jwt = AuthManager.decodeJWT(ctx.header("Authentication"));
                Integer group = Integer.valueOf(jwt.getClaim("custom:groupID").asString());
                if (jwt.getClaim("cognito:username").asString().startsWith("student")) ctx.status(401);
                else
                    ctx.result(UserFileManager.downloadEmblem(group, ctx.body()));
            } catch (Exception e){ctx.status(401);}
        });

        app.put("*/report/media", (ctx)-> {
            try {
                DecodedJWT jwt = AuthManager.decodeJWT(ctx.header("Authentication"));
                //Integer group = Integer.valueOf(jwt.getClaim("custom:groupID").asString());
                User u = userMap.get(jwt.getClaim("cognito:username").asString());
                ctx.result(UserFileManager.downloadMedia(u, ctx.body()));
            } catch (Exception e){
                ctx.status(401);}
        });
    }

    public static void main(String[] args){
        initAWSCrap();
        FireMessage.SERVER_KEY = System.getenv("BERTHA_FCM_KEY");

//        List<UserType> l = idp.listUsers(new ListUsersRequest().withUserPoolId(awsUserPool)).getUsers();
//        for(UserType u : l){
//            if(!u.getUsername().startsWith("student")) continue;
//            idp.adminDeleteUser(new AdminDeleteUserRequest().withUserPoolId(awsUserPool).withUsername(u.getUsername()));
//        }

        List<Group> groups = db.scan(Group.class, new DynamoDBScanExpression());
        List<Report> reports = db.scan(Report.class, new DynamoDBScanExpression());
        List<User> users = db.scan(User.class, new DynamoDBScanExpression());
        if(groups == null) groups = new ArrayList<>();
        if(reports == null) reports = new ArrayList<>();
        if(users == null) users = new ArrayList<>();


        for(Group g : groups) {
            log("Loading group " + g.getGroupID(), "INIT");
            groupMap.put(g.getGroupID(), g);
            reportMap.put(g.getGroupID(), new HashMap<>());
        }

        for(Report r : reports){
            log("Loading report " + r.getReportID() + " for group " + r.getGroupID(), "INIT");
            Map<Integer, Report> groupReports = reportMap.get(r.getGroupID());
                groupReports.put(r.getReportID(), r);
            }

        for(User u : users){
            log("Loading user " + u.getUsername(), "INIT");
            if(!u.getUsername().startsWith("student")) u.setAdmin(true);
            userMap.put(u.getUsername(), u);
        }

        //User me = userMap.get("ssinischo@gmail.com");
        //new FireMessage(me).withRecipient(me.getUsername()).withTitle("hello").withBody("test").send();

        Integer port = Integer.valueOf(System.getenv("BERTHA_PORT"));
        Javalin app = Javalin.create();
        app.enableStaticFiles("/userfiles");
        app.start(port);
        addPaths(app);

        app.before("/app/*", ctx->{
            log(ctx.path(), "PATH");
            if(ctx.path().endsWith("emblem")) return;
            if(ctx.path().endsWith("media")) return;
            AuthManager.storeSecureCookies(ctx);
        });

        app.after("/app/*", ctx->{
            AuthManager.encryptResponseBody(ctx);
        });
    }
}
