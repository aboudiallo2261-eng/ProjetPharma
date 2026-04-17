package com.pharmacie.controllers;

import com.pharmacie.dao.GenericDAO;
import com.pharmacie.models.Fournisseur;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

public class FournisseurController {

    @FXML private TextField txtNom;
    @FXML private TextField txtContact;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAdresse;
    @FXML private TextArea txtConditions;
    @FXML private Label lblErrorText;
    @FXML private Button btnSave;

    @FXML private TableView<Fournisseur> tableFournisseurs;
    @FXML private TableColumn<Fournisseur, Long> colId;
    @FXML private TableColumn<Fournisseur, String> colNom;
    @FXML private TableColumn<Fournisseur, String> colContact;
    @FXML private TableColumn<Fournisseur, String> colTelephone;
    @FXML private TableColumn<Fournisseur, String> colEmail;

    private GenericDAO<Fournisseur> fournisseurDAO = new GenericDAO<>(Fournisseur.class);
    private Fournisseur selectedFournisseur;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadFournisseurs();

        tableFournisseurs.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });
    }

    private void loadFournisseurs() {
        try {
            List<Fournisseur> list = fournisseurDAO.findAll();
            tableFournisseurs.setItems(FXCollections.observableArrayList(list));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void populateForm(Fournisseur f) {
        selectedFournisseur = f;
        txtNom.setText(f.getNom() != null ? f.getNom() : "");
        txtContact.setText(f.getContact() != null ? f.getContact() : "");
        txtTelephone.setText(f.getTelephone() != null ? f.getTelephone() : "");
        txtEmail.setText(f.getEmail() != null ? f.getEmail() : "");
        txtAdresse.setText(f.getAdresse() != null ? f.getAdresse() : "");
        txtConditions.setText(f.getConditions() != null ? f.getConditions() : "");
        
        btnSave.setText("Mettre à jour");
        lblErrorText.setVisible(false);
    }

    @FXML
    public void handleReset() {
        selectedFournisseur = null;
        txtNom.clear();
        txtContact.clear();
        txtTelephone.clear();
        txtEmail.clear();
        txtAdresse.clear();
        txtConditions.clear();
        
        btnSave.setText("Enregistrer");
        lblErrorText.setVisible(false);
        tableFournisseurs.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSave() {
        String nom = txtNom.getText();
        if (nom == null || nom.trim().isEmpty()) {
            showError("Le nom de l'entreprise est obligatoire.");
            return;
        }

        if (selectedFournisseur == null) {
            Fournisseur f = new Fournisseur();
            fillFournisseurDetails(f);
            fournisseurDAO.save(f);
        } else {
            fillFournisseurDetails(selectedFournisseur);
            fournisseurDAO.update(selectedFournisseur);
        }

        handleReset();
        loadFournisseurs();
        if (tableFournisseurs != null) tableFournisseurs.refresh();
    }

    private void fillFournisseurDetails(Fournisseur f) {
        f.setNom(txtNom.getText().trim());
        f.setContact(txtContact.getText());
        f.setTelephone(txtTelephone.getText());
        f.setEmail(txtEmail.getText());
        f.setAdresse(txtAdresse.getText());
        f.setConditions(txtConditions.getText());
    }

    @FXML
    public void handleDelete() {
        Fournisseur selected = tableFournisseurs.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean success = fournisseurDAO.delete(selected);
            if (!success) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Suppression Refusée");
                alert.setHeaderText("Ce fournisseur ne peut pas être supprimé !");
                alert.setContentText("Il possède déjà un historique d'achats rattaché dans la base comptable.\nPour ne pas briser la traçabilité des commandes, le système bloque la destruction stricte de sa fiche.");
                alert.showAndWait();
            } else {
                handleReset();
                loadFournisseurs();
            }
        } else {
            showError("Veuillez sélectionner un fournisseur à supprimer.");
        }
    }

    private void showError(String message) {
        lblErrorText.setText(message);
        lblErrorText.setVisible(true);
    }
}
