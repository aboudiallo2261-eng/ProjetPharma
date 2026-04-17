package com.pharmacie.dao;

import com.pharmacie.models.Produit;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.List;

public class ProduitDAO extends GenericDAO<Produit> {
    public ProduitDAO() {
        super(Produit.class);
    }

    public List<Produit> rechercherParNom(String nom) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Produit> query = session.createQuery("from Produit p where lower(p.nom) like lower(:nom)", Produit.class);
            query.setParameter("nom", "%" + nom + "%");
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
