import com.auth0.jwt.interfaces.DecodedJWT;

class Log {
    public long timestamp;
    public String text;
    public String sender;

    public Log(String text, DecodedJWT jwt){
        this.timestamp = System.currentTimeMillis();
        this.text = text;
        this.sender = jwt.getClaim("name").asString();
    }
}