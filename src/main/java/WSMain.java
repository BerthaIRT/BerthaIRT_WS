import io.javalin.Javalin;

public class WSMain{
    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create().start(80);
    }
}