package com.pharmacie.services;

import com.pharmacie.dao.LotDAO;
import com.pharmacie.models.Achat;
import com.pharmacie.models.LigneAchat;
import com.pharmacie.models.Lot;
import com.pharmacie.models.MouvementStock;
import com.pharmacie.utils.HibernateUtil;
import com.pharmacie.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AchatService {

    private LotDAO lotDAO = new LotDAO();

    /**
     * Valide l'intégralité d'un achat dans une stricte transaction ACID.
     * En cas d'erreur à n'importe quelle étape, toutes les insertions sont annulées (rollback).
     *
     * Responsabilités de ce service :
     *  1. Créer l'entité Achat
     *  2. Créer ou compléter les Lots (déconditionnement si nécessaire)
     *  3. Créer les LignesAchat liées
     *  4. Enregistrer les MouvementStock (Audit Trail)
     *  5. Mettre à jour produit.prixAchat (référence pour la prochaine comparaison de marge)
     */
    public boolean enregistrerCommandeTransactionnelle(Achat achat, List<LigneAchat> panier) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            session.persist(achat);

            List<Lot> existingLotsInDB = lotDAO.findAll();

            for (LigneAchat l : panier) {

                // ── Déconditionnement ─────────────────────────────────────────
                int stockToAdd = l.getQuantiteAchetee();
                if (l.getProduit().getEstDeconditionnable() != null
                        && l.getProduit().getEstDeconditionnable()
                        && l.getProduit().getUnitesParBoite() != null) {
                    stockToAdd = stockToAdd * l.getProduit().getUnitesParBoite();
                }

                // ── Lot : existant ou nouveau ─────────────────────────────────
                Optional<Lot> existant = existingLotsInDB.stream()
                        .filter(lot -> lot.getProduit().getId().equals(l.getProduit().getId())
                                && lot.getNumeroLot().equals(l.getLot().getNumeroLot()))
                        .findFirst();

                Lot lotDb;
                if (existant.isPresent()) {
                    // POINT 1 : session.merge() remplace session.update() (deprecated Hibernate 6)
                    lotDb = session.merge(existant.get());
                    lotDb.setQuantiteStock(lotDb.getQuantiteStock() + stockToAdd);
                    if (lotDb.getQuantiteStock() > 0) {
                        lotDb.setEstArchive(false); // Désarchive si réalimenté
                    }
                } else {
                    lotDb = l.getLot();
                    lotDb.setQuantiteStock(stockToAdd);
                    session.persist(lotDb);
                }

                // ── Liaison bidirectionnelle ──────────────────────────────────
                l.setLot(lotDb);
                l.setAchat(achat);
                achat.getLignesAchat().add(l);
                session.persist(l);

                // ── Audit Trail (MouvementStock) ──────────────────────────────
                String ref = (achat.getReferenceFacture() != null && !achat.getReferenceFacture().isEmpty())
                        ? achat.getReferenceFacture()
                        : String.valueOf(achat.getId());
                MouvementStock mouvement = new MouvementStock(
                        l.getProduit(),
                        lotDb,
                        SessionManager.getCurrentUser(),
                        MouvementStock.TypeMouvement.ACHAT,
                        stockToAdd,
                        LocalDateTime.now(),
                        "Achat " + ref
                );
                session.persist(mouvement);

                // ── POINT 2 : Mise à jour produit.prixAchat (curseur de référence) ──
                // On met à jour uniquement la référence de comparaison future.
                // LigneAchat.prixUnitaire reste intact → l'historique par lot est préservé.
                com.pharmacie.models.Produit produitManaged = session.merge(l.getProduit());
                produitManaged.setPrixAchat(l.getPrixUnitaire());
            }

            transaction.commit();
            return true;

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }
}
