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

import javax.crypto.SecretKey;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

public class AuthenticationManager {
    UrlJwkProvider provider;
    Map<String, SecretKey> aesKeyMap;
    AWSCredentialsProvider acp;
    public AuthenticationManager() {
        try {
            provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_1abyUmkI0/.well-known/jwks.json"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        AWSCredentials creds = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return "AKIAJ6S3TAMGW55HZ3ZQ";
            }

            @Override
            public String getAWSSecretKey() {
                return "AAlyEqh68aM1taBWpEl+NGxbml+sWRUeC5WLac8q";
            }
        };
        acp = new AWSStaticCredentialsProvider(creds);
    }

    public DecodedJWT verifyJWT(String encoded){
        DecodedJWT decoded = JWT.decode(new String(Base64.getUrlDecoder().decode(encoded)));
        Jwk jwk = null;
        try {
            jwk = provider.get(decoded.getKeyId());
            Algorithm al = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier j = JWT.require(al).acceptLeeway(3).build();
            return j.verify(decoded.getToken());
        } catch (JwkException e) {
            e.printStackTrace();
        }
        return null;
    }
}
