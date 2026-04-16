import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Authenticator {
    private Map<String, User> users = new HashMap<>();
    private Map<String, User> sessions = new HashMap<>();

    public Authenticator() {
        registerUser("admin", "admin123", Role.ADMIN);
        registerUser("member", "member123", Role.MEMBER);
    }

    public void registerUser(String username, String password, Role role) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        users.put(username, new User(username, hash, salt, role));
    }

    public String login(String username, String password) {
        User user = users.get(username);
        if (user != null) {
            String hash = hashPassword(password, user.getSalt());
            if (hash.equals(user.getPasswordHash())) {
                String sessionId = UUID.randomUUID().toString();
                sessions.put(sessionId, user);
                return sessionId;
            }
        }
        return null; // Login failed
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = users.get(username);
        if (user != null) {
            String checkHash = hashPassword(oldPassword, user.getSalt());
            if (checkHash.equals(user.getPasswordHash())) {
                String newHash = hashPassword(newPassword, user.getSalt());
                user.setPasswordHash(newHash);
                return true;
            }
        }
        return false;
    }

    public void logout(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public User getUserBySession(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }
    
    public int getActiveSessionCount() { return sessions.size(); }
    public int getTotalUsers() { return users.size(); }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
