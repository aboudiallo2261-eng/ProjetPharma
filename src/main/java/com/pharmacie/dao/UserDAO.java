package com.pharmacie.dao;

import com.pharmacie.models.User;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDAO extends GenericDAO<User> {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public UserDAO() {
        super(User.class);
    }

    public User findByIdentifiant(String identifiant) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("from User where identifiant = :ident", User.class);
            query.setParameter("ident", identifiant);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Erreur DAO findByIdentifiant pour '{}'", identifiant, e);
            return null;
        }
    }
}
