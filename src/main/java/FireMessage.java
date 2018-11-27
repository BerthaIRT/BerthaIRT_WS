import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class FireMessage {
    private final String SERVER_KEY = "XXX";
    private final String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
    private JSONObject root;

    public FireMessage(String title, String message) throws JSONException {
        root = new JSONObject();

        JSONObject notification = new JSONObject();
        notification.put("title", title);
        notification.put("body", message);
        root.put("notification", notification);

        JSONObject data = new JSONObject();
        data.put("title", title);
        data.put("body", message);
        root.put("data", data);
    }

    /**
     * Sends to multiple devices based off ID
     * @param tokens the IDs
     * @return
     * @throws Exception
     */
    public String sendToToken(List<String> tokens) throws Exception {
        root.put("registration_ids", new JSONArray(tokens));
        return sendPushNotification();
    }


    private String sendPushNotification() throws Exception {
        URL url = new URL(API_URL_FCM);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "key=" + SERVER_KEY);

        System.out.println("MESSAGE");
        System.out.println(root.toString());
        System.out.println("END MESSAGE");

        try {
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(root.toString());
            wr.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            StringBuilder builder = new StringBuilder();
            while ((output = br.readLine()) != null) {
                builder.append(output);
            }
            System.out.println(builder);
            String result = builder.toString();

            JSONObject obj = new JSONObject(result);

            int success = Integer.parseInt(obj.getString("success"));
            if (success > 0)
                return "SUCCESS";

            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }
}
