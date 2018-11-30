import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import javax.crypto.Cipher;

@DynamoDBTable(tableName = "users")
public class User {
    private String username;
    private Integer groupID;

    private String fcmToken;
    private Long lastUpdated;
    private boolean isAdmin;

    private String sub;
    private Cipher encrypter;
    private Cipher decrypter;

    public User(String sub, String username, Integer groupID, String fcmToken, boolean isAdmin){
        this.sub = sub;
        this.username = username;
        this.groupID = groupID;
        this.fcmToken = fcmToken;
        this.isAdmin = isAdmin;
        this.lastUpdated = System.currentTimeMillis();
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

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    @DynamoDBIgnore
    public String getSub() {
        return sub;
    }

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
}
