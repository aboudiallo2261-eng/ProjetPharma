package com.pharmacie.dao;

import com.pharmacie.models.Vente;
import com.pharmacie.models.Produit;
import com.pharmacie.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.List;

public class VenteDAO extends GenericDAO<Vente> {

    public VenteDAO() {
        super(Vente.class);
    }

    /**
     * Recherche optimisée des ventes (Filtre côté Serveur DB)
     * Empêche l'engorgement de la RAM (Out Of Memory) en rapatriant uniquement 
     * les factures de la période ciblée.
     * 
     * @param debut Date de début
     * @param fin Date de fin
     * @param produitFilter Produit optionnel pour affiner (null si sans filtre)
     * @return Liste de ventes correspondant aux critères
     */
    public List<Vente> findVentesByPeriode(LocalDateTime debut, LocalDateTime fin, Produit produitFilter, com.pharmacie.models.User agentFilter, com.pharmacie.models.Vente.ModePaiement modeFilter) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("SELECT DISTINCT v FROM Vente v ");
            
            if (produitFilter != null) {
                hql.append("JOIN v.lignesVente lv WHERE lv.produit.id = :produitId AND ");
            } else {
                hql.append("WHERE ");
            }
            hql.append("v.dateVente BETWEEN :debut AND :fin ");
            
            if (agentFilter != null) {
                hql.append("AND v.user.id = :agentId ");
            }
            if (modeFilter != null) {
                hql.append("AND v.modePaiement = :modePaiement ");
            }
            
            hql.append("ORDER BY v.dateVente DESC");
            
            Query<Vente> query = session.createQuery(hql.toString(), Vente.class);
            query.setParameter("debut", debut);
            query.setParameter("fin", fin);
            
            if (produitFilter != null) {
                query.setParameter("produitId", produitFilter.getId());
            }
            if (agentFilter != null) {
                query.setParameter("agentId", agentFilter.getId());
            }
            if (modeFilter != null) {
                query.setParameter("modePaiement", modeFilter);
            }
            
            return query.list();
        }
    }
}
