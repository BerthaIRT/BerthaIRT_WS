import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.ArrayList;
import java.util.List;

public class Report {
    public String reportId = "";
    public long creationTimestamp = new Long(0);
    public long incidentTimeStamp = new Long(0);
    public String status = "";
    public String location = "";
    public String threatLevel = "";
    public String description = "";
    public String media = "";
    public List<String> assignedTo = new ArrayList<>();
    public List<String> tags = new ArrayList<>();
    public List<String> categories = new ArrayList<>();
    public List<Message> messages = new ArrayList<>();
    public List<Log> logs = new ArrayList<>();
    public List<Log> notes = new ArrayList<>();
}

class Message {
    public String text;
    public String sender;
    public long timestamp;
    public boolean sendingError;
    public boolean lastSent;
}

