  package com.pharmacie.dao;

import com.pharmacie.models.MouvementStock;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MouvementDAO extends GenericDAO<MouvementStock> {

    private static final Logger logger = LoggerFactory.getLogger(MouvementDAO.class);
    
    public MouvementDAO() {
        super(MouvementStock.class);
    }

    // Le findAll du GenericDAO ne doit pas être utilisé sans limite sur la table MouvementStock.
    // La méthode de recherche ci-dessous est le moteur du Livre de Bord.
    
    public List<MouvementStock> rechercher(LocalDate debut, LocalDate fin, Long produitId, MouvementStock.TypeMouvement type, Long userId) {
        if (debut == null || fin == null) return Collections.emptyList();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder(
                "SELECT m FROM MouvementStock m " +
                "JOIN FETCH m.produit p " +
                "JOIN FETCH m.user u " +
                "LEFT JOIN FETCH m.lot l " + // LEFT JOIN car lot_id peut être null
                "WHERE m.dateMouvement >= :debut AND m.dateMouvement < :fin "
            );
            
            if (produitId != null) {
                hql.append("AND p.id = :produitId ");
            }
            if (type != null) {
                hql.append("AND m.typeMouvement = :type ");
            }
            if (userId != null) {
                hql.append("AND u.id = :userId ");
            }

            hql.append("ORDER BY m.dateMouvement DESC");

            Query<MouvementStock> query = session.createQuery(hql.toString(), MouvementStock.class);
            
            // On ajoute 1 jour à "fin" pour inclure toute la journée de fin (jusqu'à 23h59)
            query.setParameter("debut", debut.atStartOfDay());
            query.setParameter("fin", fin.plusDays(1).atStartOfDay());

            if (produitId != null) {
                query.setParameter("produitId", produitId);
            }
            if (type != null) {
                query.setParameter("type", type);
            }
            if (userId != null) {
                query.setParameter("userId", userId);
            }

            return query.list();
        } catch (Exception e) {
            logger.error("Erreur DAO rechercher", e);
            return Collections.emptyList();
        }
    }
    public List<MouvementStock> findByLotId(Long lotId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT m FROM MouvementStock m " +
                "JOIN FETCH m.produit p " +
                "LEFT JOIN FETCH m.user u " +  // LEFT JOIN in case a system movement doesn't have a user
                "JOIN FETCH m.lot l " +
                "WHERE l.id = :lotId " +
                "ORDER BY m.dateMouvement DESC", MouvementStock.class)
            .setParameter("lotId", lotId)
            .list();
        } catch (Exception e) {
            logger.error("Erreur DAO findByLotId", e);
            return Collections.emptyList();
        }
    }
}
