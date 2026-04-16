import java.util.Date;

public class User {
    private String username;
    private String passwordHash;
    private String salt;
    private Role role;
    private String membershipType;
    private Date membershipExpiry;

    public User(String username, String passwordHash, String salt, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        
        // Memberships default to Public, 1 Year
        this.membershipType = role == Role.ADMIN ? "Staff" : "Public";
        this.membershipExpiry = new Date(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000));
    }

    public String getUsername() { return username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public Role getRole() { return role; }
    public String getMembershipType() { return membershipType; }
    public Date getMembershipExpiry() { return membershipExpiry; }
}
