package ua.cs495f18.berthairt;

import java.io.File;
import java.io.FileOutputStream;

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
}
