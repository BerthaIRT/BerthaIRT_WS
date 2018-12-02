import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.javalin.Handler;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WSMain extends AWSManager{

    static Map<String, User> userMap = new HashMap<>();
    static Map<Integer, List<Report>> reportMap = new HashMap<>();
    static Map<Integer, Group> groupMap = new HashMap<>();
    static Gson gson = new Gson();
    static JsonParser jp = new JsonParser();

    interface AuthCookieHandler {
        String withIdentifiedRequest(User u, String body);
    }

    static Handler requestIdentifier(AuthCookieHandler authCookieRequest){
        return ctx -> {
            User u = ctx.cookieStore("user");
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

        app.put("*/admin/group/togglestatus", requestIdentifier(GroupManager::toggleStatus));
        app.put("*/admin/group/uploademblem", requestIdentifier(UserFileManager::downloadEmblem));
    }

    public static void main(String[] args){
        initAWSCrap();
        List<Group> groups = db.scan(Group.class, new DynamoDBScanExpression());
        List<Report> reports = db.scan(Report.class, new DynamoDBScanExpression());
        List<User> users = db.scan(User.class, new DynamoDBScanExpression());
        if(reports != null)
            for(Report r : reports){
                List<Report> groupReports = reportMap.get(r.getGroupID());
                if(groupReports == null){
                    List<Report> newList = new ArrayList<>();
                    newList.add(r);
                    reportMap.put(r.getGroupID(), newList);
                }
                else groupReports.add(r);
            }
        if(groups != null) for(Group g : groups) groupMap.put(g.getGroupID(), g);
        if(users != null) for(User u : users) userMap.put(u.getUsername(), u);

        Integer port = Integer.valueOf(System.getenv("BERTHA_PORT"));
        Javalin app = Javalin.create();
        app.enableStaticFiles("/userfiles");
        app.start(port);

        app.before("/*", ctx->{
            log(ctx.path(), "REQUEST");
            AuthManager.storeSecureCookies(ctx);
        });

        app.after("/*", ctx->{
            AuthManager.encryptResponseBody(ctx);
            ctx.clearCookieStore();
        });

        addPaths(app);
    }
}
