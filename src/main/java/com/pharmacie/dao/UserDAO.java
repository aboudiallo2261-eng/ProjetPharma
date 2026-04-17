package com.pharmacie.dao;

import com.pharmacie.models.User;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class UserDAO extends GenericDAO<User> {
    public UserDAO() {
        super(User.class);
    }

    public User findByIdentifiant(String identifiant) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("from User where identifiant = :ident", User.class);
            query.setParameter("ident", identifiant);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
