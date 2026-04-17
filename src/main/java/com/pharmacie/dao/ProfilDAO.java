package com.pharmacie.dao;

import com.pharmacie.models.Profil;

public class ProfilDAO extends GenericDAO<Profil> {
    
    public ProfilDAO() {
        super(Profil.class);
    }

    // Vous pouvez rajouter des méthodes spécifiques, par exemple findByName
    public Profil findByNom(String nom) {
        try (var session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Profil WHERE nom = :nom", Profil.class)
                    .setParameter("nom", nom)
                    .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
