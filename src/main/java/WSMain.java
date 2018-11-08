import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.Javalin;
import com.auth0.jwk.UrlJwkProvider;

import java.net.URL;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class WSMain{
    static String poolID = "us-east-1_1abyUmkI0";


    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(6969);


        app.put("/test", ctx->{
        });
    }
}