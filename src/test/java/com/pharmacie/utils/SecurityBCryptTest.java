package com.pharmacie.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour la sécurité de l'authentification.
 *
 * <p>Valide que le mécanisme de hachage BCrypt fonctionne correctement
 * et qu'aucun mot de passe en clair ne peut être récupéré depuis le hash.
 * Ces tests sont essentiels pour garantir la conformité sécuritaire
 * du système d'authentification de la pharmacie.</p>
 *
 * @see org.mindrot.jbcrypt.BCrypt
 * @see com.pharmacie.controllers.LoginController
 */
class SecurityBCryptTest {

    @Test
    @DisplayName("BCrypt : un hash valide est reconnu par checkpw()")
    void testBCryptHashEtVerification() {
        // Arrange
        String motDePasse = "Pharmacie2025!";
        String hash = BCrypt.hashpw(motDePasse, BCrypt.gensalt());

        // Act & Assert
        assertThat(BCrypt.checkpw(motDePasse, hash)).isTrue();
    }

    @Test
    @DisplayName("BCrypt : un mot de passe incorrect est rejeté")
    void testBCryptRejetMotDePasseIncorrect() {
        // Arrange
        String motDePasse = "Pharmacie2025!";
        String hash = BCrypt.hashpw(motDePasse, BCrypt.gensalt());

        // Act & Assert : un mot de passe différent doit être rejeté
        assertThat(BCrypt.checkpw("mauvais_mot_de_passe", hash)).isFalse();
    }

    @Test
    @DisplayName("BCrypt : deux hashages du même mot de passe produisent des hashs différents (salage)")
    void testBCryptSalageUnique() {
        // Arrange
        String motDePasse = "admin";

        // Act : deux hashages successifs
        String hash1 = BCrypt.hashpw(motDePasse, BCrypt.gensalt());
        String hash2 = BCrypt.hashpw(motDePasse, BCrypt.gensalt());

        // Assert : les hashs sont différents (sel aléatoire) mais les deux vérifient
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(BCrypt.checkpw(motDePasse, hash1)).isTrue();
        assertThat(BCrypt.checkpw(motDePasse, hash2)).isTrue();
    }

    @Test
    @DisplayName("BCrypt : le hash ne contient jamais le mot de passe en clair")
    void testBCryptNeContientPasLeMotDePasse() {
        // Arrange
        String motDePasse = "SuperSecret123";
        String hash = BCrypt.hashpw(motDePasse, BCrypt.gensalt());

        // Assert : le hash ne doit JAMAIS contenir le mot de passe en clair
        assertThat(hash).doesNotContain(motDePasse);
        assertThat(hash).startsWith("$2a$"); // Format BCrypt standard
    }
}
