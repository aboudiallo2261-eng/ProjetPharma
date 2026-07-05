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

        // Authentification BCrypt standard — pas de bypass hardcodé
        User user = userDAO.findByIdentifiant(identifiant);
        if (user != null) {
            if (BCrypt.checkpw(password, user.getMotDePasseHash())) {
                // Sécurité : mot de passe par défaut ou réinitialisé → changement obligatoire
                if (Boolean.TRUE.equals(user.getMustChangePassword())) {
                    if (!forcerChangementMotDePasse(user, password)) {
                        showError("Vous devez définir un nouveau mot de passe pour continuer.");
                        return;
                    }
                }
                SessionManager.setCurrentUser(user);
                logger.info("Connexion réussie: {}", user.getNom());
                MainApp.showMainLayout();
            } else {
                showError("Identifiant ou mot de passe incorrect.");
            }
        } else {
            showError("Identifiant ou mot de passe incorrect.");
        }
    }

    /**
     * Dialogue modal bloquant : l'utilisateur doit définir un nouveau mot de passe
     * (minimum 8 caractères, différent de l'ancien) avant d'accéder à l'application.
     *
     * @return true si le mot de passe a été changé et persisté, false si annulé.
     */
    private boolean forcerChangementMotDePasse(User user, String ancienMotDePasse) {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Changement de mot de passe obligatoire");
        dialog.setHeaderText("Première connexion : définissez votre nouveau mot de passe.\n(8 caractères minimum, différent de l'ancien)");

        javafx.scene.control.ButtonType btnValider =
                new javafx.scene.control.ButtonType("Valider", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValider, javafx.scene.control.ButtonType.CANCEL);

        PasswordField nouveau = new PasswordField();
        nouveau.setPromptText("Nouveau mot de passe");
        PasswordField confirmation = new PasswordField();
        confirmation.setPromptText("Confirmez le mot de passe");
        Label erreur = new Label();
        erreur.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");

        javafx.scene.layout.VBox contenu = new javafx.scene.layout.VBox(10, nouveau, confirmation, erreur);
        contenu.setPrefWidth(340);
        dialog.getDialogPane().setContent(contenu);

        // Validation à la volée : on bloque le clic "Valider" tant que les règles ne sont pas respectées
        javafx.scene.control.Button boutonValider =
                (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btnValider);
        boutonValider.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String mdp = nouveau.getText();
            if (mdp == null || mdp.length() < 8) {
                erreur.setText("Le mot de passe doit contenir au moins 8 caractères.");
                event.consume();
            } else if (mdp.equals(ancienMotDePasse)) {
                erreur.setText("Le nouveau mot de passe doit être différent de l'ancien.");
                event.consume();
            } else if (!mdp.equals(confirmation.getText())) {
                erreur.setText("Les deux mots de passe ne correspondent pas.");
                event.consume();
            }
        });

        dialog.setResultConverter(bouton -> bouton == btnValider ? nouveau.getText() : null);

        java.util.Optional<String> resultat = dialog.showAndWait();
        if (resultat.isPresent()) {
            user.setMotDePasseHash(BCrypt.hashpw(resultat.get(), BCrypt.gensalt()));
            user.setMustChangePassword(false);
            userDAO.update(user);
            logger.info("Mot de passe changé (obligatoire) pour l'utilisateur: {}", user.getIdentifiant());
            return true;
        }
        return false;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
