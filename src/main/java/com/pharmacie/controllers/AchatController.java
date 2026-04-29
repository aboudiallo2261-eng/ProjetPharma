package com.pharmacie.controllers;

import com.pharmacie.dao.GenericDAO;
import com.pharmacie.dao.LotDAO;
import com.pharmacie.models.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import com.pharmacie.utils.SessionManager;
import com.pharmacie.dao.MouvementDAO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AchatController {

    private static final Logger logger = LoggerFactory.getLogger(AchatController.class);

    // --- FORMULAIRE ---
    @FXML
    private ComboBox<Fournisseur> cmbFournisseur;
    @FXML
    private TextField txtRefFacture;
    @FXML
    private DatePicker dpDateAchat;
    @FXML
    private Button btnExportExcelAchats;

    @FXML
    private ComboBox<Produit> cmbProduit;
    @FXML
    private TextField txtNumLot;
    @FXML
    private DatePicker dpExpLot;
    @FXML
    private TextField txtQuantite;
    @FXML
    private TextField txtPrixAchat;

    @FXML
    private Label lblAchatError;
    @FXML
    private CheckBox chkImprimerBon;

    @FXML
    private Button btnRetirerPanier;
    @FXML
    private Button btnViderPanier;
    @FXML
    private Button btnValiderAchat;

    // --- PANIER ---
    @FXML
    private TableView<LigneAchat> tableLignesPanier;
    @FXML
    private TableColumn<LigneAchat, String> colPanierProd;
    @FXML
    private TableColumn<LigneAchat, String> colPanierLot;
    @FXML
    private TableColumn<LigneAchat, Integer> colPanierQte;
    @FXML
    private TableColumn<LigneAchat, Double> colPanierPrix;
    @FXML
    private TableColumn<LigneAchat, Double> colPanierSousTotal;

    @FXML
    private Label lblTotalCommande;

    // --- HISTORIQUE ---
    @FXML
    private TableView<Achat> tableHistorique;
    @FXML
    private TableColumn<Achat, Long> colHistId;
    @FXML
    private TableColumn<Achat, String> colHistDate;
    @FXML
    private TableColumn<Achat, String> colHistFournisseur;
    @FXML
    private TableColumn<Achat, String> colHistRef;
    @FXML
    private TableColumn<Achat, String> colHistMontant;
    @FXML
    private TableColumn<Achat, String> colHistNbLignes;

    // --- FILTRES HISTORIQUE ---
    @FXML
    private DatePicker dpAchatDebut;
    @FXML
    private DatePicker dpAchatFin;
    @FXML
    private ComboBox<Produit> cmbFiltreAchatProduit;
    @FXML
    private ComboBox<Fournisseur> cmbFiltreAchatFournisseur;
    @FXML
    private TextField txtSearchAchat;
    @FXML
    private Label lblTotalAchats;
    @FXML
    private Button btnImprimerBonCmd;
    @FXML
    private Button btnVoirDetailsAchat;

    private GenericDAO<Fournisseur> fournisseurDAO = new GenericDAO<>(Fournisseur.class);
    private GenericDAO<Produit> produitDAO = new GenericDAO<>(Produit.class);
    private com.pharmacie.dao.AchatDAO achatDAO = new com.pharmacie.dao.AchatDAO();
    private com.pharmacie.services.AchatService achatService = new com.pharmacie.services.AchatService();

    private ObservableList<LigneAchat> panier = FXCollections.observableArrayList();
    private javafx.beans.property.BooleanProperty isProcessingTransaction = new javafx.beans.property.SimpleBooleanProperty(
            false);
    private final java.text.NumberFormat moneyFormat = java.text.NumberFormat
            .getInstance(new java.util.Locale("fr", "FR"));

    @FXML
    public void initialize() {
        initFormatters();
        initPanierColumns();
        initHistoriqueColumns();
        loadDonneesBase();
        dpDateAchat.setValue(java.time.LocalDate.now());

        // Lock ALL datepickers — keyboard input is forbidden, calendar only
        if (dpDateAchat.getEditor() != null)
            dpDateAchat.getEditor().setEditable(false);
        if (dpExpLot.getEditor() != null)
            dpExpLot.getEditor().setEditable(false);
        if (dpAchatDebut.getEditor() != null)
            dpAchatDebut.getEditor().setEditable(false);
        if (dpAchatFin.getEditor() != null)
            dpAchatFin.getEditor().setEditable(false);

        com.pharmacie.utils.DateUtils.bindDateFilters(dpAchatDebut, dpAchatFin);

        if (btnValiderAchat != null)
            btnValiderAchat.disableProperty()
                    .bind(javafx.beans.binding.Bindings.isEmpty(panier).or(isProcessingTransaction));
        if (btnViderPanier != null) {
            btnViderPanier.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(panier));
            // Point 6 : Couleur dynamique du bouton Vider (rouge = actif, gris = panier
            // vide)
            panier.addListener((javafx.collections.ListChangeListener<LigneAchat>) c -> {
                if (panier.isEmpty()) {
                    btnViderPanier
                            .setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-background-radius: 6;");
                } else {
                    btnViderPanier.setStyle(
                            "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
                }
            });
            btnViderPanier.setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-background-radius: 6;");
        }

        // POINT 2 : Bouton Retirer — gris par défaut, rouge uniquement quand une ligne
        // est sélectionnée
        if (btnRetirerPanier != null) {
            btnRetirerPanier.disableProperty().bind(
                    tableLignesPanier.getSelectionModel().selectedItemProperty().isNull());
            tableLignesPanier.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (newSel != null) {
                    btnRetirerPanier
                            .setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 6;");
                } else {
                    btnRetirerPanier
                            .setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-background-radius: 6;");
                }
            });
        }

        // Charger les fournisseurs pour le filtre historique
        javafx.collections.ObservableList<Fournisseur> fournisseursFiltre = FXCollections.observableArrayList();
        fournisseursFiltre.add(null);
        fournisseursFiltre.addAll(fournisseurDAO.findAll());
        cmbFiltreAchatFournisseur.setItems(fournisseursFiltre);
        cmbFiltreAchatFournisseur.setConverter(new javafx.util.StringConverter<Fournisseur>() {
            @Override
            public String toString(Fournisseur f) {
                return f != null ? f.getNom() : "Tous les fournisseurs";
            }

            @Override
            public Fournisseur fromString(String s) {
                return null;
            }
        });

        // Charger les produits pour le filtre
        javafx.collections.ObservableList<Produit> produitsFiltre = FXCollections.observableArrayList();
        produitsFiltre.add(null);
        produitsFiltre.addAll(produitDAO.findAll());
        cmbFiltreAchatProduit.setItems(produitsFiltre);
        cmbFiltreAchatProduit.setConverter(new javafx.util.StringConverter<Produit>() {
            @Override
            public String toString(Produit p) {
                return p != null ? p.getNom() : "Tous les produits";
            }

            @Override
            public Produit fromString(String s) {
                return null;
            }
        });

        javafx.event.EventHandler<javafx.event.ActionEvent> autoFilterEvent = e -> {
            if (!isUpdatingHistoriqueAchatsFiltres)
                filtrerHistoriqueAchats();
        };
        cmbFiltreAchatFournisseur.setOnAction(autoFilterEvent);
        cmbFiltreAchatProduit.setOnAction(autoFilterEvent);
        dpAchatDebut.setOnAction(autoFilterEvent);
        dpAchatFin.setOnAction(autoFilterEvent);

        // Auto-filtre : dès qu'on tape dans le champ de recherche
        if (txtSearchAchat != null) {
            txtSearchAchat.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingHistoriqueAchatsFiltres)
                    filtrerHistoriqueAchats();
            });
        }
    }

    private boolean isUpdatingHistoriqueAchatsFiltres = false;

    private void initFormatters() {
        cmbFournisseur.setConverter(new StringConverter<>() {
            @Override
            public String toString(Fournisseur f) {
                return f != null ? f.getNom() : "";
            }

            @Override
            public Fournisseur fromString(String s) {
                return null;
            }
        });

        cmbProduit.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produit p) {
                return p != null ? p.getNom() : "";
            }

            @Override
            public Produit fromString(String s) {
                return null;
            }
        });

        // 5. Force numeric input only for Quantité and Prix
        txtQuantite.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtQuantite.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        txtPrixAchat.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtPrixAchat.setText(oldVal); // Reject non-digit and non-dot chars
            }
        });

        // Keyboard First: Touche Entrée pour valider dynamiquement
        javafx.event.EventHandler<javafx.scene.input.KeyEvent> enterHandler = e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                addLigneAchat();
                cmbProduit.requestFocus();
            }
        };
        txtQuantite.setOnKeyPressed(enterHandler);
        txtPrixAchat.setOnKeyPressed(enterHandler);
    }

    private void initPanierColumns() {
        colPanierProd.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduit().getNom()));
        colPanierLot.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLot().getNumeroLot()));

        // FIX 2: Colonnes Qté et Prix éditables directement dans le tableau
        colPanierQte.setCellValueFactory(new PropertyValueFactory<>("quantiteAchetee"));
        // Point 4 : CellFactory avec TextFormatter - uniquement des chiffres positifs
        colPanierQte.setCellFactory(col -> {
            TextFieldTableCell<LigneAchat, Integer> cell = new TextFieldTableCell<>(
                    new javafx.util.converter.IntegerStringConverter() {
                        @Override
                        public Integer fromString(String s) {
                            if (s == null || s.isBlank())
                                return 1;
                            try {
                                return Math.max(1, Integer.parseInt(s.trim()));
                            } catch (NumberFormatException ex) {
                                return 1;
                            }
                        }
                    }) {
                @Override
                public void startEdit() {
                    super.startEdit();
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.Node g = getGraphic();
                        if (g instanceof javafx.scene.control.TextField tf) {
                            tf.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
                                String txt = change.getText();
                                return (txt.isEmpty() || txt.matches("[0-9]+")) ? change : null;
                            }));
                        }
                    });
                }
            };
            return cell;
        });
        colPanierQte.setOnEditCommit(event -> {
            int newQte = event.getNewValue() != null ? event.getNewValue() : event.getOldValue();
            if (newQte > 0) {
                event.getRowValue().setQuantiteAchetee(newQte);
            } else {
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.ERROR, "Erreur", "Quantité invalide",
                        "La quantité doit être supérieure à 0.");
            }
            tableLignesPanier.refresh();
            calculerTotal();
        });

        colPanierPrix.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        // Point 4 : Filtre chiffres + point décimal pour le prix
        colPanierPrix.setCellFactory(col -> {
            TextFieldTableCell<LigneAchat, Double> cell = new TextFieldTableCell<>(
                    new javafx.util.converter.DoubleStringConverter() {
                        @Override
                        public Double fromString(String s) {
                            if (s == null || s.isBlank())
                                return 0.0;
                            try {
                                return Math.max(0.0, Double.parseDouble(s.trim().replace(",", ".")));
                            } catch (NumberFormatException ex) {
                                return 0.0;
                            }
                        }
                    }) {
                @Override
                public void startEdit() {
                    super.startEdit();
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.Node g = getGraphic();
                        if (g instanceof javafx.scene.control.TextField tf) {
                            tf.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
                                String txt = change.getControlNewText();
                                return txt.matches("\\d*\\.?\\d*") ? change : null;
                            }));
                        }
                    });
                }
            };
            return cell;
        });
        colPanierPrix.setOnEditCommit(event -> {
            double newPrix = event.getNewValue() != null ? event.getNewValue() : event.getOldValue();
            if (newPrix >= 0) {
                event.getRowValue().setPrixUnitaire(newPrix);
            } else {
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.ERROR, "Erreur", "Prix invalide",
                        "Le prix ne peut pas être négatif.");
            }
            tableLignesPanier.refresh();
            calculerTotal();
        });

        colPanierSousTotal.setCellValueFactory(cell -> {
            Double total = cell.getValue().getQuantiteAchetee() * cell.getValue().getPrixUnitaire();
            return new javafx.beans.property.SimpleObjectProperty<>(total);
        });
        colPanierSousTotal.setCellFactory(tc -> new TableCell<LigneAchat, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(moneyFormat.format(price) + " FCFA");
                }
            }
        });
        tableLignesPanier.setItems(panier);
        // Note : pas de CONSTRAINED ici, prefWidth gère l'équilibre
    }

    private void initHistoriqueColumns() {
        colHistId.setCellValueFactory(new PropertyValueFactory<>("id"));

        // CORRECTION 2 : Format date humain (ex: "15 Mar. 2024")
        java.text.DateFormatSymbols dfs = new java.text.DateFormatSymbols(java.util.Locale.FRENCH);
        colHistDate.setCellValueFactory(cell -> {
            java.time.LocalDate d = cell.getValue().getDateAchat().toLocalDate();
            String[] mois = { "Jan.", "Fév.", "Mar.", "Avr.", "Mai", "Juin", "Juil.", "Août", "Sep.", "Oct.", "Nov.",
                    "Déc." };
            return new SimpleStringProperty(
                    String.format("%02d %s %d", d.getDayOfMonth(), mois[d.getMonthValue() - 1], d.getYear()));
        });

        colHistFournisseur
                .setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFournisseur().getNom()));
        colHistRef.setCellValueFactory(new PropertyValueFactory<>("referenceFacture"));
        colHistMontant.setCellValueFactory(cell -> {
            double montant = cell.getValue().getLignesAchat() != null
                    ? cell.getValue().getLignesAchat().stream()
                            .mapToDouble(la -> la.getQuantiteAchetee() * la.getPrixUnitaire()).sum()
                    : 0.0;
            return new SimpleStringProperty(moneyFormat.format(montant) + " FCFA");
        });

        // CORRECTION 1 : Colonne Nb Produits (valeur numérique uniquement)
        if (colHistNbLignes != null) {
            colHistNbLignes.setCellValueFactory(cell -> {
                int nb = cell.getValue().getLignesAchat() != null ? cell.getValue().getLignesAchat().size() : 0;
                return new SimpleStringProperty(String.valueOf(nb));
            });
            colHistNbLignes.setStyle("-fx-alignment: CENTER;");
        }

        // Activer/désactiver les boutons selon la sélection
        tableHistorique.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    btnImprimerBonCmd.setDisable(selected == null);
                    if (btnVoirDetailsAchat != null)
                        btnVoirDetailsAchat.setDisable(selected == null);
                });

        tableHistorique.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableHistorique.getSelectionModel().getSelectedItem() != null) {
                showSelectedAchatDetail();
            }
        });
        // Note : pas de CONSTRAINED ici, prefWidth gère l'équilibre
    }

    @FXML
    public void showSelectedAchatDetail() {
        Achat selecAchat = tableHistorique.getSelectionModel().getSelectedItem();
        if (selecAchat == null)
            return;

        Stage stage = (Stage) tableHistorique.getScene().getWindow();
        com.pharmacie.utils.PdfService.genererBonDeCommande(selecAchat, stage);
    }

    private void loadDonneesBase() {
        try {
            cmbFournisseur.setItems(FXCollections.observableArrayList(fournisseurDAO.findAll()));
            cmbProduit.setItems(FXCollections.observableArrayList(produitDAO.findAll()));

            // Appliquer l'auto-completion après le chargement initial
            com.pharmacie.utils.ComboBoxAutoComplete.setup(cmbFournisseur);
            com.pharmacie.utils.ComboBoxAutoComplete.setup(cmbProduit);

            loadHistorique();
        } catch (Exception e) {
            logger.error("Erreur chargement donnees base achat", e);
        }
    }

    @FXML
    public void addLigneAchat() {
        try {
            // VALIDATION PRIORITAIRE : informations de l'achat obligatoires
            if (cmbFournisseur.getValue() == null) {
                showErrorEffect(cmbFournisseur);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Champ Manquant",
                        "Fournisseur non sélectionné",
                        "Veuillez d'abord sélectionner un fournisseur dans la section \"Informations de l'achat\".");
                return;
            }
            if (dpDateAchat.getValue() == null) {
                showErrorEffect(dpDateAchat);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Champ Manquant",
                        "Date d'achat manquante",
                        "Veuillez renseigner la date de l'achat avant d'ajouter des produits au panier.");
                return;
            }

            Produit p = cmbProduit.getValue();
            String numLot = txtNumLot.getText().trim();
            String qteText = txtQuantite.getText().trim();
            String prixText = txtPrixAchat.getText().trim();

            // FIX 1: Validation champ par champ avec message explicite et effet flash
            if (p == null) {
                showErrorEffect(cmbProduit);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Champ Manquant",
                        "Produit non sélectionné", "Veuillez sélectionner un produit dans la liste.");
                return;
            }
            if (numLot.isEmpty()) {
                showErrorEffect(txtNumLot);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Champ Manquant",
                        "Numéro de lot requis", "Veuillez saisir le numéro de lot du produit.");
                return;
            }
            if (dpExpLot.getValue() == null) {
                showErrorEffect(dpExpLot);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Champ Manquant",
                        "Date d'expiration requise", "Veuillez sélectionner la date d'expiration du lot.");
                return;
            }
            if (qteText.isEmpty()) {
                showErrorEffect(txtQuantite);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Champ Manquant",
                        "Quantité requise", "Veuillez saisir la quantité achetée.");
                return;
            }
            if (prixText.isEmpty()) {
                showErrorEffect(txtPrixAchat);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Champ Manquant",
                        "Prix requis", "Veuillez saisir le prix d'achat unitaire.");
                return;
            }

            int qte = Integer.parseInt(qteText);
            double prix = Double.parseDouble(prixText);

            if (qte <= 0) {
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Valeur Invalide",
                        "Quantité invalide", "La quantité doit être supérieure à 0.");
                return;
            }
            if (prix < 0) {
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING, "Valeur Invalide",
                        "Prix invalide", "Le prix d'achat ne peut pas être négatif.");
                return;
            }

            // POINT 4 : Garde réglementaire — date expiration cohérente
            if (dpExpLot.getValue() != null && dpDateAchat.getValue() != null) {
                if (dpExpLot.getValue().isBefore(dpDateAchat.getValue())) {
                    com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.ERROR,
                            "Erreur Réglementaire", "Date d'expiration impossible",
                            "La date d'expiration est antérieure à la date d'achat.\nUn lot ne peut pas être déjà périmé à la réception.");
                    return;
                }
                if (dpExpLot.getValue().isBefore(java.time.LocalDate.now())) {
                    com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.ERROR,
                            "Erreur Réglementaire", "Lot déjà expiré",
                            "La date d'expiration indiquée est dans le passé.\nIl est interdit de réceptionner un lot périmé.");
                    return;
                }
            }

            // POINT 3 : Détection de doublon [Produit + Numéro de Lot] dans le panier
            final Produit prodFinal = p;
            final String numLotFinal = numLot;
            boolean doublonPanier = panier.stream()
                    .anyMatch(ligne -> ligne.getProduit().getId().equals(prodFinal.getId()) &&
                            ligne.getLot().getNumeroLot().equalsIgnoreCase(numLotFinal));
            if (doublonPanier) {
                com.pharmacie.utils.AlertUtils.showPremiumAlert(Alert.AlertType.WARNING,
                        "Doublon détecté", "Ce lot est déjà dans le panier",
                        "\"" + p.getNom() + "\" \u2014 Lot \"" + numLot
                                + "\" a déjà été ajouté.\nDouble-cliquez sur la ligne dans le panier pour modifier la quantité.");
                return;
            }

            // POINTS 5 & 6 : Sécurisation des marges et Vente à Perte
            // L'alerte ne se déclenche QUE si le nouveau Prix d'Achat dépasse le Prix de
            // Vente public (Vente à perte)
            if (p.getPrixVente() != null && prix > p.getPrixVente()) {

                Dialog<ButtonType> alertDlg = new Dialog<>();
                alertDlg.setTitle("Alerte de Vente à Perte");

                javafx.scene.layout.VBox alertContent = new javafx.scene.layout.VBox(15);
                alertContent.setPadding(new javafx.geometry.Insets(25));
                alertContent.setPrefWidth(450);

                Label alertTitle = new Label("Le prix d'achat dépasse votre prix de vente !");
                alertTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E11D48;"); // Rose 600

                Label alertMsg = new Label("Attention : Le nouveau prix d'achat (" + moneyFormat.format(prix)
                        + " FCFA) est supérieur au prix public de la boîte (" + moneyFormat.format(p.getPrixVente())
                        + " FCFA).\nVous vendez à perte.\n\nSouhaitez-vous ajuster vos Prix de Vente pour ce produit de toute urgence ?");
                alertMsg.setWrapText(true);
                alertMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155; -fx-line-spacing: 5px;");

                alertContent.getChildren().addAll(alertTitle, alertMsg);

                alertDlg.getDialogPane().setContent(alertContent);
                alertDlg.getDialogPane().setStyle(
                        "-fx-background-color: #F8FAFC; -fx-border-color: #FDA4AF; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6;");

                ButtonType btnOui = new ButtonType("Oui, Ajuster", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNon = new ButtonType("Ignorer", ButtonBar.ButtonData.CANCEL_CLOSE);

                alertDlg.getDialogPane().getButtonTypes().addAll(btnOui, btnNon);

                javafx.application.Platform.runLater(() -> {
                    javafx.scene.Node ouiNode = alertDlg.getDialogPane().lookupButton(btnOui);
                    if (ouiNode != null) {
                        ouiNode.setStyle(
                                "-fx-background-color: #E11D48; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 6;");
                    }
                    javafx.scene.Node nonNode = alertDlg.getDialogPane().lookupButton(btnNon);
                    if (nonNode != null) {
                        nonNode.setStyle(
                                "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-border-color: #CBD5E1; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-padding: 7 20; -fx-cursor: hand;");
                    }
                });

                boolean confirm = alertDlg.showAndWait().map(b -> b == btnOui).orElse(false);

                if (confirm) {
                    boolean estDetail = p.getEstDeconditionnable() != null && p.getEstDeconditionnable();

                    Dialog<java.util.List<String>> dialog = new Dialog<>();
                    dialog.setTitle("Mise à jour d'Urgence des Prix");

                    javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox(20);
                    contentBox.setPadding(new javafx.geometry.Insets(25));
                    contentBox.setPrefWidth(400);

                    Label titleLbl = new Label(
                            "Ajustement des prix pour : " + p.getNom() + (estDetail ? " (Boîte + Détail)" : ""));
                    titleLbl.setWrapText(true);
                    titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

                    javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
                    grid.setHgap(15);
                    grid.setVgap(15);

                    TextField txtPrixBoite = new TextField(
                            String.valueOf(p.getPrixVente() != null ? p.getPrixVente() : ""));
                    txtPrixBoite.setStyle("-fx-font-size: 14px; -fx-padding: 8;");

                    Label lblBoite = new Label("Nouveau Prix Boîte (FCFA) :");
                    lblBoite.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");
                    grid.add(lblBoite, 0, 0);
                    grid.add(txtPrixBoite, 1, 0);

                    final TextField txtPrixUnite = new TextField(
                            estDetail ? String.valueOf(p.getPrixVenteUnite() != null ? p.getPrixVenteUnite() : "")
                                    : "");
                    txtPrixUnite.setStyle("-fx-font-size: 14px; -fx-padding: 8;");

                    if (estDetail) {
                        Label lblUnite = new Label("Nouveau Prix Unité (FCFA) :");
                        lblUnite.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");
                        grid.add(lblUnite, 0, 1);
                        grid.add(txtPrixUnite, 1, 1);

                        // Action: Suggerer prix detail automatiquement
                        txtPrixBoite.textProperty().addListener((obs, oldVal, newVal) -> {
                            try {
                                if (p.getUnitesParBoite() != null && p.getUnitesParBoite() > 0) {
                                    double np = Double.parseDouble(newVal);
                                    txtPrixUnite.setText(String.valueOf(Math.round(np / p.getUnitesParBoite())));
                                }
                            } catch (Exception e) {
                            }
                        });
                    }

                    contentBox.getChildren().addAll(titleLbl, grid);
                    dialog.getDialogPane().setContent(contentBox);
                    dialog.getDialogPane().setStyle(
                            "-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 1.5; -fx-border-radius: 6;");

                    ButtonType btnValider = new ButtonType("Mettre à jour", ButtonBar.ButtonData.OK_DONE);
                    ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                    dialog.getDialogPane().getButtonTypes().addAll(btnValider, btnAnnuler);

                    javafx.application.Platform.runLater(() -> {
                        txtPrixBoite.requestFocus();

                        javafx.scene.Node valNode = dialog.getDialogPane().lookupButton(btnValider);
                        if (valNode != null) {
                            valNode.setStyle(
                                    "-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 6;");
                        }
                        javafx.scene.Node annNode = dialog.getDialogPane().lookupButton(btnAnnuler);
                        if (annNode != null) {
                            annNode.setStyle(
                                    "-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-border-color: #CBD5E1; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-padding: 7 20; -fx-cursor: hand;");
                        }
                    });

                    final TextField tfUnite = estDetail ? txtPrixUnite : null;
                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == btnValider) {
                            java.util.List<String> results = new java.util.ArrayList<>();
                            results.add(txtPrixBoite.getText());
                            if (tfUnite != null)
                                results.add(tfUnite.getText());
                            return results;
                        }
                        return null;
                    });

                    dialog.showAndWait().ifPresent(results -> {
                        try {
                            if (!results.get(0).isEmpty()) {
                                double newPrixBoite = Double.parseDouble(results.get(0));
                                p.setPrixVente(newPrixBoite);
                            }
                            if (estDetail && results.size() > 1 && !results.get(1).isEmpty()) {
                                double newPrixUnite = Double.parseDouble(results.get(1));
                                p.setPrixVenteUnite(newPrixUnite);
                            }
                            produitDAO.update(p);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
            // Create temporary lot mapping
            Lot lot = new Lot();
            lot.setProduit(p);
            lot.setNumeroLot(numLot);
            if (dpExpLot.getValue() != null) {
                lot.setDateExpiration(dpExpLot.getValue());
            }

            LigneAchat ligne = new LigneAchat();
            ligne.setProduit(p);
            ligne.setLot(lot);
            ligne.setQuantiteAchetee(qte);
            ligne.setPrixUnitaire(prix);

            panier.add(ligne);
            calculerTotal();
            resetProduitForm();
        } catch (NumberFormatException e) {
            showError("Veuillez saisir des nombres valides pour la quantité et le prix.");
        }
    }

    @FXML
    public void removeLigneAchat() {
        LigneAchat selected = tableLignesPanier.getSelectionModel().getSelectedItem();
        if (selected != null) {
            panier.remove(selected);
            calculerTotal();
        }
    }

    private void calculerTotal() {
        double total = panier.stream().mapToDouble(l -> l.getQuantiteAchetee() * l.getPrixUnitaire()).sum();
        // POINT 5 : Formatage monétaire uniforme avec séparateur de milliers
        com.pharmacie.utils.AnimationUtils.animerValeurMonetaire(lblTotalCommande, total, "");
    }

    private void resetProduitForm() {
        // FIX 1: Réinitialisation propre du ComboBox avec le prompt text visible
        resetComboBox(cmbProduit, "Sélectionner Produit *");
        txtNumLot.clear();
        dpExpLot.setValue(null);
        txtQuantite.clear();
        txtPrixAchat.clear();
        lblAchatError.setVisible(false);
    }

    /**
     * FIX 1: JavaFX ne réaffiche pas le promptText après setValue(null) si le
     * ComboBox
     * a déjà eu une sélection. Ce workaround force un repaint propre.
     */
    private <T> void resetComboBox(ComboBox<T> combo, String prompt) {
        combo.setValue(null);
        combo.getSelectionModel().clearSelection();
        combo.setPromptText(prompt);
        // Force JavaFX to redraw the prompt text by replacing the button cell
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(prompt);
                    setStyle("-fx-text-fill: -fx-prompt-text-fill;");
                } else {
                    setText(combo.getConverter() != null ? combo.getConverter().toString(item) : item.toString());
                    setStyle("");
                }
            }
        });
    }

    @FXML
    public void clearPanier() {
        panier.clear();
        calculerTotal();
    }

    @FXML
    public void validerAchat() {
        if (panier.isEmpty() || isProcessingTransaction.get()) {
            return; // Protected by binding and transaction lock
        }

        isProcessingTransaction.set(true);
        try {
            Fournisseur f = cmbFournisseur.getValue();
            if (f == null || dpDateAchat.getValue() == null) {
                if (f == null)
                    showErrorEffect(cmbFournisseur);
                if (dpDateAchat.getValue() == null)
                    showErrorEffect(dpDateAchat);
                com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                        "Champs Manquants", "Fournisseur et Date d'achat sont obligatoires.");
                return;
            }

            Achat achat = new Achat();
            achat.setFournisseur(f);
            achat.setDateAchat(dpDateAchat.getValue().atStartOfDay());
            achat.setReferenceFacture(txtRefFacture.getText());
            achat.setLignesAchat(new ArrayList<>()); // will be repopulated securely by service

            boolean success = achatService.enregistrerCommandeTransactionnelle(achat, new ArrayList<>(panier));

            if (success) {
                com.pharmacie.utils.ToastService.showSuccess(txtRefFacture.getScene().getWindow(), "Achat Enregistré",
                        "L'approvisionnement a été validé avec succès !");

                // 2. Impression du bon
                if (chkImprimerBon != null && chkImprimerBon.isSelected()) {
                    Stage stage = (Stage) txtRefFacture.getScene().getWindow();
                    com.pharmacie.utils.PdfService.genererBonDeCommande(achat, stage);
                }

                clearPanier();
                // FIX 1: Forcer le réaffichage du promptText — setValue(null) ne suffit pas
                // avec JavaFX
                resetComboBox(cmbFournisseur, "Sélectionner Fournisseur *");
                txtRefFacture.clear();

                // FIX : Recharger la liste des produits pour que les modifications de prix
                // (Achat et Vente)
                // soient immédiatement visibles sans avoir à relancer l'application.
                cmbProduit.setItems(javafx.collections.FXCollections.observableArrayList(produitDAO.findAll()));

                loadHistorique();
                logger.info("Achat validé avec succès (ACID).");
            } else {
                logger.error("L'achat n'a pas pu être enregistré, transaction annulée");
                com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR,
                        "Erreur Critique BDD", "L'achat n'a pas pu être enregistré",
                        "La transaction a été totalement annulée pour protéger vos données.");
            }
        } finally {
            // Toujours déverrouiller le bouton valider
            isProcessingTransaction.set(false);
        }
    }

    // Fonction utilitaire pour flasher un champ en rouge
    private void showErrorEffect(javafx.scene.Node node) {
        if (node == null)
            return;
        String originalStyle = node.getStyle();
        // Force la bordure rouge sans casser le CSS sous-jacent complet
        node.setStyle(originalStyle + "; -fx-border-color: #E74C3C; -fx-border-width: 2px; -fx-border-radius: 4px;");
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> node.setStyle(originalStyle));
        pause.play();
    }

    @FXML
    public void loadHistorique() {
        // Point 8 : Filtre par défaut = Aujourd'hui (optimisation massive des perfs DB)
        LocalDate aujourdhui = LocalDate.now();
        if (dpAchatDebut != null && dpAchatDebut.getValue() == null)
            dpAchatDebut.setValue(aujourdhui);
        if (dpAchatFin != null && dpAchatFin.getValue() == null)
            dpAchatFin.setValue(aujourdhui);
        filtrerHistoriqueAchats();
    }

    @FXML
    public void filtrerHistoriqueAchats() {
        LocalDate debut = dpAchatDebut.getValue();
        LocalDate fin = dpAchatFin.getValue();
        Produit produitFiltre = cmbFiltreAchatProduit.getValue();
        Fournisseur fournisseurFiltre = (cmbFiltreAchatFournisseur != null) ? cmbFiltreAchatFournisseur.getValue()
                : null;
        String searchRef = (txtSearchAchat != null && txtSearchAchat.getText() != null)
                ? txtSearchAchat.getText().trim().toLowerCase()
                : "";

        // CORRECTION 3 : Recherche universelle — Fournisseur + Réf. Facture
        List<Achat> all = achatDAO.findAllWithDetails();
        List<Achat> filtered = all.stream().filter(a -> {
            if (a.getDateAchat() == null)
                return false;
            LocalDate da = a.getDateAchat().toLocalDate();
            if (debut != null && da.isBefore(debut))
                return false;
            if (fin != null && da.isAfter(fin))
                return false;
            if (fournisseurFiltre != null
                    && (a.getFournisseur() == null || !a.getFournisseur().getId().equals(fournisseurFiltre.getId())))
                return false;
            if (!searchRef.isEmpty()) {
                // Recherche dans la référence de facture ET dans le nom du fournisseur
                String ref = a.getReferenceFacture() != null ? a.getReferenceFacture().toLowerCase() : "";
                String nomFournisseur = (a.getFournisseur() != null && a.getFournisseur().getNom() != null)
                        ? a.getFournisseur().getNom().toLowerCase()
                        : "";
                if (!ref.contains(searchRef) && !nomFournisseur.contains(searchRef))
                    return false;
            }
            if (produitFiltre != null) {
                boolean contient = a.getLignesAchat() != null && a.getLignesAchat().stream()
                        .anyMatch(la -> la.getProduit().getId().equals(produitFiltre.getId()));
                if (!contient)
                    return false;
            }
            return true;
        }).collect(Collectors.toList());
        tableHistorique.getItems().setAll(filtered);
        mettreAJourTotalAchats(filtered, fournisseurFiltre != null || produitFiltre != null || !searchRef.isEmpty());
    }

    @FXML
    public void resetFiltresHistorique() {
        isUpdatingHistoriqueAchatsFiltres = true;
        dpAchatDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpAchatFin.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        if (cmbFiltreAchatFournisseur != null)
            cmbFiltreAchatFournisseur.getSelectionModel().select(0);
        if (cmbFiltreAchatProduit != null)
            cmbFiltreAchatProduit.getSelectionModel().select(0);
        if (txtSearchAchat != null)
            txtSearchAchat.clear();
        isUpdatingHistoriqueAchatsFiltres = false;
        filtrerHistoriqueAchats();
    }

    private void mettreAJourTotalAchats(List<Achat> achats) {
        mettreAJourTotalAchats(achats, false);
    }

    // CORRECTION 5 : Badge couleur dynamique selon l'état de filtrage
    private void mettreAJourTotalAchats(List<Achat> achats, boolean filtreActif) {
        if (lblTotalAchats == null)
            return;
        double total = achats.stream()
                .flatMap(
                        a -> a.getLignesAchat() != null ? a.getLignesAchat().stream() : java.util.stream.Stream.empty())
                .mapToDouble(la -> la.getQuantiteAchetee() * la.getPrixUnitaire()).sum();
        com.pharmacie.utils.AnimationUtils.animerValeurMonetaire(lblTotalAchats, total, "");
    }

    @FXML
    public void imprimerBonDeCommande() {
        Achat selected = tableHistorique.getSelectionModel().getSelectedItem();
        if (selected == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Aucun achat sélectionné");
            alert.setHeaderText("Sélection requise");
            alert.setContentText(
                    "Veuillez cliquer sur une ligne dans le tableau pour sélectionner l'achat à imprimer.");
            alert.showAndWait();
            return;
        }
        Stage stage = (Stage) tableHistorique.getScene().getWindow();
        com.pharmacie.utils.PdfService.genererBonDeCommande(selected, stage);
    }

    @FXML
    public void exporterAchatsExcel() {
        if (tableHistorique.getItems() == null || tableHistorique.getItems().isEmpty()) {
            com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.WARNING, "Aucune donnée",
                    "Export impossible", "Aucun achat ne correspond à vos filtres.");
            return;
        }
        Stage stage = (Stage) tableHistorique.getScene().getWindow();
        String periode = "";
        if (dpAchatDebut.getValue() != null && dpAchatFin.getValue() != null) {
            periode = dpAchatDebut.getValue().toString() + "_au_" + dpAchatFin.getValue().toString();
        }
        com.pharmacie.utils.ExcelExportService.genererHistoriqueAchatsExcel(new java.util.ArrayList<>(tableHistorique.getItems()), periode, stage);
    }

    private void showError(String msg) {
        lblAchatError.setText(msg);
        lblAchatError.setVisible(true);
    }

    @FXML
    public void showAlertesPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/alertes_stock.fxml"));
            Parent root = loader.load();
            com.pharmacie.controllers.AlerteStockController controller = loader.getController();
            controller.setAchatController(this);
            Stage stage = new Stage();
            stage.setTitle("Alertes d'Approvisionnement");
            stage.initModality(Modality.APPLICATION_MODAL);
            // On peut définir la taille pour que ce soit une belle popup
            Scene scene = new Scene(root, 1000, 600);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            logger.error("Erreur lors de l'ouverture des alertes", e);
            showError("Erreur lors de l'ouverture des alertes : " + e.getMessage());
        }
    }

    public void preparerCommandeAutomatique(com.pharmacie.models.Produit p, int quantite) {
        // 1. Réinitialisation partielle pour préparer l'entrée
        txtNumLot.clear();
        dpExpLot.setValue(null);

        // 2. Sélection du Produit
        for (com.pharmacie.models.Produit cmbP : cmbProduit.getItems()) {
            if (cmbP.getId().equals(p.getId())) {
                cmbProduit.setValue(cmbP);
                break;
            }
        }

        // 3. Injection Quantité et Prix
        txtQuantite.setText(String.valueOf(quantite));
        txtPrixAchat.setText(String.valueOf(p.getPrixAchat()));

        // 4. Recherche intelligente du dernier fournisseur (Smart Fill)
        com.pharmacie.models.Fournisseur lastFournisseur = null;
        if (tableHistorique.getItems() != null) {
            java.util.List<com.pharmacie.models.Achat> historique = new java.util.ArrayList<>(
                    tableHistorique.getItems());
            // On cherche du plus récent au plus ancien
            for (com.pharmacie.models.Achat achat : historique) {
                if (achat.getLignesAchat() != null) {
                    for (com.pharmacie.models.LigneAchat la : achat.getLignesAchat()) {
                        if (la.getProduit() != null && la.getProduit().getId().equals(p.getId())) {
                            lastFournisseur = achat.getFournisseur();
                            break;
                        }
                    }
                }
                if (lastFournisseur != null)
                    break;
            }
        }

        if (lastFournisseur != null) {
            for (com.pharmacie.models.Fournisseur f : cmbFournisseur.getItems()) {
                if (f.getId().equals(lastFournisseur.getId())) {
                    cmbFournisseur.setValue(f);
                    break;
                }
            }
        }

        com.pharmacie.utils.AlertUtils.showPremiumAlert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "Pré-remplissage Automatique",
                "Alerte Traitée",
                "Le produit " + p.getNom() + " a été positionné avec la quantité minimale vitale de " + quantite
                        + ". Vous pouvez modifier avant d'ajouter au panier.");
    }

    @FXML
    public void imprimerRecapitulatifAchats() {
        List<Achat> achats = tableHistorique.getItems();
        if (achats == null || achats.isEmpty()) {
            com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.WARNING, "Aucun achat",
                    "Tableau vide", "Il n'y a aucun achat à imprimer. Appliquez un filtre ou rafraîchissez d'abord.");
            return;
        }
        Stage stage = (Stage) tableHistorique.getScene().getWindow();
        String periode = "";
        if (dpAchatDebut.getValue() != null)
            periode += dpAchatDebut.getValue().toString();
        if (dpAchatFin.getValue() != null)
            periode += "_au_" + dpAchatFin.getValue().toString();
        com.pharmacie.utils.PdfService.genererRecapitulatifAchats(new java.util.ArrayList<>(achats), periode, stage);
    }
}
