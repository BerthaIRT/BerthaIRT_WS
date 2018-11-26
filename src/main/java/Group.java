import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

//Main group class.  Holds information relevant for all users/admins in a specific group.
@DynamoDBTable(tableName="group")
public class Group {

    private Integer groupID = 0;
    private String groupName = "";

    //May be "Open" or "Closed"
    private String groupStatus = "";

    //A map of all report IDs owned by this group and when it was last updated
    //Used for refreshing
    private Map<Integer, Long> groupReports = new HashMap<>();

    //A map where the key is the admin username/email
    //The value is a list of the alertIDs this admin has not yet dismissed
    private Map<String, List<Integer>> groupAdmins = new HashMap<>();

    //Same as admin map, but instead of alertIDs this list contains groupReports created by the student
    private Map<String, List<Integer>> groupStudents = new HashMap<>();

    //List of this group's alerts
    private Map<Integer, Message> groupAlerts = new HashMap<>();

    public Group(){
    }

    @DynamoDBHashKey(attributeName="groupID")
    public Integer getGroupID() {
        return groupID;
    }

    public void setGroupID(Integer groupID) {
        this.groupID = groupID;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupStatus() {
        return groupStatus;
    }

    public void setGroupStatus(String groupStatus) {
        this.groupStatus = groupStatus;
    }

    @DynamoDBAttribute(attributeName = "groupReports")
    public String getDBGroupReports(){ return WSMain.gson.toJson(this.groupReports); }
    public void setDBGroupReports(String groupReports){
        Type typeOfHashMap = new TypeToken<Map<Integer, Long>>() { }.getType();
        this.groupReports = WSMain.gson.fromJson(groupReports, typeOfHashMap); 
    }

    @DynamoDBIgnore
    public Map<Integer, Long> getGroupReports() {
        return this.groupReports;
    }

    @DynamoDBIgnore
    public void setGroupReports(Map<Integer, Long> groupReports) {
        this.groupReports = groupReports;
    }

    @DynamoDBAttribute(attributeName = "groupStudents")
    public String getDBGroupStudents(){ return WSMain.gson.toJson(this.groupStudents); }
    public void setDBGroupStudents(String groupStudents){
        Type typeOfHashMap = new TypeToken<Map<String, List<Integer>>>() { }.getType();
        this.groupStudents = WSMain.gson.fromJson(groupStudents, typeOfHashMap); 
    }

    @DynamoDBIgnore
    public Map<String, List<Integer>> getGroupStudents() {
        return this.groupStudents;
    }

    @DynamoDBIgnore
    public void setGroupStudents(Map<String, List<Integer>> groupStudents) {
        this.groupStudents = groupStudents;
    }

    @DynamoDBAttribute(attributeName = "groupAlerts")
    public String getDBGroupAlerts(){ return WSMain.gson.toJson(this.groupAlerts); }
    public void setDBGroupAlerts(String groupAlerts){
        Type typeOfHashMap = new TypeToken<Map<Integer, Message>>() { }.getType();
        this.groupAlerts = WSMain.gson.fromJson(groupAlerts, typeOfHashMap); 
    }

    @DynamoDBIgnore
    public Map<Integer, Message> getGroupAlerts() {
        return this.groupAlerts;
    }

    @DynamoDBIgnore
    public void setGroupAlerts(Map<Integer, Message> groupAlerts) {
        this.groupAlerts = groupAlerts;
    }

    @DynamoDBAttribute(attributeName = "groupAdmins")
    public String getDBGroupAdmins(){ return WSMain.gson.toJson(this.groupAdmins); }
    public void setDBGroupAdmins(String groupAdmins){
        Type typeOfHashMap = new TypeToken<Map<String, List<Integer>>>() { }.getType();
        this.groupAdmins = WSMain.gson.fromJson(groupAdmins, typeOfHashMap); 
    }

    @DynamoDBIgnore
    public Map<String, List<Integer>> getGroupAdmins() {
        return this.groupAdmins;
    }

    @DynamoDBIgnore
    public void setGroupAdmins(Map<String, List<Integer>> groupAdmins) {
        this.groupAdmins = groupAdmins;
    }

    public void notifyUpdate(Integer reportID){
        groupReports.remove(reportID);
        groupReports.put(reportID, System.currentTimeMillis());
    }

    public void addAlertForAll(Message a, String exceptAdmin){
        a.setMessageID(groupAlerts.size() + 1);
        groupAlerts.put(a.getMessageID(), a);
        for(Map.Entry<String, List<Integer>> e : groupAdmins.entrySet()){
            if(exceptAdmin != null && e.getKey().equals(exceptAdmin)) continue;
            e.getValue().add(a.getMessageID());
        }
    }

    public void addAlertForAdmins(Message a, List<String> admins){
        a.setMessageID(groupAlerts.size() + 1);
        groupAlerts.put(a.getMessageID(), a);
        for(String s : admins)
            groupAdmins.get(s).add(a.getMessageID());
    }

//    public void viewedAlert(String admin, Integer id){
//        groupAdmins.get(admin).remove(id);
//    }
}
