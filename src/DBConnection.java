import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SECTION: Database connection (JDBC)
 * This class has one job: give other classes a connection to MySQL.
 * Change USER and PASSWORD below to match your own MySQL setup.
 */
public class DBConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/bankdb";
    private static final String USER     = "root";
    private static final String PASSWORD = "your_password_here";   // <-- change this

    // Opens and returns a new connection. The caller closes it (try-with-resources).
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
