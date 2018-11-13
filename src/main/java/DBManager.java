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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
                .withInt("base_reportID", 1000)
                .withStringSet("admins", ""));

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

    public static void registerNewAdmin(String groupID, String adminName) {
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        Set<String> i = t.getItem(s).getStringSet("admins");
        i.add(adminName);
        t.updateItem(new UpdateItemSpec().withPrimaryKey("id", groupID)
                .withUpdateExpression("set admins = :ss")
                .withValueMap(new ValueMap().withStringSet(":ss", i)));
    }

    public static void getAdmins(Context ctx) {
        DecodedJWT jwt = AuthenticationManager.verifyJWT(ctx);
        if(!AuthenticationManager.verifyAdminAccess(jwt)) ctx.status(401);
        String groupID = jwt.getClaim("custom:groupID").asString();

        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        JsonArray response = new JsonArray();
        for(String i : t.getItem(s).getStringSet("admins"))
            response.add(i);

        ctx.result(AuthenticationManager.encryptResponse(jwt, response.toString()));
    }

    public static void removeAdmin(String groupID, String adminName) {
        Table t = db.getTable("group");
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        Set<String> i = t.getItem(s).getStringSet("admins");
        i.removeIf(v -> v.equals(adminName));
        t.updateItem(new UpdateItemSpec().withPrimaryKey("id", groupID)
                .withUpdateExpression("set admins = :ss")
                .withValueMap(new ValueMap().withStringSet(":ss", i)));
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

    public static void updateReport(Context ctx){
        DecodedJWT jwt = AuthenticationManager.verifyJWT(ctx);
        String groupID = jwt.getClaim("custom:groupID").asString();
        Report rNew = WSMain.gson.fromJson(AuthenticationManager.decryptRequest(jwt, ctx.body()), Report.class);

        Table t = db.getTable("reports-" + groupID);
        GetItemSpec s = new GetItemSpec().withPrimaryKey("id", groupID);
        Item i = t.getItem(s);
        Report rOld = WSMain.gson.fromJson(i.getString("data"), Report.class);

        if(rNew.status != rOld.status) rNew.logs.add(new Log("Changed status from " + rOld.status + " to " + rNew.status + ".", jwt));
        if(rNew.assignedTo != rOld.assignedTo){
            List<String> removed = new ArrayList<>(rOld.assignedTo);
            List<String> added = new ArrayList<>(rNew.assignedTo);
            for(String z : removed)
                if(rNew.assignedTo.contains(z)) removed.remove(z);

            for(String z : added)
                if(rOld.assignedTo.contains(z)) added.remove(z);

            if(added.size() > 0) rNew.logs.add(new Log("Added assignee: " + added + ".", jwt));
            if(removed.size() > 0) rNew.logs.add(new Log("Removed assignee: " + removed + ".", jwt));
        }

        if(rNew.tags != rOld.tags){
            List<String> removed = new ArrayList<>(rOld.tags);
            List<String> added = new ArrayList<>(rNew.tags);
            for(String z : removed)
                if(rNew.tags.contains(z)) removed.remove(z);

            for(String z : added)
                if(rOld.tags.contains(z)) added.remove(z);

            if(added.size() > 0) rNew.logs.add(new Log("Added tag: " + added + ".", jwt));
            if(removed.size() > 0) rNew.logs.add(new Log("Removed tag: " + removed + ".", jwt));
        }

        if(rNew.categories != rOld.categories){
            List<String> removed = new ArrayList<>(rOld.categories);
            List<String> added = new ArrayList<>(rNew.categories);
            for(String z : removed)
                if(rNew.categories.contains(z)) removed.remove(z);

            for(String z : added)
                if(rOld.categories.contains(z)) added.remove(z);

            if(added.size() > 0) rNew.logs.add(new Log("Added category: " + added + ".", jwt));
            if(removed.size() > 0) rNew.logs.add(new Log("Removed category: " + removed + ".", jwt));
        }

        if(rNew.notes != rOld.notes)
            rNew.logs.add(new Log("Added note " + ((Integer) (rNew.notes.size()+1)).toString() + ".", jwt));

        String jay = WSMain.gson.toJson(rNew);
        t.updateItem(new UpdateItemSpec().withPrimaryKey("id", groupID)
                .withUpdateExpression("set data = :s")
                .withValueMap(new ValueMap().withString(":s", jay)));
        ctx.result(AuthenticationManager.encryptResponse(jwt, jay));
    }
}
