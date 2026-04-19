package com.pharmacie.dao;

import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO générique paramétrique fournissant les opérations CRUD de base pour toutes les entités.
 *
 * <p>Chaque opération (save, update, delete) est encapsulée dans sa propre transaction
 * Hibernate avec gestion automatique du rollback en cas d'erreur.</p>
 *
 * <p>Les DAOs spécialisés ({@link LigneVenteDAO}, {@link LotDAO}, etc.) étendent cette
 * classe pour ajouter leurs requêtes HQL métier spécifiques.</p>
 *
 * @param <T> le type de l'entité JPA gérée
 */
public class GenericDAO<T> {
    private static final Logger logger = LoggerFactory.getLogger(GenericDAO.class);
    private final Class<T> type;

    /**
     * @param type La classe de l'entité JPA gérée (ex: {@code Produit.class}).
     */
    public GenericDAO(Class<T> type) {
        this.type = type;
    }

    /**
     * Persiste une nouvelle entité en base de données (INSERT).
     * @param entity L'entité à sauvegarder. Son ID sera généré automatiquement
     *               après persistance ({@code @GeneratedValue}).
     */
    public void save(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(entity);
            transaction.commit();
        } catch (Exception e) {
            // Toujours logger l'erreur complète pour l'auditabilité technique
            logger.error("Erreur critique BDD (save) pour [{}] - Transaction annulée.", type.getSimpleName(), e);
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (Exception rbe) {
                    logger.error("Erreur secondaire lors du rollback (save {}): {}", type.getSimpleName(), rbe.getMessage());
                }
            }
            // Propager l'exception pour que l'appelant (Service, Controller) sache que la sauvegarde a échoué.
            // Sans ce re-throw, l'interface affichait un faux succès (toast vert) alors que la donnée n'est pas persistee.
            throw new RuntimeException("Erreur de persistance [" + type.getSimpleName() + "] : " + e.getMessage(), e);
        }
    }

    /**
     * Met à jour une entité existante en base de données (UPDATE via Hibernate merge).
     * @param entity L'entité détachée de la session Hibernate à re-rattacher et mettre à jour.
     */
    public void update(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(entity);
            transaction.commit();
        } catch (Exception e) {
            logger.error("Erreur critique BDD (update) pour [{}] - Transaction annulée.", type.getSimpleName(), e);
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (Exception rbe) {
                    logger.error("Erreur secondaire lors du rollback (update {}): {}", type.getSimpleName(), rbe.getMessage());
                }
            }
            throw new RuntimeException("Erreur de mise à jour [" + type.getSimpleName() + "] : " + e.getMessage(), e);
        }
    }

    /**
     * Supprime une entité en base de données.
     * En cas de violation de clé étrangère (ex: fournisseur lié à des achats),
     * retourne {@code false} sans lever d'exception.
     * @param entity L'entité à supprimer.
     * @return {@code true} si supprimée, {@code false} si contràint (FK violation).
     */
    public boolean delete(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(session.contains(entity) ? entity : session.merge(entity));
            transaction.commit();
            return true;
        } catch (Exception e) {
            logger.error("Erreur BDD (delete) pour " + type.getSimpleName(), e);
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch(Exception rbe) { }
            }
            return false;
        }
    }

    /**
     * Recherche une entité par sa clé primaire.
     * @param id L'identifiant unique de l'entité.
     * @return L'entité trouvée ou {@code null} si aucun enregistrement ne correspond.
     */
    public T findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(type, id);
        }
    }

    /**
     * Récupère tous les enregistrements de l'entité.
     * <p><b>⚠️ Attention :</b> à utiliser avec précaution sur les tables volumineuses.
     * Préférez les DAOs spécialisés avec pagination ou filtrage HQL dédié.</p>
     * @return La liste complète des entités ou une liste vide si la table est vide.
     */
    public List<T> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<T> query = session.createQuery("from " + type.getName(), type);
            return query.list();
        }
    }
}
