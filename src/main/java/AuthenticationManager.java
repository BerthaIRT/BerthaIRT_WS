import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
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
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationManager {
    static class AESCiphers{
        Cipher encrypter;
        Cipher decrypter;
        public AESCiphers(Cipher e, Cipher d){
             encrypter = e;
             decrypter = d;
        }
    }
    static UrlJwkProvider provider;
    static Map<String, AESCiphers> aesCipherMap;
    static AWSCredentialsProvider acp;

    public static void init() {
        try {
            provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_4mPbywTgw/.well-known/jwks.json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        AWSCredentials creds = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return "AKIAIBHF4DER4NBPELGA";
            }

            @Override
            public String getAWSSecretKey() {
                return "PLOYzaL6aUk5kdlfW21auSLPgF8XTH6JNXnReiGt";
            }
        };
        aesCipherMap = new HashMap<>();
        acp = new AWSStaticCredentialsProvider(creds);
    }

    public static DecodedJWT verifyJWT(Context ctx){
        String encoded = ctx.header("Authentication");
        DecodedJWT decoded = JWT.decode(new String(Util.fromHexString(encoded)));
        Jwk jwk = null;
        try {
            jwk = provider.get(decoded.getKeyId());
            Algorithm al = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier j = JWT.require(al).acceptLeeway(3).build();
            DecodedJWT verified = j.verify(decoded.getToken());
            //System.out.println("DECODED JWT \n" + verified.toString());
            //for (String s : verified.getClaims().keySet()) System.out.println(s + ": " + verified.getClaim(s).asString());
            return verified;
        } catch (JwkException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void encryptNewAESKey(Context ctx){
        DecodedJWT jwt = verifyJWT(ctx);
        String hexPublicKey = CognitoManager.getRSAPublicKey(jwt.getClaim("cognito:username").asString());
        System.out.println("THE PUBLIC KEY \n\n" + hexPublicKey);
        try {
            byte[] bytePublicKey = Util.fromHexString(hexPublicKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPublicKey clientKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(bytePublicKey));

            Cipher rsaEncrypter = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaEncrypter.init(Cipher.ENCRYPT_MODE, clientKey);

            AESCiphers aCiphers = new AESCiphers(Cipher.getInstance("AES/CBC/PKCS5Padding"), Cipher.getInstance("AES/CBC/PKCS5Padding"));

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256);
            SecretKey aesKey = keygen.generateKey();

            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] ivParams = new byte[aCiphers.encrypter.getBlockSize()];
            sr.nextBytes(ivParams);
            IvParameterSpec initializationVectors = new IvParameterSpec(ivParams);

            aCiphers.encrypter.init(Cipher.ENCRYPT_MODE, aesKey, initializationVectors);
            aCiphers.decrypter.init(Cipher.DECRYPT_MODE, aesKey, initializationVectors);
            for(String s : jwt.getClaims().keySet()) System.out.println(s);
            String s = jwt.getClaim("sub").asString();
            aesCipherMap.put(s, aCiphers);

            String encryptedKey = Util.asHex(rsaEncrypter.doFinal(aesKey.getEncoded()));
            String encryptedIv = Util.asHex(rsaEncrypter.doFinal(initializationVectors.getIV()));

            JsonObject jay = new JsonObject();
            jay.addProperty("key", encryptedKey);
            jay.addProperty("iv", encryptedIv);
            ctx.result(jay.toString());
        } catch (NullPointerException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }

    public static String encryptToSend(DecodedJWT jwt, String response) throws BadPaddingException, IllegalBlockSizeException {
        AESCiphers aCiphers = aesCipherMap.get(jwt.getClaim("sub").asString());
        byte[] encrypted = aCiphers.encrypter.doFinal(response.getBytes());
        String encoded = Util.asHex(encrypted);
        return encoded;
    }

    public static void testEncryption(Context ctx){
        DecodedJWT jwt = verifyJWT(ctx);
        try {
            AESCiphers aCiphers = aesCipherMap.get(jwt.getClaim("sub").asString());
            String decrypted = new String(aCiphers.decrypter.doFinal(Util.fromHexString(ctx.formParam("data"))));
            if(decrypted.equals("bertha"))
                ctx.result(encryptToSend(jwt, "success"));
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }
}
