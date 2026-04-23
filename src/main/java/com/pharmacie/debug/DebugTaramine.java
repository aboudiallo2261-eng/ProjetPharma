package com.pharmacie.debug;

import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import java.time.LocalDate;
import java.util.List;

public class DebugTaramine {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        System.out.println("=== DEBUG TARAMINE ===");
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            
            String hql = "FROM com.pharmacie.models.Lot l WHERE l.produit.nom LIKE '%taramine%'";
            List<com.pharmacie.models.Lot> lots = session.createQuery(hql, com.pharmacie.models.Lot.class).list();
            
            for (com.pharmacie.models.Lot lot : lots) {
                System.out.println("LOT ID: " + lot.getId());
                System.out.println("  Qte: " + lot.getQuantiteStock());
                System.out.println("  DateExp: " + lot.getDateExpiration());
                System.out.println("  EstArchive: " + lot.getEstArchive());
                com.pharmacie.models.Produit p = lot.getProduit();
                System.out.println("  PRODUIT ID: " + p.getId());
                System.out.println("  Nom: " + p.getNom());
                System.out.println("  Prix: " + p.getPrixVente());
                System.out.println("  Decond: " + p.getEstDeconditionnable());
            }
            
            System.out.println("--- Test Query Dashboard pour taramine ---");
            String hqlDash =
                "SELECT p.nom, l.quantiteStock, p.estDeconditionnable, p.unitesParBoite, p.prixVente, p.prixVenteUnite " +
                "FROM Lot l JOIN l.produit p " +
                "WHERE l.quantiteStock > 0 " +
                "  AND (l.dateExpiration IS NULL OR l.dateExpiration > :today) " +
                "  AND p.nom LIKE '%taramine%'";
            List<Object[]> rows = session.createQuery(hqlDash, Object[].class)
                .setParameter("today", today)
                .list();
            System.out.println("Rows found by Dashboard query: " + rows.size());
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
