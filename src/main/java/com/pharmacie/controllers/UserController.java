package com.pharmacie.controllers;

import com.pharmacie.dao.ProfilDAO;
import com.pharmacie.dao.UserDAO;
import com.pharmacie.models.Profil;
import com.pharmacie.models.User;
import com.pharmacie.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.mindrot.jbcrypt.BCrypt;
import com.pharmacie.utils.AlertUtils;

import java.util.List;

public class UserController {

    // --- Onglet Utilisateurs ---
    @FXML private TextField txtNom;
    @FXML private TextField txtIdentifiant;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtMotDePasse;
    @FXML private TextField txtMotDePasseVisible;
    @FXML private PasswordField txtConfirmMotDePasse;
    @FXML private TextField txtConfirmMotDePasseVisible;
    @FXML private ToggleButton btnTogglePassword;
    @FXML private javafx.scene.shape.SVGPath iconPassword;
    @FXML private ToggleButton btnToggleConfirmPassword;
    @FXML private javafx.scene.shape.SVGPath iconConfirmPassword;
    @FXML private ComboBox<Profil> cmbProfil;
    @FXML private Label lblErrorText;
    @FXML private Button btnSaveUser;

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colIdentifiant;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;

    // --- Onglet Profils ---
    @FXML private TextField txtProfilNom;
    @FXML private TextField txtProfilDesc;
    @FXML private CheckBox chkDashboard, chkVentes, chkStock, chkAchats, chkFournisseurs, chkRapports, chkParametres;
    @FXML private Label lblProfilError;
    @FXML private Button btnSaveProfil;
    @FXML private TableView<Profil> tableProfils;
    @FXML private TableColumn<Profil, String> colProfilNom, colProfilDesc;

    // --- Onglet Infos Pharmacie ---
    @FXML private TextField txtInfoNom;
    @FXML private TextField txtInfoAdresse;
    @FXML private TextField txtInfoPhone;
    @FXML private TextField txtInfoEmail;
    @FXML private TextArea txtInfoMessage;
    @FXML private Label lblInfoMsg;
    
    // --- Onglet Sauvegarde ---
    @FXML private TextField txtBackupPath;

    private static final String PATH_EYE = "M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z";
    private static final String PATH_EYE_SLASH = "M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7.028 7.028 0 0 0-2.79.588l.77.771A5.944 5.944 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.134 13.134 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755-.165.165-.337.328-.517.486l.708.709z M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829l.822.822zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829z M3.35 5.47c-.18.16-.353.322-.518.487A13.134 13.134 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7.029 7.029 0 0 1 8 13.5C3 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12-.708.708z";

    private UserDAO userDAO = new UserDAO();
    private ProfilDAO profilDAO = new ProfilDAO();
    private com.pharmacie.dao.PharmacieInfoDAO infoDAO = new com.pharmacie.dao.PharmacieInfoDAO();
    
    private User selectedUser;
    private Profil selectedProfil;
    private com.pharmacie.models.PharmacieInfo currentInfo;

    @FXML
    public void initialize() {
        // Init table utilisateurs
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colIdentifiant.setCellValueFactory(new PropertyValueFactory<>("identifiant"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(cellData -> {
            Profil p = cellData.getValue().getProfil();
            return new SimpleStringProperty(p != null ? p.getNom() : "Sans Profil");
        });

        // Init table profils
        colProfilNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colProfilDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        cmbProfil.setConverter(new javafx.util.StringConverter<Profil>() {
            @Override public String toString(Profil p) { return p == null ? "" : p.getNom(); }
            @Override public Profil fromString(String string) { return null; }
        });

        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) populateForm(newSel);
        });
        tableProfils.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) populateProfilForm(newSel);
        });

        loadProfils();
        loadUsers();
        loadInfosPharmacie();
        
        // Charger le chemin de sauvegarde USB
        String backupPath = com.pharmacie.utils.ConfigService.getBackupPath();
        if (backupPath != null && !backupPath.isEmpty()) {
            txtBackupPath.setText(backupPath);
        }

        // Lier le champ de texte visible au champ de mot de passe caché
        txtMotDePasseVisible.textProperty().bindBidirectional(txtMotDePasse.textProperty());
        txtConfirmMotDePasseVisible.textProperty().bindBidirectional(txtConfirmMotDePasse.textProperty());
    }

    @FXML
    public void togglePasswordVisibility() {
        if (btnTogglePassword.isSelected()) {
            txtMotDePasseVisible.setVisible(true);
            txtMotDePasseVisible.setManaged(true);
            txtMotDePasse.setVisible(false);
            txtMotDePasse.setManaged(false);
            iconPassword.setContent(PATH_EYE_SLASH);
            iconPassword.setFill(javafx.scene.paint.Color.web("#3B82F6"));
        } else {
            txtMotDePasseVisible.setVisible(false);
            txtMotDePasseVisible.setManaged(false);
            txtMotDePasse.setVisible(true);
            txtMotDePasse.setManaged(true);
            iconPassword.setContent(PATH_EYE);
            iconPassword.setFill(javafx.scene.paint.Color.web("#94A3B8"));
        }
    }

    @FXML
    public void toggleConfirmPasswordVisibility() {
        if (btnToggleConfirmPassword.isSelected()) {
            txtConfirmMotDePasseVisible.setVisible(true);
            txtConfirmMotDePasseVisible.setManaged(true);
            txtConfirmMotDePasse.setVisible(false);
            txtConfirmMotDePasse.setManaged(false);
            iconConfirmPassword.setContent(PATH_EYE_SLASH);
            iconConfirmPassword.setFill(javafx.scene.paint.Color.web("#3B82F6"));
        } else {
            txtConfirmMotDePasseVisible.setVisible(false);
            txtConfirmMotDePasseVisible.setManaged(false);
            txtConfirmMotDePasse.setVisible(true);
            txtConfirmMotDePasse.setManaged(true);
            iconConfirmPassword.setContent(PATH_EYE);
            iconConfirmPassword.setFill(javafx.scene.paint.Color.web("#94A3B8"));
        }
    }

    private void loadProfils() {
        List<Profil> profils = profilDAO.findAll();
        tableProfils.setItems(FXCollections.observableArrayList(profils));
        com.pharmacie.utils.TableUtils.ajusterSansDefilement(tableProfils);
        cmbProfil.setItems(FXCollections.observableArrayList(profils));
    }

    private void loadUsers() {
        List<User> users = userDAO.findAll();
        tableUsers.setItems(FXCollections.observableArrayList(users));
        com.pharmacie.utils.TableUtils.ajusterAvecDefilement(tableUsers);
    }

    // --- LOGIQUE UTILISATEUR ---

    private void populateForm(User user) {
        selectedUser = user;
        txtNom.setText(user.getNom());
        txtIdentifiant.setText(user.getIdentifiant());
        txtEmail.setText(user.getEmail());
        
        // Sélectionner le profil dans la combobox
        if (user.getProfil() != null) {
            for (Profil p : cmbProfil.getItems()) {
                if (p.getId().equals(user.getProfil().getId())) {
                    cmbProfil.getSelectionModel().select(p);
                    break;
                }
            }
        }
        
        txtMotDePasse.clear();
        txtConfirmMotDePasse.clear();
        btnSaveUser.setText("Mettre à jour");
        lblErrorText.setVisible(false);
    }

    @FXML
    public void handleReset() {
        selectedUser = null;
        txtNom.clear();
        txtIdentifiant.clear();
        txtEmail.clear();
        txtMotDePasse.clear();
        txtConfirmMotDePasse.clear();
        cmbProfil.getSelectionModel().clearSelection();
        btnSaveUser.setText("Enregistrer l'agent");
        lblErrorText.setVisible(false);
        tableUsers.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSave() {
        String nom = txtNom.getText().trim();
        String identifiant = txtIdentifiant.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtMotDePasse.getText();
        String confirmPassword = txtConfirmMotDePasse.getText();
        Profil profil = cmbProfil.getValue();

        if (nom.isEmpty()) {
            showErrorEffect(txtNom);
            showError("Le nom complet est obligatoire.");
            txtNom.requestFocus();
            return;
        }
        if (identifiant.isEmpty()) {
            showErrorEffect(txtIdentifiant);
            showError("L'identifiant de connexion est obligatoire.");
            txtIdentifiant.requestFocus();
            return;
        }
        if (profil == null) {
            showError("Veuillez sélectionner un profil d'accès.");
            return;
        }
        
        if (!password.isEmpty() && !password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        if (selectedUser == null) { // CREATE
            if (password.isEmpty()) {
                showErrorEffect(txtMotDePasse);
                showError("Le mot de passe est obligatoire pour un nouvel agent.");
                return;
            }
            // Seul l'identifiant doit être unique (pas le nom complet)
            if (userDAO.findAll().stream().anyMatch(u -> u.getIdentifiant().equalsIgnoreCase(identifiant))) {
                showErrorEffect(txtIdentifiant);
                showError("Cet identifiant est déjà pris ! Veuillez en choisir un autre.");
                txtIdentifiant.requestFocus();
                return;
            }
            User newUser = new User();
            newUser.setNom(nom);
            newUser.setIdentifiant(identifiant);
            newUser.setEmail(email);
            newUser.setProfil(profil);
            newUser.setMotDePasseHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            userDAO.save(newUser);
            com.pharmacie.utils.ToastService.showSuccess(tableUsers.getScene().getWindow(), "Utilisateur Créé", "L'agent a été ajouté avec succès.");
        } else { // UPDATE
            // Seul l'identifiant doit être unique en modification (pas le nom)
            if (!selectedUser.getIdentifiant().equalsIgnoreCase(identifiant) && 
                userDAO.findAll().stream().anyMatch(u -> u.getIdentifiant().equalsIgnoreCase(identifiant))) {
                showErrorEffect(txtIdentifiant);
                showError("Impossible de modifier : cet identifiant appartient déjà à un autre agent !");
                txtIdentifiant.requestFocus();
                return;
            }
            selectedUser.setNom(nom);
            selectedUser.setIdentifiant(identifiant);
            selectedUser.setEmail(email);
            selectedUser.setProfil(profil);
            if (!password.isEmpty()) {
                selectedUser.setMotDePasseHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            }
            userDAO.update(selectedUser);
            com.pharmacie.utils.ToastService.showSuccess(tableUsers.getScene().getWindow(), "Utilisateur Modifié", "Les accès de l'agent ont été mis à jour.");
            
            // Si on met à jour son propre compte, on met à jour le session manager
            if (SessionManager.getCurrentUser().getId().equals(selectedUser.getId())) {
                SessionManager.setCurrentUser(selectedUser);
                // Idéalement notifier l'App pour actualiser le menu de gauche...
            }
        }
        handleReset();
        loadUsers();
    }

    @FXML
    public void handleDelete() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (SessionManager.getCurrentUser().getId().equals(selected.getId())) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible de désactiver ou supprimer votre propre compte en cours d'utilisation.");
                alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                alert.getDialogPane().setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                alert.showAndWait();
                return;
            }
            boolean success = userDAO.delete(selected);
            if (!success) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible de supprimer l'utilisateur car il a effectué des ventes ou opérations.");
                alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                alert.getDialogPane().setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                alert.showAndWait();
            } else {
                com.pharmacie.utils.ToastService.showSuccess(tableUsers.getScene().getWindow(), "Utilisateur Supprimé", "L'agent a été révoqué et supprimé.");
                handleReset();
                loadUsers();
            }
        } else {
            showError("Sélectionnez un utilisateur à supprimer.");
        }
    }

    private void showError(String message) {
        lblErrorText.setText(message);
        lblErrorText.setVisible(true);
    }

    private void showProfilError(String message) {
        if (lblProfilError != null) {
            lblProfilError.setText(message);
            lblProfilError.setVisible(true);
        }
    }

    private void showErrorEffect(javafx.scene.Node node) {
        if (node == null) return;
        String originalStyle = node.getStyle();
        node.setStyle(originalStyle + "; -fx-border-color: #E74C3C; -fx-border-width: 2px; -fx-border-radius: 4px;");
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> node.setStyle(originalStyle));
        javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), node);
        shake.setFromX(0); shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        new javafx.animation.ParallelTransition(shake).play();
        pause.play();
    }

    // --- LOGIQUE PROFIL ---

    private void populateProfilForm(Profil p) {
        selectedProfil = p;
        txtProfilNom.setText(p.getNom());
        txtProfilDesc.setText(p.getDescription());
        
        chkDashboard.setSelected(p.isCanAccessDashboard());
        chkVentes.setSelected(p.isCanAccessVentes());
        chkStock.setSelected(p.isCanAccessStock());
        chkAchats.setSelected(p.isCanAccessAchats());
        chkFournisseurs.setSelected(p.isCanAccessFournisseurs());
        chkRapports.setSelected(p.isCanAccessRapports());
        chkParametres.setSelected(p.isCanAccessParametres());
        
        btnSaveProfil.setText("Mettre à jour");
    }

    @FXML
    public void resetProfilForm() {
        selectedProfil = null;
        txtProfilNom.clear();
        txtProfilDesc.clear();
        
        chkDashboard.setSelected(false);
        chkVentes.setSelected(false);
        chkStock.setSelected(false);
        chkAchats.setSelected(false);
        chkFournisseurs.setSelected(false);
        chkRapports.setSelected(false);
        chkParametres.setSelected(false);
        
        btnSaveProfil.setText("Enregistrer Profil");
        tableProfils.getSelectionModel().clearSelection();
        if (lblProfilError != null) lblProfilError.setVisible(false);
    }

    @FXML
    public void saveProfil() {
        String nom = txtProfilNom.getText().trim();
        if (nom.isEmpty()) {
            showErrorEffect(txtProfilNom);
            showProfilError("Le nom du profil est obligatoire.");
            txtProfilNom.requestFocus();
            return;
        }

        if (selectedProfil == null) {
            boolean doublon = profilDAO.findAll().stream().anyMatch(p -> p.getNom() != null && p.getNom().trim().equalsIgnoreCase(nom));
            if (doublon) {
                showErrorEffect(txtProfilNom);
                showProfilError("Un profil avec le nom \"" + nom + "\" existe déjà.");
                txtProfilNom.requestFocus();
                return;
            }
            Profil p = new Profil(nom, txtProfilDesc.getText());
            p.setCanAccessDashboard(chkDashboard.isSelected());
            p.setCanAccessVentes(chkVentes.isSelected());
            p.setCanAccessStock(chkStock.isSelected());
            p.setCanAccessAchats(chkAchats.isSelected());
            p.setCanAccessFournisseurs(chkFournisseurs.isSelected());
            p.setCanAccessRapports(chkRapports.isSelected());
            p.setCanAccessParametres(chkParametres.isSelected());
            profilDAO.save(p);
            com.pharmacie.utils.ToastService.showSuccess(tableProfils.getScene().getWindow(), "Profil Créé", "Le nouveau rôle a été enregistré.");
        } else {
            final Long currentId = selectedProfil.getId();
            boolean doublon = profilDAO.findAll().stream().anyMatch(p -> p.getNom() != null && p.getNom().trim().equalsIgnoreCase(nom) && !p.getId().equals(currentId));
            if (doublon) {
                showErrorEffect(txtProfilNom);
                showProfilError("Un autre profil avec le nom \"" + nom + "\" existe déjà.");
                txtProfilNom.requestFocus();
                return;
            }
            selectedProfil.setNom(nom);
            selectedProfil.setDescription(txtProfilDesc.getText());
            selectedProfil.setCanAccessDashboard(chkDashboard.isSelected());
            selectedProfil.setCanAccessVentes(chkVentes.isSelected());
            selectedProfil.setCanAccessStock(chkStock.isSelected());
            selectedProfil.setCanAccessAchats(chkAchats.isSelected());
            selectedProfil.setCanAccessFournisseurs(chkFournisseurs.isSelected());
            selectedProfil.setCanAccessRapports(chkRapports.isSelected());
            selectedProfil.setCanAccessParametres(chkParametres.isSelected());
            profilDAO.update(selectedProfil);
            com.pharmacie.utils.ToastService.showSuccess(tableProfils.getScene().getWindow(), "Profil Modifié", "Les permissions du rôle ont été mises à jour.");
        }
        resetProfilForm();
        loadProfils();
    }

    @FXML
    public void deleteProfil() {
        Profil p = tableProfils.getSelectionModel().getSelectedItem();
        if (p != null) {
            // Check if any users have this profile
            List<User> list = userDAO.findAll();
            for (User u : list) {
                if (u.getProfil() != null && u.getProfil().getId().equals(p.getId())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Impossible : des utilisateurs utilisent ce profil !");
                    alert.show();
                    return;
                }
            }
            profilDAO.delete(p);
            com.pharmacie.utils.ToastService.showSuccess(tableProfils.getScene().getWindow(), "Profil Supprimé", "Le profil a été retiré avec succès.");
            resetProfilForm();
            loadProfils();
        }
    }

    // --- LOGIQUE INFOS PHARMACIE ---

    private void loadInfosPharmacie() {
        currentInfo = infoDAO.getInfo();
        if (currentInfo != null) {
            txtInfoNom.setText(currentInfo.getNom());
            txtInfoAdresse.setText(currentInfo.getAdresse());
            txtInfoPhone.setText(currentInfo.getTelephone());
            txtInfoEmail.setText(currentInfo.getEmail());
            txtInfoMessage.setText(currentInfo.getMessageTicket());
        }
    }

    @FXML
    public void savePharmacieInfo() {
        if (currentInfo == null) {
            currentInfo = new com.pharmacie.models.PharmacieInfo();
        }
        currentInfo.setNom(txtInfoNom.getText());
        currentInfo.setAdresse(txtInfoAdresse.getText());
        currentInfo.setTelephone(txtInfoPhone.getText());
        currentInfo.setEmail(txtInfoEmail.getText());
        currentInfo.setMessageTicket(txtInfoMessage.getText());
        
        if (currentInfo.getId() == null) {
            infoDAO.save(currentInfo);
        } else {
            infoDAO.update(currentInfo);
        }
        
        com.pharmacie.utils.ToastService.showSuccess(txtInfoNom.getScene().getWindow(), "Informations Sauvegardées", "Les paramètres de la pharmacie ont été mis à jour.");
        lblInfoMsg.setText("Informations enregistrées !");
        lblInfoMsg.setVisible(true);
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> lblInfoMsg.setVisible(false));
            }
        }, 3000);
    }

    // --- LOGIQUE SAUVEGARDE BASE DE DONNEES ---
    
    @FXML
    public void handleChooseBackupPath() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Sélectionner le dossier de sauvegarde USB");
        
        String currentPath = com.pharmacie.utils.ConfigService.getBackupPath();
        if (currentPath != null && !currentPath.isEmpty()) {
            java.io.File initialDir = new java.io.File(currentPath);
            if (initialDir.exists() && initialDir.isDirectory()) {
                directoryChooser.setInitialDirectory(initialDir);
            }
        }
        
        javafx.stage.Window window = tableUsers.getScene().getWindow();
        java.io.File selectedDirectory = directoryChooser.showDialog(window);
        
        if (selectedDirectory != null) {
            String path = selectedDirectory.getAbsolutePath();
            txtBackupPath.setText(path);
            com.pharmacie.utils.ConfigService.saveBackupPath(path);
            com.pharmacie.utils.ToastService.showSuccess(window, "Chemin Enregistré", "Les sauvegardes automatiques se feront dans :\n" + path);
        }
    }
    
    @FXML
    public void handleBackupDB() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer la sauvegarde de la base de données");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier SQL", "*.sql"));
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
        String defaultName = "pharmacie_backup_" + java.time.LocalDateTime.now().format(formatter) + ".sql";
        fileChooser.setInitialFileName(defaultName);
        
        javafx.stage.Window window = tableUsers.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(window);
        
        if (file != null) {
            boolean success = com.pharmacie.utils.DatabaseBackupService.exportDatabase(file);
            if (success && file.exists() && file.length() > 0) {
                com.pharmacie.utils.AuditLogger.log("BACKUP_SQL", "SUCCESS (Manuel)");
                
                // 3. Phase 5 - Sauvegarde Google Drive (Format .zip)
                boolean driveSuccess = false;
                try {
                    java.io.File zipFile = com.pharmacie.utils.ZipUtils.compressSqlToZip(file);
                    if (zipFile != null && zipFile.exists() && zipFile.length() > 0) {
                        driveSuccess = com.pharmacie.utils.GoogleDriveService.uploadBackup(zipFile);
                        if (driveSuccess) {
                            com.pharmacie.utils.AuditLogger.log("BACKUP_DRIVE", "SUCCESS (Manuel)");
                        } else {
                            com.pharmacie.utils.AuditLogger.log("BACKUP_DRIVE", "FAILED - Erreur upload (Manuel)");
                        }
                        zipFile.delete(); 
                    } else {
                        com.pharmacie.utils.AuditLogger.log("BACKUP_DRIVE", "FAILED - Echec compression ZIP (Manuel)");
                    }
                } catch (Exception e) {
                    com.pharmacie.utils.AuditLogger.log("BACKUP_DRIVE", "ERROR - " + e.getMessage() + " (Manuel)");
                }

                if (driveSuccess) {
                    com.pharmacie.utils.ToastService.showSuccess(txtInfoNom.getScene().getWindow(), "Sauvegarde Complète", "Fichier sauvegardé en local ET synchronisé avec succès sur Google Drive !");
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Le fichier a bien été sauvegardé sur cet ordinateur, MAIS l'envoi vers Google Drive a échoué.\nVeuillez vérifier la connexion internet.");
                    alert.setHeaderText("Alerte: Échec Cloud");
                    alert.show();
                }
            } else {
                com.pharmacie.utils.AuditLogger.log("BACKUP_SQL", "FAILED (Manuel) - Fichier vide");
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la sauvegarde.\nVérifiez que mysqldump est installé et accessible sur ce PC.");
                alert.setHeaderText("Echec du Backup");
                alert.show();
            }
        }
    }
}
