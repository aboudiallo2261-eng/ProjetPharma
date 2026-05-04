import java.sql.*;

public class TestValeur {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/pharmacie_vet_db?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url, "root", "")) {
            System.out.println("--- GET VALEUR TOTALE STOCK QUERY ---");
            String q1 = "SELECT l.quantiteStock, p.estDeconditionnable, p.unitesParBoite, p.prixVente, p.prixVenteUnite, p.nom " +
                        "FROM lot l JOIN produits p ON p.id = l.produit_id " +
                        "WHERE l.quantiteStock > 0 AND (l.dateExpiration IS NULL OR l.dateExpiration >= CURRENT_DATE)";
            double total = 0.0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(q1)) {
                while (rs.next()) {
                    int qte = rs.getInt(1);
                    boolean decond = rs.getBoolean(2);
                    int upb = rs.getInt(3);
                    double pb = rs.getDouble(4);
                    double pu = rs.getDouble(5);
                    String nom = rs.getString(6);
                    double ligne = 0;
                    if (decond && upb > 0) {
                        ligne = (qte / upb) * pb + (qte % upb) * pu;
                    } else {
                        ligne = qte * pb;
                    }
                    total += ligne;
                    if (ligne > 100000) {
                        System.out.println("Gros lot: " + nom + " | Qte=" + qte + " | pb=" + pb + " | decond=" + decond + " -> " + ligne);
                    }
                }
            }
            System.out.println("TOTAL CALCULÉ: " + total);

            System.out.println("\n--- GET PROCHE PEREMPTION QUERY ---");
            String q2 = "SELECT p.nom, l.quantiteStock, p.prixAchat, p.estDeconditionnable, p.unitesParBoite " +
                        "FROM lot l JOIN produits p ON p.id = l.produit_id " +
                        "WHERE l.quantiteStock > 0 AND l.dateExpiration IS NOT NULL " +
                        "AND l.dateExpiration > CURRENT_DATE AND l.dateExpiration <= DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY)";
            double totalRisque = 0.0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(q2)) {
                while (rs.next()) {
                    String nom = rs.getString(1);
                    int qte = rs.getInt(2);
                    double pa = rs.getDouble(3);
                    boolean decond = rs.getBoolean(4);
                    int upb = rs.getInt(5);
                    double ligne = 0;
                    if (decond && upb > 0) {
                        ligne = qte * (pa / upb);
                    } else {
                        ligne = qte * pa;
                    }
                    totalRisque += ligne;
                    if (ligne > 100000) {
                        System.out.println("Gros risque: " + nom + " | Qte=" + qte + " | pa=" + pa + " | decond=" + decond + " -> " + ligne);
                    }
                }
            }
            System.out.println("TOTAL RISQUE CALCULÉ: " + totalRisque);

        } catch (Exception e) { e.printStackTrace(); }
    }
}
