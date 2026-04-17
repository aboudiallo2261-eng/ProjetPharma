package com.pharmacie.dao;

import com.pharmacie.models.Achat;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;

import java.util.Collections;
import java.util.List;

public class AchatDAO extends GenericDAO<Achat> {
    public AchatDAO() {
        super(Achat.class);
    }

    public List<Achat> findAllWithDetails() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Utiliser JOIN FETCH pour récupérer proprement les entités associées d'un seul coup (Eviter N+1)
            // DISTINCT est nécessaire pour éviter les doublons quand on FETCH une collection (@OneToMany)
            return session.createQuery(
                "SELECT DISTINCT a FROM Achat a " +
                "JOIN FETCH a.fournisseur f " +
                "LEFT JOIN FETCH a.lignesAchat la " +
                "LEFT JOIN FETCH la.produit p " +
                "LEFT JOIN FETCH la.lot l " +
                "ORDER BY a.dateAchat DESC", Achat.class)
            .list();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
