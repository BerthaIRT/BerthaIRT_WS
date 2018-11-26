public interface SecureRequestHandler {
    public String withDecryptedRequest(Client c, String body);
}
