package ua.cs495f18.berthairt;

import java.io.File;
import java.io.FileOutputStream;

public class UserFileManager extends WSMain {

    static String resourcePath = new File(WSMain.class.getClassLoader().getResource("userfiles").getFile()).getAbsolutePath();

    public static String downloadEmblem(User u, String body) {
        byte[] imgData = Util.fromHexString(body);
        try {
            FileOutputStream stream = new FileOutputStream(resourcePath + "/emblem/" + u.getGroupID() + ".png");
            stream.write(imgData);
            return "OK";
        } catch(Exception e ){e.printStackTrace();}
        return null;
    }
}
