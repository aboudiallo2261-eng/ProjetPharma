package com.pharmacie.debug;

import com.pharmacie.dao.GenericDAO;
import com.pharmacie.models.Lot;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import java.time.LocalDate;
import java.util.List;

public class DebugStock {
    public static void main(String[] args) {
        System.out.println("=== DEBUG VALORISATION STOCK ===");
        LocalDate today = LocalDate.now();
        
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 1. Calcul faon ProduitController (tous les lots actifs ou filtrs)
            String hqlLots = "FROM Lot l WHERE l.quantiteStock > 0";
            List<Lot> lots = session.createQuery(hqlLots, Lot.class).list();
            
            double totalUI_Tous = 0.0;
            double totalUI_NonExpires = 0.0;
            
            System.out.println("--- Dtail des anomalies potentielles ---");
            for (Lot lot : lots) {
                com.pharmacie.models.Produit p = lot.getProduit();
                boolean isExp = lot.getDateExpiration() != null && lot.getDateExpiration().isBefore(today);
                
                double val = 0.0;
                if (p.getEstDeconditionnable() != null && p.getEstDeconditionnable() && p.getUnitesParBoite() != null && p.getUnitesParBoite() > 0) {
                    int boites = lot.getQuantiteStock() / p.getUnitesParBoite();
                    int unites = lot.getQuantiteStock() % p.getUnitesParBoite();
                    double pxB = p.getPrixVente() != null ? p.getPrixVente() : 0.0;
                    double pxU = p.getPrixVenteUnite() != null ? p.getPrixVenteUnite() : 0.0;
                    val = (boites * pxB) + (unites * pxU);
                } else {
                    double pxB = p.getPrixVente() != null ? p.getPrixVente() : 0.0;
                    val = lot.getQuantiteStock() * pxB;
                }
                
                totalUI_Tous += val;
                if (!isExp) {
                    totalUI_NonExpires += val;
                }
                
                if (val > 1000000) {
                    System.out.println("PRODUIT MASSIF DTECT : " + p.getNom() + " | Qte: " + lot.getQuantiteStock() + " | Prix: " + p.getPrixVente() + " | Valeur: " + val + " | Expir: " + isExp);
                }
            }
            
            System.out.println("\n--- Rsultats UI (ProduitController) ---");
            System.out.printf("Total UI (Tous): %.0f\n", totalUI_Tous);
            System.out.printf("Total UI (Non Expirs): %.0f\n", totalUI_NonExpires);
            
            // 2. Calcul faon Dashboard (StatistiquesDAO)
            com.pharmacie.dao.StatistiquesDAO dao = new com.pharmacie.dao.StatistiquesDAO();
            double totalDashboard = dao.getValeurTotaleStock(today);
            System.out.printf("\n--- Rsultat Dashboard --- \nTotal: %.0f\n", totalDashboard);
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
