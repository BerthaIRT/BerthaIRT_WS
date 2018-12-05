package ua.cs495f18.berthairt;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FireMessage {
    public static String SERVER_KEY;
    private final String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
    
    private JsonObject payload;
    private List<String> recipients;
    private User sender;
    private String cardMessage;

    enum MessageType{
        REFRESH,
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
        sender = u;
    }

    public FireMessage withType(MessageType type, Report r, Group g){
        withTitle("REFRESH");
        withRecipients(g.getAdminList()).withRecipient(r.getStudentID());
        withReportID(r.getReportID().toString());
        send();

        recipients = new ArrayList<String>();
        withTitle("BerthaIRT");

        //make click action Admin by default
        withClickAction("ADMIN_REPORT");

        switch (type){
            case NEW_REPORT:
                withBody("A new report has been submitted");
                withCardMessage("Report Submitted");
                withRecipients(g.getAdminList());
                break;
            case REPORT_OPENED:
                withBody("A report has been opened");
                withCardMessage("Report Opened");
                withRecipients(g.getAdminList());
                break;
            case ASSIGNED_REMOVED:
                withBody("Assignees have been updated");
                withCardMessage("Updated Assignees");
                break;
            case ASSIGNED_TO_ME:
                withBody("You have been assigned to a report");
                withCardMessage("Assigned to Me");
                break;
            case REPORT_MESSAGE:
                withCardMessage("Message");
                if(sender.isAdmin()){
                    //change the click action to student report
                    withClickAction("STUDENT_REPORT");
                    withBody("An administrator has sent you a message!");
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
                }
                //withExtras("messages");
                break;

            case REPORT_EDITED:
                withCardMessage("New Update");
                if(r.getAssignedTo().size() == 0) {
                    withRecipients(g.getAdminList());
                    withBody("An open report has been updated.");
                }
                else{
                    withRecipients(r.getAssignedTo());
                    withBody("A report you are assigned to has been updated");
                }
                break;

            case REPORT_NOTES:
                withCardMessage("New Notes");
                if(r.getAssignedTo().size() == 0) {
                    withRecipients(g.getAdminList());
                    withBody("An administrator left a note on an open report");
                }
                else{
                    withRecipients(r.getAssignedTo());
                    withBody("An administrator left a note on a report you are assigned to");
                }
        }
        return this;
    }

    public FireMessage withTitle(String title){ payload.addProperty("title", title); return this;}
    public FireMessage withBody(String body){ payload.addProperty("body", body); return this;}
    public FireMessage withClickAction(String clickAction){ payload.addProperty("clickAction", clickAction); return this;}
    public FireMessage withReportID(String reportID){ payload.addProperty("reportID", reportID); return this;}
    public FireMessage withExtras(String extras){ payload.addProperty("extras",  extras); return this;}
    public FireMessage withRecipients(List<String> recipients){
        this.recipients = new ArrayList<>(recipients);
        return this;
    }
    public FireMessage withRecipient(String recipient){recipients.add(recipient); return this;}
    public FireMessage withCardMessage(String cardMessage){this.cardMessage=cardMessage; return this;}

    public void send(){
        System.out.println("[FCM] Recipients: " + recipients);
        if(recipients.size() == 0){
            System.out.println("[FCM] Error: no recipients specified for message!");
            return;
        }
        JsonArray tokens = new JsonArray();
        for(String s : recipients){
            User rec = WSMain.userMap.get(s);
            if(cardMessage != null) {
                rec.getAlerts().add(new Message(sender, cardMessage, payload.get("reportID").getAsInt()));
                WSMain.db.save(rec);
            }
            tokens.add(rec.getFcmToken());
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

            System.out.println(SERVER_KEY);
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
            System.out.println("Message delivered to " + result.get("success").getAsString() + " devices with " + result.get("failure").getAsString() + " failures.");
        }catch (Exception e){e.printStackTrace();}
    }
}
