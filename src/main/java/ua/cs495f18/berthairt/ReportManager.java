package ua.cs495f18.berthairt;

import com.google.gson.JsonArray;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReportManager extends WSMain{

    static String sendSingle(User u, String reportID){
        Report r = reportMap.get(u.getGroupID()).get(new Integer(reportID) - 1000);
        String jay = gson.toJson(r);
        if(u.isAdmin()) return jay.replace(r.getStudentID(), "Hidden");
        else if(u.getUsername().equals(r.getStudentID())) return jay;
        else return null;
    }

    static String sendAll(User u, String body){
//        Map<String, AttributeValue> attribs = new HashMap<>();
//        attribs.put(":id", new AttributeValue().withN(u.getGroupID().toString()));
//        List<ua.cs495f18.berthairt.Report> query = db.scan(ua.cs495f18.berthairt.Report.class, new DynamoDBScanExpression().withFilterExpression("groupID = :id").withExpressionAttributeValues(attribs));

        JsonArray jarray = new JsonArray();
        List<Report> l = reportMap.get(u.getGroupID());
        if(l != null){
            for(Report r : l){
                String jay = gson.toJson(r);
                if(!u.isAdmin() && !u.getUsername().equals(r.getStudentID()))
                    continue;
                else if(u.isAdmin())
                    jay = jay.replace(r.getStudentID(), "Hidden");
                jarray.add(jay);
            }
        }
        return jarray.toString();
    }

    static String createNewReport(User u, String body){
        Report r = gson.fromJson(body, Report.class);
        r.setCreationDate(System.currentTimeMillis());
        r.setStudentID(u.getUsername());
        r.setGroupID(u.getGroupID());
        r.setStatus("New");

        Group g = groupMap.get(r.getGroupID());
        Integer i = g.getReportCount();
        g.setReportCount(i+1);
        r.setReportID(i + 1000);
        r.addLog(new Message(u, "Report created"));

        List<Report> groupReports = reportMap.get(r.getGroupID());
        if(groupReports == null){
            List<Report> newList = new ArrayList<>();
            newList.add(r);
            reportMap.put(r.getGroupID(), newList);
        }
        else groupReports.add(r);

        db.save(r);
        db.save(g);

        new FireMessage(u).withType(FireMessage.MessageType.NEW_REPORT, r, g).send();
        return gson.toJson(r);
    }

    static String updateReport(User u, String body){
        Report neww = gson.fromJson(body, Report.class);
        Report oldd = reportMap.get(neww.getGroupID()).get(neww.getReportID()-1000);
        Group g = groupMap.get(neww.getGroupID());

        if(neww.getStudentID().equals("Hidden")) neww.setStudentID(oldd.getStudentID());


        List<Field> changes = new ArrayList<>();
        try {
            for (Field f : Report.class.getDeclaredFields()) {
                if (!gson.toJson(f.get(neww)).equals(gson.toJson(f.get(oldd))))
                    changes.add(f);
            }
        }catch (Exception e){e.printStackTrace();}

        while(changes.size() > 0){
            Field changedField = changes.remove(0);

            FireMessage combineChangesMessage = null;
            switch (changedField.getName()) {
                case "status":
                    String status = neww.getStatus();
                    neww.addLog(new Message(u, "Status changed to: " + status));

                    if(status.equals("Open"))
                        new FireMessage(u).withType(FireMessage.MessageType.REPORT_OPENED, neww, g).send();

                    break;
                case "assignedTo":
                    List<List<String>> li = Util.listSubtraction(oldd.getAssignedTo(), neww.getAssignedTo());
                    List<String> unchangedAdmins = new ArrayList<>(neww.getAssignedTo());
                    unchangedAdmins.removeIf((a)->oldd.getAssignedTo().contains(a));
                    if (li.get(0).size() > 0){
                        neww.addLog(new Message(u, "Removed assignees " + li.get(0)));
                        new FireMessage(u).withType(FireMessage.MessageType.ASSIGNED_REMOVED, neww, g).withRecipients(li.get(0)).send();
                    }
                    if (li.get(1).size() > 0){
                        neww.addLog(new Message(u, "Added assignees " + li.get(1)));
                        new FireMessage(u).withType(FireMessage.MessageType.ASSIGNED_TO_ME, neww, g).withRecipients(li.get(1)).send();
                    }
                    combineChangesMessage = new FireMessage(u).withType(FireMessage.MessageType.REPORT_EDITED, neww, g);
                    break;
                case "categories":
                    li = Util.listSubtraction(oldd.getCategories(), neww.getCategories());
                    if (li.get(0).size() > 0) neww.addLog(new Message(u, "Removed categories " + li.get(0)));
                    if (li.get(1).size() > 0) neww.addLog(new Message(u, "Added categories " + li.get(1)));
                    combineChangesMessage = new FireMessage(u).withType(FireMessage.MessageType.REPORT_EDITED, neww, g);
                    break;
                case "tags":
                    li = Util.listSubtraction(oldd.getTags(), neww.getTags());
                    if (li.get(0).size() > 0) neww.addLog(new Message(u, "Removed tags " + li.get(0)));
                    if (li.get(1).size() > 0) neww.addLog(new Message(u, "Added tags " + li.get(1)));
                    combineChangesMessage = new FireMessage(u).withType(FireMessage.MessageType.REPORT_EDITED, neww, g);
                    break;
                case "notes":
                    Message m = neww.getNotes().get(neww.getNotes().size() - 1);
                    m.setMessageSubject(u.getUsername());
                    m.setMessageTimestamp(System.currentTimeMillis());
                    new FireMessage(u).withType(FireMessage.MessageType.REPORT_NOTES, neww, g).send();
                    break;
                case "messages":
                    if (neww.getMessages().size() != oldd.getMessages().size()) {
                        neww.addLog(new Message(u, "Sent new message"));
                        m = neww.getMessages().get(neww.getMessages().size() - 1);
                        m.setMessageSubject(u.getUsername());
                        m.setMessageTimestamp(System.currentTimeMillis());
                        new FireMessage(u).withType(FireMessage.MessageType.REPORT_MESSAGE, neww, g).send();
                    }
                    break;
            }
            if(combineChangesMessage != null) combineChangesMessage.send();
        }
        reportMap.get(neww.getGroupID()).add(neww);
        db.save(neww);
        return gson.toJson(neww, Report.class);
    }
}
