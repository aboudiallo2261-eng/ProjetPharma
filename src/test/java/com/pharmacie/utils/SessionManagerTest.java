package com.pharmacie.utils;

import com.pharmacie.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {

    @BeforeEach
    void setUp() {
        // Garantit l'isolation de chaque test
        SessionManager.clearSession();
    }

    @Test
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
    void testClearSession() {
        // Arrange
        User user = new User();
        SessionManager.setCurrentUser(user);

        // Act
        SessionManager.clearSession();

        // Assert
        assertThat(SessionManager.getCurrentUser()).isNull();
    }
}
