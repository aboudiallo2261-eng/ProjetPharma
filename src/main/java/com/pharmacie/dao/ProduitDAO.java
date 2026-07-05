package com.pharmacie.dao;

import com.pharmacie.models.Produit;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class ProduitDAO extends GenericDAO<Produit> {

    private static final Logger logger = LoggerFactory.getLogger(ProduitDAO.class);

    public ProduitDAO() {
        super(Produit.class);
    }

    public List<Produit> rechercherParNom(String nom) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Produit> query = session.createQuery("from Produit p where lower(p.nom) like lower(:nom)", Produit.class);
            query.setParameter("nom", "%" + nom + "%");
            return query.list();
        } catch (Exception e) {
            logger.error("Erreur DAO rechercherParNom pour '{}'", nom, e);
            return null;
        }
    }
}
