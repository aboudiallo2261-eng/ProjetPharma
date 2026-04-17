package com.pharmacie.services;

import com.pharmacie.dao.LotDAO;
import com.pharmacie.dao.MouvementDAO;
import com.pharmacie.dao.ProduitDAO;
import com.pharmacie.dao.GenericDAO;
import com.pharmacie.models.LigneVente;
import com.pharmacie.models.Lot;
import com.pharmacie.models.MouvementStock;
import com.pharmacie.models.Produit;
import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.Vente;
import com.pharmacie.utils.SessionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service Métier dédié aux Ventes.
 * Extrait la logique métier hors du contrôleur pour respecter l'architecture MVC/Services.
 */
public class VenteService {

    private final ProduitDAO produitDAO = new ProduitDAO();
    private final LotDAO lotDAO = new LotDAO();
    private final GenericDAO<Vente> venteDAO = new GenericDAO<>(Vente.class);
    private final MouvementDAO mouvementDAO = new MouvementDAO();

    /**
     * Valide une vente, débite le stock selon la méthode FIFO, et trace les mouvements.
     * Cette méthode est hautement critique (Transaction métier).
     * 
     * @param panier Le panier d'articles à facturer
     * @param modePaiement Le mode de paiement sélectionné
     * @param currentSession La session de caisse en cours
     * @param cacheDispoStock Le cache des stocks pour la vérification concurrentielle JAT
     * @return La Vente enregistrée en base de données
     * @throws Exception En cas d'erreur de concurrence ou de stock insuffisant
     */
    public Vente validerVente(List<LigneVente> panier, Vente.ModePaiement modePaiement, Double montantEspeces, Double montantMobile, Double montantRecu, Double monnaieRendue, SessionCaisse currentSession, Map<Long, Integer> cacheDispoStock) throws Exception {
        if (panier == null || panier.isEmpty()) {
            throw new Exception("Le panier est vide.");
        }

        double grandTotal = 0;

        // 1. Vérification JAT (Just-In-Time) avant encaissement (Sécurité)
        java.util.Map<Long, Integer> requiredByProduct = new java.util.HashMap<>();
        for (LigneVente lv : panier) {
            int baseUnitsToDeduct = lv.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE && lv.getProduit().getEstDeconditionnable() != null && lv.getProduit().getEstDeconditionnable() 
                                    ? lv.getQuantiteVendue() * lv.getProduit().getUnitesParBoite() 
                                    : lv.getQuantiteVendue();
            requiredByProduct.put(lv.getProduit().getId(), requiredByProduct.getOrDefault(lv.getProduit().getId(), 0) + baseUnitsToDeduct);
            grandTotal += lv.getSousTotal();
        }

        for (java.util.Map.Entry<Long, Integer> entry : requiredByProduct.entrySet()) {
            int dispo = cacheDispoStock.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > dispo) {
                Produit p = produitDAO.findById(entry.getKey());
                throw new Exception("ALERTE MAJEURE DE CONCURRENCE :\nLe stock de [" + p.getNom() + "] a changé !\nRequis: " + entry.getValue() + " | Dispo restant: " + dispo + ".\n\nVeuillez ajuster le panier avant de valider.");
            }
        }

        // 2. Création de l'entité Vente
        Vente vente = new Vente();
        vente.setUser(SessionManager.getCurrentUser());
        vente.setDateVente(LocalDateTime.now());
        vente.setModePaiement(modePaiement);
        vente.setMontantEspeces(montantEspeces);
        vente.setMontantMobile(montantMobile);
        vente.setMontantRecu(montantRecu);
        vente.setMonnaieRendue(monnaieRendue);
        vente.setSessionCaisse(currentSession);
        vente.setLignesVente(new ArrayList<>());

        List<MouvementStock> auditTrailList = new ArrayList<>();

        // 3. Débit des lots (FIFO) et création du Livre de Bord (Audit Trail)
        for (LigneVente lv : panier) {
            int baseUnitsToDeduct = lv.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE && lv.getProduit().getEstDeconditionnable() != null && lv.getProduit().getEstDeconditionnable() 
                                    ? lv.getQuantiteVendue() * lv.getProduit().getUnitesParBoite() 
                                    : lv.getQuantiteVendue();
            
            // Logique d'expiration FIFO
            List<Lot> lotsDispos = lotDAO.findAll().stream()
                .filter(l -> l.getProduit().getId().equals(lv.getProduit().getId()) && l.getQuantiteStock() > 0)
                .filter(l -> l.getDateExpiration() == null || !l.getDateExpiration().isBefore(java.time.LocalDate.now()))
                .sorted((l1, l2) -> {
                    if (l1.getDateExpiration() == null) return 1;
                    if (l2.getDateExpiration() == null) return -1;
                    return l1.getDateExpiration().compareTo(l2.getDateExpiration());
                }).toList();

            for (Lot l : lotsDispos) {
                if (baseUnitsToDeduct <= 0) break;
                int taken = Math.min(l.getQuantiteStock(), baseUnitsToDeduct);
                l.setQuantiteStock(l.getQuantiteStock() - taken);
                // Archivage automatique si le lot est totalement épuisé
                if (l.getQuantiteStock() == 0) {
                    l.setEstArchive(true);
                }
                lotDAO.update(l);
                
                // Préparation LOG AUDIT TRAIL : VENTE
                auditTrailList.add(new MouvementStock(
                     lv.getProduit(),
                     l,
                     SessionManager.getCurrentUser(),
                     MouvementStock.TypeMouvement.VENTE,
                     -taken,
                     LocalDateTime.now(),
                     "" // Référence sera remplie juste après avoir eu l'ID de la Vente
                ));
                
                baseUnitsToDeduct -= taken;
                lv.setLot(l); // Lien avec le lot impacté
            }
            
            lv.setVente(vente);
            vente.getLignesVente().add(lv);
        }

        // 4. Persistence
        vente.setTotal(grandTotal);
        venteDAO.save(vente);
        
        // Finalisation Audit Trail avec la vraie Référence de Ticket standard (JJMMAAAA-HHMM-ID)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String ticketRef = String.format("TK-%02d%02d%04d-%02d%02d-%03d", 
                now.getDayOfMonth(), now.getMonthValue(), now.getYear(), 
                now.getHour(), now.getMinute(), vente.getId());
                
        for(MouvementStock mvt : auditTrailList) {
            mvt.setReference(ticketRef);
            mouvementDAO.save(mvt);
        }

        return vente;
    }
}
