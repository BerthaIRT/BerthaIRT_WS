
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;

import java.util.List;

@DynamoDBDocument
public class Alert {
    public long tStamp;
    public String alertText;

    public String reportID;
    public List<String> unseenBy;
    public List<String> categories;

    public Alert(){}

    public Alert(Report r, String text){
        this.tStamp = System.currentTimeMillis();
        this.reportID = r.getReportID();
        this.categories = r.getCategories();
        this.alertText = text;
    }

    @DynamoDBIgnore
    public Alert addAdmins(List<String> admins){
        unseenBy = admins;
        return this;
    }

    public long gettStamp() {
        return tStamp;
    }

    public void settStamp(long tStamp) {
        this.tStamp = tStamp;
    }

    public String getAlertText() {
        return alertText;
    }

    public void setAlertText(String alertText) {
        this.alertText = alertText;
    }

    public String getReportID() {
        return reportID;
    }

    public void setReportID(String reportID) {
        this.reportID = reportID;
    }

    public List<String> getUnseenBy() {
        return unseenBy;
    }

    public void setUnseenBy(List<String> unseenBy) {
        this.unseenBy = unseenBy;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}