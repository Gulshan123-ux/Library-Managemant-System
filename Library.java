import java.util.ArrayList;
import java.util.List;

public class Library {
    private ArrayList<Book> books;
    private List<Transaction> transactions;
    private List<AuditLog> auditLogs;
    private Authenticator authenticator;

    public Library(Authenticator auth) {
        this.books = new ArrayList<>();
        this.transactions = new ArrayList<>();
        this.auditLogs = new ArrayList<>();
        this.authenticator = auth;
        logAction("System", "BOOT", "Library tracking structures initialized.");
    }

    public Authenticator getAuthenticator() { return authenticator; }

    public void logAction(String username, String action, String details) {
        auditLogs.add(new AuditLog(username, action, details));
    }

    public void addBook(Book book, String adminUsername) {
        books.add(book);
        logAction(adminUsername, "ADD_BOOK", "Added new book: " + book.getTitle() + " at " + book.getBranchName());
    }
    
    public void removeBook(String isbn, String adminUsername) {
        books.removeIf(b -> b.getIsbn() != null && b.getIsbn().equals(isbn));
        logAction(adminUsername, "DELETE_BOOK", "Removed book by ISBN: " + isbn);
    }

    public String borrowBookMessage(String title, String author, String username) {
        for (Book book : books) {
            if (book.getTitle().equalsIgnoreCase(title) && book.getAuthor().equalsIgnoreCase(author)) {
                if (book.getAvailableCopies() > 0) {
                    for(Transaction t : transactions) {
                        if(t.getUsername().equals(username) && t.getIsbn() != null && t.getIsbn().equals(book.getIsbn()) && !t.isReturned()) {
                            logAction(username, "BORROW_ERROR", "Attempted to double-borrow " + title);
                            return "Error: You have already borrowed a copy of this book!";
                        }
                    }
                    book.borrowBook();
                    transactions.add(new Transaction(book.getIsbn(), username));
                    logAction(username, "BORROW_BOOK", "Successfully borrowed: " + title);
                    return "You have successfully borrowed '" + title + "'. It is due in 14 days.";
                } else {
                    return "Sorry, '" + title + "' is currently out of stock.";
                }
            }
        } 
        return "Sorry, the book is not available!";
    }

    public String returnBookMessage(String title, String author, String username) {
        for (Book book : books) {
            if (book.getTitle().equalsIgnoreCase(title) && book.getAuthor().equalsIgnoreCase(author)) {
                Transaction openT = null;
                for(Transaction t : transactions) {
                    if(t.getUsername().equals(username) && t.getIsbn() != null && t.getIsbn().equals(book.getIsbn()) && !t.isReturned()) {
                        openT = t;
                        break;
                    }
                }
                
                if(openT != null) {
                    book.returnBook();
                    openT.markReturned();
                    logAction(username, "RETURN_BOOK", "Successfully returned: " + title);
                    return "You have successfully returned '" + title + "'. Thank you!";
                } else {
                    return "Database Error: You haven't borrowed this book, or it's incorrectly logging.";
                }
            }
         }
         return "This book wasn't borrowed from the Library!";
    }
    
    public List<Book> getBooks() { return books; }
    public List<Transaction> getTransactions() { return transactions; }
    public List<AuditLog> getAuditLogs() { return auditLogs; }
    
    public List<Transaction> getUserTransactions(String username) {
        List<Transaction> res = new ArrayList<>();
        for(Transaction t : transactions) {
            if(t.getUsername().equals(username)) res.add(t);
        }
        return res;
    }
}