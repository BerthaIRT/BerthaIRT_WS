import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBDocument
public class Message {

    private Long messageTimestamp = 0L;
    private String messageSubject = "";
    private String messageBody = "";

    public Message(){}

    public Message(User u, String body){
        messageTimestamp = System.currentTimeMillis();
        messageSubject = u.getUsername();
        messageBody = body;
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
}

