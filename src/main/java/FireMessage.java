import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FireMessage {
    private final String SERVER_KEY = System.getenv("BERTHA_FCM_KEY");
    private final String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
    
    private JsonObject payload;
    private List<String> recipients;
    private String sender;

    enum MessageType{
        //Messages which display a notification
        NEW_REPORT,
        REPORT_OPENED,
        REPORT_MESSAGE,
        ASSIGNED_REMOVED,
        ASSIGNED_TO_ME,
        REPORT_EDITED,
        REPORT_NOTES,
        NEW_ADMIN,
    }

//    {
//        "message":{
//            "token":"...",
//            "notification":{
//                "title": string,
//                "body": string,
//                "icon": string,
//                "color": string,
//                "sound": string,
//                "tag": string,
//                "click_action": string,
//                "body_loc_key": string,
//                "body_loc_args": [string],
//                "title_loc_key": string,
//                "title_loc_args": [string]
//            },
//            "data" : {
//                "yourKey" : "yourValue"
//            }
//        }
//    }

    public FireMessage(User u){
        recipients = new ArrayList<>();
        payload = new JsonObject();
        sender = u.getUsername();
    }

    public FireMessage withType(MessageType type, Report r, Group g){
        switch (type){
            case NEW_REPORT:
                return this.withBody("A new report has been submitted")
                        .withClickAction("ADMIN_REPORT")
                        .withReportID(r.getReportID().toString())
                        .withRecipients(g.getAdminList());

            case REPORT_OPENED:
                return this.withBody("A report has been opened")
                        .withClickAction("ADMIN_REPORT")
                        .withReportID(r.getReportID().toString())
                        .withRecipients(g.getAdminList());

            case ASSIGNED_REMOVED:
                return this.withBody("Assignees have been updated")
                        .withClickAction("ADMIN_REPORT")
                        .withReportID(r.getReportID().toString());

            case ASSIGNED_TO_ME:
                return this.withBody("You have been assigned to a report")
                        .withClickAction("ADMIN_REPORT")
                        .withReportID(r.getReportID().toString());

            case REPORT_MESSAGE:
                if(WSMain.userMap.get(sender).isAdmin()){
                    withBody("An administrator has sent you a message!");
                    withRecipient(r.getStudentID());
                    withClickAction("STUDENT_REPORT");
                }
                else{
                    if(r.getAssignedTo().size() == 0) {
                        withRecipients(g.getAdminList());
                        withBody("A open report has a new student message");
                    }
                    else{
                        withRecipients(r.getAssignedTo());
                        withBody("A report you are assigned to has a new student message");
                    }
                    withClickAction("ADMIN_REPORT");
                }
                return this.withReportID(r.getReportID().toString());

            case REPORT_EDITED:
                if(r.getAssignedTo().size() == 0) {
                    withRecipients(g.getAdminList());
                    withBody("An open report has been updated.");
                }
                else{
                    withRecipients(r.getAssignedTo());
                    withBody("A report you are assigned to has been updated");
                }
                return this.withClickAction("ADMIN_REPORT")
                        .withReportID(r.getReportID().toString());

            case REPORT_NOTES:
                if(r.getAssignedTo().size() == 0) {
                    withRecipients(g.getAdminList());
                    withBody("An administrator left a note on an open report");
                }
                else{
                    withRecipients(r.getAssignedTo());
                    withBody("An administrator left a note on a report you are assigned to");
                }
                return this.withClickAction("ADMIN_REPORT")
                        .withReportID(r.getReportID().toString());
        }
        return null;
    }

    public FireMessage withTitle(String title){ payload.addProperty("title", title); return this;}
    public FireMessage withBody(String body){ payload.addProperty("body", body); return this;}
    public FireMessage withClickAction(String clickAction){ payload.addProperty("clickAction", clickAction); return this;}
    public FireMessage withReportID(String reportID){ payload.addProperty("reportID", reportID); return this;}
    public FireMessage withExtras(String extras){ payload.addProperty("extras",  extras); return this;}
    public FireMessage withRecipients(List<String> recipients){
        this.recipients = recipients;
        this.recipients.removeIf((a)->a.equals(sender));
        return this;
    }
    public FireMessage withRecipient(String recipient){recipients.add(WSMain.userMap.get(recipient).getFcmToken()); return this;}

    public void send(){
        if(recipients.size() == 0){
            System.out.println("[FCM] Error: no recipients specified for message!");
            return;
        }
        JsonArray tokens = new JsonArray();
        for(String s : recipients){
            User u = WSMain.userMap.get(s);
            u.getAlerts().add(payload.toString());
            WSMain.db.save(u);
            tokens.add(u.getFcmToken());
        }
        try {
            JsonObject root = new JsonObject();

            root.add("registration_ids", tokens);
            root.add("data", payload);
            URL url = new URL(API_URL_FCM);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "key=" + SERVER_KEY);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(root.toString());
            wr.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder b = new StringBuilder();
            String line = br.readLine();
            while(line != null){
                b.append(line);
                line = br.readLine();
            }
            JsonObject result = new JsonParser().parse(b.toString()).getAsJsonObject();
            System.out.println("[FCM] Message delivered to " + result.get("success").getAsString() + " devices with " + result.get("failure").getAsString());
        }catch (Exception e){e.printStackTrace();}
    }
}
