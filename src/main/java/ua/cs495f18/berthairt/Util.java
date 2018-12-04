package ua.cs495f18.berthairt;

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

    public static List<List<String>> listSubtraction(List<String> removedFrom, List<String> addedTo){
        List<String> removed = new ArrayList<>(removedFrom);
        removed.removeIf((s)->!addedTo.contains(s));
        List<String> added = new ArrayList<>(addedTo);
        removed.removeIf((s)->!removedFrom.contains(s));
        List<List<String>> returnList = new ArrayList<>();
        returnList.add(removed);
        returnList.add(added);
        return returnList;
    }
}
