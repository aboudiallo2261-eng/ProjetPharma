package com.pharmacie.controllers;

import com.pharmacie.dao.GenericDAO;
import com.pharmacie.dao.LotDAO;
import com.pharmacie.dao.ProduitDAO;
import com.pharmacie.models.LigneVente;
import com.pharmacie.models.Lot;
import com.pharmacie.models.Produit;
import com.pharmacie.models.Vente;
import com.pharmacie.models.MouvementStock;
import com.pharmacie.models.SessionCaisse;
import com.pharmacie.dao.SessionCaisseDAO;
import com.pharmacie.utils.SessionManager;
import com.pharmacie.models.TicketEnAttente;
import com.pharmacie.dao.MouvementDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VenteController {

    private static final Logger logger = LoggerFactory.getLogger(VenteController.class);

    @FXML
    private Label lblCaisseUser;

    // --- CAISSE SESSION ---
    @FXML
    private Label lblStatutSession;
    @FXML
    private Button btnOuvrirCaisse;
    @FXML
    private Button btnCloturerCaisse;
    @FXML
    private Button btnVerrouiller;
    @FXML
    private Button btnTicketsEnAttente;
    @FXML
    private javafx.scene.layout.VBox overlayCaisseFermee;
    @FXML
    private javafx.scene.layout.VBox overlaySessionVerrouillee;
    @FXML private javafx.scene.control.PasswordField txtPinDeverrouillage;
    @FXML private javafx.scene.control.TextField txtPinVisible;
    @FXML private javafx.scene.control.Button btnTogglePin;
    @FXML private Label lblVerrouilleAgent;
    @FXML private Label lblVerrouilleErreur;
    @FXML
    private javafx.scene.layout.HBox boxVenteMain;

    // --- RECHERCHE & STOCK ---
    @FXML
    private TextField txtRechercheProduit;
    @FXML
    private TableView<Produit> tableStock;
    @FXML
    private TableColumn<Produit, String> colStkNom;
    @FXML
    private TableColumn<Produit, Double> colStkPrix;
    @FXML
    private TableColumn<Produit, Double> colStkPrixDetail;
    @FXML
    private TableColumn<Produit, String> colStkQte;

    @FXML
    private ComboBox<String> cmbQuantite;
    @FXML
    private ComboBox<LigneVente.TypeUnite> cmbUniteType;
    @FXML
    private Label lblVenteError;

    @FXML
    private TableView<LigneVente> tablePanierVente;
    @FXML
    private TableColumn<LigneVente, String> colPanVenteProd;
    @FXML
    private TableColumn<LigneVente, Integer> colPanVenteQte;
    @FXML
    private TableColumn<LigneVente, LigneVente.TypeUnite> colPanVenteType;
    @FXML
    private TableColumn<LigneVente, Double> colPanVenteTotal;

    @FXML
    private Label lblTotalVente;
    @FXML
    private ComboBox<Vente.ModePaiement> cmbModePaiement;
    @FXML
    private CheckBox chkImprimerRecu;

    @FXML
    private javafx.scene.layout.HBox boxPaiementSimple;
    @FXML
    private javafx.scene.layout.HBox boxPaiementMixte;
    @FXML
    private TextField txtMontantRecu;
    @FXML
    private TextField txtMontantEspeces;
    @FXML
    private TextField txtMontantMobile;

    @FXML
    private Label lblMonnaieRendre;
    @FXML
    private Button btnAnnulerVente;
    @FXML
    private Button btnValiderVente;
    @FXML
    private Button btnMettreEnAttente;
    @FXML
    private Button btnRetirerDuPanier;

    // --- HISTORIQUE ---
    @FXML
    private TableView<Vente> tableHistoriqueVentes;
    @FXML
    private TableColumn<Vente, String> colHistVenteId;
    @FXML
    private TableColumn<Vente, String> colHistVenteDate;
    @FXML
    private TableColumn<Vente, String> colHistVenteAgent;
    @FXML
    private TableColumn<Vente, Integer> colHistVenteNbProd;
    @FXML
    private TableColumn<Vente, String> colHistVenteMode;
    @FXML
    private TableColumn<Vente, Double> colHistVenteTotal;
    @FXML
    private DatePicker dpHistoDebut;
    @FXML
    private DatePicker dpHistoFin;
    @FXML
    private Label lblTotalHistorique;
    @FXML
    private javafx.scene.control.TextField txtFiltreVenteTicketId;
    @FXML
    private ComboBox<com.pharmacie.models.User> cmbFiltreVenteAgent;
    @FXML
    private ComboBox<com.pharmacie.models.Vente.ModePaiement> cmbFiltreVenteMode;
    @FXML
    private Button btnReimprimerRecu;
    @FXML
    private Button btnVoirDetailsVente;

    // --- FILTRES CAISSE ---
    @FXML
    private ComboBox<com.pharmacie.models.Categorie> cmbFiltreCategorie;
    @FXML
    private ComboBox<com.pharmacie.models.Espece> cmbFiltreEspece;

    private ProduitDAO produitDAO = new ProduitDAO();
    private LotDAO lotDAO = new LotDAO();
    private com.pharmacie.dao.VenteDAO venteDAO = new com.pharmacie.dao.VenteDAO();
    private MouvementDAO mouvementDAO = new MouvementDAO();

    private boolean isUpdatingHistoriqueFiltres = false;
    private SessionCaisseDAO sessionDAO = new SessionCaisseDAO();
    private com.pharmacie.dao.CategorieDAO categorieDAO = new com.pharmacie.dao.CategorieDAO();
    private com.pharmacie.dao.EspeceDAO especeDAO = new com.pharmacie.dao.EspeceDAO();

    private final com.pharmacie.services.VenteService venteService = new com.pharmacie.services.VenteService();
    private final java.text.NumberFormat moneyFormat = java.text.NumberFormat.getInstance(new java.util.Locale("fr", "FR"));

    private ObservableList<LigneVente> panier = FXCollections.observableArrayList();
    private SessionCaisse currentSession;

    // Cache RAM pour la recherche ultra-rapide (Performance)
    private java.util.List<Produit> tousLesProduitsCache = new java.util.ArrayList<>();

    // Cache du stock par produit (reconstruit en 1 query) — élimine le N+1 sur la
    // table caisse
    private java.util.Map<Long, Integer> stockCache = new java.util.HashMap<>();

    // --- TICKETS EN ATTENTE ---
    // GÉRÉ DÉSORMAIS PAR SESSIONMANAGER POUR LA PERSISTANCE

    @FXML
    public void initialize() {
        panier.addListener((javafx.collections.ListChangeListener.Change<? extends LigneVente> c) -> {
            boolean isVide = panier.isEmpty();
            btnAnnulerVente.setDisable(isVide);
            if (btnValiderVente != null)
                btnValiderVente.setDisable(isVide);
            if (btnMettreEnAttente != null)
                btnMettreEnAttente.setDisable(isVide);

            if (isVide) {
                btnAnnulerVente.setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-padding: 8 15;");
                if (btnValiderVente != null)
                    btnValiderVente.setStyle(
                            "-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-font-size:13px; -fx-font-weight:bold; -fx-padding: 7 22; -fx-background-radius:5;");
            } else {
                btnAnnulerVente.setStyle(
                        "-fx-background-color: #3498DB; -fx-text-fill: white; -fx-padding: 8 15; -fx-font-weight: bold;"); // Bleu
                if (btnValiderVente != null)
                    btnValiderVente.setStyle(
                            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size:13px; -fx-font-weight:bold; -fx-padding: 7 22; -fx-background-radius:5;");
            }
        });

        // Initialisation de l'état des boutons au lancement
        btnAnnulerVente.setDisable(true);
        if (btnValiderVente != null)
            btnValiderVente.setDisable(true);
        if (btnMettreEnAttente != null)
            btnMettreEnAttente.setDisable(true);
            
        // Style dynamique pour btnRetirerDuPanier
        if (btnRetirerDuPanier != null) {
            btnRetirerDuPanier.disableProperty().bind(
                tablePanierVente.getSelectionModel().selectedItemProperty().isNull()
            );
        }

        // RESTAURATION DE L'ÉTAT GLOBAL (SI RETOUR DE NAVIGATION)
        Platform.runLater(() -> {
            mettreAJourBadgeTickets();
            if (SessionManager.isCaisseVerrouillee()) {
                verrouillerSession();
            }
        });

        // CHRONO: Purge automatique des tickets gelés > 2 heures (Point 8 & 9)
        javafx.animation.Timeline purgeChrono = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.minutes(1), e -> {
                boolean hadExpired = false;
                java.util.Iterator<TicketEnAttente> it = SessionManager.getFileAttente().iterator();
                while (it.hasNext()) {
                    TicketEnAttente t = it.next();
                    if (t.isExpired()) {
                        restaurerStockReserve(t.getReservedLines());
                        it.remove();
                        hadExpired = true;
                    }
                }
                if (hadExpired) {
                    mettreAJourBadgeTickets();
                }
            })
        );
        purgeChrono.setCycleCount(javafx.animation.Animation.INDEFINITE);
        purgeChrono.play();

        tablePanierVente.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                btnRetirerDuPanier.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-padding: 4 8; -fx-font-size: 12px; -fx-background-radius: 6;");
            } else {
                btnRetirerDuPanier.setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-padding: 4 8; -fx-font-size: 12px; -fx-background-radius: 6;");
            }
        });

        btnAnnulerVente.setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-padding: 8 15;");
        if (btnValiderVente != null)
            btnValiderVente.setStyle(
                    "-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-font-size:13px; -fx-font-weight:bold; -fx-padding: 7 22; -fx-background-radius:5;");

        cmbQuantite.getItems().addAll("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        cmbQuantite.getSelectionModel().selectFirst();

        cmbQuantite.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                cmbQuantite.getEditor().setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // Filtres TextFormatter pour n'accepter stricement que des chiffres (plus
        // propre que listener)
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> numFilter = change -> {
            String txt = change.getText();
            if (txt.matches("[0-9]*"))
                return change;
            return null;
        };
        cmbQuantite.getEditor().setTextFormatter(new javafx.scene.control.TextFormatter<>(numFilter));

        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> numFilterRecu = change -> {
            if (change.getText().matches("[0-9]*"))
                return change;
            return null;
        };
        txtMontantRecu.setTextFormatter(new javafx.scene.control.TextFormatter<>(numFilterRecu));

        txtMontantRecu.textProperty().addListener((obs, oldVal, newVal) -> {
            calculerMonnaie();
        });

        cmbUniteType.setItems(FXCollections.observableArrayList(LigneVente.TypeUnite.values()));
        cmbUniteType.setConverter(new javafx.util.StringConverter<LigneVente.TypeUnite>() {
            @Override
            public String toString(LigneVente.TypeUnite t) {
                if (t == null)
                    return "";
                return switch (t) {
                    case BOITE_ENTIERE -> "📦 Boîte entière";
                    case DETAIL -> "💊 Unité (détail)";
                };
            }

            @Override
            public LigneVente.TypeUnite fromString(String s) {
                return null;
            }
        });
        cmbUniteType.getSelectionModel().selectFirst();

        cmbModePaiement.setItems(FXCollections.observableArrayList(Vente.ModePaiement.values()));
        cmbModePaiement.setConverter(new javafx.util.StringConverter<Vente.ModePaiement>() {
            @Override
            public String toString(Vente.ModePaiement m) {
                if (m == null)
                    return "";
                return switch (m) {
                    case ESPECES -> "💵 Espèces";
                    case MOBILE_MONEY -> "📱 Mobile Money";
                    case MIXTE -> "💳 Mixte (Espèces + Mobile)";
                };
            }

            @Override
            public Vente.ModePaiement fromString(String s) {
                return null;
            }
        });
        cmbModePaiement.getSelectionModel().selectFirst();

        // UX: Mobile Money auto-fill and disable
        cmbModePaiement.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == Vente.ModePaiement.MOBILE_MONEY) {
                double total = panier.stream().mapToDouble(LigneVente::getSousTotal).sum();
                txtMontantRecu.setText(String.format(java.util.Locale.US, "%.0f", total));
                txtMontantRecu.setDisable(true);
                boxPaiementMixte.setVisible(false);
                boxPaiementMixte.setManaged(false);
                boxPaiementSimple.setVisible(true);
                boxPaiementSimple.setManaged(true);
            } else if (newV == Vente.ModePaiement.MIXTE) {
                boxPaiementSimple.setVisible(false);
                boxPaiementSimple.setManaged(false);
                boxPaiementMixte.setVisible(true);
                boxPaiementMixte.setManaged(true);
                txtMontantEspeces.clear();
                txtMontantMobile.clear();
            } else {
                txtMontantRecu.setDisable(false);
                txtMontantRecu.clear();
                boxPaiementMixte.setVisible(false);
                boxPaiementMixte.setManaged(false);
                boxPaiementSimple.setVisible(true);
                boxPaiementSimple.setManaged(true);
            }
            calculerMonnaie();
        });

        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> numFilterMixte = change -> {
            if (change.getText().matches("[0-9]*"))
                return change;
            return null;
        };
        txtMontantEspeces.setTextFormatter(new javafx.scene.control.TextFormatter<>(numFilterMixte));
        java.util.function.UnaryOperator<javafx.scene.control.TextFormatter.Change> numFilterMixte2 = change -> {
            if (change.getText().matches("[0-9]*"))
                return change;
            return null;
        };
        txtMontantMobile.setTextFormatter(new javafx.scene.control.TextFormatter<>(numFilterMixte2));

        txtMontantEspeces.textProperty().addListener((obs, oldVal, newVal) -> calculerMonnaie());
        txtMontantMobile.textProperty().addListener((obs, oldVal, newVal) -> calculerMonnaie());

        initStockColumns();
        initPanierColumns();
        initHistoriqueColumns();
        loadStockDispo();
        loadHistoriqueVentes();

        checkSessionStatus();

        // Charger les produits dans le filtre : REMPLACÉ PAR LA RECHERCHE TICKET ID (Point 3)
        // L'UI utilisera txtFiltreVenteTicketId au lieu de cmbFiltreVenteProduit.
        if (txtFiltreVenteTicketId != null) {
            txtFiltreVenteTicketId.textProperty().addListener((obs, oldV, newV) -> {
                if (!isUpdatingHistoriqueFiltres && !newV.equals(oldV)) filtrerHistorique();
            });
        }

        com.pharmacie.dao.UserDAO userFilterDAO = new com.pharmacie.dao.UserDAO();
        javafx.collections.ObservableList<com.pharmacie.models.User> agents = FXCollections.observableArrayList();
        agents.add(null);
        agents.addAll(userFilterDAO.findAll());
        cmbFiltreVenteAgent.setItems(agents);
        cmbFiltreVenteAgent.setConverter(new javafx.util.StringConverter<com.pharmacie.models.User>() {
            @Override
            public String toString(com.pharmacie.models.User u) { return u != null ? u.getNom() : "Tous les agents"; }
            @Override
            public com.pharmacie.models.User fromString(String s) { return null; }
        });
        com.pharmacie.utils.ComboBoxAutoComplete.setup(cmbFiltreVenteAgent);

        javafx.collections.ObservableList<com.pharmacie.models.Vente.ModePaiement> modes = FXCollections.observableArrayList();
        modes.add(null);
        modes.addAll(com.pharmacie.models.Vente.ModePaiement.values());
        cmbFiltreVenteMode.setItems(modes);
        cmbFiltreVenteMode.setConverter(new javafx.util.StringConverter<com.pharmacie.models.Vente.ModePaiement>() {
            @Override
            public String toString(com.pharmacie.models.Vente.ModePaiement m) { return m != null ? m.name() : "Tous moyens"; }
            @Override
            public com.pharmacie.models.Vente.ModePaiement fromString(String s) { return null; }
        });
        com.pharmacie.utils.ComboBoxAutoComplete.setup(cmbFiltreVenteMode);

        // Gérer les dates intelligemment
        com.pharmacie.utils.DateUtils.bindDateFilters(dpHistoDebut, dpHistoFin);

        // Auto-filtrage
        javafx.event.EventHandler<javafx.event.ActionEvent> autoFilterEvent = e -> {
            if (!isUpdatingHistoriqueFiltres) filtrerHistorique();
        };
        /* txtFiltreVenteTicketId a déjà un listener sur textProperty */
        cmbFiltreVenteAgent.setOnAction(autoFilterEvent);
        cmbFiltreVenteMode.setOnAction(autoFilterEvent);
        dpHistoDebut.setOnAction(autoFilterEvent);
        dpHistoFin.setOnAction(autoFilterEvent);

        // Charger les filtres Catégorie et Espèce
        cmbFiltreCategorie.getItems().add(null); // Option "Toutes"
        cmbFiltreCategorie.getItems().addAll(categorieDAO.findAll());
        cmbFiltreCategorie.setConverter(new javafx.util.StringConverter<com.pharmacie.models.Categorie>() {
            @Override
            public String toString(com.pharmacie.models.Categorie c) {
                return c != null ? c.getNom() : "Toutes les catégories";
            }

            @Override
            public com.pharmacie.models.Categorie fromString(String s) {
                return null;
            }
        });

        cmbFiltreEspece.getItems().add(null); // Option "Toutes"
        cmbFiltreEspece.getItems().addAll(especeDAO.findAll());
        cmbFiltreEspece.setConverter(new javafx.util.StringConverter<com.pharmacie.models.Espece>() {
            @Override
            public String toString(com.pharmacie.models.Espece e) {
                return e != null ? e.getNom() : "Toutes les espèces";
            }

            @Override
            public com.pharmacie.models.Espece fromString(String s) {
                return null;
            }
        });

        // Listeners temps réel sur les filtres
        cmbFiltreCategorie.valueProperty().addListener((obs, o, n) -> appliqueFiltres());
        cmbFiltreEspece.valueProperty().addListener((obs, o, n) -> appliqueFiltres());

        // Recherche auto live filter
        txtRechercheProduit.textProperty().addListener((obs, oldV, newV) -> {
            rechercherProduits();
        });

        // Écouteur pour activer/désactiver le type d'unité selon que le produit est
        // déconditionnable
        tableStock.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                if (newVal.getEstDeconditionnable() != null && newVal.getEstDeconditionnable()) {
                    cmbUniteType.setDisable(false);
                } else {
                    cmbUniteType.getSelectionModel().select(LigneVente.TypeUnite.BOITE_ENTIERE);
                    cmbUniteType.setDisable(true);
                }
            }
        });

        // --- Optimisations Caisse (Raccourcis & Clavier fixes via sceneProperty) ---
        txtRechercheProduit.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.F5) {
                        e.consume();
                        validerVente();
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        e.consume();
                        annulerVente();
                    } else if (e.getCode() == KeyCode.F1) {
                        e.consume();
                        txtRechercheProduit.requestFocus();
                    }
                });
            }
        });

        txtRechercheProduit.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (!tableStock.getItems().isEmpty()) {
                    tableStock.getSelectionModel().selectFirst();
                    tableStock.requestFocus();
                }
            }
        });

        tableStock.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ajouterAuPanier();
                txtRechercheProduit.requestFocus();
                txtRechercheProduit.selectAll();
                e.consume();
            }
        });

        // P2.A: Synchroniser les champs de mot de passe (caché et visible)
        txtPinVisible.textProperty().bindBidirectional(txtPinDeverrouillage.textProperty());

        Platform.runLater(() -> txtRechercheProduit.requestFocus());
    }

    private void checkSessionStatus() {
        if (SessionManager.getCurrentUser() != null) {
            lblCaisseUser.setText("Agent: " + SessionManager.getCurrentUser().getNom());
            java.util.Optional<SessionCaisse> optSession = sessionDAO
                    .findSessionOuverteByUser(SessionManager.getCurrentUser());
            if (optSession.isPresent()) {
                currentSession = optSession.get();
                setSessionOuverte(true);
            } else {
                currentSession = null;
                setSessionOuverte(false);
            }
        }
    }

    private void setSessionOuverte(boolean ouverte) {
        if (ouverte) {
            lblStatutSession.setText("Caisse: OUVERTE");
            lblStatutSession.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            btnOuvrirCaisse.setVisible(false);
            btnOuvrirCaisse.setManaged(false);
            btnCloturerCaisse.setVisible(true);
            btnCloturerCaisse.setManaged(true);
            btnVerrouiller.setVisible(true);
            btnVerrouiller.setManaged(true);
            btnTicketsEnAttente.setVisible(true);
            btnTicketsEnAttente.setManaged(true);
            overlayCaisseFermee.setVisible(false);
            overlaySessionVerrouillee.setVisible(false);
            overlaySessionVerrouillee.setManaged(false);
            boxVenteMain.setDisable(false);
            txtRechercheProduit.requestFocus();
        } else {
            lblStatutSession.setText("Caisse: FERMÉE");
            lblStatutSession.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E74C3C;");
            btnOuvrirCaisse.setVisible(true);
            btnOuvrirCaisse.setManaged(true);
            btnCloturerCaisse.setVisible(false);
            btnCloturerCaisse.setManaged(false);
            btnVerrouiller.setVisible(false);
            btnVerrouiller.setManaged(false);
            btnTicketsEnAttente.setVisible(false);
            btnTicketsEnAttente.setManaged(false);
            overlayCaisseFermee.setVisible(true);
            overlaySessionVerrouillee.setVisible(false);
            overlaySessionVerrouillee.setManaged(false);
            boxVenteMain.setDisable(true);
        }
    }

    private void nettoyerTicketsExpiresEtRestaurerStock() {
        java.util.Iterator<TicketEnAttente> it = SessionManager.getFileAttente().iterator();
        while (it.hasNext()) {
            TicketEnAttente t = it.next();
            if (t.isExpired()) {
                restaurerStockReserve(t.getReservedLines());
                it.remove();
            }
        }
    }

    private void restaurerStockReserve(java.util.List<LigneVente> reservedLines) {
        if (reservedLines == null) return;
        for (LigneVente lv : reservedLines) {
            if (lv.getLot() != null) {
                com.pharmacie.models.Lot l = lotDAO.findById(lv.getLot().getId());
                if (l != null) {
                    l.setQuantiteStock(l.getQuantiteStock() + lv.getQuantiteVendue());
                    if (l.getQuantiteStock() > 0) l.setEstArchive(false);
                    lotDAO.update(l);
                }
            }
        }
        loadStockDispo();
    }

    /** Met le panier actuel en attente et le vide pour un nouveau client */
    @FXML
    public void mettreEnAttente() {
        if (panier.isEmpty()) {
            showError("Le panier est vide. Rien à mettre en attente.");
            return;
        }

        nettoyerTicketsExpiresEtRestaurerStock();

        double total = panier.stream().mapToDouble(LigneVente::getSousTotal).sum();
        
        // Point 9 : Hard Hold - Réservation stricte du stock
        java.util.List<LigneVente> reservedLines = new java.util.ArrayList<>();
        try {
            for (LigneVente lv : panier) {
                int baseUnitsToDeduct = lv.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE && lv.getProduit().getEstDeconditionnable() != null && lv.getProduit().getEstDeconditionnable() 
                                        ? lv.getQuantiteVendue() * lv.getProduit().getUnitesParBoite() 
                                        : lv.getQuantiteVendue();
                
                java.util.List<com.pharmacie.models.Lot> lotsDispos = lotDAO.findAll().stream()
                    .filter(l -> l.getProduit().getId().equals(lv.getProduit().getId()) && l.getQuantiteStock() > 0)
                    .filter(l -> l.getDateExpiration() == null || !l.getDateExpiration().isBefore(java.time.LocalDate.now()))
                    .sorted((l1, l2) -> {
                        if (l1.getDateExpiration() == null) return 1;
                        if (l2.getDateExpiration() == null) return -1;
                        return l1.getDateExpiration().compareTo(l2.getDateExpiration());
                    }).toList();

                int remaining = baseUnitsToDeduct;
                for (com.pharmacie.models.Lot l : lotsDispos) {
                    if (remaining <= 0) break;
                    int taken = Math.min(l.getQuantiteStock(), remaining);
                    l.setQuantiteStock(l.getQuantiteStock() - taken);
                    if (l.getQuantiteStock() == 0) l.setEstArchive(true);
                    lotDAO.update(l);
                    
                    LigneVente reservedLv = new LigneVente();
                    reservedLv.setProduit(lv.getProduit());
                    reservedLv.setLot(l);
                    reservedLv.setQuantiteVendue(taken);
                    reservedLines.add(reservedLv);
                    
                    remaining -= taken;
                }
            }
            loadStockDispo();
        } catch (Exception e) {
            logger.error("Erreur lors de la réservation Hard Hold", e);
        }

        TicketEnAttente ticket = new TicketEnAttente(SessionManager.getNextTicketNumber(), panier, reservedLines, total);
        SessionManager.getFileAttente().add(ticket);
        panier.clear();
        calculerTotalVente();
        mettreAJourBadgeTickets();
        txtMontantRecu.clear();
        com.pharmacie.utils.ToastService.showInfo(boxVenteMain.getScene().getWindow(), "Ticket en Attente", "Le ticket a été mis de côté avec succès (Stock réservé pour 2h).");
        txtRechercheProduit.requestFocus();
    }

    /** Met à jour le texte du bouton avec le nombre de tickets en attente */
    private void mettreAJourBadgeTickets() {
        nettoyerTicketsExpiresEtRestaurerStock();
        int nb = SessionManager.getFileAttente().size();
        if (nb == 0) {
            btnTicketsEnAttente.setText("Tickets en Attente (0)");
            btnTicketsEnAttente.setStyle(
                    "-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        } else {
            btnTicketsEnAttente.setText("Tickets en Attente (" + nb + ") 🔴");
            btnTicketsEnAttente.setStyle(
                    "-fx-background-color: #E67E22; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-effect: dropshadow(gaussian, #e67e22, 10, 0.5, 0, 0);");
        }
    }

    /**
     * Affiche la liste des tickets en attente et permet de les rappeler ou supprimer.
     * Point 7 : Un seul Dialog (plus de double popup) — les actions Rappeler/Supprimer
     * sont directement dans le premier Dialog via un flag atomique.
     */
    @FXML
    public void voirTicketsEnAttente() {
        if (SessionManager.getFileAttente().isEmpty()) {
            com.pharmacie.utils.ToastService.showInfo(
                boxVenteMain.getScene().getWindow(),
                "File d'Attente",
                "Aucun ticket n'est en attente."
            );
            return;
        }

        // --- Construction du Dialog premium ---
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("File d'Attente");
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");

        javafx.scene.layout.VBox headerBox = new javafx.scene.layout.VBox(5);
        headerBox.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));
        Label headerTitle = new Label("Tickets Suspendus");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label headerSub = new Label(SessionManager.getFileAttente().size() + " ticket(s) en attente • Sélectionnez puis choisissez une action");
        headerSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        headerBox.getChildren().addAll(headerTitle, headerSub);

        // Boutons directs (pas de deuxième popup)
        ButtonType btnRappeler  = new ButtonType("Rappeler le Ticket",  ButtonBar.ButtonData.OK_DONE);
        ButtonType btnSupprimer = new ButtonType("Supprimer", ButtonBar.ButtonData.LEFT);
        ButtonType btnFermer    = new ButtonType("Fermer",       ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnRappeler, btnSupprimer, btnFermer);

        Platform.runLater(() -> {
            javafx.scene.Node nodeRap = dialog.getDialogPane().lookupButton(btnRappeler);
            if (nodeRap != null) {
                nodeRap.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-min-width: 140px;");
                nodeRap.setCursor(javafx.scene.Cursor.HAND);
            }
            javafx.scene.Node nodeSupp = dialog.getDialogPane().lookupButton(btnSupprimer);
            if (nodeSupp != null) {
                nodeSupp.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-border-color: #FCA5A5; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-padding: 7 15; -fx-min-width: 100px;");
                nodeSupp.setCursor(javafx.scene.Cursor.HAND);
            }
            javafx.scene.Node nodeFerm = dialog.getDialogPane().lookupButton(btnFermer);
            if (nodeFerm != null) {
                nodeFerm.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: bold; -fx-border-color: #CBD5E1; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-padding: 7 20; -fx-min-width: 100px;");
                nodeFerm.setCursor(javafx.scene.Cursor.HAND);
            }
        });

        // ListView stylisée premium
        javafx.scene.control.ListView<TicketEnAttente> listView = new javafx.scene.control.ListView<>();
        listView.getItems().addAll(SessionManager.getFileAttente());
        listView.getSelectionModel().selectFirst();
        listView.setPrefHeight(250);
        listView.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6;");

        listView.setCellFactory(lv -> new javafx.scene.control.ListCell<TicketEnAttente>() {
            @Override
            protected void updateItem(TicketEnAttente item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox();
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    box.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
                    
                    javafx.scene.layout.VBox leftBox = new javafx.scene.layout.VBox(4);
                    Label lblId = new Label("Ticket #" + item.getNumero());
                    lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");
                    Label lblDate = new Label(item.getHeure().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "  •  " + item.getLignes().size() + " articles");
                    lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
                    leftBox.getChildren().addAll(lblId, lblDate);
                    
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    
                    Label lblTotal = new Label(String.format("%,.0f FCFA", item.getTotal()));
                    lblTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: #059669; -fx-font-size: 15px;");
                    
                    box.getChildren().addAll(leftBox, spacer, lblTotal);
                    setGraphic(box);
                    setText(null);
                    
                    if (isSelected()) {
                        setStyle("-fx-background-color: #F1F5F9;");
                    } else {
                        setStyle("-fx-background-color: white;");
                    }
                }
            }
        });

        javafx.scene.layout.VBox mainContent = new javafx.scene.layout.VBox(15);
        mainContent.setPadding(new javafx.geometry.Insets(10));
        mainContent.getChildren().addAll(headerBox, listView);

        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().setMinWidth(480);

        // Flag pour savoir quel bouton a été cliqué (évite le double popup)
        final boolean[] doRappeler  = {false};
        final boolean[] doSupprimer = {false};

        dialog.getDialogPane().lookupButton(btnRappeler).addEventFilter(
            javafx.event.ActionEvent.ACTION, e -> doRappeler[0]  = true
        );
        dialog.getDialogPane().lookupButton(btnSupprimer).addEventFilter(
            javafx.event.ActionEvent.ACTION, e -> doSupprimer[0] = true
        );

        dialog.setResultConverter(btn -> null); // Void result — on utilise les flags

        dialog.showAndWait();

        TicketEnAttente selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (doRappeler[0]) {
            // Vérification panier non vide → confirmation légère (1 seul dialog)
            if (!panier.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING,
                    "Votre panier actuel sera écrasé ! Voulez-vous continuer ?",
                    ButtonType.OK, ButtonType.CANCEL);
                a.setHeaderText("⚠️ Panier actuel non vide");
                if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            }

            // On restaure le stock pour qu'il soit disponible lors de la validation finale
            restaurerStockReserve(selected.getReservedLines());

            panier.setAll(selected.getLignes());
            calculerTotalVente();
            SessionManager.getFileAttente().remove(selected);
            mettreAJourBadgeTickets();
            txtRechercheProduit.requestFocus();
            com.pharmacie.utils.ToastService.showSuccess(
                boxVenteMain.getScene().getWindow(),
                "Ticket Rappelé",
                "Le ticket a été restauré dans le panier (Stock débloqué)."
            );

        } else if (doSupprimer[0]) {
            // Suppression propre — restauration du stock
            restaurerStockReserve(selected.getReservedLines());
            SessionManager.getFileAttente().remove(selected);
            mettreAJourBadgeTickets();
            com.pharmacie.utils.ToastService.showInfo(
                boxVenteMain.getScene().getWindow(),
                "Ticket Supprimé",
                "Le ticket a été retiré de la file d'attente."
            );
        }
    }


    @FXML
    public void verrouillerSession() {
        if (SessionManager.getCurrentUser() == null)
            return;
        SessionManager.setCaisseVerrouillee(true);
        // Afficher l'overlay de verrouillage
        lblVerrouilleAgent.setText("Agent : " + SessionManager.getCurrentUser().getNom());
        lblVerrouilleErreur.setVisible(false);
        lblVerrouilleErreur.setManaged(false);
        txtPinDeverrouillage.clear();
        overlaySessionVerrouillee.setVisible(true);
        overlaySessionVerrouillee.setManaged(true);
        overlayCaisseFermee.setVisible(false);
        boxVenteMain.setDisable(true);
        // Masquer les boutons d'action caisse (branding pro)
        btnCloturerCaisse.setVisible(false);
        btnCloturerCaisse.setManaged(false);
        btnVerrouiller.setVisible(false);
        btnVerrouiller.setManaged(false);
        // Point 3 : Masquer aussi les Tickets en Attente (inutile quand la caisse est verrouillée)
        btnTicketsEnAttente.setVisible(false);
        btnTicketsEnAttente.setManaged(false);
        
        // P2.A: Réinitialisation propre à la fermeture
        if (txtPinVisible.isVisible()) {
            togglePasswordVisibility(); // Repasser en mode caché par défaut
        }
        
        Platform.runLater(() -> txtPinDeverrouillage.requestFocus());
    }

    @FXML
    public void togglePasswordVisibility() {
        if (txtPinVisible.isVisible()) {
            // Passer en mode caché
            txtPinVisible.setVisible(false);
            txtPinVisible.setManaged(false);
            txtPinDeverrouillage.setVisible(true);
            txtPinDeverrouillage.setManaged(true);
            btnTogglePin.setText("👁");
            txtPinDeverrouillage.requestFocus();
            txtPinDeverrouillage.positionCaret(txtPinDeverrouillage.getText().length());
        } else {
            // Passer en mode visible
            txtPinDeverrouillage.setVisible(false);
            txtPinDeverrouillage.setManaged(false);
            txtPinVisible.setVisible(true);
            txtPinVisible.setManaged(true);
            btnTogglePin.setText("👁‍🗨");
            txtPinVisible.requestFocus();
            txtPinVisible.positionCaret(txtPinVisible.getText().length());
        }
    }

    @FXML
    public void deverrouillerSession() {
        String motDePasse = txtPinDeverrouillage.getText();
        if (motDePasse == null || motDePasse.isEmpty()) {
            lblVerrouilleErreur.setText("Veuillez entrer votre mot de passe.");
            lblVerrouilleErreur.setVisible(true);
            lblVerrouilleErreur.setManaged(true);
            return;
        }
        // Vérification BCrypt
        String hashBD = SessionManager.getCurrentUser().getMotDePasseHash();
        if (org.mindrot.jbcrypt.BCrypt.checkpw(motDePasse, hashBD)) {
            // Mot de passe correct : on déverrouille proprement la session
            SessionManager.setCaisseVerrouillee(false);
            overlaySessionVerrouillee.setVisible(false);
            overlaySessionVerrouillee.setManaged(false);
            lblVerrouilleErreur.setVisible(false);
            lblVerrouilleErreur.setManaged(false);
            txtPinDeverrouillage.clear();
            boxVenteMain.setDisable(false);
            btnCloturerCaisse.setVisible(true);
            btnCloturerCaisse.setManaged(true);
            btnVerrouiller.setVisible(true);
            btnVerrouiller.setManaged(true);
            // Point 3 : Rétablir le bouton Tickets en Attente au déverrouillage
            btnTicketsEnAttente.setVisible(true);
            btnTicketsEnAttente.setManaged(true);
            lblStatutSession.setText("Caisse: OUVERTE");
            lblStatutSession.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            txtRechercheProduit.requestFocus();
        } else {
            // Mauvais mot de passe
            lblVerrouilleErreur.setText("⚠️ Mot de passe incorrect. Réessayez.");
            lblVerrouilleErreur.setVisible(true);
            lblVerrouilleErreur.setManaged(true);
            
            // P2.A: UX Haut de gamme (Ne PAS vider le champ, sélectionner + Shake animation)
            txtPinDeverrouillage.selectAll();
            if (txtPinVisible.isVisible()) {
                txtPinVisible.selectAll();
                com.pharmacie.utils.AnimationUtils.shake(txtPinVisible);
            } else {
                com.pharmacie.utils.AnimationUtils.shake(txtPinDeverrouillage);
            }
        }
    }

    @FXML
    public void ouvrirCaisse() {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Ouverture de Caisse");
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");

        ButtonType btnValider = new ButtonType("Ouvrir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValider, ButtonType.CANCEL);

        Platform.runLater(() -> {
            javafx.scene.Node vBtn = dialog.getDialogPane().lookupButton(btnValider);
            if (vBtn != null) {
                vBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 25; -fx-min-width: 120px; -fx-background-radius: 6;");
                vBtn.setCursor(javafx.scene.Cursor.HAND);
            }
            javafx.scene.Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelBtn != null) cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        });

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(14);
        vbox.setPadding(new javafx.geometry.Insets(24, 28, 16, 28));
        vbox.setMinWidth(420);

        Label lblTitle = new Label("Déclaration du Fond de Caisse");
        lblTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();

        javafx.scene.layout.VBox infoBox = new javafx.scene.layout.VBox(4);
        infoBox.setStyle("-fx-background-color: #F0FDF4; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #10B981; -fx-border-width: 1; -fx-border-radius: 8;");
        Label lblAgent = new Label("Agent : " + (SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getNom() : "—"));
        lblAgent.setStyle("-fx-font-size: 13px; -fx-text-fill: #059669; -fx-font-weight: bold;");
        Label lblHint = new Label("Comptez les billets du tiroir puis saisissez le montant total.");
        lblHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");
        infoBox.getChildren().addAll(lblAgent, lblHint);

        Label lblInst = new Label("Montant physique dans le tiroir (FCFA) :");
        lblInst.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        TextField txtFond = new TextField();
        txtFond.setPromptText("Ex: 5 000");
        txtFond.setPrefWidth(360);
        txtFond.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 10 14; -fx-background-radius: 6;");
        txtFond.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String txt = change.getText();
            return (txt.isEmpty() || txt.matches("[0-9]+")) ? change : null;
        }));

        vbox.getChildren().addAll(lblTitle, sep, infoBox, lblInst, txtFond);
        dialog.getDialogPane().setContent(vbox);
        Platform.runLater(() -> txtFond.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnValider) {
                try {
                    return Double.parseDouble(txtFond.getText().trim());
                } catch (Exception e) {
                    return -1.0;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(fond -> {
            SessionCaisse session = new SessionCaisse();
            session.setUser(SessionManager.getCurrentUser());
            session.setDateOuverture(LocalDateTime.now());
            session.setFondDeCaisse(fond);
            session.setStatut(SessionCaisse.StatutSession.OUVERTE);
            sessionDAO.save(session);
            checkSessionStatus();
            logger.info("Caisse ouverte avec un fond de {}", fond);
        });
    }

    @FXML
    public void cloturerCaisse() {
        if (currentSession == null)
            return;

        // Faille 2 : Sécurité Caisse - Bloquer si tickets en mémoire
        if (!SessionManager.getFileAttente().isEmpty()) {
            showError("IMPOSSIBLE DE CLÔTURER :\nVous avez " + SessionManager.getFileAttente().size()
                    + " ticket(s) en attente.\nVeuillez les valider ou les supprimer avant de clôturer la caisse.");
            return;
        }

        // Calcul rigoureux des encaissements de la session.
        // Règle d'or : pour les ventes MIXTES, on décompose via les champs
        // montantEspeces / montantMobile persistés en DB. Sans cela, l'argent mixte
        // disparaît des totaux de clôture et crée des écarts fictifs.
        List<Vente> ventesSession = venteDAO.findAll().stream()
                .filter(v -> v.getSessionCaisse() != null
                        && v.getSessionCaisse().getId().equals(currentSession.getId()))
                .collect(Collectors.toList());

        double totalEspeces = ventesSession.stream().mapToDouble(v -> {
            if (v.getModePaiement() == Vente.ModePaiement.ESPECES) {
                // Monopaiement Espèces : le montant net est le total de la vente
                return v.getTotal() != null ? v.getTotal() : 0.0;
            } else if (v.getModePaiement() == Vente.ModePaiement.MIXTE) {
                // Paiement MIXTE : on récupère la part espèces exacte (normalisée à l'encaissement)
                return v.getMontantEspeces() != null ? v.getMontantEspeces() : 0.0;
            }
            return 0.0; // MOBILE_MONEY ne rentre pas dans le tiroir physique
        }).sum();

        double totalMobile = ventesSession.stream().mapToDouble(v -> {
            if (v.getModePaiement() == Vente.ModePaiement.MOBILE_MONEY) {
                // Monopaiement Mobile : le montant net est le total de la vente
                return v.getTotal() != null ? v.getTotal() : 0.0;
            } else if (v.getModePaiement() == Vente.ModePaiement.MIXTE) {
                // Paiement MIXTE : on récupère la part mobile exacte (normalisée à l'encaissement)
                return v.getMontantMobile() != null ? v.getMontantMobile() : 0.0;
            }
            return 0.0;
        }).sum();

        double theorieEspeces = currentSession.getFondDeCaisse() + totalEspeces;

        class ClotureResult {
            Double especes;
            Double mobile;

            ClotureResult(Double e, Double m) {
                especes = e;
                mobile = m;
            }
        }

        Dialog<ClotureResult> dialog = new Dialog<>();
        dialog.setTitle("Clôture de Caisse (Z)");
        dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");

        ButtonType btnValider = new ButtonType("Clôturer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValider, ButtonType.CANCEL);
        Platform.runLater(() -> {
            javafx.scene.Node vBtn = dialog.getDialogPane().lookupButton(btnValider);
            if (vBtn != null) {
                vBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 25; -fx-min-width: 120px; -fx-background-radius: 6;");
                vBtn.setCursor(javafx.scene.Cursor.HAND);
            }
            javafx.scene.Node cancelBtn = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelBtn != null) cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        });

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(16);
        vbox.setPadding(new javafx.geometry.Insets(20));
        vbox.setMinWidth(480);

        // Point 8 : Sections visuelles differenciees Especes / Mobile
        double ventesPuresEsp = ventesSession.stream()
                .filter(v -> v.getModePaiement() == Vente.ModePaiement.ESPECES)
                .mapToDouble(Vente::getTotal).sum();
        double ventesPuresMob = ventesSession.stream()
                .filter(v -> v.getModePaiement() == Vente.ModePaiement.MOBILE_MONEY)
                .mapToDouble(Vente::getTotal).sum();
        // Variables MIXTES (réinjectées après refactoring)
        long nbMixtes = ventesSession.stream()
                .filter(v -> v.getModePaiement() == Vente.ModePaiement.MIXTE)
                .count();
        double totalMixtesEsp = ventesSession.stream()
                .filter(v -> v.getModePaiement() == Vente.ModePaiement.MIXTE)
                .mapToDouble(v -> v.getMontantEspeces() != null ? v.getMontantEspeces() : 0.0).sum();
        double totalMixtesMob = ventesSession.stream()
                .filter(v -> v.getModePaiement() == Vente.ModePaiement.MIXTE)
                .mapToDouble(v -> v.getMontantMobile() != null ? v.getMontantMobile() : 0.0).sum();

        // SECTION ESPECES
        javafx.scene.layout.VBox sectionEsp = new javafx.scene.layout.VBox(6);
        sectionEsp.setStyle("-fx-background-color: #FFFBEB; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #F59E0B; -fx-border-width: 1.5; -fx-border-radius: 8;");
        Label espTitle = new Label("Tiroir Caisse \u2014 Esp\u00e8ces");
        espTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #B45309;");
        javafx.scene.layout.HBox rowFond = buildRow("Fond de caisse initial :", String.format("%,.0f FCFA", currentSession.getFondDeCaisse()), "#334155", false);
        javafx.scene.layout.HBox rowEspPures = buildRow("Ventes esp\u00e8ces (pures) :", String.format("%,.0f FCFA", ventesPuresEsp), "#334155", false);
        javafx.scene.layout.HBox rowEspMixtes = buildRow(String.format("Part esp\u00e8ces (%d mixtes) :", nbMixtes), String.format("%,.0f FCFA", totalMixtesEsp), "#64748B", false);
        javafx.scene.layout.HBox rowEspAttendu = buildRow("ATTENDU DANS LE TIROIR :", String.format("%,.0f FCFA", theorieEspeces), "#3B82F6", true);
        sectionEsp.getChildren().addAll(espTitle, rowFond, rowEspPures, rowEspMixtes, new javafx.scene.control.Separator(), rowEspAttendu);

        // SECTION MOBILE
        javafx.scene.layout.VBox sectionMob = new javafx.scene.layout.VBox(6);
        sectionMob.setStyle("-fx-background-color: #EFF6FF; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3B82F6; -fx-border-width: 1.5; -fx-border-radius: 8;");
        Label mobTitle = new Label("Mobile Money \u2014 Digital");
        mobTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1D4ED8;");
        javafx.scene.layout.HBox rowMobPures = buildRow("Ventes mobile (pures) :", String.format("%,.0f FCFA", ventesPuresMob), "#334155", false);
        javafx.scene.layout.HBox rowMobMixtes = buildRow(String.format("Part mobile (%d mixtes) :", nbMixtes), String.format("%,.0f FCFA", totalMixtesMob), "#64748B", false);
        javafx.scene.layout.HBox rowMobTotal = buildRow("TOTAL MOBILE ENCAISS\u00c9 :", String.format("%,.0f FCFA", totalMobile), "#1D4ED8", true);
        Label mobAuto = new Label("Automatiquement valid\u00e9 \u2014 aucune saisie requise");
        mobAuto.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic; -fx-font-size: 11px;");
        sectionMob.getChildren().addAll(mobTitle, rowMobPures, rowMobMixtes, new javafx.scene.control.Separator(), rowMobTotal, mobAuto);

        // SECTION INPUT COMPTAGE PHYSIQUE
        javafx.scene.layout.VBox sectionInput = new javafx.scene.layout.VBox(8);
        sectionInput.setStyle("-fx-background-color: #F0F9FF; -fx-padding: 14; -fx-background-radius: 8; -fx-border-color: #3B82F6; -fx-border-width: 1.5; -fx-border-radius: 8;");
        Label inputTitle = new Label("Saisie du Comptage Physique");
        inputTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1D4ED8;");
        Label inputHint = new Label("Comptez les billets et pi\u00e8ces du tiroir, puis entrez le total :");
        inputHint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        TextField txtDeclareEspeces = new TextField();
        txtDeclareEspeces.setPromptText("Ex: 125 000");
        txtDeclareEspeces.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 6;");
        
        Label lblDynamicEcart = new Label("");
        lblDynamicEcart.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Point 4 : TextFormatter - chiffres uniquement
        txtDeclareEspeces.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String txt = change.getText();
            return (txt.isEmpty() || txt.matches("[0-9]+")) ? change : null;
        }));

        txtDeclareEspeces.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.isEmpty()) {
                lblDynamicEcart.setText("");
            } else {
                try {
                    double declare = Double.parseDouble(newV);
                    double ecart = declare - theorieEspeces;
                    String signe = ecart >= 0 ? "+" : "";
                    String color = ecart == 0 ? "#10B981" : (ecart > 0 ? "#3B82F6" : "#EF4444");
                    lblDynamicEcart.setText(String.format("Écart : %s%,.0f FCFA", signe, ecart));
                    lblDynamicEcart.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + color + ";");
                } catch (Exception e) {
                    lblDynamicEcart.setText("");
                }
            }
        });

        sectionInput.getChildren().addAll(inputTitle, inputHint, txtDeclareEspeces, lblDynamicEcart);

        vbox.getChildren().addAll(sectionEsp, sectionMob, sectionInput);

        dialog.getDialogPane().setContent(vbox);
        Platform.runLater(() -> txtDeclareEspeces.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnValider) {
                try {
                    double de = txtDeclareEspeces.getText().isEmpty() ? 0
                            : Double.parseDouble(txtDeclareEspeces.getText().trim());
                    // Le mobile est 100% digital, pas de déclaration manuelle
                    double dm = totalMobile;
                    return new ClotureResult(de, dm);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            double ecartEspeces = result.especes - theorieEspeces;
            double ecartMobile = 0.0; // Automatiquement juste

            // Populate Session
            currentSession.setDateCloture(LocalDateTime.now());
            currentSession.setTotalEspecesAttendu(theorieEspeces);
            currentSession.setEspecesDeclare(result.especes);
            currentSession.setEcartEspeces(ecartEspeces);

            currentSession.setTotalMobileAttendu(totalMobile);
            currentSession.setMobileDeclare(result.mobile);
            currentSession.setEcartMobile(ecartMobile);

            currentSession.setStatut(SessionCaisse.StatutSession.FERMEE);

            sessionDAO.update(currentSession);

            // BILAN PREMIUM post-cloture
            Dialog<ButtonType> bilanDialog = new Dialog<>();
            bilanDialog.setTitle("Bilan de Clôture — Journal Z");
            bilanDialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC;");
            javafx.scene.layout.VBox bilanContent = new javafx.scene.layout.VBox(14);
            bilanContent.setPadding(new javafx.geometry.Insets(24, 28, 16, 28));
            bilanContent.setMinWidth(460);

            javafx.scene.layout.HBox hdrBox = new javafx.scene.layout.HBox(10);
            hdrBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label bilanHdr = new Label("Caisse Fermée avec Succès");
            bilanHdr.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
            hdrBox.getChildren().add(bilanHdr);

            javafx.scene.control.Separator bilanSep = new javafx.scene.control.Separator();

            // BILAN ESPECES
            javafx.scene.layout.VBox bilanEsp = new javafx.scene.layout.VBox(6);
            bilanEsp.setStyle("-fx-background-color: #FFFBEB; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #F59E0B; -fx-border-width: 1.5; -fx-border-radius: 8;");
            Label bilanEspTitle = new Label("Tiroir Caisse — Bilan Espèces");
            bilanEspTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #B45309;");
            javafx.scene.layout.HBox bRowAtt = buildRow("Théorique attendu :", String.format("%,.0f FCFA", theorieEspeces), "#334155", false);
            javafx.scene.layout.HBox bRowDec = buildRow("Compté réellement :", String.format("%,.0f FCFA", result.especes), "#0F172A", true);
            double ecartEsp = result.especes - theorieEspeces;
            String ecartEspColor = ecartEsp < -0.5 ? "#EF4444" : ecartEsp > 0.5 ? "#10B981" : "#64748B";
            javafx.scene.layout.HBox bRowEcart = buildRow(String.format("Écart : %s", ecartEsp >= 0 ? "+" : ""), String.format("%,.0f FCFA", ecartEsp), ecartEspColor, true);
            bilanEsp.getChildren().addAll(bilanEspTitle, bRowAtt, bRowDec, new javafx.scene.control.Separator(), bRowEcart);

            // BILAN MOBILE
            javafx.scene.layout.VBox bilanMob = new javafx.scene.layout.VBox(6);
            bilanMob.setStyle("-fx-background-color: #EFF6FF; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #3B82F6; -fx-border-width: 1.5; -fx-border-radius: 8;");
            Label bilanMobTitle = new Label("Mobile Money — Bilan Digital");
            bilanMobTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4ED8;");
            javafx.scene.layout.HBox bRowMobAmt = buildRow("Total encaissé numériquement :", String.format("%,.0f FCFA", totalMobile), "#0F172A", true);
            javafx.scene.layout.HBox bRowMobOk = buildRow("Écart :", "0 FCFA — Traçabilité digitale garantie", "#10B981", false);
            bilanMob.getChildren().addAll(bilanMobTitle, bRowMobAmt, bRowMobOk);

            Label bilanFooter = new Label("Journal Z archivé dans Rapports > Historique des Clôtures.");
            bilanFooter.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-style: italic;");

            bilanContent.getChildren().addAll(hdrBox, bilanSep, bilanEsp, bilanMob, bilanFooter);
            bilanDialog.getDialogPane().setContent(bilanContent);
            bilanDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            Platform.runLater(() -> {
                javafx.scene.Node okBtn = bilanDialog.getDialogPane().lookupButton(ButtonType.OK);
                if (okBtn != null) {
                    okBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 22; -fx-background-radius: 6;");
                    okBtn.setCursor(javafx.scene.Cursor.HAND);
                }
            });
            bilanDialog.showAndWait();

            logger.info("Clôture Z : Espèces attendues={} FCFA | Mobile attendu={} FCFA | Écart caisse={} FCFA",
                    String.format("%.0f", theorieEspeces), String.format("%.0f", totalMobile),
                    String.format("%.0f", result.especes - theorieEspeces));

            checkSessionStatus();
        });
    }

    /**
     * Construit une ligne HBox avec label à gauche et valeur alignée à droite.
     * Respect du principe Dés IAtisation : montants toujours alignés à droite.
     */
    private javafx.scene.layout.HBox buildRow(String label, String value, String valueColor, boolean bold) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblText = new Label(label);
        lblText.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;" + (bold ? " -fx-font-weight: bold;" : ""));
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 12px; -fx-text-fill: " + valueColor + ";" + (bold ? " -fx-font-weight: bold;" : ""));
        row.getChildren().addAll(lblText, spacer, lblValue);
        return row;
    }

    private void initStockColumns() {
        colStkNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colStkPrix.setCellValueFactory(new PropertyValueFactory<>("prixVente"));
        colStkPrixDetail.setCellValueFactory(new PropertyValueFactory<>("prixVenteUnite"));
        colStkQte.setCellValueFactory(cell -> {
            int qteTotal = calculerQteTotaleProduit(cell.getValue().getId());
            Produit p = cell.getValue();
            if (p.getEstDeconditionnable() != null && p.getEstDeconditionnable() && p.getUnitesParBoite() != null
                    && p.getUnitesParBoite() > 0) {
                int boites = qteTotal / p.getUnitesParBoite();
                int unites = qteTotal % p.getUnitesParBoite();
                return new javafx.beans.property.SimpleStringProperty(boites + " Bte(s) et " + unites + " Unité(s)");
            } else {
                return new javafx.beans.property.SimpleStringProperty(qteTotal + " Unité(s)");
            }
        });

        tableStock.setRowFactory(tv -> new TableRow<Produit>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    getStyleClass().remove("stock-zero-row");
                } else {
                    int qty = calculerQteTotaleProduit(item.getId());
                    if (qty <= 0) {
                        if (!getStyleClass().contains("stock-zero-row")) {
                            getStyleClass().add("stock-zero-row");
                        }
                    } else {
                        getStyleClass().remove("stock-zero-row");
                    }
                }
            }
        });
    }

    private int calculerQteTotaleProduit(Long produitId) {
        // Utilise le cache reconstruit lors de loadStockDispo()
        // AVANT : lotDAO.findAll() appelé à chaque ligne (N+1 queries)
        // APRÈS : simple lookup O(1) dans un HashMap
        return stockCache.getOrDefault(produitId, 0);
    }

    private void initPanierColumns() {
        tablePanierVente.setEditable(true);
        tablePanierVente.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        colPanVenteProd.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduit().getNom()));

        colPanVenteQte.setCellValueFactory(new PropertyValueFactory<>("quantiteVendue"));
        // Point 4 : CellFactory avec TextFormatter pour n'accepter QUE des chiffres positifs
        colPanVenteQte.setCellFactory(col -> {
            TextFieldTableCell<LigneVente, Integer> cell = new TextFieldTableCell<>(new javafx.util.converter.IntegerStringConverter() {
                @Override public Integer fromString(String s) {
                    if (s == null || s.isBlank()) return 0;
                    try { return Math.max(0, Integer.parseInt(s.trim())); }
                    catch (NumberFormatException e) { return 0; }
                }
            }) {
                @Override
                public void startEdit() {
                    super.startEdit();
                    // Appliquer le TextFormatter APRES que JavaFX a cree le TextField
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
        colPanVenteQte.setOnEditCommit(e -> {
            LigneVente ligne = e.getRowValue();
            int newQte = e.getNewValue();
            if (newQte > 0) {
                Produit p = ligne.getProduit();
                int baseReq = ligne.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE
                        && p.getEstDeconditionnable() != null && p.getEstDeconditionnable()
                                ? newQte * p.getUnitesParBoite()
                                : newQte;
                int maxDispo = calculerQteTotaleProduit(p.getId());

                if (baseReq > maxDispo) {
                    showError("QUANTITÉ REFUSÉE :\nLe stock physique maximum est de " + maxDispo + " unités pour "
                            + p.getNom() + ".\nL'ajustement manuel a été annulé.");
                    tablePanierVente.refresh();
                    return;
                }
                ligne.setQuantiteVendue(newQte);
                ligne.setSousTotal(ligne.getPrixUnitaire() * newQte);
            }
            tablePanierVente.refresh();
            calculerTotalVente();
        });

        colPanVenteType.setCellValueFactory(new PropertyValueFactory<>("typeUnite"));
        colPanVenteType.setCellFactory(javafx.scene.control.cell.ComboBoxTableCell.forTableColumn(
                new javafx.util.StringConverter<LigneVente.TypeUnite>() {
                    @Override
                    public String toString(LigneVente.TypeUnite t) {
                        if (t == null)
                            return "";
                        return switch (t) {
                            case BOITE_ENTIERE -> "📦 Boîte entière";
                            case DETAIL -> "💊 Unité (détail)";
                        };
                    }

                    @Override
                    public LigneVente.TypeUnite fromString(String s) {
                        return null;
                    }
                },
                LigneVente.TypeUnite.values()));
        colPanVenteType.setOnEditCommit(e -> {
            LigneVente ligne = e.getRowValue();
            LigneVente.TypeUnite newType = e.getNewValue();
            Produit p = ligne.getProduit();

            if (newType == LigneVente.TypeUnite.DETAIL
                    && (p.getEstDeconditionnable() == null || !p.getEstDeconditionnable())) {
                showError("MODIFICATION REFUSÉE :\nCe produit ne peut pas être vendu au détail.");
                tablePanierVente.refresh();
                return;
            }

            int baseReq = newType == LigneVente.TypeUnite.BOITE_ENTIERE && p.getEstDeconditionnable() != null
                    && p.getEstDeconditionnable()
                            ? ligne.getQuantiteVendue() * p.getUnitesParBoite()
                            : ligne.getQuantiteVendue();
            int maxDispo = calculerQteTotaleProduit(p.getId());

            if (baseReq > maxDispo) {
                showError("STOCK INSUFFISANT :\nLe stock physique maximum est de " + maxDispo + " unités pour "
                        + p.getNom() + ".\nL'ajustement a été annulé.");
                tablePanierVente.refresh();
                return;
            }

            ligne.setTypeUnite(newType);
            double prixU = newType == LigneVente.TypeUnite.BOITE_ENTIERE
                    ? (p.getPrixVente() != null ? p.getPrixVente() : 0.0)
                    : (p.getPrixVenteUnite() != null ? p.getPrixVenteUnite() : 0.0);
            ligne.setPrixUnitaire(prixU);
            ligne.setSousTotal(prixU * ligne.getQuantiteVendue());
            tablePanierVente.refresh();
            calculerTotalVente();
        });
        colPanVenteTotal.setCellValueFactory(new PropertyValueFactory<>("sousTotal"));
        tablePanierVente.setItems(panier);
    }

    private void loadStockDispo() {
        tousLesProduitsCache = produitDAO.findAll();
        rebuildStockCache(); // 1 seule requête pour tout le stock
        
        // Point 7 (Phase 2): Tri intelligent (Disponibilité > 0 d'abord, puis Quantité décroissante, puis nom alphabétique)
        tousLesProduitsCache.sort((p1, p2) -> {
            int q1 = stockCache.getOrDefault(p1.getId(), 0);
            int q2 = stockCache.getOrDefault(p2.getId(), 0);
            boolean hasStock1 = q1 > 0;
            boolean hasStock2 = q2 > 0;
            
            if (hasStock1 && !hasStock2) return -1;
            if (!hasStock1 && hasStock2) return 1;
            if (q1 != q2) return Integer.compare(q2, q1); // décroissant
            return p1.getNom().compareToIgnoreCase(p2.getNom());
        });
        
        tableStock.setItems(FXCollections.observableArrayList(tousLesProduitsCache));
        tableStock.refresh(); // Force le re-dessin immédiat des cellules (colStkQte lit stockCache)
    }

    /**
     * Reconstruit le cache stock en une seule requête Hibernate.
     * Élimine le problème N+1 : sans ce cache, JavaFX appelait
     * lotDAO.findAll() pour CHAQUE ligne visible dans le tableau.
     */
    private void rebuildStockCache() {
        stockCache.clear();
        java.time.LocalDate today = java.time.LocalDate.now();
        lotDAO.findAll().stream()
                .filter(l -> l.getDateExpiration() == null || !l.getDateExpiration().isBefore(today))
                .forEach(l -> stockCache.merge(l.getProduit().getId(), l.getQuantiteStock(), Integer::sum));
    }

    /** Filtre combiné Texte + Catégorie + Espèce — en temps réel depuis RAM */
    private void appliqueFiltres() {
        String q = txtRechercheProduit.getText() != null ? txtRechercheProduit.getText().toLowerCase().trim() : "";
        com.pharmacie.models.Categorie catFiltre = cmbFiltreCategorie.getValue();
        com.pharmacie.models.Espece espFiltre = cmbFiltreEspece.getValue();

        List<Produit> filtres = tousLesProduitsCache.stream()
                .filter(p -> q.isEmpty() || p.getNom().toLowerCase().contains(q))
                .filter(p -> catFiltre == null
                        || (p.getCategorie() != null && p.getCategorie().getId().equals(catFiltre.getId())))
                .filter(p -> espFiltre == null
                        || (p.getEspece() != null && p.getEspece().getId().equals(espFiltre.getId())))
                .collect(Collectors.toList());
        tableStock.setItems(FXCollections.observableArrayList(filtres));
    }

    @FXML
    public void rechercherProduits() {
        appliqueFiltres();
    }

    @FXML
    public void reinitialiserFiltres() {
        txtRechercheProduit.clear();
        cmbFiltreCategorie.getSelectionModel().clearSelection();
        cmbFiltreEspece.getSelectionModel().clearSelection();
        loadStockDispo();
        txtRechercheProduit.requestFocus();
    }

    @FXML
    public void ajouterAuPanier() {
        Produit p = tableStock.getSelectionModel().getSelectedItem();
        if (p == null) {
            showErrorEffect(tableStock);
            showError("Veuillez sélectionner un produit.");
            return;
        }

        int qte = 1;
        try {
            if (!cmbQuantite.getEditor().getText().isEmpty()) {
                qte = Integer.parseInt(cmbQuantite.getEditor().getText());
            }
        } catch (NumberFormatException e) {
            showErrorEffect(cmbQuantite);
            showError("Quantité invalide.");
            return;
        }

        if (qte <= 0) {
            showErrorEffect(cmbQuantite);
            showError("La quantité doit être supérieure à 0.");
            return;
        }

        LigneVente.TypeUnite type = cmbUniteType.getValue();

        // Calcul de la quantité requise en unités de base (stocks)
        int requiredBaseUnits = type == LigneVente.TypeUnite.BOITE_ENTIERE && p.getEstDeconditionnable() != null
                && p.getEstDeconditionnable()
                        ? qte * p.getUnitesParBoite()
                        : qte;

        int stockMaxDispo = calculerQteTotaleProduit(p.getId());

        // Déduire la quantité déjà bloquée dans le panier
        int dejaAuPanier = panier.stream()
                .filter(l -> l.getProduit().getId().equals(p.getId()))
                .mapToInt(l -> l.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE
                        && p.getEstDeconditionnable() != null && p.getEstDeconditionnable()
                                ? l.getQuantiteVendue() * p.getUnitesParBoite()
                                : l.getQuantiteVendue())
                .sum();

        if (requiredBaseUnits + dejaAuPanier > stockMaxDispo) {
            showError("Stock insuffisant ! (Reste: " + (stockMaxDispo - dejaAuPanier) + " unités max)");
            return;
        }

        // Null pointer safety for units
        double safePrixVente = p.getPrixVente() != null ? p.getPrixVente() : 0.0;
        double safePrixVenteUnite = p.getPrixVenteUnite() != null ? p.getPrixVenteUnite() : 0.0;

        double prixU = type == LigneVente.TypeUnite.BOITE_ENTIERE ? safePrixVente : safePrixVenteUnite;
        double sousT = prixU * qte;

        // Fix Doublons: Si le produit + type d'unité existent déjà, on incrémente juste
        // la quantité
        boolean found = false;
        for (LigneVente lv : panier) {
            if (lv.getProduit().getId().equals(p.getId()) && lv.getTypeUnite() == type) {
                lv.setQuantiteVendue(lv.getQuantiteVendue() + qte);
                lv.setSousTotal(lv.getPrixUnitaire() * lv.getQuantiteVendue());
                found = true;
                break;
            }
        }

        if (!found) {
            LigneVente ligne = new LigneVente();
            ligne.setProduit(p);
            ligne.setQuantiteVendue(qte);
            ligne.setTypeUnite(type);
            ligne.setPrixUnitaire(prixU);
            ligne.setSousTotal(sousT);
            panier.add(ligne);
        }

        tablePanierVente.refresh();
        calculerTotalVente();

        // Faille 4 : reset après action pour prêt à scanner à nouveau
        cmbQuantite.getEditor().setText("1");
        txtRechercheProduit.clear();
        txtRechercheProduit.requestFocus();
    }

    @FXML
    public void retirerDuPanier() {
        LigneVente selected = tablePanierVente.getSelectionModel().getSelectedItem();
        if (selected != null) {
            panier.remove(selected);
            calculerTotalVente();
        }
    }

    private void calculerTotalVente() {
        double total = panier.stream().mapToDouble(LigneVente::getSousTotal).sum();
        lblTotalVente.setText(moneyFormat.format(total) + " FCFA");

        // Flash Vert pour le retour visuel
        String targetColor = (total > 0) ? "#27ae60" : "#18BC9C";
        lblTotalVente.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + targetColor + "; -fx-scale-x: 1.2; -fx-scale-y: 1.2;");
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(0.3));
        pt.setOnFinished(e -> lblTotalVente.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #18BC9C; -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));
        pt.play();

        // Auto-fill update if mobile money
        if (cmbModePaiement.getValue() == Vente.ModePaiement.MOBILE_MONEY) {
            txtMontantRecu.setText(String.format(java.util.Locale.US, "%.0f", total));
        }
        calculerMonnaie();
    }

    private void calculerMonnaie() {
        if (lblMonnaieRendre == null)
            return;
        try {
            double total = panier.stream().mapToDouble(LigneVente::getSousTotal).sum();
            double recu = 0;
            if (cmbModePaiement.getValue() == Vente.ModePaiement.MIXTE) {
                double esp = txtMontantEspeces.getText().isEmpty() ? 0
                        : Double.parseDouble(txtMontantEspeces.getText());
                double mob = txtMontantMobile.getText().isEmpty() ? 0 : Double.parseDouble(txtMontantMobile.getText());
                recu = esp + mob;
            } else {
                String resStr = txtMontantRecu.getText();
                recu = resStr.isEmpty() ? 0 : Double.parseDouble(resStr);
            }
            
            if (recu > total) {
                double monnaie = recu - total;
                // Vert qaund on doit rendre la monnaie
                lblMonnaieRendre.setText(moneyFormat.format(monnaie) + " FCFA");
                lblMonnaieRendre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#27ae60;");
            } else if (recu == total) {
                lblMonnaieRendre.setText("0 FCFA");
                lblMonnaieRendre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#BDC3C7;");
            } else {
                double monnaie = recu - total;
                // Rouge (négatif) qaund il manque de l'argent
                lblMonnaieRendre.setText(moneyFormat.format(monnaie) + " FCFA");
                lblMonnaieRendre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#E74C3C;");
            }
        } catch (Exception e) {
            lblMonnaieRendre.setText("0 FCFA");
            lblMonnaieRendre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#BDC3C7;");
        }
    }

    @FXML
    public void annulerVente() {
        panier.clear();
        calculerTotalVente();
        // Reset
        cmbQuantite.getEditor().setText("1");
        txtMontantRecu.clear();
        cmbModePaiement.getSelectionModel().selectFirst();
        txtRechercheProduit.clear(); // optionnel mais clean
        txtRechercheProduit.requestFocus();
    }

    @FXML
    public void validerVente() {
        if (panier.isEmpty()) {
            showError("Le panier est vide.");
            return;
        }

        double totalAPayer = panier.stream().mapToDouble(LigneVente::getSousTotal).sum();

        Double mEspeces = null;
        Double mMobile = null;
        double recu = 0;
        try {
            if (cmbModePaiement.getValue() == Vente.ModePaiement.MIXTE) {
                mEspeces = txtMontantEspeces.getText().isEmpty() ? 0.0
                        : Double.parseDouble(txtMontantEspeces.getText());
                mMobile = txtMontantMobile.getText().isEmpty() ? 0.0 : Double.parseDouble(txtMontantMobile.getText());
                recu = mEspeces + mMobile;
            } else {
                String resStr = txtMontantRecu.getText();
                recu = resStr.isEmpty() ? 0.0 : Double.parseDouble(resStr);
                if (cmbModePaiement.getValue() == Vente.ModePaiement.ESPECES)
                    mEspeces = recu;
                else if (cmbModePaiement.getValue() == Vente.ModePaiement.MOBILE_MONEY)
                    mMobile = recu;
            }

            if (recu < totalAPayer) {
                showError("Le montant reçu (" + String.format("%.0f", recu) + " FCFA) est insuffisant. Il manque "
                        + String.format("%.0f", (totalAPayer - recu)) + " FCFA.");
                return;
            }

            // ─────────────────────────────────────────────────────────────────────
            // NORMALISATION COMPTABLE (Règle d'or : montants persistés = montants NETS)
            // Les champs montantEspeces et montantMobile en DB doivent refléter
            // exactement ce qui entre dans le tiroir / dans le portefeuille mobile,
            // sans la monnaie rendue. Cette normalisation est la clé de voûte de
            // la fiabilité des rapports et de la clôture Z.
            // ─────────────────────────────────────────────────────────────────────
            if (cmbModePaiement.getValue() == Vente.ModePaiement.ESPECES) {
                // Monopaiement Espèces : on persist le montant NET = total dû (pas le brut reçu)
                mEspeces = totalAPayer;
                mMobile  = 0.0;
            } else if (cmbModePaiement.getValue() == Vente.ModePaiement.MOBILE_MONEY) {
                // Monopaiement Mobile : exact, car le virement est pour le montant exact
                mEspeces = 0.0;
                mMobile  = totalAPayer;
            } else if (cmbModePaiement.getValue() == Vente.ModePaiement.MIXTE) {
                // Paiement MIXTE : la monnaie est TOUJOURS rendue depuis la part Espèces
                // (on ne rembourse pas de Mobile Money). Règle physique universelle.
                double overpay = recu - totalAPayer;
                if (overpay > 0) {
                    // On déduit le surplus uniquement du cash
                    double cashNet = mEspeces - overpay;
                    if (cashNet < 0) {
                        // Cas pathologique : le surplus dépasse la mise de fonds en cash.
                        // Impossible physiquement si la validation précédente a passé.
                        // On le bloque par sécurité.
                        showError("Incohérence de paiement MIXTE : le surplus (" +
                                String.format("%.0f", overpay) + " FCFA) dépasse la part en espèces (" +
                                String.format("%.0f", mEspeces) + " FCFA). Veuillez corriger les montants.");
                        return;
                    }
                    mEspeces = cashNet;
                    // mMobile reste intouché (le virement mobile est de montant exact)
                }
                // Assertion finale : mEspeces + mMobile doit être = totalAPayer
                // En cas d'imprecision flottante, on recadre mEspeces
                mEspeces = totalAPayer - mMobile;
                if (mEspeces < 0) mEspeces = 0.0;
            }

        } catch (NumberFormatException e) {
            showError("Le montant reçu est invalide.");
            return;
        }

        try {
            double monnaieR = Math.max(0, recu - totalAPayer);
            // DELEGATION ARCHITECTURALE : Appel du Service Vente
            Vente vente = venteService.validerVente(new ArrayList<>(panier), cmbModePaiement.getValue(), mEspeces,
                    mMobile, recu, monnaieR, currentSession, stockCache);

            if (chkImprimerRecu.isSelected()) {
                imprimerRecu(vente);
            }

            panier.clear();
            calculerTotalVente();

            // Remise à Zéro Totale de la Caisse
            cmbQuantite.getEditor().setText("1");
            txtMontantRecu.clear();
            txtRechercheProduit.clear();
            cmbModePaiement.getSelectionModel().selectFirst();

            loadStockDispo();
            tableStock.refresh(); // Force column color update
            loadHistoriqueVentes(); // Mise à jour de l'onglet Historique
            com.pharmacie.utils.ToastService.showSuccess(boxVenteMain.getScene().getWindow(), "Vente Validée", "Le ticket a été encaissé et enregistré avec succès !");
            logger.info("Vente (ID: {}) validée et enregistrée via VenteService !", vente.getId());

            txtRechercheProduit.requestFocus();

        } catch (Exception e) {
            // Log complet pour traçabilité technique dans les fichiers logs
            logger.error("Échec de la validation de la vente (mode: {}). Aucune donnée n'a été persistée.",
                    cmbModePaiement.getValue(), e);
            // Affichage d'un message explicite à l'utilisateur
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() + " (voir logs pour détails)";
            showError("Erreur lors de l'enregistrement de la vente :\n" + msg);
        }
    }

    private void imprimerRecu(Vente vente) {
        com.pharmacie.utils.PrinterService.imprimerTicket(vente);
    }

    private void initHistoriqueColumns() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colHistVenteId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNumeroTicketOfficiel()));
        colHistVenteDate.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDateVente() != null ? cell.getValue().getDateVente().format(fmt) : ""));
        colHistVenteAgent.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getUser() != null ? cell.getValue().getUser().getNom() : "---"));
        colHistVenteNbProd.setCellValueFactory(cell -> {
            int nb = cell.getValue().getLignesVente() != null ? cell.getValue().getLignesVente().size() : 0;
            return new javafx.beans.property.SimpleObjectProperty<>(nb);
        });
        colHistVenteMode.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getModePaiement() != null ? cell.getValue().getModePaiement().name() : ""));
        colHistVenteTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colHistVenteTotal.setCellFactory(column -> new javafx.scene.control.TableCell<Vente, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(moneyFormat.format(item) + " FCFA");
                }
            }
        });

        // Activer/désactiver les boutons selon la sélection
        tableHistoriqueVentes.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    btnReimprimerRecu.setDisable(selected == null);
                    if (btnVoirDetailsVente != null)
                        btnVoirDetailsVente.setDisable(selected == null);
                });

        // Écouteur de Double-Clic
        tableHistoriqueVentes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableHistoriqueVentes.getSelectionModel().getSelectedItem() != null) {
                showSelectedVenteDetail();
            }
        });
    }

    // ─── Helpers ticket ──────────────────────────────────────────────────────────
    private static final int TICKET_LINE_WIDTH = 38;

    private javafx.scene.shape.Polygon createZigZagEdge(double width, boolean isTop) {
        javafx.scene.shape.Polygon zigzag = new javafx.scene.shape.Polygon();
        double tw = 10, th = 6;
        if (isTop) {
            zigzag.getPoints().addAll(0.0, th);
            for (double x = 0; x < width; x += tw) {
                zigzag.getPoints().addAll(x + tw / 2, 0.0);
                zigzag.getPoints().addAll(Math.min(x + tw, width), th);
            }
            zigzag.getPoints().addAll(width, th);
        } else {
            zigzag.getPoints().addAll(0.0, 0.0);
            for (double x = 0; x < width; x += tw) {
                zigzag.getPoints().addAll(x + tw / 2, th);
                zigzag.getPoints().addAll(Math.min(x + tw, width), 0.0);
            }
            zigzag.getPoints().addAll(width, 0.0);
        }
        zigzag.setFill(javafx.scene.paint.Color.WHITE);
        return zigzag;
    }

    private Label makeMonoRow(String key, String value, String style) {
        String keyPart = key + " : ";
        int spaces = Math.max(1, TICKET_LINE_WIDTH - keyPart.length() - value.length());
        StringBuilder sb = new StringBuilder(keyPart);
        for (int i = 0; i < spaces; i++) sb.append('\u00A0'); // espace insécable monospace
        sb.append(value);
        Label lbl = new Label(sb.toString());
        lbl.setStyle(style);
        return lbl;
    }
    // ─────────────────────────────────────────────────────────────────────────────

    @FXML
    public void showSelectedVenteDetail() {
        Vente selecVente = tableHistoriqueVentes.getSelectionModel().getSelectedItem();
        if (selecVente == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Aperçu du Ticket Premium");
        dialog.setHeaderText(null);

        // Infos Pharmacie
        com.pharmacie.dao.PharmacieInfoDAO infoDAO = new com.pharmacie.dao.PharmacieInfoDAO();
        com.pharmacie.models.PharmacieInfo info = infoDAO.getInfo();
        String nomPharma = info != null && info.getNom() != null ? info.getNom() : "PHARMACIE VÉTÉRINAIRE";
        String telPharma = info != null && info.getTelephone() != null ? "Tél: " + info.getTelephone() : "";
        String addrPharma = info != null && info.getAdresse() != null ? info.getAdresse() : "";
        String msgPharma = info != null && info.getMessageTicket() != null ? info.getMessageTicket().trim() : "Merci de votre confiance !";

        // Styles Monospace — Noir pur (simulation imprimante thermique)
        String monoNormal = "-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px; -fx-text-fill: #000000;";
        String monoBold   = "-fx-font-family: 'Courier New', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;";

        // ── Ticket Body ───────────────────────────────────────────────────────────
        javafx.scene.layout.VBox ticketBox = new javafx.scene.layout.VBox(4);
        ticketBox.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 12 28 12 28;");
        ticketBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // En-tête centré
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(2);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        Label lblNom = new Label(nomPharma.toUpperCase());
        lblNom.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        header.getChildren().add(lblNom);
        if (!addrPharma.isEmpty()) {
            Label l = new Label(addrPharma); l.setStyle(monoNormal); header.getChildren().add(l);
        }
        if (!telPharma.isEmpty()) {
            Label l = new Label(telPharma); l.setStyle(monoNormal); header.getChildren().add(l);
        }
        ticketBox.getChildren().addAll(header, new Label(""));

        // Séparateur
        Label sep1 = new Label("- - - - - - - - - - - - - - - - - - - -");
        sep1.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #000000;");
        sep1.setAlignment(javafx.geometry.Pos.CENTER); sep1.setMaxWidth(Double.MAX_VALUE);
        ticketBox.getChildren().add(sep1);

        // Infos ticket
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr  = selecVente.getDateVente() != null ? selecVente.getDateVente().format(fmt) : "";
        String agent    = selecVente.getUser() != null ? selecVente.getUser().getNom() : "Inconnu";
        String customRef = selecVente.getDateVente() != null
                ? selecVente.getDateVente().format(DateTimeFormatter.ofPattern("ddMMyy-HHmm")) + "-" + String.format("%03d", selecVente.getId())
                : String.valueOf(selecVente.getId());

        ticketBox.getChildren().addAll(
            makeMonoRow("Date",       dateStr,   monoNormal),
            makeMonoRow("Caissier",   agent,     monoNormal),
            makeMonoRow("Ticket N°",  customRef, monoNormal)
        );

        // Séparateur articles
        Label sep2 = new Label("----------------------------------------");
        sep2.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: #000000;");
        ticketBox.getChildren().addAll(new Label(""), sep2);

        // Lignes de produits
        for (LigneVente lv : selecVente.getLignesVente()) {
            String nom = lv.getProduit().getNom();
            if (nom.length() > 38) nom = nom.substring(0, 38);
            Label lblProd = new Label(nom); lblProd.setStyle(monoBold);

            String leftPart  = "  " + lv.getQuantiteVendue() + " x " + String.format(java.util.Locale.FRANCE, "%.0f", lv.getPrixUnitaire()) + " F ";
            String rightPart = String.format(java.util.Locale.FRANCE, "%.0f FCFA", lv.getSousTotal());
            int dots = Math.max(1, TICKET_LINE_WIDTH - leftPart.length() - rightPart.length());
            StringBuilder lineStr = new StringBuilder(leftPart);
            for (int i = 0; i < dots; i++) lineStr.append('.');
            lineStr.append(rightPart);

            Label lblLine = new Label(lineStr.toString()); lblLine.setStyle(monoNormal);
            ticketBox.getChildren().addAll(lblProd, lblLine);
        }

        // Séparateur total
        Label sep3 = new Label("----------------------------------------");
        sep3.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-text-fill: #000000;");
        ticketBox.getChildren().addAll(sep3, new Label(""));

        // ── TOTAL \u2014 Noir pur, style imprimante thermique (bordure simple)
        javafx.scene.layout.StackPane totalPane = new javafx.scene.layout.StackPane();
        totalPane.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #000000; -fx-border-width: 1.5; -fx-padding: 6;");
        Label lTotal = new Label("*** TOTAL : " + String.format(java.util.Locale.FRANCE, "%,.0f FCFA", selecVente.getTotal()) + " ***");
        lTotal.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        totalPane.getChildren().add(lTotal);
        ticketBox.getChildren().add(totalPane);

        // ── Pied de ticket aligné ──────────────────────────────────────────────
        ticketBox.getChildren().add(new Label(""));
        String mode = selecVente.getModePaiement() != null ? selecVente.getModePaiement().name().replace("_", " ") : "INCONNU";
        ticketBox.getChildren().add(makeMonoRow("Payé par", mode, monoNormal));

        if (selecVente.getModePaiement() == Vente.ModePaiement.MIXTE || selecVente.getModePaiement() == Vente.ModePaiement.ESPECES) {
            double rec = selecVente.getMontantRecu()    != null ? selecVente.getMontantRecu()    : selecVente.getTotal();
            double mon = selecVente.getMonnaieRendue()  != null ? selecVente.getMonnaieRendue()  : 0.0;
            ticketBox.getChildren().add(makeMonoRow("Montant Reçu", String.format(java.util.Locale.FRANCE, "%,.0f FCFA", rec), monoNormal));
            ticketBox.getChildren().add(makeMonoRow("Monnaie",      String.format(java.util.Locale.FRANCE, "%,.0f FCFA", mon), monoNormal));
        }
        if (selecVente.getModePaiement() == Vente.ModePaiement.MIXTE) {
            double c = selecVente.getMontantEspeces() != null ? selecVente.getMontantEspeces() : 0.0;
            double m = selecVente.getMontantMobile()  != null ? selecVente.getMontantMobile()  : 0.0;
            ticketBox.getChildren().add(makeMonoRow("Espèces", String.format(java.util.Locale.FRANCE, "%,.0f FCFA", c), monoNormal));
            ticketBox.getChildren().add(makeMonoRow("Mobile",  String.format(java.util.Locale.FRANCE, "%,.0f FCFA", m), monoNormal));
        }

        // Message de fin
        ticketBox.getChildren().add(new Label(""));
        Label sepFin = new Label("- - - - - - - - - - - - - - - - - - - -");
        sepFin.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #000000;");
        sepFin.setAlignment(javafx.geometry.Pos.CENTER); sepFin.setMaxWidth(Double.MAX_VALUE);
        ticketBox.getChildren().add(sepFin);

        Label lMsg = new Label(msgPharma);
        lMsg.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; -fx-text-fill: #000000;");
        lMsg.setWrapText(true); lMsg.setAlignment(javafx.geometry.Pos.CENTER); lMsg.setMaxWidth(Double.MAX_VALUE);
        ticketBox.getChildren().addAll(lMsg, new Label(""));

        // ── Wrapper zigzag ─────────────────────────────────────────────────────
        javafx.scene.layout.VBox ticketWrapper = new javafx.scene.layout.VBox();
        ticketWrapper.setPrefWidth(380); ticketWrapper.setMinWidth(380); ticketWrapper.setMaxWidth(380);
        ticketWrapper.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 20, 0, 0, 6);");
        ticketWrapper.getChildren().addAll(createZigZagEdge(380, true), ticketBox, createZigZagEdge(380, false));

        // ── ScrollPane pour limiter la hauteur de la fenêtre ──────────────────
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(ticketWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(580);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E293B; -fx-border-color: transparent;");

        // ── Fond Slate + Boutons ───────────────────────────────────────────────
        javafx.scene.layout.VBox rootBox = new javafx.scene.layout.VBox(20);
        rootBox.setAlignment(javafx.geometry.Pos.CENTER);
        rootBox.setStyle("-fx-padding: 25; -fx-background-color: #1E293B;");
        rootBox.getChildren().add(scrollPane);

        javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(12);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER);

        // Bouton Imprimer avec icône SVG
        Button btnPrint = new Button("Imprimer le Ticket");
        javafx.scene.shape.SVGPath printIcon = new javafx.scene.shape.SVGPath();
        printIcon.setContent("M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z");
        printIcon.setFill(javafx.scene.paint.Color.WHITE);
        btnPrint.setGraphic(printIcon);
        btnPrint.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand; -fx-graphic-text-gap: 8;");
        btnPrint.setOnAction(e -> com.pharmacie.utils.PrinterService.imprimerTicket(selecVente));

        // Bouton Fermer propre
        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color: white; -fx-text-fill: #475569; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 9 23; -fx-cursor: hand;");
        btnClose.setOnAction(e -> dialog.close());

        actionBox.getChildren().addAll(btnClose, btnPrint);
        rootBox.getChildren().add(actionBox);

        dialog.getDialogPane().setContent(rootBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setManaged(false);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent;");
        dialog.showAndWait();
    }

    @FXML
    public void loadHistoriqueVentes() {
        isUpdatingHistoriqueFiltres = true;
        // Par défaut, ne charger que les ventes de la journée courante pour protéger la RAM
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = java.time.LocalDate.now().atTime(23, 59, 59);

        // MAJ Visuelle de l'interface pour correspondre à cette requête par défaut
        if (dpHistoDebut != null)
            dpHistoDebut.setValue(java.time.LocalDate.now());
        if (dpHistoFin != null)
            dpHistoFin.setValue(java.time.LocalDate.now());
        if (txtFiltreVenteTicketId != null)
            txtFiltreVenteTicketId.clear();
        if (cmbFiltreVenteAgent != null)
            cmbFiltreVenteAgent.getSelectionModel().select(0);
        if (cmbFiltreVenteMode != null)
            cmbFiltreVenteMode.getSelectionModel().select(0);
        
        isUpdatingHistoriqueFiltres = false;

        List<Vente> todaySales = venteDAO.findVentesByPeriode(startOfDay, endOfDay, null, null, null);
        tableHistoriqueVentes.setItems(FXCollections.observableArrayList(todaySales));
        mettreAJourTotalFiltre(todaySales);
    }

    @FXML
    public void resetHistoriqueFiltres() {
        loadHistoriqueVentes();
    }

    @FXML
    public void filtrerHistorique() {
        LocalDate d = dpHistoDebut.getValue();
        LocalDate f = dpHistoFin.getValue();
        Long ticketId = null;
        if (txtFiltreVenteTicketId != null && !txtFiltreVenteTicketId.getText().trim().isEmpty()) {
            String inputText = txtFiltreVenteTicketId.getText().trim();
            if (inputText.contains("-")) {
                inputText = inputText.substring(inputText.lastIndexOf("-") + 1);
            }
            try {
                ticketId = Long.parseLong(inputText);
            } catch (NumberFormatException e) {
                // Ignore and don't filter if it's not a valid number
            }
        }
        com.pharmacie.models.User agentFiltre = cmbFiltreVenteAgent.getValue();
        com.pharmacie.models.Vente.ModePaiement modeFiltre = cmbFiltreVenteMode.getValue();

        // Si aucune date n'est précisée, on cherche très large
        LocalDateTime debut = d != null ? d.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime fin = f != null ? f.atTime(23, 59, 59) : LocalDateTime.of(2100, 1, 1, 23, 59);

        // Recherche optimisée directement sur le Serveur de Base de données
        List<Vente> filtered = venteDAO.findVentesByPeriode(debut, fin, ticketId, agentFiltre, modeFiltre);

        // Filtrage post-recherche strict (Smart Search : anti-fraude)
        if (txtFiltreVenteTicketId != null && !txtFiltreVenteTicketId.getText().trim().isEmpty()) {
            String inputText = txtFiltreVenteTicketId.getText().trim();
            // Si l'utilisateur a tapé une référence complexe (avec tirets), on exige une correspondance exacte
            if (inputText.contains("-")) {
                filtered = filtered.stream()
                        .filter(v -> v.getNumeroTicketOfficiel().equalsIgnoreCase(inputText))
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        tableHistoriqueVentes.setItems(FXCollections.observableArrayList(filtered));
        mettreAJourTotalFiltre(filtered);
    }

    private void mettreAJourTotalFiltre(List<Vente> ventes) {
        double total = ventes.stream().mapToDouble(Vente::getTotal).sum();
        lblTotalHistorique.setText(moneyFormat.format(total) + " FCFA");
    }

    @FXML
    public void imprimerHistoriquePdf() {
        List<Vente> ventes = tableHistoriqueVentes.getItems();
        if (ventes == null || ventes.isEmpty()) {
            showError("Aucune vente à imprimer.");
            return;
        }
        Stage stage = (Stage) tableHistoriqueVentes.getScene().getWindow();
        String periode = "";
        if (dpHistoDebut.getValue() != null)
            periode += dpHistoDebut.getValue().toString();
        if (dpHistoFin.getValue() != null)
            periode += "_au_" + dpHistoFin.getValue().toString();
        com.pharmacie.utils.PdfService.genererRecapitulatifVentes(new java.util.ArrayList<>(ventes), periode, stage);
    }

    @FXML
    public void exporterHistoriqueExcel() {
        List<Vente> ventes = tableHistoriqueVentes.getItems();
        if (ventes == null || ventes.isEmpty()) {
            showError("Aucune vente à exporter.");
            return;
        }
        Stage stage = (Stage) tableHistoriqueVentes.getScene().getWindow();
        String periode = "";
        if (dpHistoDebut.getValue() != null)
            periode += dpHistoDebut.getValue().toString();
        if (dpHistoFin.getValue() != null)
            periode += "_au_" + dpHistoFin.getValue().toString();
            
        com.pharmacie.utils.ExcelExportService.genererHistoriqueVentesExcel(new java.util.ArrayList<>(ventes), periode, stage);
    }

    @FXML
    public void reimprimerRecuHistorique() {
        Vente selected = tableHistoriqueVentes.getSelectionModel().getSelectedItem();
        if (selected != null) {
            com.pharmacie.utils.PrinterService.imprimerTicket(selected);
        }
    }

    private void showError(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Opération Impossible");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Fonction utilitaire pour flasher un champ en rouge
    private void showErrorEffect(javafx.scene.Node node) {
        if (node == null) return;
        String originalStyle = node.getStyle();
        // Force la bordure rouge sans casser le CSS sous-jacent complet
        node.setStyle(originalStyle + "; -fx-border-color: #E74C3C; -fx-border-width: 2px; -fx-border-radius: 4px;");
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> node.setStyle(originalStyle));
        pause.play();
    }
}
