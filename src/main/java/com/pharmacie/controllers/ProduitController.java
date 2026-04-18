package com.pharmacie.controllers;

import com.pharmacie.dao.CategorieDAO;
import com.pharmacie.dao.EspeceDAO;
import com.pharmacie.dao.GenericDAO;
import com.pharmacie.dao.LotDAO;
import com.pharmacie.dao.ProduitDAO;
import com.pharmacie.models.Categorie;
import com.pharmacie.models.Espece;
import com.pharmacie.models.LigneVente;
import com.pharmacie.models.Lot;
import com.pharmacie.models.Produit;
import com.pharmacie.models.AjustementStock;
import com.pharmacie.dao.AjustementStockDAO;
import com.pharmacie.utils.SessionManager;
import com.pharmacie.dao.MouvementDAO;
import com.pharmacie.models.MouvementStock;
import javafx.collections.FXCollections;
import java.time.LocalDateTime;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.util.List;

public class ProduitController {

    // --- TAB PRODUITS ---
    @FXML
    private TextField txtProdNom, txtProdPrixVente, txtProdUnites, txtProdPrixUnite, txtProdSeuil, txtSearchProd;
    @FXML
    private ComboBox<Categorie> cmbProdCategorie;
    @FXML
    private ComboBox<Espece> cmbProdEspece;
    @FXML
    private CheckBox chkDeconditionnable;
    @FXML
    private HBox boxDeconditionnement;
    @FXML
    private Label lblProdError;
    @FXML
    private Button btnSaveProd;
    @FXML
    private TableView<Produit> tableProduits;
    @FXML
    private TableColumn<Produit, Long> colProdId;
    @FXML
    private TableColumn<Produit, String> colProdNom, colProdCat, colProdEsp;
    @FXML
    private TableColumn<Produit, Double> colProdPrixVente;

    // --- TAB CATEGORIES ---
    @FXML
    private TextField txtCatNom;
    @FXML
    private Label lblCatError;
    @FXML
    private Button btnSaveCat;
    @FXML
    private TableView<Categorie> tableCategories;
    @FXML
    private TableColumn<Categorie, Long> colCatId;
    @FXML
    private TableColumn<Categorie, String> colCatNom;

    // --- TAB ETAT STOCK ---
    @FXML
    private ComboBox<String> cmbFiltreStockCat, cmbFiltreStockEsp, cmbFiltreStockStatut;
    @FXML
    private TextField txtSearchStock;
    @FXML
    private javafx.scene.control.DatePicker dpFiltreExpAvant; // Filtre expiration avant date
    @FXML
    private TableView<EtatStockDTO> tableEtatStock;
    @FXML
    private TableColumn<EtatStockDTO, String> colStockProd, colStockLot, colStockExp;
    @FXML
    private TableColumn<EtatStockDTO, String> colStockQte;
    @FXML
    private TableColumn<EtatStockDTO, Integer> colStockVendus, colStockSeuil;
    @FXML
    private TableColumn<EtatStockDTO, Double> colStockPrix;
    @FXML
    private TableColumn<EtatStockDTO, String> colStockJours;   // Jours restants
    @FXML
    private TableColumn<EtatStockDTO, String> colStockValeur;  // Valeur du lot
    @FXML
    private Label lblTotalStockValeur;  // KPI valeur totale en bas de tableau
    @FXML
    private Button btnAjustementStock, btnVoirLot;
    @FXML
    private CheckBox chkInclureArchives;

    // --- TAB ESPECES ---
    @FXML
    private TextField txtEspNom;
    @FXML
    private Label lblEspError;
    @FXML
    private Button btnSaveEsp;
    @FXML
    private TableView<Espece> tableEspeces;
    @FXML
    private TableColumn<Espece, Long> colEspId;
    @FXML
    private TableColumn<Espece, String> colEspNom;

    private ProduitDAO produitDAO = new ProduitDAO();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private EspeceDAO especeDAO = new EspeceDAO();
    private LotDAO lotDAO = new LotDAO();
    private GenericDAO<LigneVente> ligneVenteDAO = new GenericDAO<>(LigneVente.class);
    private AjustementStockDAO ajustementStockDAO = new AjustementStockDAO();
    private MouvementDAO mouvementDAO = new MouvementDAO();

    private List<EtatStockDTO> masterStockList = new java.util.ArrayList<>();

    private Produit selectedProduit;
    private Categorie selectedCategorie;
    private Espece selectedEspece;

    @FXML
    public void initialize() {
        initFormatters();
        initTableColumns();
        loadAllData();
        setupListeners();
    }

    private void initFormatters() {
        StringConverter<Categorie> catConverter = new StringConverter<>() {
            @Override
            public String toString(Categorie object) {
                return object != null ? object.getNom() : "";
            }

            @Override
            public Categorie fromString(String string) {
                return null;
            } // Pas nécessaire pour cette UI
        };
        cmbProdCategorie.setConverter(catConverter);

        StringConverter<Espece> espConverter = new StringConverter<>() {
            @Override
            public String toString(Espece object) {
                return object != null ? object.getNom() : "";
            }

            @Override
            public Espece fromString(String string) {
                return null;
            } // Pas nécessaire
        };
        cmbProdEspece.setConverter(espConverter);
    }

    private void initTableColumns() {
        colProdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProdNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colProdCat.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCategorie().getNom()));
        colProdEsp.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEspece().getNom()));
        colProdPrixVente.setCellValueFactory(new PropertyValueFactory<>("prixVente"));

        colCatId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCatNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        colEspId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEspNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        initEtatStockColumns();
    }

    private void initEtatStockColumns() {
        colStockProd.setCellValueFactory(new PropertyValueFactory<>("produitNom"));
        colStockLot.setCellValueFactory(new PropertyValueFactory<>("lotNumero"));
        colStockExp.setCellValueFactory(new PropertyValueFactory<>("dateExpiration"));
        colStockQte.setCellValueFactory(new PropertyValueFactory<>("quantiteFormatee"));
        colStockVendus.setCellValueFactory(new PropertyValueFactory<>("quantiteVendue"));
        colStockPrix.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));

        // Nouvelles colonnes intelligentes
        if (colStockJours != null)
            colStockJours.setCellValueFactory(new PropertyValueFactory<>("joursRestantsFormate"));
        if (colStockValeur != null)
            colStockValeur.setCellValueFactory(new PropertyValueFactory<>("valeurFormatee"));
        if (colStockSeuil != null)
            colStockSeuil.setCellValueFactory(new PropertyValueFactory<>("seuilAlerte")); // #6

        // Sélection multiple pour pertes groupées (#3)
        tableEtatStock.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // Pseudo-classes CSS pour coloration des alertes — compatibles avec la sélection native JavaFX
        javafx.css.PseudoClass expirePseudo = javafx.css.PseudoClass.getPseudoClass("expire");
        javafx.css.PseudoClass alertePseudo = javafx.css.PseudoClass.getPseudoClass("alerte");

        tableEtatStock.setRowFactory(tv -> new TableRow<EtatStockDTO>() {
            @Override
            protected void updateItem(EtatStockDTO item, boolean empty) {
                super.updateItem(item, empty);
                boolean isExpire = !empty && item != null && item.isEstExpire();
                boolean isAlerte = !empty && item != null && !isExpire && item.getQuantiteStock() <= item.getSeuilAlerte();
                pseudoClassStateChanged(expirePseudo, isExpire);
                pseudoClassStateChanged(alertePseudo, isAlerte);
            }
        });

        // Double-clic → Fiche Lot Premium (#9)
        tableEtatStock.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableEtatStock.getSelectionModel().getSelectedItem() != null) {
                showLotDetail(tableEtatStock.getSelectionModel().getSelectedItem());
            }
        });

        // Activation dynamique des boutons
        tableEtatStock.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener.Change<? extends EtatStockDTO> c) -> {
            int selectedCount = tableEtatStock.getSelectionModel().getSelectedItems().size();
            if (btnAjustementStock != null) btnAjustementStock.setDisable(selectedCount == 0);
            // La fiche détails ne s'ouvre que pour UNE SEULE ligne sélectionnée
            if (btnVoirLot != null) btnVoirLot.setDisable(selectedCount != 1); 
        });

        // Tri par défaut : les lots expirant le plus tôt en premier
        if (colStockJours != null) {
            colStockJours.setSortType(javafx.scene.control.TableColumn.SortType.ASCENDING);
            tableEtatStock.getSortOrder().add(colStockJours);
        }
        // Note : pas de CONSTRAINED_RESIZE_POLICY ici — trop de colonnes (9),
        // on utilise prefWidth dans le FXML pour un rendu équilibré.
    }

    private void setupListeners() {
        chkDeconditionnable.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boxDeconditionnement.setDisable(!newVal);
            if (!newVal) {
                txtProdUnites.clear();
                txtProdPrixUnite.clear();
            }
        });

        tableProduits.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null)
                populateProdForm(newVal);
        });
        tableCategories.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null)
                populateCatForm(newVal);
        });
        tableEspeces.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null)
                populateEspForm(newVal);
        });

        txtSearchProd.textProperty().addListener((obs, oldV, newV) -> searchProduit());
        txtSearchStock.textProperty().addListener((obs, oldV, newV) -> filtrerStock());

        tableEtatStock.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (btnAjustementStock != null) {
                btnAjustementStock.setDisable(newVal == null);
            }
        });

        // 5. Force numeric input only for specific numeric fields
        txtProdUnites.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*"))
                txtProdUnites.setText(newV.replaceAll("[^\\d]", ""));
        });
        txtProdSeuil.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*"))
                txtProdSeuil.setText(newV.replaceAll("[^\\d]", ""));
        });
        txtProdPrixVente.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*(\\.\\d*)?"))
                txtProdPrixVente.setText(oldV);
        });
        txtProdPrixUnite.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*(\\.\\d*)?"))
                txtProdPrixUnite.setText(oldV);
        });
    }

    private void loadAllData() {
        loadCategories();
        loadEspeces();
        loadProduits();
        loadEtatStock();
    }

    // --- LOGIQUE PRODUITS ---
    private void loadProduits() {
        tableProduits.getItems().setAll(produitDAO.findAll());
    }

    private void populateProdForm(Produit p) {
        selectedProduit = p;
        txtProdNom.setText(p.getNom());
        cmbProdCategorie.getSelectionModel().select(p.getCategorie());
        cmbProdEspece.getSelectionModel().select(p.getEspece());
        txtProdPrixVente.setText(String.valueOf(p.getPrixVente()));
        chkDeconditionnable.setSelected(p.getEstDeconditionnable() != null && p.getEstDeconditionnable());
        txtProdUnites.setText(p.getUnitesParBoite() != null ? String.valueOf(p.getUnitesParBoite()) : "");
        txtProdPrixUnite.setText(p.getPrixVenteUnite() != null ? String.valueOf(p.getPrixVenteUnite()) : "");
        txtProdSeuil.setText(p.getSeuilAlerte() != null ? String.valueOf(p.getSeuilAlerte()) : "5");

        btnSaveProd.setText("Mettre à jour");
        lblProdError.setVisible(false);
    }

    @FXML
    public void resetProdForm() {
        selectedProduit = null;
        txtProdNom.clear();
        // setValue(null) force le retour du promptText visible dans le ComboBox
        cmbProdCategorie.setValue(null);
        cmbProdCategorie.setPromptText("Catégorie *");
        cmbProdEspece.setValue(null);
        cmbProdEspece.setPromptText("Espèce *");
        txtProdPrixVente.clear();
        chkDeconditionnable.setSelected(false);
        txtProdUnites.clear();
        txtProdPrixUnite.clear();
        txtProdSeuil.clear();
        btnSaveProd.setText("Enregistrer");
        lblProdError.setVisible(false);
        if (tableProduits != null)
            tableProduits.getSelectionModel().clearSelection();
    }

    @FXML
    public void saveProduit() {
        try {
            // --- VALIDATION CHAMP PAR CHAMP avec feedback visuel premium ---
            String nom = txtProdNom.getText() == null ? "" : txtProdNom.getText().trim();
            if (nom.isEmpty()) {
                showErrorEffect(txtProdNom);
                showProdError("Le nom du produit est obligatoire.");
                txtProdNom.requestFocus();
                return;
            }

            Categorie cat = cmbProdCategorie.getValue();
            if (cat == null) {
                showErrorEffect(cmbProdCategorie);
                showProdError("Veuillez sélectionner une Catégorie.");
                return;
            }

            Espece esp = cmbProdEspece.getValue();
            if (esp == null) {
                showErrorEffect(cmbProdEspece);
                showProdError("Veuillez sélectionner une Espèce.");
                return;
            }

            String prixVenStr = txtProdPrixVente.getText() == null ? "" : txtProdPrixVente.getText().trim();
            if (prixVenStr.isEmpty()) {
                showErrorEffect(txtProdPrixVente);
                showProdError("Le prix de vente est obligatoire.");
                txtProdPrixVente.requestFocus();
                return;
            }

            double prixVen;
            try {
                prixVen = Double.parseDouble(prixVenStr.replace(",", "."));
                if (prixVen < 0) {
                    showErrorEffect(txtProdPrixVente);
                    showProdError("Le prix de vente ne peut pas être négatif.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showErrorEffect(txtProdPrixVente);
                showProdError("Prix de vente invalide. Saisissez un nombre (ex: 1500).");
                return;
            }

            int seuil = 5; // valeur par défaut
            if (!txtProdSeuil.getText().trim().isEmpty()) {
                try {
                    seuil = Integer.parseInt(txtProdSeuil.getText().trim());
                    if (seuil < 0) {
                        showErrorEffect(txtProdSeuil);
                        showProdError("Le seuil d'alerte ne peut pas être négatif.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showErrorEffect(txtProdSeuil);
                    showProdError("Seuil d'alerte invalide. Saisissez un entier.");
                    return;
                }
            }

            Produit p = selectedProduit == null ? new Produit() : selectedProduit;
            p.setNom(nom);
            p.setCategorie(cat);
            p.setEspece(esp);
            p.setPrixAchat(0.0);
            p.setPrixVente(prixVen);
            p.setEstDeconditionnable(chkDeconditionnable.isSelected());
            p.setSeuilAlerte(seuil);

            if (p.getEstDeconditionnable()) {
                String unitesStr = txtProdUnites.getText().trim();
                String prixUniteStr = txtProdPrixUnite.getText().trim();
                if (unitesStr.isEmpty()) {
                    showErrorEffect(txtProdUnites);
                    showProdError("Le nombre d'unités par boîte est requis (déconditionnement).");
                    return;
                }
                if (prixUniteStr.isEmpty()) {
                    showErrorEffect(txtProdPrixUnite);
                    showProdError("Le prix de vente à l'unité est requis (déconditionnement).");
                    return;
                }
                try {
                    p.setUnitesParBoite(Integer.parseInt(unitesStr));
                    p.setPrixVenteUnite(Double.parseDouble(prixUniteStr.replace(",", ".")));
                } catch (NumberFormatException ex) {
                    showErrorEffect(txtProdUnites);
                    showProdError("Valeurs de déconditionnement invalides.");
                    return;
                }
            } else {
                p.setUnitesParBoite(null);
                p.setPrixVenteUnite(null);
            }

            if (selectedProduit == null)
                produitDAO.save(p);
            else
                produitDAO.update(p);

            resetProdForm();
            loadProduits();
            tableProduits.refresh();

        } catch (Exception e) {
            showProdError("Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    public void deleteProduit() {
        if (selectedProduit == null) {
            showProdError("Sélectionnez un produit à supprimer.");
            return;
        }
        // Dialogue de confirmation avant suppression
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de Suppression");
        confirm.setHeaderText("Supprimer \"" + selectedProduit.getNom() + "\" ?");
        confirm.setContentText("Cette action est irréversible. Voulez-vous vraiment supprimer ce produit ?");
        ((Button) confirm.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK)).setText("Oui, Supprimer");
        ((Button) confirm.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL)).setText("Annuler");

        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                boolean success = produitDAO.delete(selectedProduit);
                if (!success) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Suppression Refusée");
                    alert.setHeaderText("Ce produit ne peut pas être supprimé !");
                    alert.setContentText(
                            "Il est actuellement lié à un lot en stock ou à des historiques de ventes/achats passés.\nPour ne pas fausser la comptabilité, le système interdit la suppression physique des produits ayant déjà circulé.");
                    alert.showAndWait();
                } else {
                    resetProdForm();
                    loadProduits();
                }
            }
        });
    }

    @FXML
    public void searchProduit() {
        String query = txtSearchProd.getText();
        if (query != null && !query.isEmpty()) {
            tableProduits.getItems().setAll(produitDAO.rechercherParNom(query));
        } else {
            loadProduits();
        }
    }

    private void showProdError(String msg) {
        lblProdError.setText(msg);
        lblProdError.setVisible(true);
    }

    /**
     * Visual Error Feedback — Standard Premium UI/UX.
     * 1. Bordure rouge de 2px autour du champ fautif (2 secondes).
     * 2. Animation "shake" (oscillation horizontale) pour capter l'œil instantanément.
     * Identique au mécanisme implémenté dans AchatController.
     */
    private void showErrorEffect(javafx.scene.Node node) {
        if (node == null) return;
        // --- 1. Bordure rouge ---
        String originalStyle = node.getStyle();
        node.setStyle(originalStyle + "; -fx-border-color: #E74C3C; -fx-border-width: 2px; -fx-border-radius: 4px;");
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> node.setStyle(originalStyle));
        // --- 2. Shake horizontal (oscillation premium) ---
        javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), node);
        shake.setFromX(0); shake.setByX(8);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        // Lancer les deux en parallèle
        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(shake);
        pt.play();
        pause.play();
    }

    /**
     * Ouvre une mini-dialog pour créer une nouvelle Catégorie à la volée,
     * sans quitter l'onglet Produit.
     */
    @FXML
    public void quickAddCategorie() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouvelle Catégorie");
        dialog.setHeaderText("Créer une catégorie rapidement");
        dialog.setContentText("Nom de la catégorie :");
        dialog.getEditor().setStyle("-fx-font-size: 14px;");
        dialog.showAndWait().ifPresent(rawNom -> {
            final String nom = rawNom.trim();
            if (!nom.isEmpty()) {
                Categorie c = new Categorie();
                c.setNom(nom);
                categorieDAO.save(c);
                loadCategories();
                // Sélectionner automatiquement la nouvelle catégorie
                cmbProdCategorie.getItems().stream()
                    .filter(cat -> cat.getNom().equalsIgnoreCase(nom))
                    .findFirst()
                    .ifPresent(cat -> cmbProdCategorie.getSelectionModel().select(cat));
            }
        });
    }

    /**
     * Ouvre une mini-dialog pour créer une nouvelle Espèce à la volée,
     * sans quitter l'onglet Produit.
     */
    @FXML
    public void quickAddEspece() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouvelle Espèce");
        dialog.setHeaderText("Créer une espèce animale rapidement");
        dialog.setContentText("Nom de l'espèce :");
        dialog.getEditor().setStyle("-fx-font-size: 14px;");
        dialog.showAndWait().ifPresent(rawNom -> {
            final String nom = rawNom.trim();
            if (!nom.isEmpty()) {
                Espece e = new Espece();
                e.setNom(nom);
                especeDAO.save(e);
                loadEspeces();
                // Sélectionner automatiquement la nouvelle espèce
                cmbProdEspece.getItems().stream()
                    .filter(esp -> esp.getNom().equalsIgnoreCase(nom))
                    .findFirst()
                    .ifPresent(esp -> cmbProdEspece.getSelectionModel().select(esp));
            }
        });
    }

    // --- LOGIQUE CATEGORIES ---
    private void loadCategories() {
        ObservableList<Categorie> cats = FXCollections.observableArrayList(categorieDAO.findAll());
        tableCategories.setItems(cats);
        cmbProdCategorie.setItems(cats);
    }

    private void populateCatForm(Categorie c) {
        selectedCategorie = c;
        txtCatNom.setText(c.getNom());
        btnSaveCat.setText("Modifier");
    }

    @FXML
    public void resetCatForm() {
        selectedCategorie = null;
        txtCatNom.clear();
        btnSaveCat.setText("Enregistrer");
        lblCatError.setVisible(false);
        if (tableCategories != null)
            tableCategories.getSelectionModel().clearSelection();
    }

    @FXML
    public void saveCategorie() {
        String nom = txtCatNom.getText() == null ? "" : txtCatNom.getText().trim();
        if (nom.isEmpty()) {
            showErrorEffect(txtCatNom);
            lblCatError.setText("Le nom de la catégorie est obligatoire.");
            lblCatError.setVisible(true);
            txtCatNom.requestFocus();
            return;
        }
        lblCatError.setVisible(false);
        Categorie c = selectedCategorie == null ? new Categorie() : selectedCategorie;
        c.setNom(nom);
        if (selectedCategorie == null)
            categorieDAO.save(c);
        else
            categorieDAO.update(c);
        resetCatForm();
        loadCategories();
        tableCategories.refresh();
    }

    @FXML
    public void deleteCategorie() {
        if (selectedCategorie != null) {
            boolean success = categorieDAO.delete(selectedCategorie);
            if (!success) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "Impossible de supprimer cette catégorie car elle contient déjà des produits enregistrés.");
                alert.showAndWait();
            } else {
                resetCatForm();
                loadCategories();
            }
        }
    }

    // --- LOGIQUE ESPECES ---
    private void loadEspeces() {
        ObservableList<Espece> esps = FXCollections.observableArrayList(especeDAO.findAll());
        tableEspeces.setItems(esps);
        cmbProdEspece.setItems(esps);
    }

    private void populateEspForm(Espece e) {
        selectedEspece = e;
        txtEspNom.setText(e.getNom());
        btnSaveEsp.setText("Modifier");
    }

    @FXML
    public void resetEspForm() {
        selectedEspece = null;
        txtEspNom.clear();
        btnSaveEsp.setText("Enregistrer");
        lblEspError.setVisible(false);
        if (tableEspeces != null)
            tableEspeces.getSelectionModel().clearSelection();
    }

    @FXML
    public void saveEspece() {
        String nom = txtEspNom.getText() == null ? "" : txtEspNom.getText().trim();
        if (nom.isEmpty()) {
            showErrorEffect(txtEspNom);
            lblEspError.setText("Le nom de l'espèce est obligatoire.");
            lblEspError.setVisible(true);
            txtEspNom.requestFocus();
            return;
        }
        lblEspError.setVisible(false);
        Espece e = selectedEspece == null ? new Espece() : selectedEspece;
        e.setNom(nom);
        if (selectedEspece == null)
            especeDAO.save(e);
        else
            especeDAO.update(e);
        resetEspForm();
        loadEspeces();
        tableEspeces.refresh();
    }

    @FXML
    public void deleteEspece() {
        if (selectedEspece != null) {
            boolean success = especeDAO.delete(selectedEspece);
            if (!success) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "Impossible de supprimer cette espèce car elle est affectée à des produits existants.");
                alert.showAndWait();
            } else {
                resetEspForm();
                loadEspeces();
            }
        }
    }

    // --- LOGIQUE ETAT STOCK ---
    @FXML
    public void loadEtatStock() {
        boolean inclureArchives = chkInclureArchives != null && chkInclureArchives.isSelected();
        // #2 : Filtre les lots au niveau SQL avec JOIN FETCH pour tuer le problème N+1
        List<Lot> lots = lotDAO.findActiveLotsWithDetails(inclureArchives);
        java.util.Map<Long, Long> mapQtesVendues = lotDAO.getQuantitesVenduesParLot();
        
        masterStockList.clear();

        for (Lot lot : lots) {
            int qteVendue = mapQtesVendues.getOrDefault(lot.getId(), 0L).intValue();

            Produit p = lot.getProduit();
            String expDate = lot.getDateExpiration() != null ? lot.getDateExpiration().toString() : "---";
            boolean isExp = lot.getDateExpiration() != null
                    && lot.getDateExpiration().isBefore(java.time.LocalDate.now());
            int seuil = p.getSeuilAlerte() != null ? p.getSeuilAlerte() : 5;

            String formatee = lot.getQuantiteStock() + " Unité(s)";
            double valeurFinanciere = 0.0;
            if (p.getEstDeconditionnable() != null && p.getEstDeconditionnable() && p.getUnitesParBoite() != null
                    && p.getUnitesParBoite() > 0) {
                int boites = lot.getQuantiteStock() / p.getUnitesParBoite();
                int unites = lot.getQuantiteStock() % p.getUnitesParBoite();
                formatee = boites + " Bte(s) et " + unites + " Unité(s)";
                
                double prixBoite = p.getPrixVente() != null ? p.getPrixVente() : 0.0;
                double prixUnite = p.getPrixVenteUnite() != null ? p.getPrixVenteUnite() : 0.0;
                valeurFinanciere = (boites * prixBoite) + (unites * prixUnite);
            } else {
                double prixBoite = p.getPrixVente() != null ? p.getPrixVente() : 0.0;
                valeurFinanciere = lot.getQuantiteStock() * prixBoite;
            }

            // Calcul du nombre de jours avant expiration (négatif = déjà expiré)
            long joursRestants = lot.getDateExpiration() != null
                    ? java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), lot.getDateExpiration())
                    : Long.MAX_VALUE; // Pas de date = pas de limite

            masterStockList.add(new EtatStockDTO(
                    lot.getId(), p.getNom(), p.getCategorie().getNom(), p.getEspece().getNom(), lot.getNumeroLot(),
                    expDate,
                    lot.getQuantiteStock(), formatee, qteVendue, p.getPrixVente(), isExp, seuil, valeurFinanciere,
                    joursRestants));
        }
        // Init combos
        List<String> cats = categorieDAO.findAll().stream()
                .map(com.pharmacie.models.Categorie::getNom)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        cats.add(0, "Toutes Catégories");
        cmbFiltreStockCat.setItems(FXCollections.observableArrayList(cats));
        if (cmbFiltreStockCat.getValue() == null)
            cmbFiltreStockCat.getSelectionModel().selectFirst();

        List<String> esps = especeDAO.findAll().stream()
                .map(com.pharmacie.models.Espece::getNom)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        esps.add(0, "Toutes Espèces");
        cmbFiltreStockEsp.setItems(FXCollections.observableArrayList(esps));
        if (cmbFiltreStockEsp.getValue() == null)
            cmbFiltreStockEsp.getSelectionModel().selectFirst();
        if (cmbFiltreStockStatut.getItems().isEmpty()) {
            cmbFiltreStockStatut.setItems(FXCollections.observableArrayList(
                    "Tous Statuts", "En alerte", "Expiré", "Normal", "Rupture (Vide)"));
            cmbFiltreStockStatut.getSelectionModel().selectFirst();
        }

        if (txtSearchStock != null)
            txtSearchStock.clear();
        filtrerStock();
    }

    @FXML
    public void resetFiltresStock() {
        if (cmbFiltreStockCat != null) cmbFiltreStockCat.getSelectionModel().selectFirst();
        if (cmbFiltreStockEsp != null) cmbFiltreStockEsp.getSelectionModel().selectFirst();
        if (cmbFiltreStockStatut != null) cmbFiltreStockStatut.getSelectionModel().selectFirst();
        if (txtSearchStock != null) txtSearchStock.clear();
        if (dpFiltreExpAvant != null) dpFiltreExpAvant.setValue(null);
        if (tableEtatStock != null) tableEtatStock.getSelectionModel().clearSelection();
        filtrerStock();
    }

    @FXML
    public void onBtnVoirLotClick() {
        EtatStockDTO selected = tableEtatStock.getSelectionModel().getSelectedItem();
        if (selected != null) showLotDetail(selected);
    }

    @FXML
    public void filtrerStock() {
        String search = txtSearchStock.getText() == null ? "" : txtSearchStock.getText().toLowerCase();
        String cat = cmbFiltreStockCat.getValue();
        String esp = cmbFiltreStockEsp.getValue();
        String statut = cmbFiltreStockStatut.getValue();
        // #5 : filtre DatePicker expiration
        java.time.LocalDate dateLimit = (dpFiltreExpAvant != null) ? dpFiltreExpAvant.getValue() : null;

        List<EtatStockDTO> filtered = masterStockList.stream()
                .filter(d -> search.isEmpty() || d.getProduitNom().toLowerCase().contains(search) ||
                        d.getLotNumero().toLowerCase().contains(search))
                .filter(d -> cat == null || cat.equals("Toutes Catégories") || d.getCategorieNom().equals(cat))
                .filter(d -> esp == null || esp.equals("Toutes Espèces") || d.getEspeceNom().equals(esp))
                .filter(d -> {
                    if (statut == null || statut.equals("Tous Statuts")) return true;
                    if (statut.equals("En alerte"))
                        return d.getQuantiteStock() <= d.getSeuilAlerte() && !d.isEstExpire() && d.getQuantiteStock() > 0;
                    if (statut.equals("Expiré")) return d.isEstExpire();
                    if (statut.equals("Normal"))
                        return d.getQuantiteStock() > d.getSeuilAlerte() && !d.isEstExpire();
                    if (statut.equals("Rupture (Vide)")) return d.getQuantiteStock() == 0; // #4
                    return true;
                })
                // #5 : filtre "expiration avant le [date]"
                .filter(d -> {
                    if (dateLimit == null || d.getDateExpiration().equals("---")) return true;
                    try {
                        return java.time.LocalDate.parse(d.getDateExpiration()).isBefore(dateLimit.plusDays(1));
                    } catch (Exception e) { return true; }
                })
                .toList();

        tableEtatStock.setItems(FXCollections.observableArrayList(filtered));

        // #7 : Calcul et affichage de la valeur financière totale du stock visible (Alignement UX Ventes)
        if (lblTotalStockValeur != null) {
            double total = filtered.stream()
                    .mapToDouble(d -> d.getValeurFinanciere() != null ? d.getValeurFinanciere() : 0.0)
                    .sum();
            lblTotalStockValeur.setText(
                    String.format(java.util.Locale.FRANCE, "%,.0f FCFA", total));
            lblTotalStockValeur.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 16px; -fx-font-weight: bold;");
        }
    }

    @FXML
    public void imprimerEtatStockPdf() {
        if (tableEtatStock.getItems().isEmpty()) {
            showProdError("Rien à exporter.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableProduits.getScene().getWindow();
        com.pharmacie.utils.PdfService.genererEtatStockPdf(new java.util.ArrayList<>(tableEtatStock.getItems()), stage);
    }

    /** #8 — Export CSV (Excel compatible) du stock visible */
    @FXML
    public void exporterCsv() {
        if (tableEtatStock.getItems().isEmpty()) { showProdError("Rien à exporter."); return; }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableProduits.getScene().getWindow();
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter le stock en CSV");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier CSV (Excel)", "*.csv"));
        fc.setInitialFileName("etat_stock_" + java.time.LocalDate.now() + ".csv");
        java.io.File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            pw.print('\uFEFF'); // BOM UTF-8 pour Excel Windows
            pw.println("Produit;Catégorie;N° Lot;Expiration;Jours Rest.;En Stock;Seuil;Prix U.;Valeur Lot");
            for (EtatStockDTO d : tableEtatStock.getItems()) {
                pw.printf(java.util.Locale.FRANCE, "%s;%s;%s;%s;%s;%s;%d;%.0f;%.0f%n",
                        d.getProduitNom(), d.getCategorieNom(), d.getLotNumero(),
                        d.getDateExpiration(), d.getJoursRestantsFormate(), d.getQuantiteFormatee(),
                        d.getSeuilAlerte(), d.getPrixUnitaire() != null ? d.getPrixUnitaire() : 0.0,
                        d.getValeurFinanciere() != null ? d.getValeurFinanciere() : 0.0);
            }
            new Alert(Alert.AlertType.INFORMATION, "Export réussi !\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur export CSV : " + e.getMessage()).showAndWait();
        }
    }

    /** #9 — Fiche Lot Premium déclenchée par double-clic */
    private void showLotDetail(EtatStockDTO dto) {
        Lot lot = lotDAO.findById(dto.getLotId());
        if (lot == null) return;

        // Appel direct du DAO avec JOIN FETCH empêchant les LazyInitializationException
        List<com.pharmacie.models.MouvementStock> mouvements = mouvementDAO.findByLotId(dto.getLotId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Fiche Lot — " + dto.getLotNumero());

        String headerStyle = dto.isEstExpire()
                ? "-fx-background-color: #ffcccc; -fx-padding: 15; -fx-background-radius: 6;"
                : (dto.getJoursRestants() <= 30 && dto.getJoursRestants() >= 0)
                ? "-fx-background-color: #fff3cd; -fx-padding: 15; -fx-background-radius: 6;"
                : "-fx-background-color: #e8f8f5; -fx-padding: 15; -fx-background-radius: 6;";

        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(5);
        header.setStyle(headerStyle);
        header.getChildren().addAll(
                styledLabel("📦  " + dto.getProduitNom(), "-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;"),
                styledLabel("Catégorie : " + dto.getCategorieNom() + "   |   Espèce : " + dto.getEspeceNom(), "-fx-font-size: 12px; -fx-text-fill: #555;"),
                styledLabel("N° Lot : " + dto.getLotNumero() + "   |   Expiration : " + dto.getDateExpiration()
                        + "  (" + dto.getJoursRestantsFormate() + ")", "-fx-font-size: 12px; -fx-text-fill: #555;"),
                styledLabel("Stock actuel : " + dto.getQuantiteFormatee() + "   |   Valeur : " + dto.getValeurFormatee(),
                        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #27AE60;")
        );

        TableView<com.pharmacie.models.MouvementStock> tableMvt = new TableView<>();
        tableMvt.setPrefHeight(250);
        tableMvt.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<com.pharmacie.models.MouvementStock, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDateMouvement().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"))));
        TableColumn<com.pharmacie.models.MouvementStock, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTypeMouvement().name()));
        TableColumn<com.pharmacie.models.MouvementStock, String> colQte = new TableColumn<>("Quantité");
        colQte.setCellValueFactory(c -> {
            int q = c.getValue().getQuantite();
            com.pharmacie.models.Produit p = c.getValue().getProduit();
            String prefix = (q > 0 ? "+" : "");
            
            if (p != null && Boolean.TRUE.equals(p.getEstDeconditionnable()) 
                && p.getUnitesParBoite() != null && p.getUnitesParBoite() > 0) {
                int boites = Math.abs(q) / p.getUnitesParBoite();
                int unites = Math.abs(q) % p.getUnitesParBoite();
                String sign = q < 0 ? "-" : "+";
                return new javafx.beans.property.SimpleStringProperty(sign + boites + " Bte(s) et " + unites + " Un.");
            }
            return new javafx.beans.property.SimpleStringProperty(prefix + q);
        });
        TableColumn<com.pharmacie.models.MouvementStock, String> colRef = new TableColumn<>("Référence");
        colRef.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getReference() != null ? c.getValue().getReference() : ""));
        TableColumn<com.pharmacie.models.MouvementStock, String> colAgent = new TableColumn<>("Agent");
        colAgent.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getUser() != null ? c.getValue().getUser().getNom() : "—"));

        tableMvt.getColumns().addAll(colDate, colType, colQte, colRef, colAgent);
        tableMvt.setItems(FXCollections.observableArrayList(mouvements));

        tableMvt.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(com.pharmacie.models.MouvementStock item, boolean empty) {
                super.updateItem(item, empty);
                Runnable updateStyle = () -> {
                    if (item == null || empty) { setStyle(""); return; }
                    if (isSelected()) { setStyle(""); return; } // Évite le bug du texte blanc invisible sur fond clair
                    switch (item.getTypeMouvement()) {
                        case ACHAT -> setStyle("-fx-background-color: #e8f8f5;"); // Vert pâle
                        case VENTE -> setStyle("-fx-background-color: #fef9e7;"); // Jaune pâle
                        case AJUSTEMENT_NEGATIF -> setStyle("-fx-background-color: #FAEDEC;"); // Rouge très pâle
                        default -> setStyle("");
                    }
                };
                selectedProperty().addListener((obs, wasSelected, isNowSelected) -> updateStyle.run());
                updateStyle.run();
            }
        });

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12);
        content.setPadding(new javafx.geometry.Insets(0, 15, 15, 15));
        content.setPrefWidth(650);
        content.getChildren().addAll(
                header,
                styledLabel("📋  Historique des mouvements (" + mouvements.size() + " entrées)",
                        "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 0 0 0;"),
                tableMvt
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #f9f9f9;");
        dialog.showAndWait();
    }

    private Label styledLabel(String text, String style) {
        Label l = new Label(text); l.setStyle(style); l.setWrapText(true); return l;
    }

    @FXML
    public void ajusterStock() {
        var selectedItems = new java.util.ArrayList<>(tableEtatStock.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) return;

        ToggleGroup groupType = new ToggleGroup();
        RadioButton rbRetrait = new RadioButton("↘ Retrait / Perte");
        rbRetrait.setToggleGroup(groupType);
        rbRetrait.setSelected(true);
        rbRetrait.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");

        RadioButton rbAjout = new RadioButton("↗ Ajout Manuel");
        rbAjout.setToggleGroup(groupType);
        rbAjout.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        HBox opBox = new HBox(15, rbRetrait, rbAjout);
        opBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        ComboBox<AjustementStock.MotifAjustement> cmbMotif = new ComboBox<>();
        Runnable updateMotifs = () -> {
            boolean estPositif = rbAjout.isSelected();
            java.util.List<AjustementStock.MotifAjustement> motifs = new java.util.ArrayList<>();
            for (AjustementStock.MotifAjustement m : AjustementStock.MotifAjustement.values()) {
                switch(m) {
                    case CASSE: case PEREMPTION: case USAGE_INTERNE:
                        if (!estPositif) motifs.add(m); break;
                    case EXCEDENT_INVENTAIRE: case RETOUR_INTERNE:
                        if (estPositif) motifs.add(m); break;
                    default:
                        motifs.add(m); break;
                }
            }
            cmbMotif.setItems(javafx.collections.FXCollections.observableArrayList(motifs));
            cmbMotif.getSelectionModel().selectFirst();
        };
        updateMotifs.run();
        rbRetrait.setOnAction(e -> updateMotifs.run());
        rbAjout.setOnAction(e -> updateMotifs.run());

        TextField obsField = new TextField();
        obsField.setPromptText("Observation (optionnel)");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajustement de Stock");

        if (selectedItems.size() == 1) {
            EtatStockDTO sel = selectedItems.get(0);
            Lot lot = lotDAO.findById(sel.getLotId());
            if (lot == null) return;

            dialog.setHeaderText("Ajustement pour : " + sel.getProduitNom() + "\nLot n°: " + lot.getNumeroLot()
                    + " (Stock actuel: " + lot.getQuantiteStock() + ")");

            TextField qteField = new TextField();
            qteField.setPromptText("Exemple: 5");
            
            // Restriction de frappe : Uniquement des chiffres
            qteField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    qteField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            });

            // Agrandissement visuel des champs optionnels
            cmbMotif.setPrefWidth(300);
            obsField.setPrefWidth(300);

            // Modification du texte d'aide dynamiquement avec protection anti-troncature
            Label lblQte = new Label("Quantité à retirer :");
            lblQte.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); 
            javafx.event.EventHandler<javafx.event.ActionEvent> toggleEvent = e -> {
                updateMotifs.run();
                lblQte.setText(rbAjout.isSelected() ? "Quantité à ajouter :" : "Quantité à retirer :");
                qteField.setStyle(rbAjout.isSelected() ? "-fx-border-color: #27ae60;" : "-fx-border-color: #e74c3c;");
            };
            rbRetrait.setOnAction(toggleEvent);
            rbAjout.setOnAction(toggleEvent);

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 10, 10, 10));
            grid.add(new Label("Opération :"), 0, 0); grid.add(opBox, 1, 0);
            grid.add(lblQte, 0, 1); grid.add(qteField, 1, 1);
            grid.add(new Label("Motif :"), 0, 2); grid.add(cmbMotif, 1, 2);
            grid.add(new Label("Observation :"), 0, 3); grid.add(obsField, 1, 3);
            
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().setPrefWidth(550); // Agrandissement général de la fenêtre
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            javafx.application.Platform.runLater(qteField::requestFocus);

            dialog.showAndWait().ifPresent(btn -> {
                if (btn != ButtonType.OK) return;
                try {
                    int qte = Integer.parseInt(qteField.getText());
                    boolean estPositif = rbAjout.isSelected();
                    if (qte <= 0) throw new NumberFormatException();
                    if (!estPositif && qte > lot.getQuantiteStock()) {
                        com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.ERROR, "Erreur", null, "La quantité à retirer ne peut excéder le stock actuel !");
                        return;
                    }
                    MouvementStock.TypeMouvement typeAjust = estPositif ? MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF : MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF;
                    appliquerAjustement(lot, qte, cmbMotif.getValue(), obsField.getText(), typeAjust);
                } catch (NumberFormatException e) {
                    com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.ERROR, "Saisie Invalide", null, "Veuillez entrer une quantité entière strictement positive.");
                }
            });

        } else {
            dialog.setHeaderText("Déclarer un ajustement massif pour les " + selectedItems.size() + " lots sélectionnés ?\n"
                    + "(La modification s'appliquera à la totalité du stock de chaque lot pour les retraits)");
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10);
            box.setPadding(new javafx.geometry.Insets(15));
            // On masque le qte pour la déclaration groupée (elle s'applique à la totalité pour un retrait)
            // Mais pour un ajout massif, ça n'a pas de sens d'ajouter la "totalité de son propre stock", donc on bloque l'ajout massif.
            rbAjout.setDisable(true); // Seul le retrait intégral est permis en lot par lot massif dans cette version
            rbRetrait.setDisable(true); 
            box.getChildren().addAll(new Label("Opération (Forcée) :"), opBox, new Label("Motif commun :"), cmbMotif, new Label("Observation :"), obsField);
            dialog.getDialogPane().setContent(box);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(btn -> {
                if (btn != ButtonType.OK) return;
                for (EtatStockDTO sel : selectedItems) {
                    Lot lot = lotDAO.findById(sel.getLotId());
                    if (lot != null && lot.getQuantiteStock() > 0) {
                        appliquerAjustement(lot, lot.getQuantiteStock(), cmbMotif.getValue(), obsField.getText(), MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF);
                    }
                }
            });
        }

        loadEtatStock();
    }

    private void appliquerAjustement(Lot lot, int qte, AjustementStock.MotifAjustement motif, String observation, MouvementStock.TypeMouvement typeAjustement) {
        AjustementStock adj = new AjustementStock();
        adj.setLot(lot);
        adj.setUser(SessionManager.getCurrentUser());
        adj.setDateAjustement(java.time.LocalDateTime.now());
        adj.setQuantite(qte);
        adj.setMotif(motif);
        adj.setObservation(observation);
        adj.setTypeAjustement(typeAjustement);
        ajustementStockDAO.save(adj);

        if (typeAjustement == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) {
            lot.setQuantiteStock(lot.getQuantiteStock() + qte);
            if (lot.getQuantiteStock() > 0 && lot.getEstArchive() != null && lot.getEstArchive()) {
                lot.setEstArchive(false);
            }
        } else {
            lot.setQuantiteStock(lot.getQuantiteStock() - qte);
            if (lot.getQuantiteStock() <= 0) lot.setEstArchive(true);
        }
        lotDAO.update(lot);

        int qteMvt = typeAjustement == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF ? qte : -qte;
        mouvementDAO.save(new MouvementStock(
                lot.getProduit(), lot, SessionManager.getCurrentUser(),
                typeAjustement, qteMvt, LocalDateTime.now(),
                "Ajustement: " + motif.name() + (observation != null && !observation.isEmpty() ? " - " + observation : "")));
    }

    // --- INNER CLASS DTO POUR STOCK ---
    public static class EtatStockDTO {
        private Long lotId;
        private String produitNom;
        private String categorieNom;
        private String especeNom;
        private String lotNumero;
        private String dateExpiration;
        private Integer quantiteStock;
        private String quantiteFormatee;
        private Integer quantiteVendue;
        private Double prixUnitaire;
        private boolean estExpire;
        private int seuilAlerte;
        private Double valeurFinanciere;
        private long joursRestants; // Nombre de jours avant péremption (négatif = expire)

        public EtatStockDTO(Long id, String p, String c, String e, String l, String d, Integer qs, String qsFormatee,
                Integer qv, Double px, boolean exp, int seuil, Double valeurFinanciere, long joursRestants) {
            this.lotId = id;
            this.produitNom = p;
            this.categorieNom = c;
            this.especeNom = e;
            this.lotNumero = l;
            this.dateExpiration = d;
            this.quantiteStock = qs;
            this.quantiteFormatee = qsFormatee;
            this.quantiteVendue = qv;
            this.prixUnitaire = px;
            this.estExpire = exp;
            this.seuilAlerte = seuil;
            this.valeurFinanciere = valeurFinanciere;
            this.joursRestants = joursRestants;
        }

        public Long getLotId() {
            return lotId;
        }

        public String getProduitNom() {
            return produitNom;
        }

        public String getCategorieNom() {
            return categorieNom;
        }

        public String getEspeceNom() {
            return especeNom;
        }

        public String getLotNumero() {
            return lotNumero;
        }

        public String getDateExpiration() {
            return dateExpiration;
        }

        public Integer getQuantiteStock() {
            return quantiteStock;
        }

        public String getQuantiteFormatee() {
            return quantiteFormatee;
        }

        public Integer getQuantiteVendue() {
            return quantiteVendue;
        }

        public Double getPrixUnitaire() {
            return prixUnitaire;
        }

        public boolean isEstExpire() {
            return estExpire;
        }

        public int getSeuilAlerte() {
            return seuilAlerte;
        }

        public Double getValeurFinanciere() {
            return valeurFinanciere;
        }

        /** Retourne la valeur du lot formatée en FCFA pour la colonne */
        public String getValeurFormatee() {
            if (valeurFinanciere == null) return "---";
            return String.format(java.util.Locale.FRANCE, "%,.0f FCFA", valeurFinanciere);
        }

        /** Retourne les jours restants avant exp : positif = OK, 0 = aujourd'hui, négatif = expiré */
        public long getJoursRestants() {
            return joursRestants;
        }

        /**
         * Formate intelligemment les jours restants pour l'affichage :
         *  "❌ Expiré", "⚠️ 3 j", "12 j", "Aucune date"
         */
        public String getJoursRestantsFormate() {
            if (dateExpiration.equals("---")) return "Sans date";
            if (joursRestants < 0) return "❌ Expiré";
            if (joursRestants == 0) return "⚠️ Auj.";
            if (joursRestants <= 30) return "⚠️ " + joursRestants + " j";
            return joursRestants + " j";
        }
    }
}
