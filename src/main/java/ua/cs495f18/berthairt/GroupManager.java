package ua.cs495f18.berthairt;

import com.amazonaws.services.cognitoidp.model.AdminDeleteUserRequest;
import com.google.gson.JsonObject;
import io.javalin.Context;

import java.util.List;
import java.util.Random;

public class GroupManager extends WSMain{

    static void sendBasicGroupInfo(Context ctx){
        Integer groupID = new Integer(ctx.body());
        Group g = groupMap.get(groupID);
        JsonObject jay = new JsonObject();
        if(g != null) {
            jay.addProperty("groupName", g.getGroupName());
            jay.addProperty("groupStatus", g.getGroupStatus());
            jay.addProperty("admins", gson.toJson(g.getAdminList()));
        }
        else jay.addProperty("groupName", "NONE");
        ctx.result(jay.toString());
    }

    static void createNewGroup(Context ctx){
        JsonObject jay = jp.parse(ctx.body()).getAsJsonObject();
        log(jay, null);
        String firstAdmin = jay.get("newAdmin").getAsString();
        Integer newGroupID = createGroupInDatabase(jay.get("groupName").getAsString(), null);
        addAdminToGroup(newGroupID, firstAdmin);
        ctx.result(createNewCognitoUser(newGroupID, firstAdmin, true));
    }

    static Integer createGroupInDatabase(String groupName, Integer newGroupID){
        if(newGroupID == null) newGroupID = new Random().nextInt(1000000);
        if(newGroupID < 100000 || db.load(Group.class, newGroupID) != null) return createGroupInDatabase(groupName, null);
        Group g = new Group();
        g.setGroupName(groupName);
        g.setGroupID(newGroupID);
        groupMap.put(g.getGroupID(), g);
        db.save(g);
        return newGroupID;
    }

    public static void addAdminToGroup(Integer groupID, String username){
        Group g = groupMap.get(groupID);
        g.getAdminList().add(username);
        db.save(g);
    }

    public static String addAdminToGroup(User u, String body){
        Group g = groupMap.get(u.getGroupID());
        g.getAdminList().add(body);
        createNewCognitoUser(u.getGroupID(), body, true);
        db.save(g);
        return "OK";
    }

    public static String removeAdminFromGroup(User u, String body){
        Group g = groupMap.get(u.getGroupID());
        g.getAdminList().remove(body);
        userMap.remove(body);
        deleteUser(body);
        db.save(g);
        return "OK";
    }

    public static void addStudentToGroup(Context ctx){
        Integer groupID = new Integer(ctx.body());
        Group g = groupMap.get(groupID);
        Integer newStudentID = 1000 + g.getStudentCount();
        String newStudentUsername = ("student-" + groupID + "-" + newStudentID);
        g.setStudentCount(newStudentID-999);
        db.save(g);
        ctx.result(createNewCognitoUser(groupID, newStudentUsername, false));
    }

    public static String toggleStatus(User u, String s) {
        Group g = groupMap.get(u.getGroupID());
        String status = "Open";
        if(g.getGroupStatus().equals("Open")) status = "Closed";
        g.setGroupStatus(status);
        db.save(g);
        return status;
    }

    public static String newInstitutionName(User u, String body) {
        Group g = groupMap.get(u.getGroupID());
        g.setGroupName(body);
        db.save(g);
        return body;
    }

    public static String dismissAlerts(User u, String body) {
        u.getAlerts().remove(Integer.valueOf(body));
        db.save(u);
        return body;
    }
}
