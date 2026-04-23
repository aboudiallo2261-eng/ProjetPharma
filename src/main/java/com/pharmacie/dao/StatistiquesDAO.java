package com.pharmacie.dao;

import com.pharmacie.models.LigneVente;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StatistiquesDAO {

    public Double getChiffreAffairesTotal(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT SUM(v.total) FROM Vente v WHERE v.dateVente BETWEEN :debut AND :fin";
            Query<Double> query = session.createQuery(hql, Double.class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            Double res = query.uniqueResult();
            return res != null ? res : 0.0;
        }
    }

    public Long getNombreVentes(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(v.id) FROM Vente v WHERE v.dateVente BETWEEN :debut AND :fin";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            Long res = query.uniqueResult();
            return res != null ? res : 0L;
        }
    }

    public Double getBeneficeNet(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT SUM(lv.sousTotal - " +
                         "(lv.quantiteVendue * " +
                         "  (SELECT COALESCE(MAX(la.prixUnitaire), 0) FROM LigneAchat la WHERE la.lot.id = lv.lot.id) " +
                         "  / (CASE WHEN lv.typeUnite = :detailType THEN COALESCE(lv.produit.unitesParBoite, 1) ELSE 1 END)" +
                         ")) " +
                         "FROM LigneVente lv JOIN lv.vente v WHERE v.dateVente BETWEEN :debut AND :fin";
            Query<Double> query = session.createQuery(hql, Double.class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            query.setParameter("detailType", LigneVente.TypeUnite.DETAIL);
            Double res = query.uniqueResult();
            return res != null ? res : 0.0;
        }
    }

    public List<Object[]> getTopProduitsVolume(LocalDateTime debut, LocalDateTime fin, int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT lv.produit.nom, SUM(cast(lv.quantiteVendue as double)) FROM LigneVente lv JOIN lv.vente v " +
                         "WHERE v.dateVente BETWEEN :debut AND :fin " +
                         "GROUP BY lv.produit.nom ORDER BY SUM(cast(lv.quantiteVendue as double)) DESC";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            query.setMaxResults(limit);
            return query.list();
        }
    }

    public List<Object[]> getCAByCategorie(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT lv.produit.categorie.nom, SUM(lv.sousTotal) FROM LigneVente lv JOIN lv.vente v " +
                         "WHERE v.dateVente BETWEEN :debut AND :fin " +
                         "GROUP BY lv.produit.categorie.nom";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            return query.list();
        }
    }

    public List<Object[]> getCAByEspece(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT lv.produit.espece.nom, SUM(lv.sousTotal) FROM LigneVente lv JOIN lv.vente v " +
                         "WHERE v.dateVente BETWEEN :debut AND :fin " +
                         "GROUP BY lv.produit.espece.nom";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            return query.list();
        }
    }

    public List<Object[]> getEvolutionCA(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT cast(v.dateVente as date), SUM(v.total) FROM Vente v " +
                         "WHERE v.dateVente BETWEEN :debut AND :fin " +
                         "GROUP BY cast(v.dateVente as date) ORDER BY cast(v.dateVente as date) ASC";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            return query.list();
        }
    }

    /**
     * Évolution horaire du Chiffre d'Affaires (pour la période "Aujourd'hui" ou sur une seule journée).
     */
    public List<Object[]> getEvolutionCAHoraire(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT hour(v.dateVente), SUM(v.total) FROM Vente v " +
                         "WHERE v.dateVente BETWEEN :debut AND :fin " +
                         "GROUP BY hour(v.dateVente) ORDER BY hour(v.dateVente) ASC";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            return query.list();
        }
    }

    public List<Object[]> getEvolutionCAMensuelle(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT year(v.dateVente), month(v.dateVente), SUM(v.total) FROM Vente v " +
                         "WHERE v.dateVente BETWEEN :debut AND :fin " +
                         "GROUP BY year(v.dateVente), month(v.dateVente) " +
                         "ORDER BY year(v.dateVente) ASC, month(v.dateVente) ASC";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            return query.list();
        }
    }

    /**
     * Calcule en base (zéro objet en RAM) les deux métriques d'alerte du dashboard.
     * @return long[2] — [0] = nb produits en dessous du seuil d'alerte,
     *                   [1] = nb lots expirés encore en stock
     */
    public long[] getAlertesKPI(LocalDate today) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // --- 1. Lots expirés avec stock > 0 (requête directe, count MySQL) ---
            String hqlExpires =
                "SELECT COUNT(l.id) FROM Lot l " +
                "WHERE l.quantiteStock > 0 " +
                "  AND l.dateExpiration IS NOT NULL " +
                "  AND l.dateExpiration <= :today";
            Long nbExpires = session.createQuery(hqlExpires, Long.class)
                .setParameter("today", today)
                .uniqueResult();

            // --- 2. Produits dont le stock total (hors expirés) <= seuil d'alerte ---
            // Sous-requête : SUM du stock des lots valides par produit, comparé au seuil
            String hqlAlertes =
                "SELECT COUNT(p.id) FROM Produit p " +
                "WHERE COALESCE( " +
                "    (SELECT SUM(l.quantiteStock) FROM Lot l " +
                "     WHERE l.produit.id = p.id " +
                "       AND (l.dateExpiration IS NULL OR l.dateExpiration > :today)) " +
                ", 0) <= COALESCE(p.seuilAlerte, 0)";
            Long nbAlertes = session.createQuery(hqlAlertes, Long.class)
                .setParameter("today", today)
                .uniqueResult();

            return new long[]{
                nbAlertes != null ? nbAlertes : 0L,
                nbExpires != null ? nbExpires : 0L
            };
        }
    }

    // ===================================================================
    // PHASE 2 : Nouvelles métriques Dashboard
    // ===================================================================

    /**
     * Valeur financière totale des stocks au prix de vente (lots non expirés, stock > 0).
     * Note : Pour les produits déconditionnables, le prix de vente de la boîte est utilisé.
     */
    public Double getValeurTotaleStock(LocalDate today) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT l.quantiteStock, p.estDeconditionnable, p.unitesParBoite, p.prixVente, p.prixVenteUnite " +
                "FROM Lot l JOIN l.produit p " +
                "WHERE l.quantiteStock > 0 " +
                "  AND (l.dateExpiration IS NULL OR l.dateExpiration >= :today)";
            
            List<Object[]> rows = session.createQuery(hql, Object[].class)
                .setParameter("today", today)
                .list();
                
            double total = 0.0;
            for (Object[] row : rows) {
                Integer qte = (Integer) row[0];
                Boolean decond = (Boolean) row[1];
                Integer unitesParBoite = (Integer) row[2];
                Double prixBoite = (Double) row[3];
                Double prixUnite = (Double) row[4];
                
                if (qte == null) qte = 0;
                if (prixBoite == null) prixBoite = 0.0;
                if (prixUnite == null) prixUnite = 0.0;
                
                if (Boolean.TRUE.equals(decond) && unitesParBoite != null && unitesParBoite > 0) {
                    int boites = qte / unitesParBoite;
                    int unites = qte % unitesParBoite;
                    total += (boites * prixBoite) + (unites * prixUnite);
                } else {
                    total += qte * prixBoite;
                }
            }
            return total;
        }
    }

    /**
     * Ventilation du CA par mode de paiement sur la période.
     * @return List de Object[] {modePaiement (String), totalCA (Double)}
     */
    public List<Object[]> getCAParModePaiement(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT " +
                "  SUM(CASE WHEN v.modePaiement = 'ESPECES' THEN v.total " +
                "           WHEN v.modePaiement = 'MIXTE' THEN COALESCE(v.montantEspeces, 0.0) " +
                "           ELSE 0.0 END), " +
                "  SUM(CASE WHEN v.modePaiement = 'MOBILE_MONEY' THEN v.total " +
                "           WHEN v.modePaiement = 'MIXTE' THEN COALESCE(v.montantMobile, 0.0) " +
                "           ELSE 0.0 END) " +
                "FROM Vente v " +
                "WHERE v.dateVente BETWEEN :debut AND :fin";
            Object[] results = session.createQuery(hql, Object[].class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .uniqueResult();
                
            Double sumEspeces = 0.0;
            Double sumMobile = 0.0;
            if (results != null) {
                sumEspeces = results[0] != null ? ((Number) results[0]).doubleValue() : 0.0;
                sumMobile = results[1] != null ? ((Number) results[1]).doubleValue() : 0.0;
            }
            
            List<Object[]> list = new java.util.ArrayList<>();
            list.add(new Object[]{com.pharmacie.models.Vente.ModePaiement.ESPECES, sumEspeces});
            list.add(new Object[]{com.pharmacie.models.Vente.ModePaiement.MOBILE_MONEY, sumMobile});
            return list;
        }
    }

    /**
     * Évolution journalière du coût des achats sur la période.
     * Utilisé pour superposer la courbe Achats sur le LineChart CA.
     */
    public List<Object[]> getEvolutionCoutsAchats(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT cast(a.dateAchat as date), SUM(la.prixUnitaire * la.quantiteAchetee) " +
                "FROM LigneAchat la JOIN la.achat a " +
                "WHERE a.dateAchat BETWEEN :debut AND :fin " +
                "GROUP BY cast(a.dateAchat as date) ORDER BY cast(a.dateAchat as date) ASC";
            return session.createQuery(hql, Object[].class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .list();
        }
    }

    /**
     * Évolution horaire des coûts d'achats (pour la période "Aujourd'hui" ou sur une seule journée).
     */
    public List<Object[]> getEvolutionCoutsAchatsHoraire(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT hour(a.dateAchat), SUM(la.prixUnitaire * la.quantiteAchetee) " +
                "FROM LigneAchat la JOIN la.achat a " +
                "WHERE a.dateAchat BETWEEN :debut AND :fin " +
                "GROUP BY hour(a.dateAchat) ORDER BY hour(a.dateAchat) ASC";
            return session.createQuery(hql, Object[].class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .list();
        }
    }

    /**
     * Évolution mensuelle du coût des achats (pour les périodes Année / Tout).
     */
    public List<Object[]> getEvolutionCoutsAchatsMensuelle(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT year(a.dateAchat), month(a.dateAchat), SUM(la.prixUnitaire * la.quantiteAchetee) " +
                "FROM LigneAchat la JOIN la.achat a " +
                "WHERE a.dateAchat BETWEEN :debut AND :fin " +
                "GROUP BY year(a.dateAchat), month(a.dateAchat) " +
                "ORDER BY year(a.dateAchat) ASC, month(a.dateAchat) ASC";
            return session.createQuery(hql, Object[].class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .list();
        }
    }

    /**
     * Pertes & ajustements regroupés par motif sur la période.
     * @return List de Object[] {motif (String), nbOccurrences (Long), totalUnites (Long)}
     */
    public List<Object[]> getPertesParMotif(LocalDateTime debut, LocalDateTime fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT a.motif, COUNT(a.id), SUM(a.quantite) " +
                "FROM AjustementStock a " +
                "WHERE a.dateAjustement BETWEEN :debut AND :fin " +
                "GROUP BY a.motif ORDER BY SUM(a.quantite) DESC";
            return session.createQuery(hql, Object[].class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .list();
        }
    }
}
