package com.pharmacie.dao;

import com.pharmacie.models.AjustementStock;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;

import java.util.Collections;
import java.util.List;

public class AjustementStockDAO extends GenericDAO<AjustementStock> {

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
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
