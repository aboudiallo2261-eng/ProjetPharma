package com.pharmacie.services;

import com.pharmacie.models.LigneVente;
import com.pharmacie.models.Lot;
import com.pharmacie.models.MouvementStock;
import com.pharmacie.models.Produit;
import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.Vente;
import com.pharmacie.utils.HibernateUtil;
import com.pharmacie.utils.SessionManager;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service Métier dédié aux Ventes.
 *
 * <p>Ce service orchestre l'enregistrement d'une vente complète en garantissant
 * l'atomicité et la cohérence des données via une <b>transaction Hibernate unique</b>.
 * Toute erreur déclenche un rollback intégral pour éviter tout état incohérent
 * (stock débité sans vente enregistrée, ou inversement).</p>
 *
 * <p>Opérations effectuées dans la même transaction ACID :
 * <ol>
 *   <li>Vérification de concurrence JAT (Just-In-Time)</li>
 *   <li>Débit FIFO des lots (First Expired First Out)</li>
 *   <li>Archivage automatique des lots épuisés</li>
 *   <li>Persistance de l'entité Vente avec ses LignesVente</li>
 *   <li>Enregistrement de l'Audit Trail (MouvementStock)</li>
 * </ol>
 *
 * @see com.pharmacie.models.Vente
 * @see com.pharmacie.models.Lot
 * @see com.pharmacie.models.MouvementStock
 */
public class VenteService {

    private static final Logger logger = LoggerFactory.getLogger(VenteService.class);

    /**
     * Valide une vente dans une stricte transaction ACID.
     * En cas d'erreur à n'importe quelle étape, toutes les modifications sont annulées (rollback).
     *
     * <p><b>Sécurité JAT</b> : Avant tout débit, le stock réel en base est comparé au cache
     * du contrôleur pour détecter toute modification concurrente survenue entre l'ajout au panier
     * et l'encaissement.</p>
     *
     * <p><b>FIFO (FEFO)</b> : Les lots sont débités par date d'expiration croissante
     * (le plus ancien/proche de péremption en premier). Les lots déjà expirés sont exclus.</p>
     *
     * @param panier          Le panier d'articles à facturer
     * @param modePaiement    Le mode de paiement sélectionné
     * @param montantEspeces  Montant NET en espèces (après déduction monnaie)
     * @param montantMobile   Montant par Mobile Money
     * @param montantRecu     Montant brut reçu du client
     * @param monnaieRendue   Monnaie rendue au client
     * @param currentSession  La session de caisse en cours
     * @param cacheDispoStock Le cache des stocks pour la vérification concurrentielle JAT
     * @return La Vente enregistrée en base de données
     * @throws Exception En cas d'erreur de concurrence, de stock insuffisant, ou de persistance
     */
    public Vente validerVente(List<LigneVente> panier, Vente.ModePaiement modePaiement,
                              Double montantEspeces, Double montantMobile,
                              Double montantRecu, Double monnaieRendue,
                              SessionCaisse currentSession,
                              Map<Long, Integer> cacheDispoStock) throws Exception {
        if (panier == null || panier.isEmpty()) {
            throw new Exception("Le panier est vide.");
        }

        // ── 1. Vérification JAT (Just-In-Time) AVANT ouverture de transaction ──
        // Compare le stock réel (cache du contrôleur) au stock requis par le panier.
        // Objectif : détecter toute modification concurrente survenue entre l'ajout
        // au panier et l'appui sur le bouton "Valider".
        double grandTotal = 0;
        Map<Long, Integer> requiredByProduct = new java.util.HashMap<>();
        for (LigneVente lv : panier) {
            int baseUnitsToDeduct = calculerUnitsDeBase(lv);
            requiredByProduct.merge(lv.getProduit().getId(), baseUnitsToDeduct, Integer::sum);
            grandTotal += lv.getSousTotal();
        }

        for (Map.Entry<Long, Integer> entry : requiredByProduct.entrySet()) {
            int dispo = cacheDispoStock.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > dispo) {
                // On utilise une session courte en lecture seule juste pour récupérer le nom
                String nomProduit = getNomProduit(entry.getKey());
                throw new Exception("ALERTE MAJEURE DE CONCURRENCE :\nLe stock de ["
                        + nomProduit + "] a changé !\nRequis: " + entry.getValue()
                        + " | Dispo restant: " + dispo
                        + ".\n\nVeuillez ajuster le panier avant de valider.");
            }
        }

        // ── 2. TRANSACTION ACID UNIQUE ──────────────────────────────────────────
        // Toutes les opérations ci-dessous sont dans UNE seule transaction Hibernate.
        // Si n'importe laquelle échoue, TOUT est annulé (rollback complet).
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            // 2a. Création de l'entité Vente
            Vente vente = new Vente();
            vente.setUser(SessionManager.getCurrentUser());
            vente.setDateVente(LocalDateTime.now());
            vente.setModePaiement(modePaiement);
            vente.setMontantEspeces(montantEspeces);
            vente.setMontantMobile(montantMobile);
            vente.setMontantRecu(montantRecu);
            vente.setMonnaieRendue(monnaieRendue);
            vente.setTotal(grandTotal);

            // Rattachement de la SessionCaisse à cette session Hibernate
            if (currentSession != null && currentSession.getId() != null) {
                SessionCaisse managedCaisse = session.get(SessionCaisse.class, currentSession.getId());
                vente.setSessionCaisse(managedCaisse);
            }

            vente.setLignesVente(new ArrayList<>());
            session.persist(vente);

            // 2b. Liste pour stocker les mouvements d'audit trail
            List<MouvementStock> auditTrailList = new ArrayList<>();
            LocalDate today = LocalDate.now();

            // 2c. Débit des lots (FIFO/FEFO) — requête HQL ciblée par produit
            for (LigneVente lv : panier) {
                int baseUnitsToDeduct = calculerUnitsDeBase(lv);

                // Requête HQL FEFO ciblée DANS la session transactionnelle
                // Remplace l'ancien lotDAO.findAll().stream().filter() qui chargeait TOUS les lots
                List<Lot> lotsDispos = session.createQuery(
                        "FROM Lot l WHERE l.produit.id = :produitId " +
                        "AND l.quantiteStock > 0 " +
                        "AND (l.dateExpiration IS NULL OR l.dateExpiration >= :today) " +
                        "ORDER BY l.dateExpiration ASC NULLS LAST",
                        Lot.class)
                    .setParameter("produitId", lv.getProduit().getId())
                    .setParameter("today", today)
                    .list();

                for (Lot lot : lotsDispos) {
                    if (baseUnitsToDeduct <= 0) break;
                    int taken = Math.min(lot.getQuantiteStock(), baseUnitsToDeduct);

                    lot.setQuantiteStock(lot.getQuantiteStock() - taken);

                    // Archivage automatique si le lot est totalement épuisé
                    if (lot.getQuantiteStock() == 0) {
                        lot.setEstArchive(true);
                    }

                    // session.merge() n'est plus nécessaire : le lot est déjà managé par la session
                    // car chargé par la requête HQL ci-dessus. Hibernate détecte automatiquement
                    // les modifications (dirty checking) et les persiste au commit.

                    // Préparation Audit Trail
                    MouvementStock mvt = new MouvementStock(
                            lv.getProduit(),
                            lot,
                            SessionManager.getCurrentUser(),
                            MouvementStock.TypeMouvement.VENTE,
                            -taken,
                            LocalDateTime.now(),
                            lv.getTypeUnite().name() // Temporaire, sera remplacé par la ref ticket
                    );
                    auditTrailList.add(mvt);

                    baseUnitsToDeduct -= taken;
                    lv.setLot(lot); // Lien avec le dernier lot impacté
                }

                if (baseUnitsToDeduct > 0) {
                    // Sécurité : si le stock ne suffit pas malgré le JAT, on rollback
                    throw new Exception("Stock insuffisant pour " + lv.getProduit().getNom()
                            + " (manque " + baseUnitsToDeduct + " unités). Transaction annulée.");
                }

                lv.setVente(vente);
                vente.getLignesVente().add(lv);
                session.persist(lv);
            }

            // 2d. Finalisation Audit Trail avec la vraie Référence de Ticket
            LocalDateTime now = LocalDateTime.now();
            String ticketRef = String.format("TK-%02d%02d%04d-%02d%02d-%03d",
                    now.getDayOfMonth(), now.getMonthValue(), now.getYear(),
                    now.getHour(), now.getMinute(), vente.getId());

            for (MouvementStock mvt : auditTrailList) {
                String tempType = mvt.getReference();
                String suffix = "DETAIL".equals(tempType) ? " (Vente au Détail)" : " (Vente en Boîte)";
                mvt.setReference(ticketRef + suffix);
                session.persist(mvt);
            }

            // ── COMMIT : Tout ou Rien ─────────────────────────────────────────
            transaction.commit();
            logger.info("Transaction ACID Vente réussie — Ticket {} | Total: {} FCFA | {} article(s)",
                    ticketRef, grandTotal, panier.size());

            return vente;

        } catch (Exception e) {
            // ── ROLLBACK COMPLET ──────────────────────────────────────────────
            // Si QUOI QUE CE SOIT échoue (lot, vente, mouvement), RIEN n'est persisté.
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
                logger.warn("Rollback complet de la transaction Vente effectué.");
            }
            logger.error("Échec transaction ACID Vente", e);
            throw e; // Re-throw pour que le contrôleur affiche l'erreur à l'utilisateur
        }
    }

    /**
     * Calcule le nombre d'unités de base à débiter pour une ligne de vente.
     * Si le produit est déconditionnable et vendu en boîte entière,
     * la quantité est multipliée par le nombre d'unités par boîte.
     */
    private int calculerUnitsDeBase(LigneVente lv) {
        if (lv.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE
                && lv.getProduit().getEstDeconditionnable() != null
                && lv.getProduit().getEstDeconditionnable()) {
            return lv.getQuantiteVendue() * lv.getProduit().getUnitesParBoite();
        }
        return lv.getQuantiteVendue();
    }

    /**
     * Récupère le nom d'un produit par son ID (session courte en lecture seule).
     * Utilisé uniquement pour les messages d'erreur de concurrence.
     */
    private String getNomProduit(Long produitId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Produit p = session.get(Produit.class, produitId);
            return p != null ? p.getNom() : "ID:" + produitId;
        } catch (Exception e) {
            return "ID:" + produitId;
        }
    }
}
