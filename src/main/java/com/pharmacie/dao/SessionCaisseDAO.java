package com.pharmacie.dao;

import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.User;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Dernière session ouverte dans le temps, close ou non.
     *
     * C'est elle qui permet de distinguer un silence de fermeture d'un silence
     * de panne : une journée qui se termine par une clôture laisse une session
     * FERMEE, une coupure laisse une session restée OUVERTE. La supervision à
     * distance s'appuie sur cette différence, faute de connaître les horaires
     * d'ouverture — qui varient d'une semaine à l'autre.
     *
     * L'utilisateur est chargé dans la foulée : la session est lue hors de
     * toute transaction ouverte, et y accéder plus tard lèverait une
     * LazyInitializationException.
     */
    public Optional<SessionCaisse> findDerniereSession() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SessionCaisse> query = session.createQuery(
                "FROM SessionCaisse s LEFT JOIN FETCH s.user ORDER BY s.dateOuverture DESC",
                SessionCaisse.class);
            query.setMaxResults(1);
            return query.uniqueResultOptional();
        }
    }

    /**
     * Sessions ouvertes depuis une date, de la plus récente à la plus ancienne.
     * Sert au cumul des écarts et au décompte des journées sans clôture.
     */
    public List<SessionCaisse> findSessionsDepuis(LocalDateTime depuis) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SessionCaisse> query = session.createQuery(
                "FROM SessionCaisse s LEFT JOIN FETCH s.user WHERE s.dateOuverture >= :depuis "
                    + "ORDER BY s.dateOuverture DESC",
                SessionCaisse.class);
            query.setParameter("depuis", depuis);
            return query.getResultList();
        }
    }
}
