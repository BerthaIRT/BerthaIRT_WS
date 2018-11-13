import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.List;

class Alert {
    public long timestamp;
    public String text;
    public String reportID;
    public List<String> unseenBy;

    public Alert(String reportID, String text){
        this.timestamp = System.currentTimeMillis();
        this.reportID = reportID;
        this.text = text;
    }
}