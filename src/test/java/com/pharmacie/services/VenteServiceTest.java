package com.pharmacie.services;

import com.pharmacie.models.LigneVente;
import com.pharmacie.models.Produit;
import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.Vente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires pour VenteService — Couche métier critique.
 *
 * <p>Stratégie de test : ces tests valident la logique métier PRÉ-TRANSACTIONNELLE
 * (vérification JAT, validation du panier) sans nécessiter de connexion à la base
 * de données. La logique transactionnelle ACID est validée par les tests d'intégration.</p>
 *
 * @see VenteService
 */
class VenteServiceTest {

    private final VenteService venteService = new VenteService();

    // ═══════════════════════════════════════════════════════════════════
    // VALIDATION DU PANIER
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Rejette un panier vide avec un message explicite")
    void testValiderVenteRejettePanierVide() {
        assertThatThrownBy(() -> venteService.validerVente(
                new ArrayList<>(),
                Vente.ModePaiement.ESPECES,
                0.0, 0.0, 0.0, 0.0,
                new SessionCaisse(),
                new HashMap<>()
        )).isInstanceOf(Exception.class)
          .hasMessageContaining("Le panier est vide");
    }

    @Test
    @DisplayName("Rejette un panier null avec un message explicite")
    void testValiderVenteRejettePanierNull() {
        assertThatThrownBy(() -> venteService.validerVente(
                null,
                Vente.ModePaiement.ESPECES,
                0.0, 0.0, 0.0, 0.0,
                new SessionCaisse(),
                new HashMap<>()
        )).isInstanceOf(Exception.class)
          .hasMessageContaining("Le panier est vide");
    }

    // ═══════════════════════════════════════════════════════════════════
    // VÉRIFICATION DE CONCURRENCE JAT (Just-In-Time)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Détecte une rupture de stock concurrentielle (JAT)")
    void testValiderVenteDetecteConcurrenceJAT() {
        // Arrange : le client veut 10 boîtes, mais le cache dit qu'il n'en reste que 5
        Produit p = creerProduitSimple(1L, "Amoxicilline Vet 250mg");

        LigneVente lv = new LigneVente();
        lv.setProduit(p);
        lv.setQuantiteVendue(10);
        lv.setTypeUnite(LigneVente.TypeUnite.BOITE_ENTIERE);
        lv.setPrixUnitaire(1500.0);
        lv.setSousTotal(15000.0);

        Map<Long, Integer> cacheDispo = new HashMap<>();
        cacheDispo.put(1L, 5); // ← Seulement 5 en cache, on en veut 10

        // Act & Assert : doit lever une alerte de concurrence
        assertThatThrownBy(() -> venteService.validerVente(
                List.of(lv),
                Vente.ModePaiement.ESPECES,
                15000.0, 0.0, 15000.0, 0.0,
                new SessionCaisse(),
                cacheDispo
        )).isInstanceOf(Exception.class)
          .hasMessageContaining("CONCURRENCE");
    }

    @Test
    @DisplayName("Détecte la concurrence avec déconditionnement (boîtes → unités)")
    void testConcurrenceAvecDeconditionnement() {
        // Arrange : produit déconditionnable, 10 unités par boîte
        // Le client demande 3 boîtes = 30 unités, mais il n'en reste que 20
        Produit p = creerProduitDeconditionnable(2L, "Vaccin Rabique", 10);

        LigneVente lv = new LigneVente();
        lv.setProduit(p);
        lv.setQuantiteVendue(3); // 3 boîtes = 30 unités de base
        lv.setTypeUnite(LigneVente.TypeUnite.BOITE_ENTIERE);
        lv.setPrixUnitaire(5000.0);
        lv.setSousTotal(15000.0);

        Map<Long, Integer> cacheDispo = new HashMap<>();
        cacheDispo.put(2L, 20); // ← 20 unités, il en faut 30

        // Act & Assert
        assertThatThrownBy(() -> venteService.validerVente(
                List.of(lv),
                Vente.ModePaiement.MOBILE_MONEY,
                0.0, 15000.0, 15000.0, 0.0,
                new SessionCaisse(),
                cacheDispo
        )).isInstanceOf(Exception.class)
          .hasMessageContaining("CONCURRENCE");
    }

    @Test
    @DisplayName("Agrège correctement les quantités multi-lignes du même produit")
    void testConcurrenceMultiLignesMêmeProduit() {
        // Arrange : deux lignes du même produit, total dépasse le stock
        Produit p = creerProduitSimple(3L, "Ivermectine 1%");

        LigneVente lv1 = new LigneVente();
        lv1.setProduit(p);
        lv1.setQuantiteVendue(3);
        lv1.setTypeUnite(LigneVente.TypeUnite.BOITE_ENTIERE);
        lv1.setPrixUnitaire(2000.0);
        lv1.setSousTotal(6000.0);

        LigneVente lv2 = new LigneVente();
        lv2.setProduit(p);
        lv2.setQuantiteVendue(5);
        lv2.setTypeUnite(LigneVente.TypeUnite.BOITE_ENTIERE);
        lv2.setPrixUnitaire(2000.0);
        lv2.setSousTotal(10000.0);

        Map<Long, Integer> cacheDispo = new HashMap<>();
        cacheDispo.put(3L, 6); // ← 6 dispo, 3+5=8 demandés

        // Act & Assert : la somme (8) dépasse le stock (6)
        assertThatThrownBy(() -> venteService.validerVente(
                List.of(lv1, lv2),
                Vente.ModePaiement.ESPECES,
                16000.0, 0.0, 16000.0, 0.0,
                new SessionCaisse(),
                cacheDispo
        )).isInstanceOf(Exception.class)
          .hasMessageContaining("CONCURRENCE");
    }

    @Test
    @DisplayName("Vente au DÉTAIL : quantité comptée en unités, sans multiplication par boîte")
    void testVenteDetailSansMultiplication() {
        // Produit déconditionnable (12 unités/boîte) vendu au DÉTAIL :
        // 5 unités demandées ne doivent PAS devenir 5×12=60.
        Produit p = creerProduitDeconditionnable(4L, "Amoxicilline 250mg", 12);

        LigneVente lv = new LigneVente();
        lv.setProduit(p);
        lv.setQuantiteVendue(5);
        lv.setTypeUnite(LigneVente.TypeUnite.DETAIL);
        lv.setPrixUnitaire(200.0);
        lv.setSousTotal(1000.0);

        Map<Long, Integer> cacheDispo = new HashMap<>();
        cacheDispo.put(4L, 4); // ← 4 unités dispo, 5 demandées → concurrence

        assertThatThrownBy(() -> venteService.validerVente(
                List.of(lv),
                Vente.ModePaiement.ESPECES,
                1000.0, 0.0, 1000.0, 0.0,
                new SessionCaisse(),
                cacheDispo
        )).isInstanceOf(Exception.class)
          .hasMessageContaining("Requis: 5"); // bien 5 unités, pas 60
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPERS — Création de produits de test
    // ═══════════════════════════════════════════════════════════════════

    private Produit creerProduitSimple(Long id, String nom) {
        Produit p = new Produit();
        p.setId(id);
        p.setNom(nom);
        p.setEstDeconditionnable(false);
        return p;
    }

    private Produit creerProduitDeconditionnable(Long id, String nom, int unitesParBoite) {
        Produit p = new Produit();
        p.setId(id);
        p.setNom(nom);
        p.setEstDeconditionnable(true);
        p.setUnitesParBoite(unitesParBoite);
        return p;
    }
}
