package com.pharmacie.dao;

import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.util.List;

public class GenericDAO<T> {
    private final Class<T> type;

    public GenericDAO(Class<T> type) {
        this.type = type;
    }

    public void save(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(entity);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("===== ERREUR SQL ROOT =====");
            e.printStackTrace();
            System.err.println("===========================");
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch(Exception rbe) {
                    System.err.println("Rollback exception (secondaire): " + rbe.getMessage());
                }
            }
        }
    }

    public void update(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(entity);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("===== ERREUR SQL ROOT =====");
            e.printStackTrace();
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch(Exception rbe) { }
            }
        }
    }

    public boolean delete(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(session.contains(entity) ? entity : session.merge(entity));
            transaction.commit();
            return true;
        } catch (Exception e) {
            System.err.println("===== ERREUR SQL ROOT =====");
            e.printStackTrace();
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch(Exception rbe){}
            }
            return false;
        }
    }

    public T findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(type, id);
        }
    }

    public List<T> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<T> query = session.createQuery("from " + type.getName(), type);
            return query.list();
        }
    }
}
