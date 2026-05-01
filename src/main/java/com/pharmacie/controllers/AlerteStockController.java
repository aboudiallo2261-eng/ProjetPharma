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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Point 1 : Contrôleur de la popup Alertes Stock refactorisé avec TabPane.
 * - Onglet 1 : Ruptures de stock (stock <= seuil d'alerte)
 * - Onglet 2 : Lots périmés ou proches de l'expiration (<=30 jours)
 */
public class AlerteStockController {

    // --- FILTRES COMMUNS ---
    @FXML private ComboBox<Categorie> cmbFiltreCategorie;
    @FXML private TextField txtRecherche;

    // --- ONGLET 1 : RUPTURES ---
    @FXML private Label lblTotalAlertes;
    @FXML private TableView<AlerteModel> tableAlertes;
    @FXML private TableColumn<AlerteModel, String>  colRef;
    @FXML private TableColumn<AlerteModel, String>  colNom;
    @FXML private TableColumn<AlerteModel, String>  colCategorie;
    @FXML private TableColumn<AlerteModel, Integer> colStockActuel;
    @FXML private TableColumn<AlerteModel, Integer> colSeuil;
    @FXML private TableColumn<AlerteModel, Integer> colQteSuggerable;

    // --- ONGLET 2 : PÉRIMÉS ---
    @FXML private Label lblTotalPerimes;
    @FXML private TableView<LotPerimeModel> tablePerimes;
    @FXML private TableColumn<LotPerimeModel, String>  colPerimNom;
    @FXML private TableColumn<LotPerimeModel, String>  colPerimLot;
    @FXML private TableColumn<LotPerimeModel, String>  colPerimExp;
    @FXML private TableColumn<LotPerimeModel, Integer> colPerimQte;
    @FXML private TableColumn<LotPerimeModel, String>  colPerimStatut;
    @FXML private TableColumn<LotPerimeModel, Long>    colPerimJours;

    private ProduitDAO  produitDAO  = new ProduitDAO();
    private LotDAO      lotDAO      = new LotDAO();
    private CategorieDAO categorieDAO = new CategorieDAO();

    private ObservableList<AlerteModel>    alertesMasterData = FXCollections.observableArrayList();
    private ObservableList<LotPerimeModel> perimesMasterData  = FXCollections.observableArrayList();
    private FilteredList<AlerteModel>      filteredAlertes;
    private FilteredList<LotPerimeModel>   filteredPerimes;

    private AchatController achatController;

    public void setAchatController(AchatController achatController) {
        this.achatController = achatController;
    }

    @FXML
    public void initialize() {
        initRupturesColumns();
        initPerimesColumns();

        // Charger les filtres Catégorie
        cmbFiltreCategorie.getItems().add(null);
        cmbFiltreCategorie.getItems().addAll(categorieDAO.findAll());
        cmbFiltreCategorie.setConverter(new javafx.util.StringConverter<Categorie>() {
            @Override public String toString(Categorie c) { return c != null ? c.getNom() : "Toutes les categories"; }
            @Override public Categorie fromString(String s) { return null; }
        });

        // Filtrage dynamique en temps réel
        txtRecherche.textProperty().addListener((obs, oldV, newV) -> filtrerAlertes());

        // Double-clic sur rupture → préparer commande automatique
        tableAlertes.setRowFactory(tv -> {
            TableRow<AlerteModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && achatController != null) {
                    AlerteModel alerte = row.getItem();
                    achatController.preparerCommandeAutomatique(alerte.getProduit(), alerte.getQuantiteSuggerable());
                    ((javafx.stage.Stage) tableAlertes.getScene().getWindow()).close();
                }
            });
            return row;
        });

        chargerAlertes();
        chargerPerimes();
    }

    // =========================================================================
    // ONGLET 1 : RUPTURES
    // =========================================================================

    private void initRupturesColumns() {
        colRef.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getProduit().getId())));
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getNom()));
        colCategorie.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getCategorie().getNom()));
        colStockActuel.setCellValueFactory(new PropertyValueFactory<>("stockActuel"));
        colSeuil.setCellValueFactory(new PropertyValueFactory<>("seuilAlerte"));
        colQteSuggerable.setCellValueFactory(new PropertyValueFactory<>("quantiteSuggerable"));

        // Couleur rouge pour stock critique
        colStockActuel.setCellFactory(col -> new TableCell<AlerteModel, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                boolean selected = getTableRow() != null && getTableRow().isSelected();
                setText(String.valueOf(item));
                setStyle(selected
                    ? "-fx-text-fill: white; -fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;"
                    : "-fx-text-fill: #E74C3C; -fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
            }
        });

        // Couleur bleue pour quantité suggérée
        colQteSuggerable.setCellFactory(col -> new TableCell<AlerteModel, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                boolean selected = getTableRow() != null && getTableRow().isSelected();
                setText(String.valueOf(item));
                setStyle(selected
                    ? "-fx-text-fill: white; -fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;"
                    : "-fx-text-fill: #2980B9; -fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
            }
        });
    }

    private void chargerAlertes() {
        alertesMasterData.clear();
        List<Produit> produits = produitDAO.findAll();
        List<Lot> tousLesLots = lotDAO.findAll();

        Map<Long, Integer> stockParProduit = new HashMap<>();
        for (Lot l : tousLesLots) {
            stockParProduit.put(l.getProduit().getId(),
                stockParProduit.getOrDefault(l.getProduit().getId(), 0) + l.getQuantiteStock());
        }

        for (Produit p : produits) {
            // On ignore les produits qui n'ont jamais eu de lot (pas de vrai stock géré)
            if (!stockParProduit.containsKey(p.getId())) continue;

            int qteAlerte = p.getSeuilAlerte() != null ? p.getSeuilAlerte() : 5;
            int qteStock  = stockParProduit.get(p.getId());
            if (qteStock <= qteAlerte) {
                int qteSuggerable = (qteAlerte * 2) - qteStock;
                if (qteSuggerable < qteAlerte) qteSuggerable = qteAlerte;
                alertesMasterData.add(new AlerteModel(p, qteStock, qteAlerte, qteSuggerable));
            }
        }

        filteredAlertes = new FilteredList<>(alertesMasterData, p -> true);
        tableAlertes.setItems(filteredAlertes);
        lblTotalAlertes.setText(String.valueOf(alertesMasterData.size()));
    }

    // =========================================================================
    // ONGLET 2 : PÉRIMÉS / PROCHES EXPIRATION
    // =========================================================================

    private static final int SEUIL_JOURS_ALERTE = 30; // Délai avant expiration considéré comme critique
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private void initPerimesColumns() {
        colPerimNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomProduit()));
        colPerimLot.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNumLot()));
        colPerimExp.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateExpStr()));
        colPerimQte.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQte()).asObject());
        colPerimStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatut()));
        colPerimJours.setCellValueFactory(data -> {
            long j = data.getValue().getJoursRestants();
            return new javafx.beans.property.SimpleLongProperty(j).asObject();
        });

        // Code couleur sur la colonne Statut (rouge = périmé, orange = proche)
        colPerimStatut.setCellFactory(col -> new TableCell<LotPerimeModel, String>() {
            {
                tableRowProperty().addListener((obs, o, n) -> {
                    if (n != null) n.selectedProperty().addListener((obs2, was, is) -> { if (!isEmpty()) updateItem(getItem(), false); });
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                boolean selected = getTableRow() != null && getTableRow().isSelected();
                setText(item);
                if (selected) {
                    setStyle("-fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;");
                } else if (item.startsWith("PERIME")) {
                    setStyle("-fx-text-fill: #DC2626; -fx-alignment: CENTER; -fx-font-weight: bold;"); // Red 600
                } else {
                    setStyle("-fx-text-fill: #F59E0B; -fx-alignment: CENTER; -fx-font-weight: bold;"); // Amber 500
                }
            }
        });

        // Colonne Jours Restants : négatif = rouge, positif = orange
        colPerimJours.setCellFactory(col -> new TableCell<LotPerimeModel, Long>() {
            {
                tableRowProperty().addListener((obs, o, n) -> {
                    if (n != null) n.selectedProperty().addListener((obs2, was, is) -> { if (!isEmpty()) updateItem(getItem(), false); });
                });
            }
            @Override protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                boolean selected = getTableRow() != null && getTableRow().isSelected();
                setText(item < 0 ? item + " j" : "+" + item + " j");
                if (selected) {
                    setStyle("-fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;");
                } else if (item < 0) {
                    setStyle("-fx-text-fill: #DC2626; -fx-alignment: CENTER; -fx-font-weight: bold;"); // Red 600
                } else {
                    setStyle("-fx-text-fill: #F59E0B; -fx-alignment: CENTER; -fx-font-weight: bold;"); // Amber 500
                }
            }
        });
    }

    private void chargerPerimes() {
        perimesMasterData.clear();
        List<Lot> tousLesLots = lotDAO.findAll();
        LocalDate today = LocalDate.now();
        LocalDate seuilAlerte = today.plusDays(SEUIL_JOURS_ALERTE);

        for (Lot l : tousLesLots) {
            if (l.getDateExpiration() == null || l.getQuantiteStock() <= 0) continue; // On ignore les lots sans date ou dont le stock est épuisé
            LocalDate exp = l.getDateExpiration();
            if (!exp.isAfter(seuilAlerte)) { // périmé ou dans les 30 prochains jours
                long joursRestants = ChronoUnit.DAYS.between(today, exp);
                String statut = joursRestants < 0 ? "PERIME" : "Expire dans " + joursRestants + "j";
                perimesMasterData.add(new LotPerimeModel(
                    l.getProduit().getNom(), l.getNumeroLot(),
                    exp.format(FMT_DATE), l.getQuantiteStock(), statut, joursRestants));
            }
        }

        // Tri : périmés d'abord, puis par date ascendante
        FXCollections.sort(perimesMasterData, (a, b) -> Long.compare(a.getJoursRestants(), b.getJoursRestants()));

        filteredPerimes = new FilteredList<>(perimesMasterData, p -> true);
        tablePerimes.setItems(filteredPerimes);
        lblTotalPerimes.setText(String.valueOf(perimesMasterData.size()));
    }

    @FXML
    private void filtrerAlertes() {
        Categorie cat = cmbFiltreCategorie.getValue();
        String search = txtRecherche.getText() != null ? txtRecherche.getText().toLowerCase() : "";

        if (filteredAlertes != null) {
            filteredAlertes.setPredicate(alerte -> {
                boolean matchCat = (cat == null) || alerte.getProduit().getCategorie().getId().equals(cat.getId());
                boolean matchNom = search.isEmpty() || alerte.getProduit().getNom().toLowerCase().contains(search);
                return matchCat && matchNom;
            });
            lblTotalAlertes.setText(String.valueOf(filteredAlertes.size()));
        }

        if (filteredPerimes != null) {
            filteredPerimes.setPredicate(lot -> {
                boolean matchNom = search.isEmpty() || lot.getNomProduit().toLowerCase().contains(search);
                return matchNom;
            });
            lblTotalPerimes.setText(String.valueOf(filteredPerimes.size()));
        }
    }

    @FXML
    private void imprimerListe() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Impression : " + alertesMasterData.size() + " rupture(s) et " + perimesMasterData.size() + " lot(s) perime(s)/proches.");
        alert.setTitle("Impression Alertes");
        alert.showAndWait();
    }

    // =========================================================================
    // MODÈLES INTERNES
    // =========================================================================

    /** Modèle pour l'onglet Ruptures */
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

        public Produit getProduit()          { return produit; }
        public int getStockActuel()          { return stockActuel; }
        public int getSeuilAlerte()          { return seuilAlerte; }
        public int getQuantiteSuggerable()   { return quantiteSuggerable; }
    }

    /** Modèle pour l'onglet Périmés */
    public static class LotPerimeModel {
        private final String  nomProduit;
        private final String  numLot;
        private final String  dateExpStr;
        private final int     qte;
        private final String  statut;
        private final long    joursRestants;

        public LotPerimeModel(String nomProduit, String numLot, String dateExpStr,
                              int qte, String statut, long joursRestants) {
            this.nomProduit    = nomProduit;
            this.numLot        = numLot;
            this.dateExpStr    = dateExpStr;
            this.qte           = qte;
            this.statut        = statut;
            this.joursRestants = joursRestants;
        }

        public String  getNomProduit()    { return nomProduit; }
        public String  getNumLot()        { return numLot; }
        public String  getDateExpStr()    { return dateExpStr; }
        public int     getQte()           { return qte; }
        public String  getStatut()        { return statut; }
        public long    getJoursRestants() { return joursRestants; }
    }
}
