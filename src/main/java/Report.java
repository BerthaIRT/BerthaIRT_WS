import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;

import java.util.ArrayList;
import java.util.List;


@DynamoDBTable(tableName="report")
public class Report {
     Integer reportID = 0;
     Integer groupID = 0;
     String studentID = "";
     Long creationDate = 0L;
     Long incidentDate = 0L;
     Integer threat = 0;
     String status = "";
     String location = "";
     String description = "";
     List<String> categories;
     List<String> tags;
     List<String> assignedTo;
     List<Message> messages;
     List<Message> logs;
     List<Message> notes;

    public Report(){
        categories = new ArrayList<>();
        tags = new ArrayList<>();
        assignedTo = new ArrayList<>();
        messages = new ArrayList<>();
        logs = new ArrayList<>();
        notes = new ArrayList<>();
    }

    @DynamoDBHashKey(attributeName="reportID")
    public Integer getReportID() {
        return reportID;
    }

    public void setReportID(Integer reportID) {
        this.reportID = reportID;
    }

    @DynamoDBRangeKey(attributeName="groupID")
    public Integer getGroupID() {
        return groupID;
    }

    public void setGroupID(Integer groupID) {
        this.groupID = groupID;
    }

    public String getStudentID() { return studentID; }

    public void setStudentID(String studentID) { this.studentID = studentID; }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public Long getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(Long incidentDate) {
        this.incidentDate = incidentDate;
    }

    public Integer getThreat() {
        return threat;
    }

    public void setThreat(Integer threat) {
        this.threat = threat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(List<String> assignedTo) {
        this.assignedTo = assignedTo;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> getLogs() {
        return logs;
    }

    public void setLogs(List<Message> logs) {
        this.logs = logs;
    }

    public List<Message> getNotes() {
        return notes;
    }

    public void setNotes(List<Message> notes) {
        this.notes = notes;
    }

    public void addLog(Message l){
        l.setMessageID(logs.size() + 1);
        logs.add(l);
    }

    public void addMessage(Message m){
        m.setMessageID(messages.size() + 1);
        messages.add(m);
    }
}
