import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class InspectDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/pharmacie_vet_db?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
        Connection conn = DriverManager.getConnection(url, "root", "");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE users");
        while (rs.next()) {
            System.out.println(rs.getString("Field") + " | " + rs.getString("Type") + " | " + rs.getString("Null") + " | " + rs.getString("Default"));
        }
    }
}
