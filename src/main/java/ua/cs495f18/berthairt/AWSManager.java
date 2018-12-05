package ua.cs495f18.berthairt;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import io.javalin.Context;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class AWSManager{
    static String awsUserPool = "us-east-1_4mPbywTgw";
    static AWSCognitoIdentityProvider idp;
    static DynamoDBMapper db;

    public static void log(Object o, String t){
        String s = "";
        if(!(o instanceof String)) s = WSMain.gson.toJson(o);
        else s = (String) o;
        if(t == null) t = "";
        System.out.println("[" + t + "] " + s);
    }

    public static void initAWSCrap(){
        AWSCredentials creds = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return System.getenv("BERTHA_AWS_KEY");
            }

            @Override
            public String getAWSSecretKey() {
                return System.getenv("BERTHA_AWS_SECRET");
            }
        };

        AWSStaticCredentialsProvider acp = new AWSStaticCredentialsProvider(creds);

        idp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(acp).withRegion(Regions.US_EAST_1).build();

        AmazonDynamoDBClientBuilder bdb = AmazonDynamoDBClientBuilder.standard();
        bdb.withRegion(Regions.US_EAST_1);
        bdb.withCredentials(acp);
        db = new DynamoDBMapper(bdb.build());
    }

    public static String createNewCognitoUser(Integer groupID, String username, boolean isAdmin){
        List<AttributeType> attribs = new ArrayList<>();
        attribs.add(new AttributeType().withName("custom:groupID").withValue(groupID.toString()));
        attribs.add(new AttributeType().withName("custom:rsaPublicKey").withValue("none"));

        String group = "Students";
        String newPassword = "BeRThAfirsttimestudent";

        if(isAdmin){
            group = "Administrators";
            attribs.add(new AttributeType().withName("email_verified").withValue("true"));
            attribs.add(new AttributeType().withName("email").withValue(username));
            newPassword = String.format("%09d", new SecureRandom().nextInt(1000000000));
        }

        AdminCreateUserRequest req = new AdminCreateUserRequest()
                .withUserPoolId(awsUserPool)
                .withUsername(username)
                .withTemporaryPassword(newPassword)
                .withUserAttributes(attribs);
        if(isAdmin) req=req.withDesiredDeliveryMediums(DeliveryMediumType.EMAIL);
        try{
            idp.adminCreateUser(req);
        }
        catch(AWSCognitoIdentityProviderException e){
            e.printStackTrace();
            return("EXISTING USER");
        }

        idp.adminAddUserToGroup(new AdminAddUserToGroupRequest()
                .withUserPoolId(awsUserPool)
                .withUsername(username)
                .withGroupName(group)
        );

        return username;
    }

    public static void deleteUser(String u){
        idp.adminDeleteUser(new AdminDeleteUserRequest().withUserPoolId(awsUserPool).withUsername(u));
    }

}
