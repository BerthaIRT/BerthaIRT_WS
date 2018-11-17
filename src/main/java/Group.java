import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

@DynamoDBTable(tableName="group")
public class Group
{
    String groupID;
    String name;
    String status;
    int baseReportID;
    int baseStudentID;
    private String alerts;
    List<String> admins;

    public Group(){}

    public Group(String groupID, String name, String firstAdmin){
        this.groupID = groupID;
        this.name = name;
        status = "Open";
        baseReportID = 1000;
        baseStudentID = 1000;
        alerts = "[]";
        admins = new ArrayList<>();
        admins.add(firstAdmin);
    }

    @DynamoDBHashKey(attributeName="groupID")
    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDBIgnore
    public void addAlert(Alert a){
        JsonArray jay = WSMain.jp.parse(alerts).getAsJsonArray();
        jay.add(WSMain.gson.toJson(a, Alert.class));
        alerts = jay.toString();
    }

    public List<Alert> getAlerts() {
        JsonArray a = WSMain.jp.parse(alerts).getAsJsonArray();
        List<Alert> l = new ArrayList<>();
        for(JsonElement s : a)
            l.add(WSMain.gson.fromJson(s.getAsString(), Alert.class));
        return l;
    }

    public void setAlerts(List<Alert> alerts) {
        JsonArray arr = new JsonArray();
        for(Alert a : alerts){
            arr.add(WSMain.gson.toJson(a, Alert.class));
        }
        this.alerts = arr.toString();
    }

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(List<String> admins) {
        this.admins = admins;
    }

    public int getBaseReportID() {
        return baseReportID;
    }

    public void setBaseReportID(int baseReportID) {
        this.baseReportID = baseReportID;
    }

    public int getBaseStudentID() {
        return baseStudentID;
    }

    public void setBaseStudentID(int baseStudentID) {
        this.baseStudentID = baseStudentID;
    }
}
