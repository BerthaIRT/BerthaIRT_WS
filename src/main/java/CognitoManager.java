import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminAddUserToGroupRequest;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.DeliveryMediumType;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.google.gson.JsonObject;
import io.javalin.Context;
import org.w3c.dom.Attr;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CognitoManager {
    final static String awsClientId = "6pnbv0ne1hdvmfgs6q2jkkeskf";
    final static String awsUserPool = "us-east-1_1abyUmkI0";

    AWSCognitoIdentityProvider idp;
    public CognitoManager(AWSCredentialsProvider acp){
        idp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(acp).withRegion(Regions.US_EAST_1).build();
    }

    public String newUser(String groupCode, String newUsername, boolean isAdmin){
        List<AttributeType> attribs = new ArrayList<>();
        attribs.add(new AttributeType().withName("custom:groupID").withValue(groupCode));
        attribs.add(new AttributeType().withName("custom:rsaPublicKey").withValue("none"));
        String group;
        if(isAdmin){
            group = "Administrators";
            attribs.add(new AttributeType().withName("email_verified").withValue("true"));
            attribs.add(new AttributeType().withName("email").withValue(newUsername));
        }
        else group = "Students";
        String newPassword = String.format("%12d", new SecureRandom().nextLong());
        AdminCreateUserRequest req = new AdminCreateUserRequest()
                .withUserPoolId(awsUserPool)
                .withUsername(newUsername)
                .withTemporaryPassword(newPassword)
                .withUserAttributes(attribs);
        if(isAdmin) req=req.withDesiredDeliveryMediums(DeliveryMediumType.EMAIL);
        idp.adminCreateUser(req);
        idp.adminAddUserToGroup(new AdminAddUserToGroupRequest()
                .withUserPoolId(awsUserPool)
                .withUsername(newUsername)
                .withGroupName(group)
            );
        JsonObject jay = new JsonObject();
        jay.addProperty("username", newUsername);
        jay.addProperty("password", newPassword);
        return jay.toString();
    }
}
