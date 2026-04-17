package com.pharmacie.controllers;

import com.pharmacie.dao.LotDAO;
import com.pharmacie.dao.ProduitDAO;
import com.pharmacie.dao.CategorieDAO;
import com.pharmacie.models.Categorie;
import com.pharmacie.models.Lot;
import com.pharmacie.models.Produit;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.print.PrinterJob;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlerteStockController {

    @FXML private ComboBox<Categorie> cmbFiltreCategorie;
    @FXML private TextField txtRecherche;
    @FXML private Label lblTotalAlertes;

    @FXML private TableView<AlerteModel> tableAlertes;
    @FXML private TableColumn<AlerteModel, String> colRef;
    @FXML private TableColumn<AlerteModel, String> colNom;
    @FXML private TableColumn<AlerteModel, String> colCategorie;
    @FXML private TableColumn<AlerteModel, Integer> colStockActuel;
    @FXML private TableColumn<AlerteModel, Integer> colSeuil;
    @FXML private TableColumn<AlerteModel, Integer> colQteSuggerable;

    private ProduitDAO produitDAO = new ProduitDAO();
    private LotDAO lotDAO = new LotDAO();
    private CategorieDAO categorieDAO = new CategorieDAO();

    private ObservableList<AlerteModel> alertesMasterData = FXCollections.observableArrayList();
    private FilteredList<AlerteModel> filteredAlertes;
    
    private AchatController achatController;

    public void setAchatController(AchatController achatController) {
        this.achatController = achatController;
    }

    @FXML
    public void initialize() {
        // Initialisation des colonnes
        colRef.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getProduit().getId())));
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getNom()));
        colCategorie.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getCategorie().getNom()));
        
        colStockActuel.setCellValueFactory(new PropertyValueFactory<>("stockActuel"));
        colSeuil.setCellValueFactory(new PropertyValueFactory<>("seuilAlerte"));
        colQteSuggerable.setCellValueFactory(new PropertyValueFactory<>("quantiteSuggerable"));

        // Charger les filtres Catégorie
        cmbFiltreCategorie.getItems().add(null);
        cmbFiltreCategorie.getItems().addAll(categorieDAO.findAll());
        cmbFiltreCategorie.setConverter(new javafx.util.StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c != null ? c.getNom() : "Toutes les catégories"; }
            @Override public Categorie fromString(String s) { return null; }
        });

        // Appliquer filtres dynamiques textuels
        txtRecherche.textProperty().addListener((obs, oldV, newV) -> filtrerAlertes());

        // Double-clic pour réapprovisionnement depuis l'alerte
        tableAlertes.setRowFactory(tv -> {
            TableRow<AlerteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty()) && achatController != null) {
                    AlerteModel alerte = row.getItem();
                    achatController.preparerCommandeAutomatique(alerte.getProduit(), alerte.getQuantiteSuggerable());
                    // Fermer la fenêtre pop-up d'alertes
                    ((javafx.stage.Stage) tableAlertes.getScene().getWindow()).close();
                }
            });
            return row;
        });

        chargerAlertes();
    }

    private void chargerAlertes() {
        alertesMasterData.clear();
        
        List<Produit> produits = produitDAO.findAll();
        List<Lot> tousLesLots = lotDAO.findAll();
        
        // Grouper les quantités par produit
        Map<Long, Integer> stockParProduit = new HashMap<>();
        for (Lot l : tousLesLots) {
            if (l.getDateExpiration() == null || !l.getDateExpiration().isBefore(LocalDate.now())) {
                stockParProduit.put(l.getProduit().getId(), 
                    stockParProduit.getOrDefault(l.getProduit().getId(), 0) + l.getQuantiteStock());
            }
        }

        for (Produit p : produits) {
            int qteAlerte = p.getSeuilAlerte() != null ? p.getSeuilAlerte() : 5;
            int qteStock = stockParProduit.getOrDefault(p.getId(), 0);

            // Règle d'alerte : Stock Actuel <= Seuil Alerte
            if (qteStock <= qteAlerte) {
                // On suggère de commander le double du seuil d'alerte ou la différence
                int qteSuggerable = (qteAlerte * 2) - qteStock;
                if(qteSuggerable < qteAlerte) qteSuggerable = qteAlerte;

                alertesMasterData.add(new AlerteModel(p, qteStock, qteAlerte, qteSuggerable));
            }
        }

        filteredAlertes = new FilteredList<>(alertesMasterData, p -> true);
        tableAlertes.setItems(filteredAlertes);
        lblTotalAlertes.setText(alertesMasterData.size() + " Produit(s)");
    }

    @FXML
    private void filtrerAlertes() {
        Categorie cat = cmbFiltreCategorie.getValue();
        String search = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase() : "";

        filteredAlertes.setPredicate(alerte -> {
            boolean matchCat = (cat == null) || alerte.getProduit().getCategorie().getId().equals(cat.getId());
            boolean matchNom = search.isEmpty() || alerte.getProduit().getNom().toLowerCase().contains(search);
            return matchCat && matchNom;
        });
        
        lblTotalAlertes.setText(filteredAlertes.size() + " Produit(s)");
    }

    @FXML
    private void imprimerListe() {
        if (filteredAlertes.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "La liste est vide.");
            alert.showAndWait();
            return;
        }

        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob != null && printerJob.showPrintDialog(tableAlertes.getScene().getWindow())) {
            // Un style simple pour l'impression
            TableView<AlerteModel> printTable = new TableView<>();
            printTable.setItems(filteredAlertes);
            printTable.getColumns().addAll(tableAlertes.getColumns());
            printTable.setStyle("-fx-font-size: 10px;");
            
            boolean success = printerJob.printPage(tableAlertes); // Simplification, idéalement on formatte tout le document
            if (success) {
                printerJob.endJob();
            }
        }
    }

    // --- Modèle interne pour la TableView ---
    public static class AlerteModel {
        private Produit produit;
        private int stockActuel;
        private int seuilAlerte;
        private int quantiteSuggerable;

        public AlerteModel(Produit produit, int stockActuel, int seuilAlerte, int quantiteSuggerable) {
            this.produit = produit;
            this.stockActuel = stockActuel;
            this.seuilAlerte = seuilAlerte;
            this.quantiteSuggerable = quantiteSuggerable;
        }

        public Produit getProduit() { return produit; }
        public int getStockActuel() { return stockActuel; }
        public int getSeuilAlerte() { return seuilAlerte; }
        public int getQuantiteSuggerable() { return quantiteSuggerable; }
    }
}
