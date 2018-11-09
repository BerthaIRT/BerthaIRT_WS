import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Report {
    public String reportId;
    public String creationTimestamp;
    public String lastActionTimestamp;
    public String incidentTimeStamp;
    public String status;
    public String location;
    public String threatLevel;
    public String description;
    public String notes;
    public String media;
    public List<String> assignedTo;
    public List<String> tags;
    public List<String> categories;
    public List<Message> messages;
    public List<Log> logs;
}

class Message {
    public String text; // message body
    public String senderId; // data of the user that sent this message
    public String receiverId;
    public String date;
    public String time;
    public boolean sendingError;
    public boolean lastSent;
}

class Log {
    public String text;
    public String oldItem;
    public String newItem;
    public String timestamp;
    public String admin;
}