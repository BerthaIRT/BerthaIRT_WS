import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
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
    final static String awsClientId = "2kssdfqe3oirdasavb7og6bh76";
    final static String awsUserPool = "us-east-1_4mPbywTgw";

    static AWSCognitoIdentityProvider idp;

    public static void init(){
        idp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(AuthenticationManager.acp).withRegion(Regions.US_EAST_1).build();
    }

    public static String newUser(String groupID, String newUsername, boolean isAdmin){
        List<AttributeType> attribs = new ArrayList<>();
        attribs.add(new AttributeType().withName("custom:groupID").withValue(groupID));
        attribs.add(new AttributeType().withName("custom:rsaPublicKey").withValue("none"));
        String group;
        String newPassword = "BeRThAfirsttimestudent";
        if(isAdmin){
            group = "Administrators";
            attribs.add(new AttributeType().withName("email_verified").withValue("true"));
            attribs.add(new AttributeType().withName("email").withValue(newUsername));
            newPassword = String.format("%09d", new SecureRandom().nextInt(1000000000));
            DBManager.registerNewAdmin(groupID,newUsername);
        }
        else group = "Students";
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

    public static String getRSAPublicKey(String username){
        List<AttributeType> attribs = idp.adminGetUser(new AdminGetUserRequest().withUsername(username).withUserPoolId(awsUserPool)).getUserAttributes();
        for(AttributeType a : attribs)
            if (a.getName().equals("custom:rsaPublicKey"))
                return a.getValue();

        return null;
    }
}
