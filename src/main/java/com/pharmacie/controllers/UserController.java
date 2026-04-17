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

import java.util.List;

public class UserController {

    // --- Onglet Utilisateurs ---
    @FXML private TextField txtNom;
    @FXML private TextField txtIdentifiant;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtMotDePasse;
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
    }

    private void loadProfils() {
        List<Profil> profils = profilDAO.findAll();
        tableProfils.setItems(FXCollections.observableArrayList(profils));
        cmbProfil.setItems(FXCollections.observableArrayList(profils));
    }

    private void loadUsers() {
        List<User> users = userDAO.findAll();
        tableUsers.setItems(FXCollections.observableArrayList(users));
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
        cmbProfil.getSelectionModel().clearSelection();
        btnSaveUser.setText("Enregistrer l'agent");
        lblErrorText.setVisible(false);
        tableUsers.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSave() {
        String nom = txtNom.getText();
        String identifiant = txtIdentifiant.getText();
        String email = txtEmail.getText();
        String password = txtMotDePasse.getText();
        Profil profil = cmbProfil.getValue();

        if (nom.isEmpty() || identifiant.isEmpty() || profil == null) {
            showError("Le nom, identifiant et profil sont obligatoires.");
            return;
        }

        if (selectedUser == null) { // CREATE
            if (password.isEmpty()) {
                showError("Mot de passe obligatoire pour un nouvel agent.");
                return;
            }
            if (userDAO.findAll().stream().anyMatch(u -> u.getIdentifiant().equalsIgnoreCase(identifiant))) {
                showError("Cet identifiant est déjà pris ! Veuillez en choisir un autre.");
                return;
            }
            User newUser = new User();
            newUser.setNom(nom);
            newUser.setIdentifiant(identifiant);
            newUser.setEmail(email);
            newUser.setProfil(profil);
            newUser.setMotDePasseHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            userDAO.save(newUser);
        } else { // UPDATE
            if (!selectedUser.getIdentifiant().equalsIgnoreCase(identifiant) && 
                userDAO.findAll().stream().anyMatch(u -> u.getIdentifiant().equalsIgnoreCase(identifiant))) {
                showError("Impossible de modifier : cet identifiant appartient déjà à un autre agent !");
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
                showError("Vous ne pouvez pas supprimer votre propre compte.");
                return;
            }
            boolean success = userDAO.delete(selected);
            if (!success) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Suppression Impossible");
                alert.setHeaderText("Cet utilisateur ne peut pas être supprimé de la base.");
                alert.setContentText("Il possède déjà un historique de ventes ou d'actions rattaché à son compte.\n\nAstuce : Pour bloquer son accès, modifiez son compte et attribuez-lui un profil factice sans aucune autorisation (ex: 'Profil Bloqué').");
                alert.showAndWait();
            } else {
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
    }

    @FXML
    public void saveProfil() {
        String nom = txtProfilNom.getText();
        if (nom.isEmpty()) {
            // Afficher alerte
            return;
        }

        if (selectedProfil == null) {
            Profil p = new Profil(nom, txtProfilDesc.getText());
            p.setCanAccessDashboard(chkDashboard.isSelected());
            p.setCanAccessVentes(chkVentes.isSelected());
            p.setCanAccessStock(chkStock.isSelected());
            p.setCanAccessAchats(chkAchats.isSelected());
            p.setCanAccessFournisseurs(chkFournisseurs.isSelected());
            p.setCanAccessRapports(chkRapports.isSelected());
            p.setCanAccessParametres(chkParametres.isSelected());
            profilDAO.save(p);
        } else {
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
            if (success) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Sauvegarde reussie avec succès !\nEmplacement : " + file.getAbsolutePath());
                alert.setHeaderText("Backup Terminé");
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la sauvegarde.\nVérifiez que mysqldump est installé et accessible sur ce PC.");
                alert.setHeaderText("Echec du Backup");
                alert.show();
            }
        }
    }
}
