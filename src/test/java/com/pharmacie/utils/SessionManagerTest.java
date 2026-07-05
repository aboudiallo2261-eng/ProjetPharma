package com.pharmacie.utils;

import com.pharmacie.models.TicketEnAttente;
import com.pharmacie.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour SessionManager — Gestionnaire de session global.
 *
 * <p>Valide le cycle de vie complet de la session utilisateur :
 * authentification, verrouillage caisse, file d'attente tickets,
 * et destruction sécurisée de la session.</p>
 *
 * @see SessionManager
 */
class SessionManagerTest {

    @BeforeEach
    void setUp() {
        // Garantit l'isolation de chaque test (état propre)
        SessionManager.clearSession();
    }

    // ═══════════════════════════════════════════════════════════════════
    // GESTION DE L'UTILISATEUR
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Stocke et restitue correctement l'utilisateur connecté")
    void testSetCurrentUser() {
        // Arrange
        User user = new User();
        user.setId(10L);
        user.setNom("Docteur Test");

        // Act
        SessionManager.setCurrentUser(user);

        // Assert
        assertThat(SessionManager.getCurrentUser()).isNotNull();
        assertThat(SessionManager.getCurrentUser().getNom()).isEqualTo("Docteur Test");
        assertThat(SessionManager.getCurrentUser().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("clearSession() détruit complètement la session (sécurité RGPD)")
    void testClearSession() {
        // Arrange : session active avec état riche
        User user = new User();
        user.setNom("Agent Caisse");
        SessionManager.setCurrentUser(user);
        SessionManager.setCaisseVerrouillee(true);

        // Act
        SessionManager.clearSession();

        // Assert : tout doit être réinitialisé
        assertThat(SessionManager.getCurrentUser()).isNull();
        assertThat(SessionManager.isCaisseVerrouillee()).isFalse();
        assertThat(SessionManager.getFileAttente()).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════
    // GESTION DU VERROUILLAGE DE CAISSE
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("La caisse démarre déverrouillée par défaut")
    void testCaisseInitialementDeverrouillee() {
        assertThat(SessionManager.isCaisseVerrouillee()).isFalse();
    }

    @Test
    @DisplayName("Le verrouillage de caisse persiste entre les appels")
    void testVerrouillageCAissePersiste() {
        // Act
        SessionManager.setCaisseVerrouillee(true);

        // Assert
        assertThat(SessionManager.isCaisseVerrouillee()).isTrue();

        // Act : déverrouillage
        SessionManager.setCaisseVerrouillee(false);

        // Assert
        assertThat(SessionManager.isCaisseVerrouillee()).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════
    // FILE D'ATTENTE DES TICKETS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("La file d'attente est vide au démarrage")
    void testFileAttenteVideAuDemarrage() {
        assertThat(SessionManager.getFileAttente()).isEmpty();
    }

    @Test
    @DisplayName("Le compteur de tickets s'incrémente séquentiellement")
    void testCompteurTicketsSequentiel() {
        int premier = SessionManager.getNextTicketNumber();
        int deuxieme = SessionManager.getNextTicketNumber();
        int troisieme = SessionManager.getNextTicketNumber();

        assertThat(premier).isEqualTo(1);
        assertThat(deuxieme).isEqualTo(2);
        assertThat(troisieme).isEqualTo(3);
    }

    @Test
    @DisplayName("clearSession() remet le compteur de tickets à 1")
    void testCompteurResetApresClear() {
        // Arrange : avancer le compteur
        SessionManager.getNextTicketNumber();
        SessionManager.getNextTicketNumber();

        // Act
        SessionManager.clearSession();

        // Assert : doit repartir de 1
        assertThat(SessionManager.getNextTicketNumber()).isEqualTo(1);
    }
}
