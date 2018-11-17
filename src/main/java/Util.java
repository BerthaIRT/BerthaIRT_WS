
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Util {
    //Used by BerthaNet to serialize base-64 encoded keys
    public static String asHex(byte buf[]) {
        StringBuilder strbuf = new StringBuilder(buf.length * 2);
        for (byte aBuf : buf) {
            if (((int) aBuf & 0xff) < 0x10) {
                strbuf.append("0");
            }
            strbuf.append(Long.toString((int) aBuf & 0xff, 16));
        }
        return strbuf.toString();
    }

    public static byte[] fromHexString(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String findRemoved(List<String> o, List<String> n){
        String r = "";
        for (String s : o) {
            if (!n.contains(s))
                r = r.concat(o + ", ");
        }

        if(r.equals("")) return null;
        else return r.substring(0, r.length()-2);
    }

    public static String findAdded(List<String> o, List<String> n){
        String a = "";
        for (String s : n) {
            if (!o.contains(s))
                a = a.concat(n + ", ");
        }

        if(a.equals("")) return null;
        else return a.substring(0, a.length()-2);
    }

    public static List<String> addedAdmins(List<String> o, List<String> n){
        List<String> a = new ArrayList<>();
        for (String s : n) {
            if (!o.contains(s))
                a.add(s);
        }
        return a;
    }

    public static String getPrettyField(String s){
        switch (s){
            case "status": return "Status";
            case "assignedTo": return "Assignees";
            case "tags": return "Tags";
            case "categories": return "Categories";
        }
        return null;
    }


}
