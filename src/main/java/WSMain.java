import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.Context;
import io.javalin.Javalin;
import com.auth0.jwk.UrlJwkProvider;

import java.net.URL;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class WSMain{

    public static void main(String[] args) throws Exception {
        AuthenticationManager.init();
        CognitoManager.init();
        DBManager.init();
        Javalin app = Javalin.create().start(6969);

        app.before(r->{
            System.out.println(r.formParamMap());
        });

        //System.out.println(cog.newUser("696969", "hi@hi.com", true));

        app.put("/keys/aes", AuthenticationManager::encryptNewAESKey);

        app.put("/keys/test", AuthenticationManager::testEncryption);

        app.put("/group/new", WSMain::createNewGroup);

        app.put("/group/lookup", CognitoManager::getGroupID);
    }

    public static void createNewGroup(Context ctx){
        //Will not be encrypted
        String newAdmin = ctx.formParam("newAdmin");
        String groupName = ctx.formParam("groupName");
        String newGroupID = DBManager.createGroup(groupName);
        CognitoManager.newUser(newGroupID, newAdmin, true);
        ctx.result("OK");
    }
}