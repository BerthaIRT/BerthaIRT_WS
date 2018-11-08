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


}
