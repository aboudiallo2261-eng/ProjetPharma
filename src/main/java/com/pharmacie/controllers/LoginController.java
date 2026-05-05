package com.pharmacie.controllers;

import com.pharmacie.MainApp;
import com.pharmacie.dao.UserDAO;
import com.pharmacie.models.User;
import com.pharmacie.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.SVGPath;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField identifiantField;

    @FXML
    private PasswordField passwordField;
    
    @FXML
    private TextField passwordVisibleField;
    
    @FXML
    private SVGPath eyeIcon;

    @FXML
    private Label errorLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // S'assurer que les rôles et permissions sont créés au démarrage
        com.pharmacie.utils.SecuritySeeder.initializeSecurity();
        
        // P2.A: Synchroniser les champs de mot de passe (caché et visible)
        if (passwordVisibleField != null && passwordField != null) {
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }
    }

    @FXML
    public void togglePasswordVisibility() {
        if (passwordVisibleField.isVisible()) {
            // Passer en mode masqué (cadenas)
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            // Icône "Œil Ouvert" (signifie : cliquez ici pour afficher)
            eyeIcon.setContent("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 9a3 3 0 1 0 0 6 3 3 0 1 0 0-6z");
            passwordField.requestFocus();
        } else {
            // Passer en mode texte clair
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            // Icône "Œil Barré" (Feather Icon eye-off)
            eyeIcon.setContent("M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24M1 1l22 22");
            passwordVisibleField.requestFocus();
        }
        // Placer le curseur à la fin du texte pour une bonne UX
        if (passwordVisibleField.isVisible()) {
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        } else {
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    public void handleLogin() {
        String identifiant = identifiantField.getText();
        String password = passwordField.getText();

        if (identifiant == null || identifiant.isEmpty() || password == null || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // Temporary hardcoded admin to allow first login if DB is empty
        if (identifiant.equals("admin") && password.equals("admin")) {
            User admin = userDAO.findByIdentifiant("admin");
            if (admin == null) {
                admin = new User();
                admin.setNom("Administrateur Setup");
                admin.setIdentifiant("admin");
                admin.setEmail("admin@pharmacie.com");
                admin.setMotDePasseHash(BCrypt.hashpw("admin", BCrypt.gensalt()));
                admin.setProfil(new com.pharmacie.dao.ProfilDAO().findByNom("SUPER-ADMIN"));
                userDAO.save(admin);
                admin = userDAO.findByIdentifiant("admin"); // Assure la récupération de l'ID généré
            }
            SessionManager.setCurrentUser(admin);
            logger.info("Connexion Setup Admin avec ID: {}", admin.getId());
            MainApp.showMainLayout();
            return;
        }

        User user = userDAO.findByIdentifiant(identifiant);
        if (user != null) {
            if (BCrypt.checkpw(password, user.getMotDePasseHash())) {
                SessionManager.setCurrentUser(user);
                logger.info("Connexion réussie: {}", user.getNom());
                MainApp.showMainLayout();
            } else {
                showError("Mot de passe incorrect.");
            }
        } else {
            showError("Identifiant introuvable.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
