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
import java.net.URL;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

public class AuthManager extends WSMain{

    private static UrlJwkProvider provider;

    public AuthManager(){
    }

    public static DecodedJWT decodeJWT(String encoded){
        if(encoded == null) return null;
        if(provider == null){
            try {provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-1.amazonaws.com/" + awsUserPool + "/.well-known/jwks.json"));}
            catch (Exception e) {e.printStackTrace();}
        }

        DecodedJWT verified = null;
        try{
            DecodedJWT decoded = JWT.decode(new String(Util.fromHexString(encoded)));
            Jwk jwk = provider.get(decoded.getKeyId());
            Algorithm al = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier j = JWT.require(al).acceptLeeway(3).build();
            verified = j.verify(decoded.getToken());
            log(verified.getClaims(), "JWT");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return verified;
    }

    public static void storeSecureCookies(Context ctx){
        String[] exceptions = new String[]{"group/info", "/group/join", "/group/create"};
        for(String s : exceptions) if(ctx.path().endsWith(s)){
            log("Allowing auth exception", "AUTH");
            return;
        }

        log("Decoding JWT", "AUTH");
        DecodedJWT jwt = decodeJWT(ctx.header("Authentication"));
        if(jwt == null) {
            log("Warning: no JWT for request", "AUTH");
            return;
        }
        String username = jwt.getClaim("cognito:username").asString();
        String sub = jwt.getClaim("sub").asString();
        List<String> l = jwt.getClaim("cognito:groups").asList(String.class);
        boolean isAdmin = l.contains("Administrators");
        if(ctx.path().endsWith("/admin") && !isAdmin){
            ctx.status(401);
            return;
        }

        User u = userMap.get(username);
        if(u != null && u.getSub() != null && u.getSub().equals(sub)){
            log("User " + username + " has an active session.", "AUTH");
            ctx.cookieStore("user", u);
            ctx.cookieStore("body", decryptRequestBody(u, ctx.body()));
            return;
        }
        Integer groupID = Integer.valueOf(jwt.getClaim("custom:groupID").asString());
        String fcmToken = ctx.body();
        if(u == null || !u.getFcmToken().equals(ctx.body())) {
            u = new User(username, sub, groupID, fcmToken, isAdmin);
            db.save(u);
            log("User " + username + " has been added to the database.", "AUTH");
        }
        log("User " + username + " is starting a new session.", "AUTH");

        String rsaEncryptedAESKey = "";
        String rsaEncryptedIvParams = "";
        try{
            Cipher rsaEncrypter = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            List<AttributeType> attribs = idp.adminGetUser(new AdminGetUserRequest().withUsername(username).withUserPoolId(WSMain.awsUserPool)).getUserAttributes();
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

        userMap.put(username, u);
        JsonObject jay = new JsonObject();
        jay.addProperty("key", rsaEncryptedAESKey);
        jay.addProperty("iv", rsaEncryptedIvParams);

        ctx.result(jay.toString());
    }

    public static String decryptRequestBody(User u, String hexEncoded){
        byte[] decoded = Util.fromHexString(hexEncoded);
        String decrypted = null;
        try {
            decrypted = new String(u.getDecrypter().doFinal(decoded));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(decrypted != null){
            log("Decrypted for user: " + u.getUsername() + "\n      " + decrypted, "AUTH");
            return decrypted;
        }
        log("ERROR: request failed to decrypt! (is cognito token still valid?) = " + u.getUsername(), "AUTH");
        return hexEncoded;
    }

    public static void encryptResponseBody(Context ctx){
        if(ctx.resultString() == null) log("WARNING: nothing to encrypt!", "AUTH");
        User u = ctx.cookieStore("user");
        if(u == null){
            return;
        }
        log("Encrypting for user: " + u.getUsername() + "\n      " + ctx.resultString(), "AUTH");
        try {
            ctx.result(Util.asHex(u.getEncrypter().doFinal(ctx.resultString().getBytes())));
            ctx.status(200);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }
}
