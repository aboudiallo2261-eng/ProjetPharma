package com.pharmacie.utils;

import com.pharmacie.models.User;

/**
 * Gestionnaire de Session (Architecture sécurisée pour le cycle de vie de l'application).
 * Conserve les informations de l'utilisateur connecté globalement.
 */
public class SessionManager {
    private static User currentUser;

    /**
     * Récupère l'utilisateur actuellement connecté.
     * @return L'entité User représentant la session actuelle, null si déconnecté.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Définit l'utilisateur courant lors du Login (Authentification validée).
     * @param user L'utilisateur dont les crédentiels ont été validés par la base de données.
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    /**
     * Détruit la session (Logout).
     * Obligatoire pour des raisons de sécurité HIPAA / RGPD.
     */
    public static void clearSession() {
        currentUser = null;
    }
}
