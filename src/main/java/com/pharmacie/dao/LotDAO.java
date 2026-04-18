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
