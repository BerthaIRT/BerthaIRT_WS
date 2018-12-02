import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import javax.crypto.Cipher;
import java.util.ArrayList;
import java.util.List;

@DynamoDBTable(tableName = "users")
public class User {

    private boolean isAdmin;
    private String username;
    private Integer groupID;

    private String fcmToken;

    private List<String> alerts;

    private String sub;
    private Cipher encrypter;
    private Cipher decrypter;

    public User(){

    }

    public User(String username, String sub, Integer groupID, String fcmToken, boolean isAdmin){
        this.username = username;
        this.sub = sub;
        this.groupID = groupID;
        this.fcmToken = fcmToken;
        this.alerts = new ArrayList<>();
        this.isAdmin = isAdmin;
    }

    @DynamoDBHashKey(attributeName="username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDBRangeKey(attributeName = "groupID")
    public Integer getGroupID() {
        return groupID;
    }

    public void setGroupID(Integer groupID) {
        this.groupID = groupID;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public List<String> getAlerts() { return alerts; }

    public void setAlerts(List<String> alerts) { this.alerts = alerts; }

    @DynamoDBIgnore
    public String getSub() { return sub; }

    public void setSub(String sub) {
        this.sub = sub;
    }

    @DynamoDBIgnore
    public Cipher getEncrypter() {
        return encrypter;
    }

    public void setEncrypter(Cipher encrypter) {
        this.encrypter = encrypter;
    }

    @DynamoDBIgnore
    public Cipher getDecrypter() {
        return decrypter;
    }

    public void setDecrypter(Cipher decrypter) {
        this.decrypter = decrypter;
    }

    @DynamoDBIgnore
    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
