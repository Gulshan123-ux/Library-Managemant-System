import java.util.Date;
import java.util.UUID;

public class AuditLog {
    private String id;
    private Date timestamp;
    private String username;
    private String action;
    private String details;

    public AuditLog(String username, String action, String details) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
        this.username = username != null ? username : "System";
        this.action = action;
        this.details = details;
    }

    public String getId() { return id; }
    public Date getTimestamp() { return timestamp; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}
