import java.sql.*;

public class TestDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/pharmacie_vet_db?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String pass = "";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String q = "SELECT p.nom, p.prixAchat, p.prixVente, p.prixVenteUnite, p.estDeconditionnable, p.unitesParBoite, l.numeroLot, l.quantiteStock, l.dateExpiration " +
                       "FROM produits p JOIN lot l ON p.id = l.produit_id " +
                       "WHERE p.nom = 'paracetamole'";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(q)) {
                System.out.println("--- PARACETAMOLE ---");
                while (rs.next()) {
                    System.out.println("Lot: " + rs.getString("numeroLot"));
                    System.out.println("Qte: " + rs.getInt("quantiteStock"));
                    System.out.println("Exp: " + rs.getString("dateExpiration"));
                    System.out.println("Prix Achat: " + rs.getDouble("prixAchat"));
                    System.out.println("Prix Vente: " + rs.getDouble("prixVente"));
                    System.out.println("Prix Vente Unite: " + rs.getDouble("prixVenteUnite"));
                    System.out.println("Decond: " + rs.getBoolean("estDeconditionnable"));
                    System.out.println("Unites/Boite: " + rs.getInt("unitesParBoite"));
                    System.out.println("--------------------");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
