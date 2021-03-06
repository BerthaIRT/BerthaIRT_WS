package ua.cs495f18.berthairt;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class UserFileManager extends WSMain {

    static String resourcePath = new File(WSMain.class.getClassLoader().getResource("userfiles").getFile()).getAbsolutePath();

    public static String downloadEmblem(Integer groupID, String data) {
        byte[] imgData = Util.fromHexString(data);
        try {
            FileOutputStream stream = new FileOutputStream(resourcePath + "/emblem/" + groupID.toString() + ".png");
            stream.write(imgData);
            return "OK";
        } catch(Exception e ){e.printStackTrace();}
        return null;
    }

    public static String downloadMedia(User u, String body) {
        JsonObject jay = jp.parse(body).getAsJsonObject();
        Integer reportID = Integer.valueOf(jay.get("reportID").getAsString());
        Report r = reportMap.get(u.getGroupID()).get(reportID);
        //r.setMediaCount(r.getMediaCount()+1);
        db.save(r);
        byte[] imgData = Util.fromHexString(jay.get("image").getAsString());
        String path = resourcePath + "/media/" + u.getGroupID() + "/" + reportID + "/";
        boolean success = (new File(path)).mkdirs();
        if(!success)
            System.err.println("Didn't make img dir");
        try {
            FileOutputStream stream = new FileOutputStream(path + r.getMediaCount() + ".png");
            stream.write(imgData);
        } catch(Exception e ){e.printStackTrace();}
        return "OK";
    }
}
