import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

@DynamoDBDocument
public class Log {
    public long tStamp;
    public String logText;
    public String sender;

    public Log(){}

    public Log(User u, String logText){
        this.tStamp = System.currentTimeMillis();
        this.logText = logText;
        if(u.isAdmin)
            this.sender = u.username;
        else this.sender = "Student";
    }

    public long gettStamp() {
        return tStamp;
    }

    public void settStamp(long tStamp) {
        this.tStamp = tStamp;
    }

    public String getLogText() {
        return logText;
    }

    public void setLogText(String logText) {
        this.logText = logText;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}