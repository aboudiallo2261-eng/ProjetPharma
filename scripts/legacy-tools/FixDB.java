import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixDB {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pharmacie_vet_db", "root", "");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE profil SET can_access_dashboard=1, can_access_ventes=1, can_access_stock=1, can_access_achats=1, can_access_fournisseurs=1, can_access_rapports=1, can_access_parametres=1 WHERE nom='SUPER-ADMIN'");
            System.out.println("Database repaired successfully!");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
