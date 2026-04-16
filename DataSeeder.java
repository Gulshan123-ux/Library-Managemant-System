import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DataSeeder {
    public static void loadFromCSV(Library library, String filepath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); 
                
                if (values.length >= 7) {
                    try {
                        String title = values[0].replace("\"", "").trim();
                        String author = values[1].replace("\"", "").trim();
                        String isbn = values[2].trim();
                        String category = values[3].trim();
                        String branch = values[4].trim();
                        String ebook = values[5].trim();
                        int copies = Integer.parseInt(values[6].trim());
                        
                        Book b = new Book(title, author, isbn, category, copies);
                        b.setBranchName(branch);
                        if (!ebook.isEmpty()) b.setEbookUrl(ebook);
                        
                        library.addBook(b, "System DataSeeder");
                    } catch (Exception parseEx) {
                        System.out.println("DataSeeder skips faulty line: " + line);
                    }
                }
            }
            System.out.println("DataSeeder successfully ingested dataset from " + filepath);
            library.logAction("System", "CSV_INGESTION", "Successfully seeded dataset from " + filepath);
        } catch (IOException e) {
            System.out.println("DataSeeder: No valid dataset found at " + filepath);
        }
    }
}
