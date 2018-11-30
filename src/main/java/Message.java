import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

//Message class used for messaging, alerts and logs.
@DynamoDBDocument
public class Message {

    private Integer messageID = 0; //to keep track of who has read it
    private Long messageTimestamp = 0L;
    private Integer reportID = 0; //used only for Alerts to link to relevant report
    private String messageSubject = "";
    private String messageBody = "";

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

    @DynamoDBAttribute(attributeName = "messageID")
    public Integer getMessageID() { return messageID; }

    public void setMessageID(Integer messageID) { this.messageID = messageID; }

    @DynamoDBAttribute(attributeName = "messageTimestamp")
    public Long getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(Long messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }

    @DynamoDBAttribute(attributeName = "reportID")
    public Integer getReportID() {
        return reportID;
    }

    public void setReportID(Integer reportID) {
        this.reportID = reportID;
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
}

