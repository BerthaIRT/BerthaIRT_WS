public interface SecureRequestHandler {
    public String withDecryptedRequest(User u, String body);
}
