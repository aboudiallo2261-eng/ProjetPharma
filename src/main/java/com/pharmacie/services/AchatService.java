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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service métier dédié aux Approvisionnements Fournisseurs.
 *
 * <p>Ce service orchestre l'enregistrement d'une commande complète en garantissant
 * l'atomicité et la cohérence des données via une transaction Hibernate unique.
 * Toute erreur déclenche un rollback intégral pour éviter tout état incohérent.</p>
 *
 * @see com.pharmacie.models.Achat
 * @see com.pharmacie.models.Lot
 * @see com.pharmacie.models.MouvementStock
 */
public class AchatService {

    private static final Logger logger = LoggerFactory.getLogger(AchatService.class);
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
    /**
     * Enregistre une commande fournisseur complète dans une transaction ACID stricte.
     *
     * <p>Opérations effectuées dans la même transaction :
     * <ol>
     *   <li><b>Achat</b> : Persist l'en-tête de commande (référence facture, date).</li>
     *   <li><b>Lots</b> : Crée un nouveau lot OU réalimente un lot existant (merge intelligent).
     *       Désarchive automatiquement un lot remis à zéro.</li>
     *   <li><b>Déconditionnement</b> : Convertit les boîtes en unités selon {@code unitesParBoite}
     *       si le produit est déconditionnable.</li>
     *   <li><b>LignesAchat</b> : Lie chaque ligne à l'achat et au lot correspondant.</li>
     *   <li><b>Audit Trail</b> : Enregistre un {@link com.pharmacie.models.MouvementStock}
     *       de type {@code ACHAT} pour chaque lot touché.</li>
     *   <li><b>Prix de référence</b> : Met à jour {@code produit.prixAchat} pour le suivi
     *       de marge (sans modifier l'historique des lignes).</li>
     * </ol>
     *
     * <p>En cas d'exception à n'importe quelle étape, un <b>rollback complet</b> est exécuté.
     * Aucune donnée partielle ne peut rester en base.
     *
     * @param achat  L'entité {@link com.pharmacie.models.Achat} pré-remplie (fournisseur, date, référence).
     * @param panier La liste des {@link com.pharmacie.models.LigneAchat} constituant la commande.
     * @return {@code true} si toute la transaction s'est terminée avec succès, {@code false} sinon.
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
            logger.error("Erreur Transaction ACID sur enregistrerCommandeTransactionnelle", e);
            return false;
        }
    }
}
