import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

//Main group class.  Holds information relevant for all users/admins in a specific group.
@DynamoDBTable(tableName="groups")
public class Group {
    private Integer groupID = 0;
    private String groupName = "";

    //May be "Open" or "Closed"
    private String groupStatus = "";

    private List<String> adminList = new ArrayList<>();
    private List<String> studentList = new ArrayList<>();

    private Integer reportCount = 0;

    public Group(){}

    @DynamoDBHashKey
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

    public List<String> getAdminList() {
        return adminList;
    }

    public void setAdminList(List<String> adminList) {
        this.adminList = adminList;
    }

    public List<String> getStudentList() {
        return studentList;
    }

    public void setStudentList(List<String> studentList) {
        this.studentList = studentList;
    }
    public Integer getReportCount() {
        return reportCount;
    }

    public void setReportCount(Integer reportCount) {
        this.reportCount = reportCount;
    }
}