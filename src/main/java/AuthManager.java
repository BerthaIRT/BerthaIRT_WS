import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import io.javalin.Context;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthManager {
    private UrlJwkProvider provider;
    private Map<String, User> userMap;

    public AuthManager(){
        userMap = new HashMap<>();
        try {
            provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-1.amazonaws.com/" + WSMain.awsUserPool + "/.well-known/jwks.json"));
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
        User thisUser = userMap.get(sub);
        if(thisUser != null){ //user already has an AES key stored in the userMap
            try {
                String decrypted = new String(thisUser.getDecrypter().doFinal(Util.fromHexString(ctx.body())));
                System.out.println(String.format("Decrypted: %s", decrypted));
                afterRequest = withDecrypted.withDecryptedRequest(thisUser, decrypted);
                System.out.println(String.format("Encrypting: %s", afterRequest));
                String encrypted = Util.asHex(thisUser.getEncrypter().doFinal(afterRequest.getBytes()));
                ctx.result(encrypted);
            } catch (IllegalBlockSizeException e) {
                ctx.status(404);
            } catch (BadPaddingException e) {
                ctx.status(401);
            }
        }
        else{
            System.out.println("doSecure failed!");
            ctx.status(401);
        }
    }

    public void issueKeys(Context ctx){
        DecodedJWT verified = decodeJWT(ctx);
        String sub = verified.getClaim("sub").asString();
        String username = verified.getClaim("cognito:username").asString();
        Integer groupID = verified.getClaim("custom:groupID").asInt();
        String fcmToken = "";//ctx.body();
        List<String> l = verified.getClaim("cognito:groups").asList(String.class);
        boolean isAdmin = l.contains("Administrators");
        User u = new User(sub, username, groupID, fcmToken, isAdmin);


        String rsaEncryptedAESKey = "";
        String rsaEncryptedIvParams = "";

        try{
            Cipher rsaEncrypter = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            List<AttributeType> attribs = WSMain.idp.adminGetUser(new AdminGetUserRequest().withUsername(username).withUserPoolId(WSMain.awsUserPool)).getUserAttributes();
            for(AttributeType a : attribs) {
                if (a.getName().equals("custom:rsaPublicKey")) {
                    byte[] bytePublicKey = Util.fromHexString(a.getValue());
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    RSAPublicKey rsaPublicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(bytePublicKey));
                    rsaEncrypter.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
                }
            }

            Cipher e = Cipher.getInstance("AES/CBC/PKCS5Padding");
            Cipher d = Cipher.getInstance("AES/CBC/PKCS5Padding");

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256);
            SecretKey aesKey = keygen.generateKey();

            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] ivParams = new byte[e.getBlockSize()];
            sr.nextBytes(ivParams);
            IvParameterSpec initializationVectors = new IvParameterSpec(ivParams);

            e.init(Cipher.ENCRYPT_MODE, aesKey, initializationVectors);
            d.init(Cipher.DECRYPT_MODE, aesKey, initializationVectors);

            u.setEncrypter(e);
            u.setDecrypter(d);

            rsaEncryptedAESKey = Util.asHex(rsaEncrypter.doFinal(aesKey.getEncoded()));
            rsaEncryptedIvParams = Util.asHex(rsaEncrypter.doFinal(initializationVectors.getIV()));
        }catch (Exception e){e.printStackTrace();}

        userMap.put(sub, u);

        JsonObject jay = new JsonObject();
        jay.addProperty("key", rsaEncryptedAESKey);
        jay.addProperty("iv", rsaEncryptedIvParams);
        ctx.result(jay.toString());
    }
}
