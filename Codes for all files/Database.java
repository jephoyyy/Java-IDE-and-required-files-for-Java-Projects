import java.sql.*;

public class Database {
    // Full path ng database (palitan kung iba ang location mo)
    private static final String DB_URL = "jdbc:sqlite:C:/Java Project/canteen.db";

    // Method para mag connect sa database
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
