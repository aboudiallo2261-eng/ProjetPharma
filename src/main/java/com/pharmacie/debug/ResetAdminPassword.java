package com.pharmacie.debug;

import com.pharmacie.utils.ConfigService;
import com.pharmacie.utils.HibernateUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Outil de dépannage : réinitialise le mot de passe d'un compte utilisateur.
 *
 * <p>À utiliser quand un administrateur est verrouillé hors de l'application
 * (mot de passe oublié) — il n'existe volontairement pas de fonction
 * « mot de passe oublié » dans l'interface.</p>
 *
 * <p>Usage (depuis la racine du projet) :</p>
 * <pre>
 *   java -cp "target/classes;target/libs/*" com.pharmacie.debug.ResetAdminPassword
 *   java -cp "target/classes;target/libs/*" com.pharmacie.debug.ResetAdminPassword awa MonMotDePasse
 * </pre>
 *
 * <p>Par défaut : compte {@code admin}, mot de passe {@code admin}, avec
 * changement obligatoire imposé à la prochaine connexion.</p>
 *
 * <p><b>Note technique</b> : le hachage et l'écriture passent par JDBC avec une
 * requête paramétrée. Les hachages BCrypt contiennent des {@code $} qui sont
 * interprétés par les shells — ne jamais faire cet UPDATE à la main en ligne
 * de commande, le hachage arriverait tronqué ou vide.</p>
 */
public class ResetAdminPassword {

    public static void main(String[] args) {
        String identifiant = args.length > 0 ? args[0] : "admin";
        String motDePasse  = args.length > 1 ? args[1] : "admin";

        System.out.println("=== Réinitialisation de mot de passe VetPharma ===");

        String probleme = HibernateUtil.diagnostiquerConnexion();
        if (probleme != null) {
            System.err.println("ÉCHEC — " + probleme);
            System.exit(1);
        }

        String hash = BCrypt.hashpw(motDePasse, BCrypt.gensalt());
        if (!BCrypt.checkpw(motDePasse, hash)) { // garde-fou : hachage cohérent
            System.err.println("ÉCHEC — le hachage généré est invalide.");
            System.exit(1);
        }

        String url  = ConfigService.getDbUrl();
        String user = ConfigService.getDbUsername();
        String pass = ConfigService.getDbPassword();

        try (Connection conn = DriverManager.getConnection(url, user, pass != null ? pass : "")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET motDePasseHash = ?, must_change_password = 1 WHERE identifiant = ?")) {
                ps.setString(1, hash);
                ps.setString(2, identifiant);
                int lignes = ps.executeUpdate();
                if (lignes == 0) {
                    System.err.println("ÉCHEC — aucun compte nommé '" + identifiant + "' en base.");
                    System.exit(1);
                }
            }

            // Relecture : on vérifie ce qui est RÉELLEMENT stocké, pas ce qu'on croit avoir écrit
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT motDePasseHash FROM users WHERE identifiant = ?")) {
                ps.setString(1, identifiant);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && BCrypt.checkpw(motDePasse, rs.getString(1))) {
                        System.out.println("OK — compte '" + identifiant + "' réinitialisé.");
                        System.out.println("   Mot de passe : " + motDePasse);
                        System.out.println("   Un nouveau mot de passe sera exigé à la prochaine connexion.");
                    } else {
                        System.err.println("ÉCHEC — la relecture du hachage en base a échoué.");
                        System.exit(1);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ÉCHEC — " + e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }
}
