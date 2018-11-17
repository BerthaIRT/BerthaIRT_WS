import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

public class User {
    String awsUserPool = "us-east-1_4mPbywTgw";
    public String sub; //UDID
    public String username;
    public String groupID;
    public boolean isAdmin;
    public long lastUpdated;
    Cipher encrypter;
    Cipher decrypter;
    String rsaEncryptedAESKey;
    String rsaEncryptedIvParams;

    public User(String sub, String username, String groupID, boolean isAdmin){
        lastUpdated = System.currentTimeMillis();
        try{
            Cipher rsaEncrypter = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            List<AttributeType> attribs = WSMain.idp.adminGetUser(new AdminGetUserRequest().withUsername(username).withUserPoolId(awsUserPool)).getUserAttributes();
            for(AttributeType a : attribs)
                if (a.getName().equals("custom:rsaPublicKey")){
                    byte[] bytePublicKey = Util.fromHexString(a.getValue());
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    RSAPublicKey rsaPublicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(bytePublicKey));
                    rsaEncrypter.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
                }

            encrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decrypter = Cipher.getInstance("AES/CBC/PKCS5Padding");

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256);
            SecretKey aesKey = keygen.generateKey();

            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] ivParams = new byte[encrypter.getBlockSize()];
            sr.nextBytes(ivParams);
            IvParameterSpec initializationVectors = new IvParameterSpec(ivParams);

            encrypter.init(Cipher.ENCRYPT_MODE, aesKey, initializationVectors);
            decrypter.init(Cipher.DECRYPT_MODE, aesKey, initializationVectors);

            rsaEncryptedAESKey = Util.asHex(rsaEncrypter.doFinal(aesKey.getEncoded()));
            rsaEncryptedIvParams = Util.asHex(rsaEncrypter.doFinal(initializationVectors.getIV()));
        }catch (Exception e){e.printStackTrace();}
        this.sub = sub;
        this.username = username;
        this.groupID = groupID;
        this.isAdmin = isAdmin;
    }

    public RSAPublicKey getRSAPublicKey() throws InvalidKeySpecException, NoSuchAlgorithmException {

        return null;
    }
}
