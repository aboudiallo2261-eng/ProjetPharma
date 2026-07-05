package com.pharmacie.dao;

import com.pharmacie.models.Profil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfilDAO extends GenericDAO<Profil> {

    private static final Logger logger = LoggerFactory.getLogger(ProfilDAO.class);
    
    public ProfilDAO() {
        super(Profil.class);
    }

    public Profil findByNom(String nom) {
        try (var session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Profil WHERE nom = :nom", Profil.class)
                    .setParameter("nom", nom)
                    .uniqueResult();
        } catch (Exception e) {
            logger.error("Erreur DAO findByNom pour '{}'", nom, e);
            return null;
        }
    }
}
