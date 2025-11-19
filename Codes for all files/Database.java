import java.sql.*;

public class Database {
    // destination or address ng database sa file.
    private static final String DB_URL = "jdbc:sqlite:C:/Java Project/canteen.db";

    // method para i-connect yung database sa main system
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
