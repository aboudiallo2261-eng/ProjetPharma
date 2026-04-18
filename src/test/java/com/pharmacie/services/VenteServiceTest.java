package com.pharmacie.services;

import com.pharmacie.dao.GenericDAO;
import com.pharmacie.dao.LotDAO;
import com.pharmacie.dao.MouvementDAO;
import com.pharmacie.dao.ProduitDAO;
import com.pharmacie.models.LigneVente;
import com.pharmacie.models.Lot;
import com.pharmacie.models.Produit;
import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.Vente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenteServiceTest {

    @Mock
    private ProduitDAO produitDAO;

    @Mock
    private LotDAO lotDAO;

    @Mock
    private GenericDAO<Vente> venteDAO;

    @Mock
    private MouvementDAO mouvementDAO;

    @InjectMocks
    private VenteService venteService;

    @BeforeEach
    void setupDDI() throws Exception {
        // Injection de dépendances via réflexion car VenteService instancie ses DAOs en dur.
        // Cela permet de tester la couche métier sans toucher à la base de données (Isolation)
        injectMock(venteService, "produitDAO", produitDAO);
        injectMock(venteService, "lotDAO", lotDAO);
        injectMock(venteService, "venteDAO", venteDAO);
        injectMock(venteService, "mouvementDAO", mouvementDAO);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    @Test
    void testValiderVenteRejettePanierVide() {
        assertThatThrownBy(() -> venteService.validerVente(
                new ArrayList<>(),
                Vente.ModePaiement.ESPECES,
                0.0, 0.0, 0.0, 0.0,
                new SessionCaisse(),
                new HashMap<>()
        )).hasMessageContaining("Le panier est vide");
    }

    @Test
    void testValiderVenteRejetteConcurrence() {
        // Arrange
        Produit p = new Produit();
        p.setId(1L);
        p.setNom("Paracetamol Vet");
        p.setEstDeconditionnable(false);

        LigneVente lv = new LigneVente();
        lv.setProduit(p);
        lv.setQuantiteVendue(10);
        lv.setTypeUnite(LigneVente.TypeUnite.BOITE_ENTIERE);
        lv.setPrixUnitaire(500.0);  // Obligatoire : getSousTotal() = prix * qte
        lv.setSousTotal(5000.0);    // Obligatoire : calcul du grandTotal en étape 1 du Service

        List<LigneVente> panier = List.of(lv);

        Map<Long, Integer> cacheDispo = new HashMap<>();
        cacheDispo.put(1L, 5); // 5 disponibles dans le cache, on en veut 10 -> Concurrence!

        when(produitDAO.findById(1L)).thenReturn(p);

        // Act & Assert
        assertThatThrownBy(() -> venteService.validerVente(
                panier,
                Vente.ModePaiement.ESPECES,
                5000.0, 0.0, 5000.0, 0.0,
                new SessionCaisse(),
                cacheDispo
        )).hasMessageContaining("ALERTE MAJEURE DE CONCURRENCE");
    }
}
