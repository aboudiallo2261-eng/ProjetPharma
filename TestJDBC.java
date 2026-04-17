import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TestJDBC {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/pharmacie_vet_db?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
        try (Connection conn = DriverManager.getConnection(url, "root", "")) {
            String sql = "INSERT INTO users (nom, identifiant, motDePasseHash, role, email) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "TestJDBCName");
                pstmt.setString(2, "TestJDBCid" + System.currentTimeMillis());
                pstmt.setString(3, "hash");
                pstmt.setString(4, "AGENT");
                pstmt.setString(5, "test@test.com");
                pstmt.executeUpdate();
                System.out.println("JDBC INSERT SUCCESSFUL!");
            } catch (SQLException ex) {
                System.err.println("JDBC SQL EXCEPTION: " + ex.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
