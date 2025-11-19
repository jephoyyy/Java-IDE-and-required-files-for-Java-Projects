import java.sql.*;
// File for testing if the database is connected to the main java.
public class TestDB {
    public static void main(String[] args) {
        try (Connection conn = Database.connect()) {
            if (conn != null) {
                System.out.println("Yayyyy!, Database connected successfully!");
            }
        } catch (SQLException e) { // Pag nag ka error sa database mag sshow yung message
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
