package com.pharmacie.dao;

import com.pharmacie.models.PharmacieInfo;

public class PharmacieInfoDAO extends GenericDAO<PharmacieInfo> {
    
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
            e.printStackTrace();
            return null;
        }
    }
}
