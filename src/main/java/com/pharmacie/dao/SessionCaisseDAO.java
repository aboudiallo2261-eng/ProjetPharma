package com.pharmacie.dao;

import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.User;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Optional;

public class SessionCaisseDAO extends GenericDAO<SessionCaisse> {

    public SessionCaisseDAO() {
        super(SessionCaisse.class);
    }

    public Optional<SessionCaisse> findSessionOuverteByUser(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SessionCaisse> query = session.createQuery(
                "FROM SessionCaisse s WHERE s.user.id = :userId AND s.statut = 'OUVERTE'", SessionCaisse.class);
            query.setParameter("userId", user.getId());
            return query.uniqueResultOptional();
        }
    }
}
