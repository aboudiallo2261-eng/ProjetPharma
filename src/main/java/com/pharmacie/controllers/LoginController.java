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
                        showError("Connexion refusée : votre mot de passe actuel doit être remplacé. "
                                + "Reconnectez-vous et choisissez un nouveau mot de passe (ne cliquez pas sur Annuler).");
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

    // Icônes Feather réutilisées de l'écran de login
    private static final String SVG_CADENAS = "M19 11H5a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2z M7 11V7a5 5 0 0 1 10 0v4";
    private static final String SVG_OEIL_OUVERT = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z";
    private static final String SVG_OEIL_BARRE = "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24M1 1l22 22";
    private static final int TAILLE_MIN_MOT_DE_PASSE = 4;

    /**
     * Dialogue modal bloquant : l'utilisateur doit définir un nouveau mot de passe
     * (minimum {@value #TAILLE_MIN_MOT_DE_PASSE} caractères, différent de l'ancien)
     * avant d'accéder à l'application.
     *
     * <p>Design aligné sur l'écran de connexion (login.css) : carte blanche,
     * champs avec icône cadenas, bouton œil pour afficher le mot de passe,
     * bouton principal vert émeraude.</p>
     *
     * @return true si le mot de passe a été changé et persisté, false si annulé.
     */
    private boolean forcerChangementMotDePasse(User user, String ancienMotDePasse) {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Changement de mot de passe obligatoire");

        javafx.scene.control.DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(MainApp.class.getResource("/css/login.css").toExternalForm());
        pane.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 12;");

        javafx.scene.control.ButtonType btnValider =
                new javafx.scene.control.ButtonType("Valider", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType btnAnnuler =
                new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(btnValider, btnAnnuler);

        // ── En-tête au style de la carte de connexion ─────────────────────
        Label titre = new Label("Sécurisez votre compte");
        titre.getStyleClass().add("title-text");
        Label sousTitre = new Label("Première connexion : définissez votre nouveau mot de passe ("
                + TAILLE_MIN_MOT_DE_PASSE + " caractères minimum, différent de l'ancien).\n"
                + "Si vous annulez, la connexion sera refusée.");
        sousTitre.getStyleClass().add("subtitle-text");
        sousTitre.setWrapText(true);

        // ── Champs avec icône cadenas + bouton œil (comme le login) ──────
        ChampMotDePasse nouveau = creerChampMotDePasse("Nouveau mot de passe");
        ChampMotDePasse confirmation = creerChampMotDePasse("Confirmez le mot de passe");

        Label erreur = new Label();
        erreur.getStyleClass().add("error-label");
        erreur.setWrapText(true);
        erreur.setVisible(false);
        erreur.setManaged(false);

        Label labelNouveau = new Label("Nouveau mot de passe");
        labelNouveau.getStyleClass().add("field-label");
        Label labelConfirmation = new Label("Confirmation");
        labelConfirmation.getStyleClass().add("field-label");

        javafx.scene.layout.VBox contenu = new javafx.scene.layout.VBox(10,
                titre, sousTitre,
                labelNouveau, nouveau.conteneur,
                labelConfirmation, confirmation.conteneur,
                erreur);
        contenu.setPrefWidth(400);
        pane.setContent(contenu);

        // ── Boutons au style de l'application ─────────────────────────────
        javafx.scene.control.Button boutonValider =
                (javafx.scene.control.Button) pane.lookupButton(btnValider);
        boutonValider.getStyleClass().add("button-primary");
        javafx.scene.control.Button boutonAnnuler =
                (javafx.scene.control.Button) pane.lookupButton(btnAnnuler);
        boutonAnnuler.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155;"
                + " -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 12 20; -fx-cursor: hand;");

        // ── Validation à la volée : bloque "Valider" tant que les règles ne passent pas ──
        boutonValider.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String mdp = nouveau.champCache.getText();
            String messageErreur = null;
            if (mdp == null || mdp.length() < TAILLE_MIN_MOT_DE_PASSE) {
                messageErreur = "Le mot de passe doit contenir au moins " + TAILLE_MIN_MOT_DE_PASSE + " caractères.";
            } else if (mdp.equals(ancienMotDePasse)) {
                messageErreur = "Le nouveau mot de passe doit être différent de l'ancien.";
            } else if (!mdp.equals(confirmation.champCache.getText())) {
                messageErreur = "Les deux mots de passe ne correspondent pas.";
            }
            if (messageErreur != null) {
                erreur.setText(messageErreur);
                erreur.setVisible(true);
                erreur.setManaged(true);
                event.consume();
            }
        });

        dialog.setResultConverter(bouton -> bouton == btnValider ? nouveau.champCache.getText() : null);

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

    /** Petit conteneur regroupant les nœuds d'un champ mot de passe avec œil. */
    private static final class ChampMotDePasse {
        final javafx.scene.layout.HBox conteneur;
        final PasswordField champCache;

        ChampMotDePasse(javafx.scene.layout.HBox conteneur, PasswordField champCache) {
            this.conteneur = conteneur;
            this.champCache = champCache;
        }
    }

    /**
     * Construit un champ mot de passe identique à celui du login :
     * conteneur arrondi, icône cadenas, champ masqué/visible superposés,
     * bouton œil pour basculer l'affichage.
     */
    private ChampMotDePasse creerChampMotDePasse(String prompt) {
        SVGPath iconeCadenas = new SVGPath();
        iconeCadenas.setContent(SVG_CADENAS);
        iconeCadenas.getStyleClass().add("input-icon");

        PasswordField champCache = new PasswordField();
        champCache.setPromptText(prompt);
        champCache.getStyleClass().add("input-field");

        TextField champVisible = new TextField();
        champVisible.setPromptText(prompt);
        champVisible.getStyleClass().add("input-field");
        champVisible.setVisible(false);
        champVisible.setManaged(false);
        champVisible.textProperty().bindBidirectional(champCache.textProperty());

        javafx.scene.layout.StackPane pile = new javafx.scene.layout.StackPane(champCache, champVisible);
        javafx.scene.layout.HBox.setHgrow(pile, javafx.scene.layout.Priority.ALWAYS);

        SVGPath iconeOeil = new SVGPath();
        iconeOeil.setContent(SVG_OEIL_OUVERT);
        iconeOeil.getStyleClass().addAll("input-icon", "eye-icon");

        javafx.scene.control.Button boutonOeil = new javafx.scene.control.Button();
        boutonOeil.setGraphic(iconeOeil);
        boutonOeil.getStyleClass().add("btn-icon");
        boutonOeil.setFocusTraversable(false);
        boutonOeil.setOnAction(e -> {
            boolean afficher = !champVisible.isVisible();
            champVisible.setVisible(afficher);
            champVisible.setManaged(afficher);
            champCache.setVisible(!afficher);
            champCache.setManaged(!afficher);
            iconeOeil.setContent(afficher ? SVG_OEIL_BARRE : SVG_OEIL_OUVERT);
            if (afficher) {
                champVisible.requestFocus();
                champVisible.positionCaret(champVisible.getText() != null ? champVisible.getText().length() : 0);
            } else {
                champCache.requestFocus();
                champCache.positionCaret(champCache.getText() != null ? champCache.getText().length() : 0);
            }
        });

        javafx.scene.layout.HBox conteneur = new javafx.scene.layout.HBox(iconeCadenas, pile, boutonOeil);
        conteneur.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        conteneur.getStyleClass().add("input-container");

        return new ChampMotDePasse(conteneur, champCache);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
