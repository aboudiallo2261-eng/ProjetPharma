package com.pharmacie.controllers;

import com.pharmacie.models.Achat;
import com.pharmacie.models.LigneAchat;
import com.pharmacie.models.PharmacieInfo;
import com.pharmacie.dao.PharmacieInfoDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Optional;

public class AchatDetailsController {

    @FXML private Label lblPharmacieNom;
    @FXML private Label lblPharmacieContact;
    @FXML private Label lblAchatDate;
    @FXML private Label lblFournisseurNom;
    @FXML private Label lblFournisseurContact;
    @FXML private Label lblFournisseurAdresse;
    @FXML private Label lblRefFacture;
    @FXML private Label lblAchatId;
    @FXML private Label lblTotalCommande;
    
    @FXML private TableView<LigneAchat> tableLignes;
    @FXML private TableColumn<LigneAchat, String> colProduit;
    @FXML private TableColumn<LigneAchat, String> colLot;
    @FXML private TableColumn<LigneAchat, Integer> colQte;
    @FXML private TableColumn<LigneAchat, Double> colPrixU;
    @FXML private TableColumn<LigneAchat, String> colTotal;

    private Achat achatReference;
    private PharmacieInfoDAO infoDAO = new PharmacieInfoDAO();

    @FXML
    public void initialize() {
        colProduit.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduit().getNom()));
        colLot.setCellValueFactory(cell -> cell.getValue().getLot() != null ? new SimpleStringProperty(cell.getValue().getLot().getNumeroLot()) : new SimpleStringProperty(""));
        colQte.setCellValueFactory(new PropertyValueFactory<>("quantiteAchetee"));
        colPrixU.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        colTotal.setCellValueFactory(cell -> {
            double lineTotal = cell.getValue().getQuantiteAchetee() * cell.getValue().getPrixUnitaire();
            return new SimpleStringProperty(String.format("%.0f", lineTotal));
        });
    }

    public void initData(Achat achat) {
        this.achatReference = achat;

        // Charger données pharmacie
        PharmacieInfo info = infoDAO.getInfo();
        if (info != null) {
            lblPharmacieNom.setText(info.getNom());
            lblPharmacieContact.setText((info.getTelephone() != null ? info.getTelephone() : "") + 
                                       (info.getEmail() != null && !info.getEmail().isEmpty() ? " - " + info.getEmail() : ""));
        } else {
            lblPharmacieNom.setText("Pharmacie Vétérinaire");
            lblPharmacieContact.setText("");
        }

        lblAchatDate.setText(achat.getDateAchat() != null ? achat.getDateAchat().toLocalDate().toString() : "");
        lblFournisseurNom.setText(achat.getFournisseur() != null ? achat.getFournisseur().getNom() : "N/A");
        lblFournisseurContact.setText(achat.getFournisseur() != null && achat.getFournisseur().getTelephone() != null ? "Tel: " + achat.getFournisseur().getTelephone() : "");
        lblFournisseurAdresse.setText(achat.getFournisseur() != null && achat.getFournisseur().getAdresse() != null ? achat.getFournisseur().getAdresse() : "");
        lblRefFacture.setText(achat.getReferenceFacture() != null && !achat.getReferenceFacture().isEmpty() ? achat.getReferenceFacture() : "Non renseignée");
        lblAchatId.setText(String.valueOf(achat.getId()));

        if (achat.getLignesAchat() != null) {
            tableLignes.setItems(FXCollections.observableArrayList(achat.getLignesAchat()));
            double total = achat.getLignesAchat().stream()
                    .mapToDouble(la -> la.getQuantiteAchetee() * la.getPrixUnitaire()).sum();
            lblTotalCommande.setText(String.format("%.0f FCFA", total));
        }
    }

    @FXML
    public void fermer() {
        Stage stage = (Stage) lblTotalCommande.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void imprimerPdf() {
        Stage stage = (Stage) lblTotalCommande.getScene().getWindow();
        com.pharmacie.utils.PdfService.genererBonDeCommande(achatReference, stage);
    }
}
