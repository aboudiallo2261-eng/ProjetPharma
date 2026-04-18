package com.pharmacie.controllers;

import com.pharmacie.MainApp;
import com.pharmacie.dao.UserDAO;
import com.pharmacie.models.User;
import com.pharmacie.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    private Label errorLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // S'assurer que les rôles et permissions sont créés au démarrage
        com.pharmacie.utils.SecuritySeeder.initializeSecurity();
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
    }
}
