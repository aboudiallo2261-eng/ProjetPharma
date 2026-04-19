package com.pharmacie.utils;

import com.pharmacie.models.User;
import com.pharmacie.models.TicketEnAttente;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de Session (Architecture sécurisée pour le cycle de vie de l'application).
 * Conserve les informations de l'utilisateur connecté globalement, ainsi que 
 * l'état éphémère de son interface (sécurité).
 */
public class SessionManager {
    private static User currentUser;
    
    // --- ÉTAT GLOBAL DE LA CAISSE (Survit au rechargement FXML) ---
    private static boolean caisseVerrouillee = false;
    private static final List<TicketEnAttente> fileAttente = new ArrayList<>();
    private static int ticketCounter = 1;

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

    // --- ACCESSEURS ÉTAT CAISSE ---
    
    public static boolean isCaisseVerrouillee() {
        return caisseVerrouillee;
    }

    public static void setCaisseVerrouillee(boolean verrouillee) {
        caisseVerrouillee = verrouillee;
    }

    public static List<TicketEnAttente> getFileAttente() {
        return fileAttente;
    }

    public static int getNextTicketNumber() {
        return ticketCounter++;
    }

    /**
     * Détruit la session (Logout).
     * Obligatoire pour des raisons de sécurité HIPAA / RGPD.
     */
    public static void clearSession() {
        currentUser = null;
        caisseVerrouillee = false;
        fileAttente.clear();
        ticketCounter = 1;
    }
}
