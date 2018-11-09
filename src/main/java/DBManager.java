import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;

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
                .withString("status", "Open")
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

    public static String lookupGroupName(String groupID){
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        return t.getItem(s).getString("name");
    }

    public static String lookupGroupStatus(String groupID){
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        return t.getItem(s).getString("status");
    }

    public static String getNewStudentID(String groupID){
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        int baseID = t.getItem(s).getInt("base_studentID");

        t.updateItem(new UpdateItemSpec().withPrimaryKey("id", groupID)
                .withUpdateExpression("set base_studentID = :s")
                .withValueMap(new ValueMap().withInt(":s", baseID+1)));

        return "student-" + groupID + "-" + baseID;
    }
}
