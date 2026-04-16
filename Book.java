public class Book {
    private String title;
    private String author;
    private String description = "A fantastic read waiting for you!";
    private String isbn;
    private String category;
    private String branchName = "Main Library";
    private String ebookUrl = ""; // external link representing the ebook
    private int totalCopies;
    private int availableCopies;

    public Book(String title, String author, String isbn, String category, int totalCopies){
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public void borrowBook() {
        if(availableCopies > 0){
            availableCopies--;
        }
    }

    public void returnBook() {
        if(availableCopies < totalCopies) {
            availableCopies++;
        }
    }

    public boolean isAvailable() { return availableCopies > 0; }
    
    public String getTitle(){ return title; } 
    public String getAuthor(){ return author; }
    public String getIsbn() { return isbn; }
    public String getCategory() { return category; }
    public String getBranchName() { return branchName; }
    public String getEbookUrl() { return ebookUrl; }
    public int getAvailableCopies() { return availableCopies; }
    public int getTotalCopies() { return totalCopies; }
    public String getDescription() { return description; }
    
    public void setDescription(String desc) { this.description = desc; }
    public void setCategory(String category) { this.category = category; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public void setEbookUrl(String ebookUrl) { this.ebookUrl = ebookUrl; }
    public void setTotalCopies(int copies) { this.totalCopies = copies; }
    public void setAvailableCopies(int copies) { this.availableCopies = copies; }
}