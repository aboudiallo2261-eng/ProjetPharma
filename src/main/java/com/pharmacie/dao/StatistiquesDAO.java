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
            // On récupère : nom, quantité totale vendue, CA total (sousTotal), et coût d'achat
            // La marge sera calculée en Java car elle dépend du type d'unité (BOITE vs DÉTAIL)
            String hql = "SELECT lv.produit.nom, " +
                         "SUM(cast(lv.quantiteVendue as double)), " +
                         "SUM(lv.sousTotal), " +
                         "MAX(lv.produit.prixAchat), " +
                         "MAX(lv.produit.unitesParBoite), " +
                         "lv.typeUnite " +
                         "FROM LigneVente lv JOIN lv.vente v " +
                         "WHERE v.dateVente BETWEEN :debut AND :fin " +
                         "GROUP BY lv.produit.nom, lv.typeUnite " +
                         "ORDER BY SUM(cast(lv.quantiteVendue as double)) DESC";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            query.setMaxResults(limit * 2); // On prend plus car groupBy typeUnite peut dupliquer
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
     * Historique du Chiffre d'Affaires sur les 3 dernières années.
     * @param today la date de référence.
     * @return List de Object[] où Object[0] = année (Integer), Object[1] = Montant (Double)
     */
    public List<Object[]> getHistoriqueCA3Ans(LocalDate today) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int currentYear = today.getYear();
            int startYear = currentYear - 2;

            String hql = "SELECT year(v.dateVente), SUM(v.total) " +
                         "FROM Vente v " +
                         "WHERE year(v.dateVente) >= :startYear " +
                         "GROUP BY year(v.dateVente) " +
                         "ORDER BY year(v.dateVente) ASC";
            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("startYear", startYear);
            return query.list();
        }
    }

    /**
     * Calcule en base (zéro objet en RAM) les deux métriques d'alerte du dashboard local.
     * @return long[2] — [0] = nb produits en dessous du seuil d'alerte,
     *                   [1] = nb lots expirés encore en stock
     */
    public long[] getAlertesKPI(LocalDate today) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            
            LocalDate dateLimite = today.plusDays(60);

            // --- 1. Lots dont le jour avant expiration est <= 60 jours (et non déjà expirés) ---
            String hqlExpires =
                "SELECT COUNT(l.id) FROM Lot l " +
                "WHERE l.quantiteStock > 0 " +
                "  AND l.dateExpiration IS NOT NULL " +
                "  AND l.dateExpiration >= :today " +
                "  AND l.dateExpiration <= :dateLimite";
            Long nbExpires = session.createQuery(hqlExpires, Long.class)
                .setParameter("today", today)
                .setParameter("dateLimite", dateLimite)
                .uniqueResult();

            // --- 2. Produits dont le stock total <= seuil d'alerte
            //        On part de Lot pour exclure les produits sans aucun lot créé.
            //        On récupère les IDs et on compte en Java (GROUP BY + uniqueResult incompatible en HQL).
            String hqlAlertes =
                "SELECT l.produit.id FROM Lot l JOIN l.produit p " +
                "GROUP BY l.produit.id, p.seuilAlerte " +
                "HAVING COALESCE(SUM(l.quantiteStock), 0) <= COALESCE(MAX(p.seuilAlerte), 5)";
            long nbAlertes = session.createQuery(hqlAlertes, Long.class).list().size();

            return new long[]{
                nbAlertes,
                nbExpires != null ? nbExpires : 0L
            };
        }
    }

    /**
     * Calcule en base les trois métriques d'alerte pour le Dashboard WEB (PWA).
     * @return long[3] — [0] = nb ruptures (stock = 0),
     *                   [1] = nb alertes (0 < stock <= seuil),
     *                   [2] = nb lots expirés encore en stock
     */
    public long[] getDashboardWebAlertesKPI(LocalDate today) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate dateLimite = today.plusDays(60);
            
            String hqlExpires =
                "SELECT COUNT(l.id) FROM Lot l WHERE l.quantiteStock > 0 AND l.dateExpiration IS NOT NULL AND l.dateExpiration >= :today AND l.dateExpiration <= :dateLimite";
            Long nbExpires = session.createQuery(hqlExpires, Long.class)
                .setParameter("today", today)
                .setParameter("dateLimite", dateLimite)
                .uniqueResult();

            // Ruptures : produits ayant au moins un lot, dont le stock total = 0
            String hqlRuptures =
                "SELECT l.produit.id FROM Lot l " +
                "GROUP BY l.produit.id " +
                "HAVING COALESCE(SUM(l.quantiteStock), 0) = 0";
            long nbRuptures = session.createQuery(hqlRuptures, Long.class).list().size();

            // Alertes : produits avec stock > 0 mais <= seuil
            String hqlAlertes =
                "SELECT l.produit.id FROM Lot l JOIN l.produit p " +
                "GROUP BY l.produit.id, p.seuilAlerte " +
                "HAVING COALESCE(SUM(l.quantiteStock), 0) > 0 " +
                "  AND COALESCE(SUM(l.quantiteStock), 0) <= COALESCE(MAX(p.seuilAlerte), 5)";
            long nbAlertes = session.createQuery(hqlAlertes, Long.class).list().size();

            return new long[]{
                nbRuptures,
                nbAlertes,
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

    /**
     * Retourne les N produits dont le stock total est <= seuil d'alerte. (Utilisé par le logiciel JavaFX Local)
     * NOTE : Produit n'ayant pas de @OneToMany lots, on part de Lot → Produit.
     * @return List de Object[] {produit.id (Long), produit.nom (String), stockTotal (Long)}
     */
    public List<Object[]> getProduitsEnRupture(int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT l.produit.id, l.produit.nom, COALESCE(SUM(l.quantiteStock), 0) " +
                "FROM Lot l " +
                "GROUP BY l.produit.id, l.produit.nom " +
                "HAVING COALESCE(SUM(l.quantiteStock), 0) <= COALESCE(MAX(l.produit.seuilAlerte), 5) " +
                "ORDER BY COALESCE(SUM(l.quantiteStock), 0) ASC";
            return session.createQuery(hql, Object[].class)
                .setMaxResults(limit)
                .list();
        }
    }

    /**
     * Retourne les produits dont le stock total est = 0. (Spécifique PWA)
     */
    public List<Object[]> getProduitsEnRuptureTotale(int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT l.produit.id, l.produit.nom, COALESCE(SUM(l.quantiteStock), 0) " +
                "FROM Lot l " +
                "GROUP BY l.produit.id, l.produit.nom " +
                "HAVING COALESCE(SUM(l.quantiteStock), 0) = 0 " +
                "ORDER BY l.produit.nom ASC";
            return session.createQuery(hql, Object[].class)
                .setMaxResults(limit)
                .list();
        }
    }

    /**
     * Retourne les N produits dont le stock total est > 0 et <= seuil d'alerte.
     */
    public List<Object[]> getProduitsEnAlerte(int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT l.produit.id, l.produit.nom, COALESCE(SUM(l.quantiteStock), 0) " +
                "FROM Lot l " +
                "GROUP BY l.produit.id, l.produit.nom " +
                "HAVING COALESCE(SUM(l.quantiteStock), 0) > 0 AND COALESCE(SUM(l.quantiteStock), 0) <= COALESCE(MAX(l.produit.seuilAlerte), 5) " +
                "ORDER BY COALESCE(SUM(l.quantiteStock), 0) ASC";
            return session.createQuery(hql, Object[].class)
                .setMaxResults(limit)
                .list();
        }
    }

    /**
     * Détails financiers des pertes enregistrées aujourd'hui (Ajustements Négatifs pour Casse/Péremption/Erreur).
     * @return List de Object[] {produit.nom (String), quantite (Integer), valeur (Double), motif (String)}
     */
    public List<Object[]> getPertesDuJourDetails(LocalDate today) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT a.lot.produit.nom, a.lot.numeroLot, a.quantite, a.lot.produit.prixAchat, cast(a.motif as string), a.lot.produit.estDeconditionnable, a.lot.produit.unitesParBoite " +
                "FROM AjustementStock a " +
                "WHERE cast(a.dateAjustement as date) = :today " +
                "  AND a.typeAjustement = 'AJUSTEMENT_NEGATIF' " +
                "  AND cast(a.motif as string) IN ('CASSE', 'PEREMPTION', 'ERREUR_INVENTAIRE')";
            return session.createQuery(hql, Object[].class)
                .setParameter("today", today)
                .list();
        }
    }

    /**
     * Retourne la liste des lots périmés (dont le stock est > 0).
     * @return List de Object[] {produit.nom, lot.numeroLot, lot.dateExpiration, lot.quantiteStock, produit.prixAchat}
     */
    public List<Object[]> getLotsPerimes(LocalDate today) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT l.produit.nom, l.numeroLot, l.dateExpiration, l.quantiteStock, l.produit.prixAchat, l.produit.estDeconditionnable, l.produit.unitesParBoite " +
                "FROM Lot l " +
                "WHERE l.quantiteStock > 0 " +
                "  AND l.dateExpiration IS NOT NULL " +
                "  AND l.dateExpiration <= :today " +
                "ORDER BY l.dateExpiration ASC";
            return session.createQuery(hql, Object[].class)
                .setParameter("today", today)
                .list();
        }
    }

    /**
     * Retourne la liste des lots proches de la péremption (dans les X prochains jours).
     * @param joursAlerte Le nombre de jours d'anticipation (ex: 90 pour 3 mois)
     * @return List de Object[] {produit.nom, lot.numeroLot, lot.dateExpiration, lot.quantiteStock, produit.prixAchat}
     */
    public List<Object[]> getLotsProchePeremption(LocalDate today, int joursAlerte) {
        LocalDate dateLimite = today.plusDays(joursAlerte);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql =
                "SELECT l.produit.nom, l.numeroLot, l.dateExpiration, l.quantiteStock, l.produit.prixAchat, l.produit.estDeconditionnable, l.produit.unitesParBoite " +
                "FROM Lot l " +
                "WHERE l.quantiteStock > 0 " +
                "  AND l.dateExpiration IS NOT NULL " +
                "  AND l.dateExpiration > :today " +
                "  AND l.dateExpiration <= :dateLimite " +
                "ORDER BY l.dateExpiration ASC";
            return session.createQuery(hql, Object[].class)
                .setParameter("today", today)
                .setParameter("dateLimite", dateLimite)
                .list();
        }
    }
}
