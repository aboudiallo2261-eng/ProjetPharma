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
    private TableColumn<EtatStockDTO, String> colStockAchatInitial, colStockPAchatBoite, colStockPAchatUnite;
    @FXML
    private TableColumn<EtatStockDTO, Integer> colStockVendus, colStockSeuil;
    @FXML
    private TableColumn<EtatStockDTO, Double> colStockPrix;
    @FXML
    private TableColumn<EtatStockDTO, String> colStockJours; // Jours restants
    @FXML
    private TableColumn<EtatStockDTO, String> colStockValeur; // Valeur du lot
    @FXML
    private Label lblTotalStockValeur; // KPI valeur totale en bas de tableau
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
        if (colStockAchatInitial != null)
            colStockAchatInitial.setCellValueFactory(new PropertyValueFactory<>("qteInitialeAchetee"));
        if (colStockPAchatBoite != null)
            colStockPAchatBoite.setCellValueFactory(new PropertyValueFactory<>("prixAchatBoiteFormate"));
        if (colStockPAchatUnite != null)
            colStockPAchatUnite.setCellValueFactory(new PropertyValueFactory<>("prixAchatUniteFormate"));
        colStockVendus.setCellValueFactory(new PropertyValueFactory<>("quantiteVendue"));
        colStockPrix.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        // Formatage FCFA — evite la notation scientifique (ex: 1.0E8 au lieu de 100 000
        // FCFA)
        colStockPrix.setCellFactory(col -> new TableCell<EtatStockDTO, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Format entier groupé (pas de décimales pour FCFA)
                    setText(String.format("%,.0f FCFA", item).replace(",", " "));
                }
            }
        });

        // Nouvelles colonnes intelligentes
        if (colStockJours != null)
            colStockJours.setCellValueFactory(new PropertyValueFactory<>("joursRestantsFormate"));
        if (colStockValeur != null)
            colStockValeur.setCellValueFactory(new PropertyValueFactory<>("valeurFormatee"));
        if (colStockSeuil != null)
            colStockSeuil.setCellValueFactory(new PropertyValueFactory<>("seuilAlerte")); // #6

        // Sélection multiple pour pertes groupées (#3)
        tableEtatStock.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // Pseudo-classes CSS pour coloration des alertes — compatibles avec la
        // sélection native JavaFX
        javafx.css.PseudoClass expirePseudo = javafx.css.PseudoClass.getPseudoClass("expire");
        javafx.css.PseudoClass alertePseudo = javafx.css.PseudoClass.getPseudoClass("alerte");

        tableEtatStock.setRowFactory(tv -> new TableRow<EtatStockDTO>() {
            @Override
            protected void updateItem(EtatStockDTO item, boolean empty) {
                super.updateItem(item, empty);
                boolean isExpire = !empty && item != null && item.isEstExpire();
                boolean isAlerte = !empty && item != null && !isExpire
                        && item.getQuantiteStock() <= item.getSeuilAlerte();
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
        tableEtatStock.getSelectionModel().getSelectedItems()
                .addListener((javafx.collections.ListChangeListener.Change<? extends EtatStockDTO> c) -> {
                    int selectedCount = tableEtatStock.getSelectionModel().getSelectedItems().size();
                    if (btnAjustementStock != null)
                        btnAjustementStock.setDisable(selectedCount == 0);
                    // La fiche détails ne s'ouvre que pour UNE SEULE ligne sélectionnée
                    if (btnVoirLot != null)
                        btnVoirLot.setDisable(selectedCount != 1);
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

            if (selectedProduit == null) {
                produitDAO.save(p);
                com.pharmacie.utils.ToastService.showSuccess(tableProduits.getScene().getWindow(), "Produit Ajouté",
                        "Le produit a été enregistré avec succès.");
            } else {
                produitDAO.update(p);
                com.pharmacie.utils.ToastService.showSuccess(tableProduits.getScene().getWindow(), "Produit Modifié",
                        "Les informations du produit ont été mises à jour.");
            }

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
                    com.pharmacie.utils.ToastService.showSuccess(tableProduits.getScene().getWindow(),
                            "Produit Supprimé", "Le produit a été retiré du catalogue.");
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
     * 2. Animation "shake" (oscillation horizontale) pour capter l'œil
     * instantanément.
     * Identique au mécanisme implémenté dans AchatController.
     */
    private void showErrorEffect(javafx.scene.Node node) {
        if (node == null)
            return;
        // --- 1. Bordure rouge ---
        String originalStyle = node.getStyle();
        node.setStyle(originalStyle + "; -fx-border-color: #E74C3C; -fx-border-width: 2px; -fx-border-radius: 4px;");
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> node.setStyle(originalStyle));
        // --- 2. Shake horizontal (oscillation premium) ---
        javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(60), node);
        shake.setFromX(0);
        shake.setByX(8);
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
                com.pharmacie.utils.ToastService.showSuccess(cmbProdCategorie.getScene().getWindow(), "Catégorie Créée",
                        "Nouvelle catégorie ajoutée avec succès.");
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
                com.pharmacie.utils.ToastService.showSuccess(cmbProdEspece.getScene().getWindow(), "Espèce Créée",
                        "Nouvelle espèce ajoutée avec succès.");
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
        if (selectedCategorie == null) {
            categorieDAO.save(c);
            com.pharmacie.utils.ToastService.showSuccess(tableCategories.getScene().getWindow(), "Catégorie Ajoutée",
                    "La catégorie a été créée.");
        } else {
            categorieDAO.update(c);
            com.pharmacie.utils.ToastService.showSuccess(tableCategories.getScene().getWindow(), "Catégorie Modifiée",
                    "La catégorie a été mise à jour.");
        }
        resetCatForm();
        loadCategories();
        tableCategories.refresh();
    }

    @FXML
    public void deleteCategorie() {
        if (selectedCategorie != null) {
            boolean success = categorieDAO.delete(selectedCategorie);
            if (!success) {
                showProdError(
                        "Impossible de supprimer cette catégorie car elle contient déjà des produits enregistrés.");
            } else {
                com.pharmacie.utils.ToastService.showSuccess(tableCategories.getScene().getWindow(),
                        "Catégorie Supprimée", "La catégorie a été retirée avec succès.");
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
        if (selectedEspece == null) {
            especeDAO.save(e);
            com.pharmacie.utils.ToastService.showSuccess(tableEspeces.getScene().getWindow(), "Espèce Ajoutée",
                    "L'espèce a été créée.");
        } else {
            especeDAO.update(e);
            com.pharmacie.utils.ToastService.showSuccess(tableEspeces.getScene().getWindow(), "Espèce Modifiée",
                    "L'espèce a été mise à jour.");
        }
        resetEspForm();
        loadEspeces();
        tableEspeces.refresh();
    }

    @FXML
    public void deleteEspece() {
        if (selectedEspece != null) {
            boolean success = especeDAO.delete(selectedEspece);
            if (!success) {
                showProdError("Impossible de supprimer cette espèce car elle est affectée à des produits existants.");
            } else {
                com.pharmacie.utils.ToastService.showSuccess(tableEspeces.getScene().getWindow(), "Espèce Supprimée",
                        "L'espèce a été retirée avec succès.");
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

        java.util.Map<Long, Integer> qtesInitAchetees = new java.util.HashMap<>();
        try (org.hibernate.Session session = com.pharmacie.utils.HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> res = session
                    .createQuery("SELECT la.lot.id, la.quantiteAchetee FROM LigneAchat la", Object[].class).list();
            for (Object[] r : res) {
                qtesInitAchetees.put((Long) r[0], (Integer) r[1]);
            }
        } catch (Exception ignored) {
        }

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

            int qteInitiale = qtesInitAchetees.getOrDefault(lot.getId(), lot.getQuantiteStock());
            Double pAchatB = p.getPrixAchat() != null ? p.getPrixAchat() : 0.0;
            Double pAchatU = null;
            if (p.getEstDeconditionnable() != null && p.getEstDeconditionnable() && p.getUnitesParBoite() != null
                    && p.getUnitesParBoite() > 0) {
                pAchatU = pAchatB / p.getUnitesParBoite();
            }

            masterStockList.add(new EtatStockDTO(
                    lot.getId(), p.getNom(), p.getCategorie().getNom(), p.getEspece().getNom(), lot.getNumeroLot(),
                    expDate,
                    lot.getQuantiteStock(), formatee, qteVendue, p.getPrixVente(), isExp, seuil, valeurFinanciere,
                    joursRestants, qteInitiale, pAchatB, pAchatU));
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
            cmbFiltreStockStatut.getSelectionModel().select("Normal");
        }

        if (txtSearchStock != null)
            txtSearchStock.clear();
        filtrerStock();
    }

    @FXML
    public void resetFiltresStock() {
        if (cmbFiltreStockCat != null)
            cmbFiltreStockCat.getSelectionModel().selectFirst();
        if (cmbFiltreStockEsp != null)
            cmbFiltreStockEsp.getSelectionModel().selectFirst();
        if (cmbFiltreStockStatut != null)
            cmbFiltreStockStatut.getSelectionModel().select("Normal");
        if (txtSearchStock != null)
            txtSearchStock.clear();
        if (dpFiltreExpAvant != null)
            dpFiltreExpAvant.setValue(null);
        if (tableEtatStock != null)
            tableEtatStock.getSelectionModel().clearSelection();
        filtrerStock();
    }

    @FXML
    public void onBtnVoirLotClick() {
        EtatStockDTO selected = tableEtatStock.getSelectionModel().getSelectedItem();
        if (selected != null)
            showLotDetail(selected);
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
                    if (statut == null || statut.equals("Tous Statuts"))
                        return true;
                    if (statut.equals("En alerte"))
                        return d.getQuantiteStock() <= d.getSeuilAlerte() && !d.isEstExpire()
                                && d.getQuantiteStock() > 0;
                    if (statut.equals("Expiré"))
                        return d.isEstExpire();
                    if (statut.equals("Normal"))
                        return d.getQuantiteStock() > d.getSeuilAlerte() && !d.isEstExpire();
                    if (statut.equals("Rupture (Vide)"))
                        return d.getQuantiteStock() == 0; // #4
                    return true;
                })
                // #5 : filtre "expiration avant le [date]"
                .filter(d -> {
                    if (dateLimit == null || d.getDateExpiration().equals("---"))
                        return true;
                    try {
                        return java.time.LocalDate.parse(d.getDateExpiration()).isBefore(dateLimit.plusDays(1));
                    } catch (Exception e) {
                        return true;
                    }
                })
                .toList();

        tableEtatStock.setItems(FXCollections.observableArrayList(filtered));

        // #7 : Calcul et affichage de la valeur financière totale du stock visible
        // (Alignement UX Ventes)
        if (lblTotalStockValeur != null) {
            double total = filtered.stream()
                    .mapToDouble(d -> d.getValeurFinanciere() != null ? d.getValeurFinanciere() : 0.0)
                    .sum();
            com.pharmacie.utils.AnimationUtils.animerValeurMonetaire(lblTotalStockValeur, total, "");
            lblTotalStockValeur.setStyle("-fx-text-fill: #059669; -fx-font-size: 20px; -fx-font-weight: 900;");
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

    /** #8 — Export Excel Premium du stock visible */
    @FXML
    public void exporterExcel() {
        if (tableEtatStock.getItems().isEmpty()) {
            showProdError("Rien à exporter.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableProduits.getScene().getWindow();
        
        // Délégation au service d'export Premium
        com.pharmacie.utils.ExcelExportService.genererEtatStockExcel(tableEtatStock.getItems(), stage);
    }

    /** #9 — Fiche Lot Premium déclenchée par double-clic */
    private void showLotDetail(EtatStockDTO dto) {
        Lot lot = lotDAO.findById(dto.getLotId());
        if (lot == null)
            return;

        // Appel direct du DAO avec JOIN FETCH empêchant les LazyInitializationException
        List<com.pharmacie.models.MouvementStock> mouvements = mouvementDAO.findByLotId(dto.getLotId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Fiche Lot — " + dto.getLotNumero());
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Erreur chargement styles.css dans Fiche Lot: " + e.getMessage());
        }
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");

        boolean isExpired = dto.isEstExpire();
        boolean isWarning = !isExpired && (dto.getJoursRestants() <= 30 && dto.getJoursRestants() >= 0);

        String headerBg = isExpired ? "#FFF1F2" : isWarning ? "#FFFBEB" : "#FFFFFF";
        String headerBorder = isExpired ? "#FECDD3" : isWarning ? "#FDE68A" : "#E2E8F0";
        String titleColor = isExpired ? "#E11D48" : isWarning ? "#D97706" : "#0F172A";
        String valueColor = isExpired ? "#E11D48" : isWarning ? "#D97706" : "#059669";

        String headerStyle = String.format(
                "-fx-background-color: %s; -fx-padding: 16; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-width: 1.5; -fx-border-radius: 8;",
                headerBg, headerBorder);

        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(8);
        header.setStyle(headerStyle);

        Label titleLbl = styledLabel(dto.getProduitNom(),
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        Label metaLbl = styledLabel("Catégorie : " + dto.getCategorieNom() + "   •   Espèce : " + dto.getEspeceNom(),
                "-fx-font-size: 13px; -fx-text-fill: #64748B;");
        Label lotLbl = styledLabel("N° Lot : " + dto.getLotNumero() + "   •   Expiration : " + dto.getDateExpiration()
                + "  (" + dto.getJoursRestantsFormate() + ")", "-fx-font-size: 13px; -fx-text-fill: #64748B;");

        javafx.scene.layout.HBox metricsBox = new javafx.scene.layout.HBox(15);
        metricsBox.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        Label stockLbl = styledLabel("Stock actuel : " + dto.getQuantiteFormatee(),
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");
        Label valLbl = styledLabel("Valeur : " + dto.getValeurFormatee(),
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");
        metricsBox.getChildren().addAll(stockLbl, styledLabel(" | ", "-fx-text-fill: #CBD5E1;"), valLbl);

        header.getChildren().addAll(titleLbl, metaLbl, lotLbl, metricsBox);

        TableView<com.pharmacie.models.MouvementStock> tableMvt = new TableView<>();
        tableMvt.setPrefHeight(280);
        tableMvt.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableMvt.setStyle("-fx-border-color: #CBD5E1; -fx-border-radius: 4; -fx-background-radius: 4;");

        TableColumn<com.pharmacie.models.MouvementStock, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDateMouvement()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"))));
        TableColumn<com.pharmacie.models.MouvementStock, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(c -> {
            String label = switch (c.getValue().getTypeMouvement()) {
                case AJUSTEMENT_POSITIF -> "Ajout de stocks";
                case AJUSTEMENT_NEGATIF -> "Perte/Retrait";
                case ACHAT -> "Achat / Réception";
                case VENTE -> "Vente";
                default -> c.getValue().getTypeMouvement().name();
            };
            return new javafx.beans.property.SimpleStringProperty(label);
        });
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
                    if (item == null || empty) {
                        setStyle("");
                        return;
                    }

                    if (isSelected() || isHover()) {
                        setStyle(""); // Laisse le CSS global (.table-row-cell:selected / :hover) agir !
                        return;
                    }

                    switch (item.getTypeMouvement()) {
                        case ACHAT -> setStyle("-fx-background-color: #E0F2FE;"); // Sky 100 (bleu clair)
                        case VENTE -> setStyle("-fx-background-color: #FFFFFF;"); // White
                        case AJUSTEMENT_NEGATIF -> setStyle("-fx-background-color: #FEF2F2;"); // Red 50
                        case AJUSTEMENT_POSITIF -> setStyle("-fx-background-color: #F0FDF4;"); // Emerald 50
                        default -> setStyle("");
                    }
                };
                selectedProperty().addListener((obs, wasSelected, isNowSelected) -> updateStyle.run());
                hoverProperty().addListener((obs, wasHovered, isNowHovered) -> updateStyle.run());
                updateStyle.run();
            }
        });

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(16);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setPrefWidth(750);
        content.getChildren().addAll(
                header,
                styledLabel("Historique des mouvements (" + mouvements.size() + " entrées)",
                        "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #334155; -fx-padding: 10 0 0 0;"),
                tableMvt);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            if (closeBtn != null) {
                closeBtn.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-padding: 7 24; -fx-min-width: 100px; -fx-border-color: #CBD5E1; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-cursor: hand;");
            }
        });

        dialog.showAndWait();
    }

    private Label styledLabel(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        l.setWrapText(true);
        return l;
    }

    @FXML
    public void ajusterStock() {
        var selectedItems = new java.util.ArrayList<>(tableEtatStock.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) return;
        if (selectedItems.size() > 1) {
            afficherDialogAjustementMassif(selectedItems);
        } else {
            EtatStockDTO sel = selectedItems.get(0);
            Lot lot = lotDAO.findById(sel.getLotId());
            if (lot != null) afficherDialogAjustementSingle(lot, sel.getProduitNom());
        }
        loadEtatStock();
    }

    private Label mkSectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        return l;
    }

    private void afficherDialogAjustementSingle(Lot lot, String produitNom) {
        final boolean[] estAjout = {false};

        ComboBox<AjustementStock.MotifAjustement> cmbMotif = new ComboBox<>();
        cmbMotif.setMaxWidth(Double.MAX_VALUE);
        cmbMotif.setStyle("-fx-border-color: #64748B; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13px; -fx-padding: 4 8;");
        cmbMotif.getStyleClass().add("combo-box");

        TextField qteField = new TextField();
        qteField.setPromptText("Quantit\u00e9...");
        qteField.setStyle("-fx-background-color: white; -fx-border-color: #64748B; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8;");
        qteField.textProperty().addListener((obs, o, n) -> { if (!n.matches("\\d*")) qteField.setText(n.replaceAll("[^\\d]", "")); });

        TextField obsField = new TextField();
        obsField.setPromptText("Observation (optionnel)");
        obsField.setStyle("-fx-background-color: white; -fx-border-color: #64748B; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13px; -fx-padding: 8;");

        Label lblQte  = new Label("Quantit\u00e9 \u00e0 retirer :");
        lblQte.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-font-weight: bold;");
        Label lblNote = new Label("");
        lblNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");

        Runnable updateQte = () -> {
            boolean peri = cmbMotif.getValue() == AjustementStock.MotifAjustement.PEREMPTION && !estAjout[0];
            if (peri) {
                qteField.setText(String.valueOf(lot.getQuantiteStock()));
                qteField.setDisable(true);
                qteField.setStyle("-fx-background-color: #FFF1F2; -fx-border-color: #FDA4AF; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8; -fx-opacity: 0.75;");
                lblNote.setText("Stock total retir\u00e9 automatiquement (lot p\u00e9rim\u00e9)");
            } else {
                if (qteField.isDisabled()) qteField.clear();
                qteField.setDisable(false);
                String bc = estAjout[0] ? "#059669" : "#E74C3C";
                qteField.setStyle("-fx-background-color: white; -fx-border-color: " + bc + "; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8;");
                lblNote.setText("");
            }
        };

        Runnable updateMotifs = () -> {
            java.util.List<AjustementStock.MotifAjustement> list = new java.util.ArrayList<>();
            for (AjustementStock.MotifAjustement m : AjustementStock.MotifAjustement.values()) {
                switch (m) {
                    case CASSE: case PEREMPTION: case USAGE_INTERNE: if (!estAjout[0]) list.add(m); break;
                    case EXCEDENT_INVENTAIRE: case RETOUR_INTERNE:  if (estAjout[0])  list.add(m); break;
                    default: list.add(m);
                }
            }
            cmbMotif.setItems(javafx.collections.FXCollections.observableArrayList(list));
            cmbMotif.getSelectionModel().selectFirst();
        };
        updateMotifs.run();
        cmbMotif.setOnAction(e -> updateQte.run());

        String sActifR = "-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;";
        String sActifA = "-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;";
        String sInact  = "-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-border-color: #CBD5E1; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;";

        Button btnR = new Button("Retrait / Perte");    btnR.setMaxWidth(Double.MAX_VALUE); btnR.setStyle(sActifR);
        Button btnA = new Button("Ajout de Stock");     btnA.setMaxWidth(Double.MAX_VALUE); btnA.setStyle(sInact);

        Runnable toggleUI = () -> {
            if (!estAjout[0]) { btnR.setStyle(sActifR); btnA.setStyle(sInact); lblQte.setText("Quantit\u00e9 \u00e0 retirer :"); }
            else              { btnA.setStyle(sActifA); btnR.setStyle(sInact); lblQte.setText("Quantit\u00e9 \u00e0 ajouter :"); }
            updateMotifs.run(); updateQte.run();
        };
        btnR.setOnAction(e -> { estAjout[0] = false; toggleUI.run(); });
        btnA.setOnAction(e -> { estAjout[0] = true;  toggleUI.run(); });

        HBox toggleBox = new HBox(8, btnR, btnA);
        toggleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(btnR, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(btnA, javafx.scene.layout.Priority.ALWAYS);

        // --- Header card ---
        Label lblTitre   = new Label("Ajustement de Stock");
        lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label lblProd    = new Label(produitNom);
        lblProd.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        String expTxt    = lot.getDateExpiration() != null && !lot.getDateExpiration().equals("---") ? " \u00b7 Exp: " + lot.getDateExpiration() : "";
        Label lblMeta    = new Label("Lot n\u00b0 " + lot.getNumeroLot() + expTxt);
        lblMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Label lblStk     = new Label("Stock actuel : " + lot.getQuantiteStock() + " unit\u00e9(s)");
        lblStk.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155; -fx-background-color: #F1F5F9; -fx-background-radius: 6; -fx-padding: 4 10;");

        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(4, lblTitre, lblProd, lblMeta, lblStk);
        header.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 16 20 14 20;");

        javafx.scene.layout.VBox form = new javafx.scene.layout.VBox(10,
            mkSectionLabel("Op\u00e9ration"), toggleBox,
            mkSectionLabel("Motif"), cmbMotif,
            lblQte, qteField, lblNote,
            mkSectionLabel("Observation"), obsField);
        form.setPadding(new javafx.geometry.Insets(16, 20, 10, 20));

        Label lblErr = new Label(""); lblErr.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px; -fx-font-weight: bold;"); lblErr.setVisible(false);

        Button btnOk  = new Button("Confirmer l'ajustement");
        btnOk.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;");
        btnOk.setOnMouseEntered(e -> btnOk.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;"));
        btnOk.setOnMouseExited(e  -> btnOk.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;"));

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 20; -fx-border-color: #CBD5E1; -fx-border-radius: 8;");
        btnCancel.setOnMouseEntered(e -> btnCancel.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 20; -fx-border-color: #94A3B8; -fx-border-radius: 8;"));
        btnCancel.setOnMouseExited(e  -> btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 20; -fx-border-color: #CBD5E1; -fx-border-radius: 8;"));

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox actions = new HBox(10, lblErr, spacer, btnCancel, btnOk);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actions.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0; -fx-padding: 12 20;");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(header, form, actions);
        root.setStyle("-fx-background-color: white;"); root.setPrefWidth(480);

        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Ajustement de Stock");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);

        btnCancel.setOnAction(e -> stage.close());
        btnOk.setOnAction(e -> {
            lblErr.setVisible(false);
            String txt = qteField.getText().trim();
            if (txt.isEmpty()) { lblErr.setText("La quantit\u00e9 est obligatoire."); lblErr.setVisible(true); return; }
            try {
                int qte = Integer.parseInt(txt);
                if (qte <= 0) throw new NumberFormatException();
                if (!estAjout[0] && qte > lot.getQuantiteStock()) {
                    lblErr.setText("Quantit\u00e9 sup\u00e9rieure au stock disponible !"); lblErr.setVisible(true); return;
                }
                MouvementStock.TypeMouvement type = estAjout[0]
                    ? MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF
                    : MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF;
                appliquerAjustement(lot, qte, cmbMotif.getValue(), obsField.getText(), type);
                stage.close();
            } catch (NumberFormatException ex) {
                lblErr.setText("Entrez un nombre entier positif."); lblErr.setVisible(true);
            }
        });

        javafx.application.Platform.runLater(cmbMotif::requestFocus);
        stage.showAndWait();
    }

    private void afficherDialogAjustementMassif(java.util.List<EtatStockDTO> items) {
        ComboBox<AjustementStock.MotifAjustement> cmbMotif = new ComboBox<>();
        cmbMotif.getItems().addAll(AjustementStock.MotifAjustement.CASSE, AjustementStock.MotifAjustement.PEREMPTION, AjustementStock.MotifAjustement.USAGE_INTERNE, AjustementStock.MotifAjustement.AUTRE);
        cmbMotif.getSelectionModel().selectFirst(); cmbMotif.setMaxWidth(Double.MAX_VALUE);
        cmbMotif.setStyle("-fx-border-color: #64748B; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13px; -fx-padding: 4 8;");
        cmbMotif.getStyleClass().add("combo-box");
        TextField obsField = new TextField(); obsField.setPromptText("Observation (optionnel)");
        obsField.setStyle("-fx-background-color: white; -fx-border-color: #64748B; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13px; -fx-padding: 8;");

        Label lblT = new Label("Ajustement Massif"); lblT.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label lblS = new Label(items.size() + " lots s\u00e9lectionn\u00e9s \u2014 retrait int\u00e9gral du stock de chaque lot"); lblS.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;"); lblS.setWrapText(true);
        Label lblW = new Label("Cette op\u00e9ration retirera la TOTALIT\u00c9 du stock de chaque lot s\u00e9lectionn\u00e9.");
        lblW.setStyle("-fx-font-size: 12px; -fx-text-fill: #B45309; -fx-background-color: #FEF3C7; -fx-background-radius: 6; -fx-padding: 8 12; -fx-font-weight: bold;"); lblW.setWrapText(true);

        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(6, lblT, lblS, lblW);
        header.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 16 20 14 20;");
        javafx.scene.layout.VBox form = new javafx.scene.layout.VBox(10, mkSectionLabel("Motif commun"), cmbMotif, mkSectionLabel("Observation"), obsField);
        form.setPadding(new javafx.geometry.Insets(16, 20, 10, 20));

        Button btnOk = new Button("Confirmer le retrait massif");
        btnOk.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;");
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 20; -fx-border-color: #CBD5E1; -fx-border-radius: 8;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox actions = new HBox(10, spacer, btnCancel, btnOk); actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actions.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0; -fx-padding: 12 20;");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(header, form, actions);
        root.setStyle("-fx-background-color: white;"); root.setPrefWidth(460);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Ajustement Massif"); stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); stage.setResizable(false);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        btnCancel.setOnAction(e -> stage.close());
        btnOk.setOnAction(e -> {
            for (EtatStockDTO sel : items) {
                Lot lot = lotDAO.findById(sel.getLotId());
                if (lot != null && lot.getQuantiteStock() > 0)
                    appliquerAjustement(lot, lot.getQuantiteStock(), cmbMotif.getValue(), obsField.getText(), MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF);
            }
            stage.close();
        });
        javafx.application.Platform.runLater(cmbMotif::requestFocus);
        stage.showAndWait();
    }

    private void appliquerAjustement(Lot lot, int qte, AjustementStock.MotifAjustement motif, String observation,
            MouvementStock.TypeMouvement typeAjustement) {
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
            if (lot.getQuantiteStock() <= 0)
                lot.setEstArchive(true);
        }
        lotDAO.update(lot);

        int qteMvt = typeAjustement == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF ? qte : -qte;
        mouvementDAO.save(new MouvementStock(
                lot.getProduit(), lot, SessionManager.getCurrentUser(),
                typeAjustement, qteMvt, java.time.LocalDateTime.now(),
                "Ajustement: " + motif.name()
                        + (observation != null && !observation.isEmpty() ? " - " + observation : "")));

        com.pharmacie.utils.ToastService.showSuccess(tableEtatStock.getScene().getWindow(), "Ajustement de Stock",
                "Le stock a été ajusté avec succès.");
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
        private long joursRestants;
        private Integer qteInitialeAchetee;
        private Double prixAchatBoite;
        private Double prixAchatUnite;

        public EtatStockDTO(Long id, String p, String c, String e, String l, String d, Integer qs, String qsFormatee,
                Integer qv, Double px, boolean exp, int seuil, Double valeurFinanciere, long joursRestants,
                Integer qteInitialeAchetee, Double prixAchatBoite, Double prixAchatUnite) {
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
            this.qteInitialeAchetee = qteInitialeAchetee;
            this.prixAchatBoite = prixAchatBoite;
            this.prixAchatUnite = prixAchatUnite;
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
            if (valeurFinanciere == null)
                return "---";
            return String.format(java.util.Locale.FRANCE, "%,.0f FCFA", valeurFinanciere);
        }

        /**
         * Retourne les jours restants avant exp : positif = OK, 0 = aujourd'hui,
         * négatif = expiré
         */
        public long getJoursRestants() {
            return joursRestants;
        }

        /**
         * Formate intelligemment les jours restants pour l'affichage :
         * "❌ Expiré", "⚠️ 3 j", "12 j", "Aucune date"
         */
        public String getJoursRestantsFormate() {
            if (dateExpiration.equals("---"))
                return "Sans date";
            if (joursRestants < 0)
                return "❌ Expiré";
            if (joursRestants == 0)
                return "⚠️ Auj.";
            if (joursRestants <= 30)
                return "⚠️ " + joursRestants + " j";
            return joursRestants + " j";
        }

        public String getQteInitialeAchetee() {
            if (qteInitialeAchetee == null)
                return "---";
            return qteInitialeAchetee + " U";
        }

        public String getPrixAchatBoiteFormate() {
            if (prixAchatBoite == null)
                return "---";
            return String.format(java.util.Locale.FRANCE, "%,.0f FCFA", prixAchatBoite);
        }

        public String getPrixAchatUniteFormate() {
            if (prixAchatUnite == null)
                return "N/A";
            return String.format(java.util.Locale.FRANCE, "%,.0f FCFA", prixAchatUnite);
        }
    }
}
