import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Report {
    public String reportId = "";
    public long creationTimestamp = new Long(0);
    public long lastActionTimestamp = new Long(0);
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

    public class Message {
        public String text; // message body
        private String senderId; // data of the user that sent this message
        public String date;
        public String time;
        public boolean sendingError;
        public boolean lastSent;
    }

    public class Log {
        public long timestamp;
        public String text;
        public String sender;
    }
}