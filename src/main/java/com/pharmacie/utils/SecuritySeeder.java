package com.pharmacie.utils;

import com.pharmacie.dao.ProfilDAO;
import com.pharmacie.dao.UserDAO;
import com.pharmacie.models.Profil;
import com.pharmacie.models.User;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialisation de sécurité au démarrage de l'application.
 *
 * <p>Ce composant garantit que le système est toujours opérationnel en créant :
 * <ol>
 *   <li>Le profil <b>SUPER-ADMIN</b> avec tous les droits d'accès</li>
 *   <li>Un utilisateur administrateur par défaut (identifiant: admin, mot de passe: admin)
 *       si aucun utilisateur n'existe en base — le mot de passe est hashé BCrypt</li>
 *   <li>Les informations par défaut de la pharmacie</li>
 *   <li>La migration des utilisateurs existants sans profil</li>
 * </ol>
 *
 * <p><b>Important</b> : Le mot de passe par défaut "admin" doit être changé
 * immédiatement après le premier login en production.</p>
 */
public class SecuritySeeder {
    
    private static final Logger logger = LoggerFactory.getLogger(SecuritySeeder.class);
    
    public static void initializeSecurity() {
        try {
            ProfilDAO profilDAO = new ProfilDAO();
            UserDAO userDAO = new UserDAO();
            
            // 1. Check if SUPER-ADMIN profile exists
            Profil superAdmin = profilDAO.findByNom("SUPER-ADMIN");
            if (superAdmin == null) {
                logger.info("Création du profil par défaut SUPER-ADMIN...");
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

            // 2. Créer l'administrateur par défaut si aucun utilisateur n'existe
            // Le mot de passe est hashé BCrypt — aucun mot de passe en clair dans le code
            List<User> allUsers = userDAO.findAll();
            if (allUsers == null || allUsers.isEmpty()) {
                logger.info("Aucun utilisateur trouvé — création du compte admin par défaut (BCrypt)...");
                User admin = new User();
                admin.setNom("Administrateur");
                admin.setIdentifiant("admin");
                admin.setEmail("admin@pharmacie.vet");
                admin.setMotDePasseHash(BCrypt.hashpw("admin", BCrypt.gensalt()));
                admin.setRole("ADMIN");
                admin.setProfil(superAdmin);
                userDAO.save(admin);
                logger.info("Compte admin par défaut créé. Identifiant: admin | Mot de passe: admin (à changer en production)");
            }
            
            // 3. Initialiser les Infos de la Pharmacie par défaut
            com.pharmacie.dao.PharmacieInfoDAO infoDAO = new com.pharmacie.dao.PharmacieInfoDAO();
            if (infoDAO.getInfo() == null) {
                logger.info("Création des informations par défaut de la Pharmacie...");
                com.pharmacie.models.PharmacieInfo defaultInfo = new com.pharmacie.models.PharmacieInfo(
                    "PHARMACIE VETERINAIRE",
                    "Adresse Non Définie",
                    "Téléphone Non Défini",
                    "A Configurer",
                    "Merci de votre confiance et prompt rétablissement !"
                );
                infoDAO.save(defaultInfo);
            }
            
            // 4. Prevent lockout by migrating older users (or assigning super admin if profil is null)
            allUsers = userDAO.findAll(); // Recharger car on a peut-être créé l'admin
            for (User u : allUsers) {
                if (u.getProfil() == null) {
                    logger.info("Migration : Profil SUPER-ADMIN affecté à " + u.getIdentifiant());
                    u.setProfil(superAdmin);
                    userDAO.update(u);
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la migration des droits d'accès", e);
        }
    }
}
