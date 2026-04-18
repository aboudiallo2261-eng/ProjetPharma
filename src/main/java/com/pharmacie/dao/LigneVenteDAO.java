package com.pharmacie.dao;

import com.pharmacie.models.LigneVente;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.hibernate.query.Query;

public class LigneVenteDAO extends GenericDAO<LigneVente> {

    private static final Logger log = LoggerFactory.getLogger(LigneVenteDAO.class);

    public LigneVenteDAO() {
        super(LigneVente.class);
    }

    /**
     * Charge toutes les lignes de vente avec leurs associations (Vente, User, Produit,
     * Catégorie, Espèce) en une seule requête SQL via JOIN FETCH.
     * <p>
     * Nécessaire pour deux raisons :
     * <ol>
     *   <li><b>LazyInitializationException</b> : Hibernate ferme la session après {@code findAll()}.
     *       Toute tentative d'accès à une association lazy (ex: {@code lv.getProduit().getCategorie()})
     *       depuis un thread extérieur à la session lève une exception silencieuse.</li>
     *   <li><b>Problème N+1</b> : Sans JOIN FETCH, Hibernate émet une requête SQL par ligne
     *       pour charger chaque association, tuant les performances sur de grands ensembles.</li>
     * </ol>
     *
     * @return liste complète des lignes de vente avec associations initialisées,
     *         ou liste vide en cas d'erreur.
     */
    public List<LigneVente> findAllWithDetails() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT DISTINCT lv FROM LigneVente lv " +
                "JOIN FETCH lv.vente v " +
                "JOIN FETCH v.user " +
                "JOIN FETCH lv.produit p " +
                "JOIN FETCH p.categorie " +
                "JOIN FETCH p.espece " +
                "ORDER BY v.dateVente DESC",
                LigneVente.class)
            .list();
        } catch (Exception e) {
            log.error("[LigneVenteDAO] Erreur lors du chargement des lignes de vente avec détails", e);
            return Collections.emptyList();
        }
    }

    /**
     * Recherche optimisée côté base de données (Serveur) pour le Livre de Bord.
     * Génère dynamiquement la requête HQL pour n'extraire et ne charger en RAM
     * que les données strictement filtrées au lieu de toute la table.
     */
    public List<LigneVente> rechercherLignesVente(LocalDateTime start, LocalDateTime end, Long userId, Long categorieId, Long especeId, Long produitId) {
        if (start == null || end == null) return Collections.emptyList();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder(
                "SELECT DISTINCT lv FROM LigneVente lv " +
                "JOIN FETCH lv.vente v " +
                "JOIN FETCH v.user u " +
                "JOIN FETCH lv.produit p " +
                "JOIN FETCH p.categorie c " +
                "JOIN FETCH p.espece e " +
                "WHERE v.dateVente >= :start AND v.dateVente < :end "
            );

            if (userId != null) {
                hql.append("AND u.id = :userId ");
            }
            if (categorieId != null) {
                hql.append("AND c.id = :categorieId ");
            }
            if (especeId != null) {
                hql.append("AND e.id = :especeId ");
            }
            if (produitId != null) {
                hql.append("AND p.id = :produitId ");
            }

            hql.append("ORDER BY v.dateVente DESC");

            Query<LigneVente> query = session.createQuery(hql.toString(), LigneVente.class);
            query.setParameter("start", start);
            query.setParameter("end", end);

            if (userId != null) query.setParameter("userId", userId);
            if (categorieId != null) query.setParameter("categorieId", categorieId);
            if (especeId != null) query.setParameter("especeId", especeId);
            if (produitId != null) query.setParameter("produitId", produitId);

            return query.list();
        } catch (Exception e) {
            log.error("Erreur DAO rechercherLignesVente optimisée", e);
            return Collections.emptyList();
        }
    }
}
