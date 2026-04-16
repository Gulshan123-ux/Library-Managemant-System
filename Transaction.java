import java.util.Date;
import java.util.UUID;

public class Transaction {
    private String id;
    private String isbn;
    private String username;
    private Date issueDate;
    private Date dueDate;
    private Date returnDate;

    public Transaction(String isbn, String username) {
        this.id = UUID.randomUUID().toString();
        this.isbn = isbn;
        this.username = username;
        this.issueDate = new Date();
        // Due 14 days later
        this.dueDate = new Date(this.issueDate.getTime() + (14L * 24 * 60 * 60 * 1000));
    }

    public void markReturned() {
        this.returnDate = new Date();
    }

    public String getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getUsername() { return username; }
    public Date getIssueDate() { return issueDate; }
    public Date getDueDate() { return dueDate; }
    public Date getReturnDate() { return returnDate; }
    public boolean isReturned() { return returnDate != null; }
    public boolean isOverdue() {
        if(isReturned()) return returnDate.after(dueDate);
        return new Date().after(dueDate);
    }
}
