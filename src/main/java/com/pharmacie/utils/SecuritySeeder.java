package com.pharmacie.utils;

import com.pharmacie.dao.ProfilDAO;
import com.pharmacie.dao.UserDAO;
import com.pharmacie.models.Profil;
import com.pharmacie.models.User;
import java.util.List;

public class SecuritySeeder {
    
    public static void initializeSecurity() {
        try {
            ProfilDAO profilDAO = new ProfilDAO();
            UserDAO userDAO = new UserDAO();
            
            // 1. Check if SUPER-ADMIN profile exists
            Profil superAdmin = profilDAO.findByNom("SUPER-ADMIN");
            if (superAdmin == null) {
                System.out.println("[SecuritySeeder] Création du profil par défaut SUPER-ADMIN...");
                superAdmin = new Profil("SUPER-ADMIN", "Accès total au système.");
                superAdmin.setCanAccessDashboard(true);
                superAdmin.setCanAccessVentes(true);
                superAdmin.setCanAccessAchats(true);
                superAdmin.setCanAccessStock(true);
                superAdmin.setCanAccessFournisseurs(true);
                superAdmin.setCanAccessRapports(true);
                superAdmin.setCanAccessParametres(true);
                profilDAO.save(superAdmin);
            }
            
            // 2. Initialiser les Infos de la Pharmacie par défaut
            com.pharmacie.dao.PharmacieInfoDAO infoDAO = new com.pharmacie.dao.PharmacieInfoDAO();
            if (infoDAO.getInfo() == null) {
                System.out.println("[SecuritySeeder] Création des informations par défaut de la Pharmacie...");
                com.pharmacie.models.PharmacieInfo defaultInfo = new com.pharmacie.models.PharmacieInfo(
                    "PHARMACIE VETERINAIRE",
                    "Adresse Non Définie",
                    "Téléphone Non Défini",
                    "A Configurer",
                    "Merci de votre confiance et prompt rétablissement !"
                );
                infoDAO.save(defaultInfo);
            }
            
            // 2. Prevent lockout by migrating older users (or assigning super admin if profil is null)
            List<User> allUsers = userDAO.findAll();
            for (User u : allUsers) {
                if (u.getProfil() == null) {
                    System.out.println("[SecuritySeeder] Migration : Profil SUPER-ADMIN affecté à " + u.getIdentifiant());
                    u.setProfil(superAdmin);
                    userDAO.update(u);
                }
            }
        } catch (Exception e) {
            System.err.println("[SecuritySeeder] Erreur lors de la migration des droits d'accès : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
