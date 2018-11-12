import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import io.javalin.Context;

import java.util.Random;

public class DBManager {
    static DynamoDB db;

    public static void init(){
        AmazonDynamoDBClientBuilder bdb = AmazonDynamoDBClientBuilder.standard();
        bdb.withRegion(Regions.US_EAST_1);
        bdb.withCredentials(AuthenticationManager.acp);
        db = new DynamoDB(bdb.build());
    }

    public static String createGroup(String newGroupName){
        String newGroupCode = ((Integer) new Random().nextInt(1000000)).toString();

        Table t = db.getTable("group");
        t.putItem(new Item().withPrimaryKey("id", newGroupCode)
                .withString("name", newGroupName)
                .withString("groupstatus", "Open")
                .withInt("base_studentID", 1000)
                .withInt("base_reportID", 1000));

        CreateTableRequest req = new CreateTableRequest()
                .withTableName("reports-" + newGroupCode)
                .withKeySchema(new KeySchemaElement("id", KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("id").withAttributeType("S"))
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(5L)
                        .withWriteCapacityUnits(6L));
        db.createTable(req);
        return newGroupCode;
    }

    public static void getGroupDetails(Context ctx){
        JsonObject jay = new JsonObject();
        String groupID = ctx.body();
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        Item i = t.getItem(s);

        if(i == null){
            jay.addProperty("groupStatus", "NONE");
            ctx.result(jay.toString());
            return;
        }

        jay.addProperty("groupName", i.getString("name"));
        jay.addProperty("groupStatus", i.getString("groupstatus"));
        ctx.result(jay.toString());
    }

    public static void registerNewStudent(Context ctx){
        String groupID = ctx.body();
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        Integer baseID = t.getItem(s).getInt("base_studentID");

        t.updateItem(new UpdateItemSpec().withPrimaryKey("id", groupID)
                .withUpdateExpression("set base_studentID = :s")
                .withValueMap(new ValueMap().withInt(":s", baseID+1)));

        String newUsername = "student-"+groupID+"-"+baseID;
        CognitoManager.newUser(groupID, newUsername, false);
        ctx.result(newUsername);
    }

    public static String getNewReportID(String groupID){
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        Integer baseID = t.getItem(s).getInt("base_reportID");

        t.updateItem(new UpdateItemSpec().withPrimaryKey("id", groupID)
                .withUpdateExpression("set base_reportID = :s")
                .withValueMap(new ValueMap().withInt(":s", baseID+1)));

        return baseID.toString();
    }

    public static void storeNewReport(Report r, String groupID){
        Table t = db.getTable("reports-" + groupID);
        t.putItem(new Item().withPrimaryKey("id", r.reportId)
                .withString("data", WSMain.gson.toJson(r)));
    }

    public static void retrieveAll(Context ctx){
        DecodedJWT jwt = AuthenticationManager.verifyJWT(ctx);
        if(!AuthenticationManager.verifyAdminAccess(jwt)) ctx.status(401);

        Table t = db.getTable("reports-" + jwt.getClaim("custom:groupID").asString());
        ScanSpec q = new ScanSpec().withConsistentRead(true);
        JsonObject response = new JsonObject();
        for(Item i : t.scan(q))
            response.addProperty(i.getString("id"), i.getString("data"));

        ctx.result(AuthenticationManager.encryptResponse(jwt, response.toString()));
    }
}
