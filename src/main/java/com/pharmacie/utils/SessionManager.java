package com.pharmacie.utils;

import com.pharmacie.models.User;

public class SessionManager {
    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    public static void clearSession() {
        currentUser = null;
    }
}
