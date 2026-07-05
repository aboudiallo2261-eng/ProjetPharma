package com.pharmacie.dao;

import com.pharmacie.models.AjustementStock;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class AjustementStockDAO extends GenericDAO<AjustementStock> {

    private static final Logger logger = LoggerFactory.getLogger(AjustementStockDAO.class);

    public AjustementStockDAO() {
        super(AjustementStock.class);
    }

    /**
     * Charge tous les ajustements avec leurs associations (Lot, Produit, User)
     * en une seule requête SQL via JOIN FETCH.
     * Nécessaire pour éviter une LazyInitializationException lorsque la session
     * Hibernate est fermée avant que le contrôleur n'accède aux entités liées.
     */
    public List<AjustementStock> findAllWithDetails() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT DISTINCT a FROM AjustementStock a " +
                "JOIN FETCH a.lot l " +
                "JOIN FETCH l.produit p " +
                "JOIN FETCH a.user u " +
                "ORDER BY a.dateAjustement DESC",
                AjustementStock.class)
            .list();
        } catch (Exception e) {
            logger.error("Erreur DAO findAllWithDetails (AjustementStock)", e);
            return Collections.emptyList();
        }
    }
}
