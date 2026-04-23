package com.pharmacie.debug;

import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import java.time.LocalDate;
import java.util.List;

public class DebugDashboardQuery {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        System.out.println("=== DEBUG DASHBOARD QUERY ===");
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT p.nom, l.quantiteStock, p.estDeconditionnable, p.unitesParBoite, p.prixVente, p.prixVenteUnite " +
                "FROM Lot l JOIN l.produit p " +
                "WHERE l.quantiteStock > 0 " +
                "  AND (l.dateExpiration IS NULL OR l.dateExpiration > :today)";
            
            List<Object[]> rows = session.createQuery(hql, Object[].class)
                .setParameter("today", today)
                .list();
            
            double total = 0.0;
            for (Object[] row : rows) {
                String nom = (String) row[0];
                Integer qte = (Integer) row[1];
                Boolean decond = (Boolean) row[2];
                Integer unitesParBoite = (Integer) row[3];
                Double prixBoite = (Double) row[4];
                Double prixUnite = (Double) row[5];
                
                if (qte == null) qte = 0;
                if (prixBoite == null) prixBoite = 0.0;
                if (prixUnite == null) prixUnite = 0.0;
                
                double rowTotal = 0.0;
                if (Boolean.TRUE.equals(decond) && unitesParBoite != null && unitesParBoite > 0) {
                    int boites = qte / unitesParBoite;
                    int unites = qte % unitesParBoite;
                    rowTotal = (boites * prixBoite) + (unites * prixUnite);
                } else {
                    rowTotal = qte * prixBoite;
                }
                total += rowTotal;
                
                if (rowTotal > 1000000) {
                    System.out.println("DASHBOARD MASSIF -> " + nom + " | Qte=" + qte + " | Decond=" + decond + " | PxBoite=" + prixBoite + " | RowTotal=" + rowTotal);
                }
            }
            System.out.println("TOTAL DASHBOARD CALCULE : " + total);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
