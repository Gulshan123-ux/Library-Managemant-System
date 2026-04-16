import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.net.InetSocketAddress;
import java.util.List;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class LibraryWebServer {
    private static Library library;
    private static Authenticator auth;

    public static void main(String[] args) throws IOException {
        auth = new Authenticator();
        library = new Library(auth);
        
        Book b1 = new Book("Verity", "Colleen Hoover", "1001", "Thriller", 3);
        b1.setDescription("Lowen Ashleigh is a struggling writer on the brink...");
        b1.setBranchName("Main Library");
        
        Book b2 = new Book("The Secret", "Rhonda Byrne", "2001", "Self-Help", 5);
        b2.setDescription("Wisdom from modern-day teachers.");
        b2.setBranchName("Main Library");

        Book b3 = new Book("Grit", "Angela Duckworth", "3001", "Science", 2);
        b3.setDescription("Angela Duckworth shows anyone striving to succeed is possible.");
        b3.setBranchName("Downtown Branch");
        b3.setEbookUrl("https://example.com/download-grit.epub");

        Book b4 = new Book("1984", "George Orwell", "4001", "Fiction", 4);
        b4.setDescription("A dystopian social science fiction novel.");
        b4.setBranchName("North Campus");

        Book b5 = new Book("Thinking, Fast and Slow", "Daniel Kahneman", "5001", "Psychology", 5);
        b5.setDescription("Tour of the mind and the two systems that drive the way we think.");
        b5.setBranchName("Downtown Branch");

        Book b6 = new Book("Steve Jobs", "Walter Isaacson", "6001", "Biography", 3);
        b6.setDescription("Exclusive biography of Apple's late co-founder.");
        b6.setBranchName("Main Library");

        Book b7 = new Book("Clean Code", "Robert C. Martin", "7001", "Computer Science", 4);
        b7.setDescription("A Handbook of Agile Software Craftsmanship.");
        b7.setBranchName("North Campus");
        b7.setEbookUrl("https://example.com/cleancode.pdf");
        
        Book b8 = new Book("Shoe Dog", "Phil Knight", "8001", "Business", 2);
        b8.setBranchName("Downtown Branch");

        library.addBook(b1, "System");
        library.addBook(b2, "System");
        library.addBook(b3, "System");
        library.addBook(b4, "System");
        library.addBook(b5, "System");
        library.addBook(b6, "System");
        library.addBook(b7, "System");
        library.addBook(b8, "System");
        
        // Execute the Batch Data Ingestion script locating CSV databases
        DataSeeder.loadFromCSV(library, "database.csv");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new StaticFileHandler());
        server.createContext("/bookdetail.html", new StaticFileHandler());
        server.createContext("/api/books", new BooksApiHandler());
        server.createContext("/api/book", new SingleBookApiHandler());
        server.createContext("/api/action", new ActionApiHandler());
        server.createContext("/api/login", new LoginApiHandler());
        server.createContext("/api/logout", new LogoutApiHandler());
        server.createContext("/api/admin/add", new AdminAddBookHandler());
        server.createContext("/api/admin/remove", new AdminRemoveBookHandler());
        server.createContext("/api/history", new HistoryApiHandler());
        server.createContext("/api/profile", new ProfileApiHandler());
        server.createContext("/api/stats", new StatsApiHandler());
        server.createContext("/api/settings/password", new SettingsApiHandler());
        server.createContext("/api/audit", new AuditApiHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Advanced Premium Web Server started on port 8080...");
        System.out.println("Visit http://localhost:8080 in your browser!");
    }

    public static User getSessionUser(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                cookie = cookie.trim();
                if (cookie.startsWith("session=")) {
                    String sessionId = cookie.substring(8);
                    return auth.getUserBySession(sessionId);
                }
            }
        }
        return null;
    }

    static private void sendJsonResponse(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    static class LoginApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), "UTF-8");
                Map<String, String> params = parseForm(body);
                
                String username = params.get("username");
                String password = params.get("password");
                String sessionId = auth.login(username, password);

                if (sessionId != null) {
                    User user = auth.getUserBySession(sessionId);
                    library.logAction(user.getUsername(), "LOGIN", "Successful login.");
                    exchange.getResponseHeaders().add("Set-Cookie", "session=" + sessionId + "; Path=/; HttpOnly");
                    sendJsonResponse(exchange, 200, "{\"success\":true, \"role\":\"" + user.getRole().toString() + "\"}");
                } else {
                    library.logAction(username, "LOGIN_FAIL", "Failed login credentials.");
                    sendJsonResponse(exchange, 401, "{\"success\":false, \"message\":\"Invalid credentials\"}");
                }
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class LogoutApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user != null) library.logAction(user.getUsername(), "LOGOUT", "User logged out.");
                
                String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
                if (cookieHeader != null) {
                    for (String cookie : cookieHeader.split(";")) {
                        cookie = cookie.trim();
                        if (cookie.startsWith("session=")) {
                            auth.logout(cookie.substring(8));
                        }
                    }
                }
                exchange.getResponseHeaders().add("Set-Cookie", "session=; Path=/; Max-Age=0");
                sendJsonResponse(exchange, 200, "{\"success\":true}");
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";
            File file = new File(path.substring(1));
            if (file.exists()) {
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody()) { Files.copy(file.toPath(), os); }
            } else {
                String response = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
            }
        }
    }

    static class BooksApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                List<Book> books = library.getBooks();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < books.size(); i++) {
                    Book b = books.get(i);
                    json.append("{")
                        .append("\"title\":\"").append(escapeJson(b.getTitle())).append("\",")
                        .append("\"author\":\"").append(escapeJson(b.getAuthor())).append("\",")
                        .append("\"isbn\":\"").append(escapeJson(b.getIsbn())).append("\",")
                        .append("\"category\":\"").append(escapeJson(b.getCategory())).append("\",")
                        .append("\"branchName\":\"").append(escapeJson(b.getBranchName())).append("\",")
                        .append("\"ebookUrl\":\"").append(escapeJson(b.getEbookUrl())).append("\",")
                        .append("\"availableCopies\":").append(b.getAvailableCopies()).append(",")
                        .append("\"totalCopies\":").append(b.getTotalCopies()).append(",")
                        .append("\"available\":").append(b.isAvailable())
                        .append("}");
                    if (i < books.size() - 1) json.append(",");
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class SingleBookApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String titleTarget = "";
                if (query != null && query.startsWith("title=")) {
                    titleTarget = URLDecoder.decode(query.substring(6), "UTF-8");
                }
                Book found = null;
                for (Book b : library.getBooks()) {
                    if (b.getTitle().equalsIgnoreCase(titleTarget)) { found = b; break; }
                }
                if (found != null) {
                    String json = "{"
                        + "\"title\":\"" + escapeJson(found.getTitle()) + "\","
                        + "\"author\":\"" + escapeJson(found.getAuthor()) + "\","
                        + "\"isbn\":\"" + escapeJson(found.getIsbn()) + "\","
                        + "\"category\":\"" + escapeJson(found.getCategory()) + "\","
                        + "\"branchName\":\"" + escapeJson(found.getBranchName()) + "\","
                        + "\"ebookUrl\":\"" + escapeJson(found.getEbookUrl()) + "\","
                        + "\"availableCopies\":" + found.getAvailableCopies() + ","
                        + "\"totalCopies\":" + found.getTotalCopies() + ","
                        + "\"description\":\"" + escapeJson(found.getDescription()) + "\","
                        + "\"available\":" + found.isAvailable()
                        + "}";
                    sendJsonResponse(exchange, 200, json);
                } else { exchange.sendResponseHeaders(404, -1); }
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class StatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                int totalBooks = library.getBooks().size();
                int activeLoans = 0;
                int overdue = 0;
                for(Transaction t : library.getTransactions()) {
                    if(!t.isReturned()) activeLoans++;
                    if(!t.isReturned() && t.isOverdue()) overdue++;
                }
                String json = "{\"totalBooks\":" + totalBooks + ","
                            + "\"activeLoans\":" + activeLoans + ","
                            + "\"overdueItems\":" + overdue + ","
                            + "\"activeSessions\":" + auth.getActiveSessionCount() + ","
                            + "\"registeredUsers\":" + auth.getTotalUsers()
                            + "}";
                sendJsonResponse(exchange, 200, json);
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class ActionApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user == null) {
                    sendJsonResponse(exchange, 401, "{\"message\":\"Unauthorized. Please login.\"}");
                    return;
                }
                
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), "UTF-8");
                Map<String, String> params = parseForm(body);

                String action = params.get("action");
                String title = params.get("title");
                String author = params.get("author");

                String message = "Unknown action.";

                if ("borrow".equals(action)) {
                    message = library.borrowBookMessage(title, author, user.getUsername());
                } else if ("return".equals(action)) {
                    message = library.returnBookMessage(title, author, user.getUsername());
                }

                sendJsonResponse(exchange, 200, "{\"message\":\"" + escapeJson(message) + "\"}");
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }
    
    static class ProfileApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user == null) {
                    sendJsonResponse(exchange, 401, "{\"message\":\"Unauthorized.\"}");
                    return;
                }
                String json = "{\"username\":\"" + escapeJson(user.getUsername()) + "\","
                            + "\"role\":\"" + user.getRole().toString() + "\","
                            + "\"membershipType\":\"" + escapeJson(user.getMembershipType()) + "\","
                            + "\"membershipExpiry\":\"" + (user.getMembershipExpiry() != null ? escapeJson(user.getMembershipExpiry().toString()) : "") + "\"}";
                sendJsonResponse(exchange, 200, json);
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class SettingsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user == null) {
                    sendJsonResponse(exchange, 401, "{\"message\":\"Unauthorized.\"}");
                    return;
                }
                InputStream is = exchange.getRequestBody();
                Map<String, String> params = parseForm(new String(is.readAllBytes(), "UTF-8"));
                boolean success = auth.changePassword(user.getUsername(), params.get("oldPassword"), params.get("newPassword"));
                if(success) {
                    library.logAction(user.getUsername(), "CHANGE_PASSWORD", "Successfully changed password.");
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Password updated!\"}");
                } else {
                    library.logAction(user.getUsername(), "CHANGE_PASSWORD_FAIL", "Failed password attempt.");
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Incorrect old password.\"}");
                }
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class AuditApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if ("GET".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user == null || user.getRole() != Role.ADMIN) {
                    sendJsonResponse(exchange, 403, "{\"message\":\"Forbidden. Admins only.\"}");
                    return;
                }
                List<AuditLog> logs = library.getAuditLogs();
                StringBuilder json = new StringBuilder("[");
                for (int i = logs.size() - 1; i >= 0; i--) { // latest first
                    AuditLog a = logs.get(i);
                    json.append("{")
                        .append("\"timestamp\":\"").append(a.getTimestamp().toString()).append("\",")
                        .append("\"username\":\"").append(escapeJson(a.getUsername())).append("\",")
                        .append("\"action\":\"").append(escapeJson(a.getAction())).append("\",")
                        .append("\"details\":\"").append(escapeJson(a.getDetails())).append("\"")
                        .append("}");
                    if (i > 0) json.append(",");
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class HistoryApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user == null) {
                    sendJsonResponse(exchange, 401, "{\"message\":\"Unauthorized.\"}");
                    return;
                }
                List<Transaction> tx = library.getUserTransactions(user.getUsername());
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < tx.size(); i++) {
                    Transaction t = tx.get(i);
                    String titleName = t.getIsbn(); 
                    for(Book b : library.getBooks()) {
                        if(b.getIsbn() != null && b.getIsbn().equals(t.getIsbn())) {
                            titleName = b.getTitle();
                        }
                    }
                    
                    json.append("{")
                        .append("\"id\":\"").append(escapeJson(t.getId())).append("\",")
                        .append("\"title\":\"").append(escapeJson(titleName)).append("\",")
                        .append("\"issueDate\":\"").append(t.getIssueDate() != null ? escapeJson(t.getIssueDate().toString()) : "").append("\",")
                        .append("\"dueDate\":\"").append(t.getDueDate() != null ? escapeJson(t.getDueDate().toString()) : "").append("\",")
                        .append("\"returnDate\":\"").append(t.getReturnDate() != null ? escapeJson(t.getReturnDate().toString()) : "").append("\",")
                        .append("\"returned\":").append(t.isReturned()).append(",")
                        .append("\"overdue\":").append(t.isOverdue())
                        .append("}");
                    if (i < tx.size() - 1) json.append(",");
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class AdminAddBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user == null || user.getRole() != Role.ADMIN) {
                    sendJsonResponse(exchange, 403, "{\"message\":\"Forbidden. Admin access required.\"}");
                    return;
                }
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), "UTF-8");
                Map<String, String> params = parseForm(body);
                
                Book bk = new Book(params.get("title"), params.get("author"), params.get("isbn"), params.get("category"), Integer.parseInt(params.get("copies")));
                if(params.containsKey("description")) bk.setDescription(params.get("description"));
                if(params.containsKey("branch")) bk.setBranchName(params.get("branch"));
                if(params.containsKey("ebook")) bk.setEbookUrl(params.get("ebook"));
                
                library.addBook(bk, user.getUsername());
                
                sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Book added successfully!\"}");
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static class AdminRemoveBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                User user = getSessionUser(exchange);
                if (user == null || user.getRole() != Role.ADMIN) {
                    sendJsonResponse(exchange, 403, "{\"message\":\"Forbidden.\"}");
                    return;
                }
                InputStream is = exchange.getRequestBody();
                Map<String, String> params = parseForm(new String(is.readAllBytes(), "UTF-8"));
                library.removeBook(params.get("isbn"), user.getUsername());
                sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Book removed successfully!\"}");
            } else { exchange.sendResponseHeaders(405, -1); }
        }
    }

    static private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"");
    }

    static Map<String, String> parseForm(String formData) throws java.io.UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        if (formData == null || formData.isEmpty()) return params;
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx != -1) {
                params.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                           URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
        }
        return params;
    }
}
