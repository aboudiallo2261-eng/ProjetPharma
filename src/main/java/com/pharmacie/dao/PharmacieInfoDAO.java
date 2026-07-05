package com.pharmacie.dao;

import com.pharmacie.models.PharmacieInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PharmacieInfoDAO extends GenericDAO<PharmacieInfo> {

    private static final Logger logger = LoggerFactory.getLogger(PharmacieInfoDAO.class);
    
    public PharmacieInfoDAO() {
        super(PharmacieInfo.class);
    }

    // Récupère toujours le premier enregistrement (Singleton en base)
    public PharmacieInfo getInfo() {
        try (var session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM PharmacieInfo", PharmacieInfo.class)
                    .setMaxResults(1)
                    .uniqueResult();
        } catch (Exception e) {
            logger.error("Erreur DAO getInfo", e);
            return null;
        }
    }
}
