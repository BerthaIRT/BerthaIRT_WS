package ua.cs495f18.berthairt;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBDocument
public class Message {

    private Long messageTimestamp = 0L;
    private String messageSubject = "";
    private String messageBody = "";
    private Integer reportID = 0;

    public Message(){}

    public Message(User u, String body){
        messageTimestamp = System.currentTimeMillis();
        messageSubject = u.getUsername();
        messageBody = body;
    }
    public Message(User u, String body, Integer reportID){
        messageTimestamp = System.currentTimeMillis();
        messageSubject = u.getUsername();
        messageBody = body;
        this.reportID = reportID;
    }

    @DynamoDBAttribute(attributeName = "messageTimestamp")
    public Long getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(Long messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }

    @DynamoDBAttribute(attributeName = "messageSubject")
    public String getMessageSubject() {
        return messageSubject;
    }

    public void setMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
    }

    @DynamoDBAttribute(attributeName = "messageBody")
    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    @DynamoDBAttribute(attributeName = "reportID")
    public Integer getReportID() {
        return reportID;
    }

    public void setReportID(Integer reportID) {
        this.reportID = reportID;
    }
}

