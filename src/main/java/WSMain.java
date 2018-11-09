import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Context;
import io.javalin.Javalin;

public class WSMain{
    static JsonParser jp;
    static Gson gson;

    public static void main(String[] args) throws Exception {
        jp = new JsonParser();
        gson = new Gson();
        AuthenticationManager.init();
        CognitoManager.init();
        DBManager.init();
        Javalin app = Javalin.create().start(6969);

        app.before(r->{
            System.out.println(r.body());
        });

        //System.out.println(cog.newUser("696969", "hi@hi.com", true));

        app.put("/keys/aes", AuthenticationManager::encryptNewAESKey);

        app.put("/keys/test", AuthenticationManager::testEncryption);

        app.put("/group/new", WSMain::createNewGroup);

        app.put("/group/lookup", DBManager::getGroupDetails);

        app.put("/group/join", DBManager::registerNewStudent);

        app.put("/report/new", WSMain::createNewReport);
    }

    public static void createNewGroup(Context ctx){
        //Will not be encrypted
        JsonObject jay = jp.parse(ctx.body()).getAsJsonObject();
        String newAdmin = jay.get("newAdmin").getAsString();
        String groupName = jay.get("groupName").getAsString();
        String newGroupID = DBManager.createGroup(groupName);
        CognitoManager.newUser(newGroupID, newAdmin, true);
        ctx.result("OK");
    }

    public static void createNewReport(Context ctx){
        DecodedJWT jwt = AuthenticationManager.verifyJWT(ctx);
        String groupID = jwt.getClaim("custom:groupID").asString();
        String decrypted = AuthenticationManager.decryptRequest(jwt, ctx.body());
        Report r = gson.fromJson(decrypted, Report.class);
        r.creationTimestamp = r.lastActionTimestamp = ((Long) System.currentTimeMillis()).toString();
        r.reportId = DBManager.getNewReportID(groupID);
        DBManager.storeNewReport(r, groupID);
        ctx.result(AuthenticationManager.encryptResponse(jwt, gson.toJson(r)));
    }
}