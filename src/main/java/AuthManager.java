import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import io.javalin.Context;
import io.javalin.Handler;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthManager {
    private UrlJwkProvider provider;
    private Map<String, Client> clientMap;

    public AuthManager(){
        clientMap = new HashMap<>();
        try {
            provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_4mPbywTgw/.well-known/jwks.json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public DecodedJWT decodeJWT(Context ctx){
        DecodedJWT verified = null;
        try{
            String encoded = ctx.header("Authentication");
            DecodedJWT decoded = JWT.decode(new String(Util.fromHexString(encoded)));
            Jwk jwk = provider.get(decoded.getKeyId());
            Algorithm al = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier j = JWT.require(al).acceptLeeway(3).build();
            verified = j.verify(decoded.getToken());
        } catch (JwkException e) {
            System.out.println(e.getMessage());
            ctx.status(401);
        }
        return verified;
    }

    public void doSecure(Context ctx, SecureRequestHandler withDecrypted){
        DecodedJWT verified = decodeJWT(ctx);
        String afterRequest = "";
        String sub = verified.getClaim("sub").asString();
        Client thisClient = clientMap.get(sub);
        if(thisClient != null){ //user already has an AES key stored in the userMap
            try {
                String decrypted = new String(thisClient.decrypter.doFinal(Util.fromHexString(ctx.body())));
                System.out.println(String.format("Decrypted: %s", decrypted));
                afterRequest = withDecrypted.withDecryptedRequest(thisClient, decrypted);
                System.out.println(String.format("Encrypting: %s", afterRequest));
                String encrypted = Util.asHex(thisClient.encrypter.doFinal(afterRequest.getBytes()));
                ctx.result(encrypted);
            } catch (IllegalBlockSizeException e) {
                ctx.status(404);
            } catch (BadPaddingException e) {
                ctx.status(401);
            }
        }
        else ctx.status(401);
    }

    public void issueKeys(Context ctx){
        DecodedJWT verified = decodeJWT(ctx);
        String sub = verified.getClaim("sub").asString();
        String username = verified.getClaim("cognito:username").asString();
        String groupID = verified.getClaim("custom:groupID").asString();
        List<String> l = verified.getClaim("cognito:groups").asList(String.class);
        boolean isAdmin = l.contains("Administrators");
        Client u = new Client(sub, username, new Integer(groupID), isAdmin);
        clientMap.put(sub, u);

        JsonObject jay = new JsonObject();
        jay.addProperty("key", u.rsaEncryptedAESKey);
        jay.addProperty("iv", u.rsaEncryptedIvParams);
        ctx.result(jay.toString());
    }
}
