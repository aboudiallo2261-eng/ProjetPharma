package com.pharmacie.dao;

import com.pharmacie.models.Lot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LotDAO extends GenericDAO<Lot> {
    private static final Logger logger = LoggerFactory.getLogger(LotDAO.class);
    public LotDAO() {
        super(Lot.class);
    }

    public java.util.List<Lot> findActiveLotsWithDetails(boolean includeArchived) {
        try (org.hibernate.Session session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            String whereClause = includeArchived ? "" : "WHERE (l.estArchive IS FALSE OR l.estArchive IS NULL) AND l.quantiteStock > 0 ";
            return session.createQuery(
                "SELECT l FROM Lot l " +
                "JOIN FETCH l.produit p " +
                "JOIN FETCH p.categorie " +
                "JOIN FETCH p.espece " +
                whereClause +
                "ORDER BY l.dateExpiration ASC", Lot.class)
            .list();
        } catch (Exception e) {
            logger.error("Erreur DAO findActiveLotsWithDetails", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Lots disponibles d'un produit, triés FEFO (First Expired First Out).
     * Remplace les anciens findAll().stream().filter() qui chargeaient TOUS les lots.
     */
    public java.util.List<Lot> findLotsDisponibles(Long produitId) {
        try (org.hibernate.Session session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "FROM Lot l WHERE l.produit.id = :produitId " +
                "AND l.quantiteStock > 0 " +
                "AND (l.dateExpiration IS NULL OR l.dateExpiration >= :today) " +
                "ORDER BY l.dateExpiration ASC NULLS LAST", Lot.class)
            .setParameter("produitId", produitId)
            .setParameter("today", java.time.LocalDate.now())
            .list();
        } catch (Exception e) {
            logger.error("Erreur DAO findLotsDisponibles", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Stock total non périmé par produit, agrégé côté base de données.
     * Une seule requête GROUP BY au lieu de charger tous les lots en mémoire.
     */
    public java.util.Map<Long, Integer> getStockDisponibleParProduit() {
        try (org.hibernate.Session session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            java.util.List<Object[]> results = session.createQuery(
                "SELECT l.produit.id, SUM(l.quantiteStock) FROM Lot l " +
                "WHERE l.dateExpiration IS NULL OR l.dateExpiration >= :today " +
                "GROUP BY l.produit.id", Object[].class)
            .setParameter("today", java.time.LocalDate.now())
            .list();
            java.util.Map<Long, Integer> map = new java.util.HashMap<>();
            for (Object[] row : results) {
                map.put((Long) row[0], ((Number) row[1]).intValue());
            }
            return map;
        } catch (Exception e) {
            logger.error("Erreur DAO getStockDisponibleParProduit", e);
            return java.util.Collections.emptyMap();
        }
    }

    public java.util.Map<Long, Long> getQuantitesVenduesParLot() {
        try (org.hibernate.Session session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            java.util.List<Object[]> results = session.createQuery(
                "SELECT lv.lot.id, SUM(lv.quantiteVendue) FROM LigneVente lv WHERE lv.lot IS NOT NULL GROUP BY lv.lot.id", 
                Object[].class).list();
            java.util.Map<Long, Long> map = new java.util.HashMap<>();
            for (Object[] row : results) {
                map.put((Long) row[0], (Long) row[1]);
            }
            return map;
        } catch (Exception e) {
            logger.error("Erreur DAO getQuantitesVenduesParLot", e);
            return java.util.Collections.emptyMap();
        }
    }
}
