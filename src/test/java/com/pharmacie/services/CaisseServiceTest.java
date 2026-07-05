package com.pharmacie.services;

import com.pharmacie.dao.VenteDAO;
import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.Vente;
import com.pharmacie.services.CaisseService.BilanCloture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires des calculs d'argent de la clôture Z — AUCUN accès base de données.
 *
 * <p>Le cœur du métier caisse : espèces pures, mobile pur, décomposition des
 * paiements MIXTES, théorie du tiroir et écart de comptage. Une erreur ici,
 * c'est de l'argent qui disparaît des totaux — d'où cette couverture dédiée.</p>
 */
@ExtendWith(MockitoExtension.class)
class CaisseServiceTest {

    @Mock
    private VenteDAO venteDAO;

    // ═══════════════════════════════════════════════════════════════════
    // CALCUL PUR — calculerBilan (sans DAO)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Session sans vente : totaux à zéro, tiroir = fond de caisse")
    void testBilanSessionVide() {
        BilanCloture bilan = CaisseService.calculerBilan(25000, Collections.emptyList());

        assertThat(bilan.totalEspeces()).isZero();
        assertThat(bilan.totalMobile()).isZero();
        assertThat(bilan.nbVentesMixtes()).isZero();
        assertThat(bilan.especesAttenduesTiroir()).isEqualTo(25000);
    }

    @Test
    @DisplayName("Ventes espèces pures : tout entre au tiroir")
    void testBilanEspecesPures() {
        List<Vente> ventes = List.of(
                venteEspeces(1500.0),
                venteEspeces(3500.0));

        BilanCloture bilan = CaisseService.calculerBilan(10000, ventes);

        assertThat(bilan.ventesPuresEspeces()).isEqualTo(5000);
        assertThat(bilan.totalEspeces()).isEqualTo(5000);
        assertThat(bilan.totalMobile()).isZero();
        assertThat(bilan.especesAttenduesTiroir()).isEqualTo(15000);
    }

    @Test
    @DisplayName("Ventes Mobile Money pures : rien n'entre au tiroir physique")
    void testBilanMobilePur() {
        List<Vente> ventes = List.of(venteMobile(8000.0));

        BilanCloture bilan = CaisseService.calculerBilan(10000, ventes);

        assertThat(bilan.totalMobile()).isEqualTo(8000);
        assertThat(bilan.totalEspeces()).isZero();
        // Le tiroir ne doit PAS gonfler avec l'argent digital
        assertThat(bilan.especesAttenduesTiroir()).isEqualTo(10000);
    }

    @Test
    @DisplayName("Paiement MIXTE : décomposé en part espèces + part mobile (règle d'or)")
    void testBilanPaiementMixte() {
        // Vente de 10 000 payée 6 000 en espèces et 4 000 en mobile
        List<Vente> ventes = List.of(venteMixte(10000.0, 6000.0, 4000.0));

        BilanCloture bilan = CaisseService.calculerBilan(5000, ventes);

        assertThat(bilan.nbVentesMixtes()).isEqualTo(1);
        assertThat(bilan.partEspecesMixtes()).isEqualTo(6000);
        assertThat(bilan.partMobileMixtes()).isEqualTo(4000);
        assertThat(bilan.totalEspeces()).isEqualTo(6000);
        assertThat(bilan.totalMobile()).isEqualTo(4000);
        assertThat(bilan.especesAttenduesTiroir()).isEqualTo(11000);
    }

    @Test
    @DisplayName("Journée complète : espèces + mobile + mixte cumulés sans perte")
    void testBilanJourneeComplete() {
        List<Vente> ventes = List.of(
                venteEspeces(2000.0),               // tiroir +2000
                venteMobile(5000.0),                // digital +5000
                venteMixte(3000.0, 1000.0, 2000.0), // tiroir +1000, digital +2000
                venteMixte(7000.0, 4500.0, 2500.0)  // tiroir +4500, digital +2500
        );

        BilanCloture bilan = CaisseService.calculerBilan(20000, ventes);

        assertThat(bilan.ventesPuresEspeces()).isEqualTo(2000);
        assertThat(bilan.ventesPuresMobile()).isEqualTo(5000);
        assertThat(bilan.nbVentesMixtes()).isEqualTo(2);
        assertThat(bilan.totalEspeces()).isEqualTo(7500);   // 2000 + 1000 + 4500
        assertThat(bilan.totalMobile()).isEqualTo(9500);    // 5000 + 2000 + 2500
        assertThat(bilan.especesAttenduesTiroir()).isEqualTo(27500);
        // Cohérence globale : rien ne disparaît
        assertThat(bilan.totalEspeces() + bilan.totalMobile()).isEqualTo(17000);
    }

    @Test
    @DisplayName("Montants null en base : traités comme zéro, jamais de NullPointerException")
    void testBilanRobusteAuxNulls() {
        Vente venteNulle = new Vente();
        venteNulle.setModePaiement(Vente.ModePaiement.MIXTE);
        // total, montantEspeces et montantMobile laissés null volontairement

        BilanCloture bilan = CaisseService.calculerBilan(1000, List.of(venteNulle));

        assertThat(bilan.totalEspeces()).isZero();
        assertThat(bilan.totalMobile()).isZero();
        assertThat(bilan.nbVentesMixtes()).isEqualTo(1);
        assertThat(bilan.especesAttenduesTiroir()).isEqualTo(1000);
    }

    // ═══════════════════════════════════════════════════════════════════
    // ÉCART DE COMPTAGE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Écart de comptage : manquant négatif, surplus positif, juste = zéro")
    void testEcartEspeces() {
        BilanCloture bilan = CaisseService.calculerBilan(10000, List.of(venteEspeces(5000.0)));
        // Théorie du tiroir : 15 000

        assertThat(bilan.ecartEspeces(15000)).isZero();          // comptage juste
        assertThat(bilan.ecartEspeces(14000)).isEqualTo(-1000);  // il manque 1000
        assertThat(bilan.ecartEspeces(16500)).isEqualTo(1500);   // surplus de 1500
    }

    // ═══════════════════════════════════════════════════════════════════
    // INTÉGRATION AVEC LE DAO (mocké — prouve que l'injection fonctionne)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("calculerBilanCloture charge les ventes de la session via le DAO")
    void testBilanClotureChargeLesVentesDeLaSession() {
        SessionCaisse session = new SessionCaisse();
        session.setId(42L);
        session.setFondDeCaisse(30000.0);
        when(venteDAO.findBySessionCaisse(42L)).thenReturn(List.of(venteEspeces(4000.0)));

        CaisseService service = new CaisseService(venteDAO);
        BilanCloture bilan = service.calculerBilanCloture(session);

        verify(venteDAO).findBySessionCaisse(eq(42L));
        assertThat(bilan.totalEspeces()).isEqualTo(4000);
        assertThat(bilan.especesAttenduesTiroir()).isEqualTo(34000);
    }

    @Test
    @DisplayName("Fond de caisse null : traité comme zéro")
    void testFondDeCaisseNull() {
        SessionCaisse session = new SessionCaisse();
        session.setId(7L);
        session.setFondDeCaisse(null);
        when(venteDAO.findBySessionCaisse(7L)).thenReturn(Collections.emptyList());

        BilanCloture bilan = new CaisseService(venteDAO).calculerBilanCloture(session);

        assertThat(bilan.especesAttenduesTiroir()).isZero();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers de construction
    // ═══════════════════════════════════════════════════════════════════

    private Vente venteEspeces(Double total) {
        Vente v = new Vente();
        v.setModePaiement(Vente.ModePaiement.ESPECES);
        v.setTotal(total);
        return v;
    }

    private Vente venteMobile(Double total) {
        Vente v = new Vente();
        v.setModePaiement(Vente.ModePaiement.MOBILE_MONEY);
        v.setTotal(total);
        return v;
    }

    private Vente venteMixte(Double total, Double especes, Double mobile) {
        Vente v = new Vente();
        v.setModePaiement(Vente.ModePaiement.MIXTE);
        v.setTotal(total);
        v.setMontantEspeces(especes);
        v.setMontantMobile(mobile);
        return v;
    }
}
