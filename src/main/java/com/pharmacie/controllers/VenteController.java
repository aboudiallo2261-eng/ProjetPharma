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

public class VenteController {

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
    @FXML
    private javafx.scene.control.PasswordField txtPinDeverrouillage;
    @FXML
    private Label lblVerrouilleAgent;
    @FXML
    private Label lblVerrouilleErreur;
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
    private TableColumn<Vente, Long> colHistVenteId;
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
    private ComboBox<Produit> cmbFiltreVenteProduit;
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

    // --- TICKETS EN ATTENTE (en mémoire uniquement) ---
    private final java.util.List<TicketEnAttente> ticketsEnAttente = new java.util.ArrayList<>();
    private int ticketCounter = 1;

    /** Représente un panier suspendu en attente de rappel */
    private static class TicketEnAttente {
        final int numero;
        final java.time.LocalTime heure;
        final java.util.List<LigneVente> lignes;
        final double total;

        TicketEnAttente(int num, java.util.List<LigneVente> lignes, double total) {
            this.numero = num;
            this.heure = java.time.LocalTime.now();
            this.lignes = new java.util.ArrayList<>(lignes);
            this.total = total;
        }

        @Override
        public String toString() {
            return String.format("Ticket #%d | %s | %.0f FCFA | %d article(s)",
                    numero, heure.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    total, lignes.size());
        }
    }

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
            tablePanierVente.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (newSel != null) {
                    btnRetirerDuPanier.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-padding: 4 8; -fx-font-size: 12px; -fx-background-radius: 6;");
                } else {
                    btnRetirerDuPanier.setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-padding: 4 8; -fx-font-size: 12px; -fx-background-radius: 6;");
                }
            });
        }

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

        // Charger les produits dans le filtre
        javafx.collections.ObservableList<Produit> prods = FXCollections.observableArrayList();
        prods.add(null);
        prods.addAll(produitDAO.findAll());
        cmbFiltreVenteProduit.setItems(prods);
        cmbFiltreVenteProduit.setConverter(new javafx.util.StringConverter<Produit>() {
            @Override
            public String toString(Produit p) {
                return p != null ? p.getNom() : "Tous les produits";
            }

            @Override
            public Produit fromString(String s) {
                return null;
            }
        });
        com.pharmacie.utils.ComboBoxAutoComplete.setup(cmbFiltreVenteProduit);

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
        cmbFiltreVenteProduit.setOnAction(autoFilterEvent);
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

    /** Met le panier actuel en attente et le vide pour un nouveau client */
    @FXML
    public void mettreEnAttente() {
        if (panier.isEmpty()) {
            showError("Le panier est vide. Rien à mettre en attente.");
            return;
        }
        double total = panier.stream().mapToDouble(LigneVente::getSousTotal).sum();
        TicketEnAttente ticket = new TicketEnAttente(ticketCounter++, panier, total);
        ticketsEnAttente.add(ticket);
        panier.clear();
        calculerTotalVente();
        mettreAJourBadgeTickets();
        txtMontantRecu.clear();
        txtRechercheProduit.requestFocus();
    }

    /** Met à jour le texte du bouton avec le nombre de tickets en attente */
    private void mettreAJourBadgeTickets() {
        int nb = ticketsEnAttente.size();
        if (nb == 0) {
            btnTicketsEnAttente.setText("⏸ Tickets en Attente (0)");
            btnTicketsEnAttente.setStyle(
                    "-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        } else {
            btnTicketsEnAttente.setText("⏸ Tickets en Attente (" + nb + ") 🔴");
            btnTicketsEnAttente.setStyle(
                    "-fx-background-color: #E67E22; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-effect: dropshadow(gaussian, #e67e22, 10, 0.5, 0, 0);");
        }
    }

    /**
     * Affiche la liste des tickets en attente et permet de les rappeler ou
     * supprimer
     */
    @FXML
    public void voirTicketsEnAttente() {
        if (ticketsEnAttente.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Aucun ticket n'est en attente.");
            a.setHeaderText("⏸ File d'Attente");
            a.showAndWait();
            return;
        }

        Dialog<TicketEnAttente> dialog = new Dialog<>();
        dialog.setTitle("File d'Attente — Tickets Suspendus");
        dialog.setHeaderText(
                "⏸ « " + ticketsEnAttente.size() + " ticket(s) en attente » — Sélectionnez un ticket pour le rappeler");

        ButtonType btnRappeler = new ButtonType("Rappeler ce Ticket", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnSupprimer = new ButtonType("🗑 Supprimer", ButtonBar.ButtonData.LEFT);
        ButtonType btnFermer = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnRappeler, btnSupprimer, btnFermer);

        javafx.scene.control.ListView<TicketEnAttente> listView = new javafx.scene.control.ListView<>();
        listView.getItems().addAll(ticketsEnAttente);
        listView.getSelectionModel().selectFirst();
        listView.setPrefHeight(200);
        listView.setStyle("-fx-font-size: 14px;");

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().setMinWidth(500);

        dialog.setResultConverter(btn -> {
            if (btn == btnRappeler || btn == btnSupprimer)
                return listView.getSelectionModel().getSelectedItem();
            return null;
        });

        dialog.showAndWait().ifPresent(selected -> {
            if (selected == null)
                return;
            // Déterminer quel bouton a été cliqué via le type de bouton
            // On réutilise l'implémentation via un deuxième dialogue simple
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Ticket " + selected);
            confirm.setHeaderText("Que voulez-vous faire avec ce ticket ?");
            ButtonType r = new ButtonType("▶ Rappeler le ticket", ButtonBar.ButtonData.YES);
            ButtonType s = new ButtonType("🗑 Supprimer (client absent)", ButtonBar.ButtonData.NO);
            ButtonType c = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(r, s, c);
            confirm.showAndWait().ifPresent(rep -> {
                if (rep == r) {
                    // Option 2 : Vérification à la volée du stock au moment du rappel
                    if (!panier.isEmpty()) {
                        Alert a = new Alert(Alert.AlertType.WARNING,
                                "Votre panier actuel sera écrasé ! Voulez-vous continuer ?",
                                ButtonType.OK, ButtonType.CANCEL);
                        a.setHeaderText("Panier actuel non vide");
                        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                            return;
                    }

                    boolean stockAjuste = false;
                    StringBuilder alertMsg = new StringBuilder();
                    java.util.List<LigneVente> lignesValables = new java.util.ArrayList<>();
                    java.util.Map<Long, Integer> qteDecompte = new java.util.HashMap<>();

                    for (LigneVente lv : selected.lignes) {
                        Long pId = lv.getProduit().getId();
                        int dispoDB = calculerQteTotaleProduit(pId);
                        int dejaTraite = qteDecompte.getOrDefault(pId, 0);
                        int dispoRestant = dispoDB - dejaTraite;

                        int baseReq = lv.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE
                                && lv.getProduit().getEstDeconditionnable() != null
                                && lv.getProduit().getEstDeconditionnable()
                                        ? lv.getQuantiteVendue() * lv.getProduit().getUnitesParBoite()
                                        : lv.getQuantiteVendue();

                        if (baseReq > dispoRestant) {
                            stockAjuste = true;
                            if (dispoRestant <= 0) {
                                alertMsg.append("- ").append(lv.getProduit().getNom())
                                        .append(" : en rupture (retiré).\n");
                                continue;
                            }

                            // Ajustement à la baisse
                            int newQte = lv.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE
                                    && lv.getProduit().getEstDeconditionnable() != null
                                    && lv.getProduit().getEstDeconditionnable()
                                            ? dispoRestant / lv.getProduit().getUnitesParBoite()
                                            : dispoRestant;

                            if (newQte <= 0) {
                                alertMsg.append("- ").append(lv.getProduit().getNom())
                                        .append(" : reste insuffisant pour 1 boîte (retiré).\n");
                                continue;
                            }

                            lv.setQuantiteVendue(newQte);
                            lv.setSousTotal(lv.getPrixUnitaire() * newQte);
                            alertMsg.append("- ").append(lv.getProduit().getNom()).append(" : Qte réduite à ")
                                    .append(newQte).append(".\n");
                            baseReq = newQte * (lv.getTypeUnite() == LigneVente.TypeUnite.BOITE_ENTIERE
                                    && lv.getProduit().getEstDeconditionnable() != null
                                    && lv.getProduit().getEstDeconditionnable() ? lv.getProduit().getUnitesParBoite()
                                            : 1);
                        }

                        qteDecompte.put(pId, dejaTraite + baseReq);
                        lignesValables.add(lv);
                    }

                    panier.setAll(lignesValables);
                    calculerTotalVente();
                    ticketsEnAttente.remove(selected);
                    mettreAJourBadgeTickets();
                    txtRechercheProduit.requestFocus();

                    if (stockAjuste) {
                        Alert aInfo = new Alert(Alert.AlertType.WARNING,
                                "Certains stocks ont changé pendant l'attente du ticket :\n\n" + alertMsg.toString(),
                                ButtonType.OK);
                        aInfo.setHeaderText("Ajustement de stock nécessaire");
                        aInfo.showAndWait();
                    }
                } else if (rep == s) {
                    // Suppression propre (pas d'impact stock)
                    ticketsEnAttente.remove(selected);
                    mettreAJourBadgeTickets();
                }
            });
        });
    }

    @FXML
    public void verrouillerSession() {
        if (SessionManager.getCurrentUser() == null)
            return;
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
        Platform.runLater(() -> txtPinDeverrouillage.requestFocus());
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
            lblStatutSession.setText("Caisse: OUVERTE");
            lblStatutSession.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            txtRechercheProduit.requestFocus();
        } else {
            // Mauvais mot de passe
            lblVerrouilleErreur.setText("⚠️ Mot de passe incorrect. Réessayez.");
            lblVerrouilleErreur.setVisible(true);
            lblVerrouilleErreur.setManaged(true);
            txtPinDeverrouillage.clear();
            txtPinDeverrouillage.requestFocus();
        }
    }

    @FXML
    public void ouvrirCaisse() {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Ouverture de Caisse");
        dialog.setHeaderText("💰 Déclaration du Fond de Caisse");

        ButtonType btnValider = new ButtonType("Ouvrir la Caisse", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValider, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 40, 10, 10));

        TextField txtFond = new TextField();
        txtFond.setPromptText("Ex: 5000");
        txtFond.setPrefWidth(200);
        txtFond.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Force numeric input
        txtFond.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtFond.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        Label lblInst = new Label("Montant physique dans le tiroir (FCFA) :");
        lblInst.setStyle("-fx-font-size: 14px;");

        grid.add(lblInst, 0, 0);
        grid.add(txtFond, 0, 1);

        dialog.getDialogPane().setContent(grid);
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
            if (fond < 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Le fond de caisse saisi est invalide !");
                alert.show();
                return;
            }
            SessionCaisse session = new SessionCaisse();
            session.setUser(SessionManager.getCurrentUser());
            session.setDateOuverture(LocalDateTime.now());
            session.setFondDeCaisse(fond);
            session.setStatut(SessionCaisse.StatutSession.OUVERTE);
            sessionDAO.save(session);

            checkSessionStatus();
            System.out.println("Caisse ouverte avec un fond de " + fond);
        });
    }

    @FXML
    public void cloturerCaisse() {
        if (currentSession == null)
            return;

        // Faille 2 : Sécurité Caisse - Bloquer si tickets en mémoire
        if (!ticketsEnAttente.isEmpty()) {
            showError("IMPOSSIBLE DE CLÔTURER :\nVous avez " + ticketsEnAttente.size()
                    + " ticket(s) en attente.\nVeuillez les valider ou les supprimer avant de clôturer la caisse.");
            return;
        }

        // Calcul du total Espèces de la session en cours
        List<Vente> ventesSession = venteDAO.findAll().stream()
                .filter(v -> v.getSessionCaisse() != null
                        && v.getSessionCaisse().getId().equals(currentSession.getId()))
                .collect(Collectors.toList());

        double totalEspeces = ventesSession.stream()
                .filter(v -> v.getModePaiement() == Vente.ModePaiement.ESPECES)
                .mapToDouble(Vente::getTotal).sum();

        double totalMobile = ventesSession.stream()
                .filter(v -> v.getModePaiement() == Vente.ModePaiement.MOBILE_MONEY)
                .mapToDouble(Vente::getTotal).sum();

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
        dialog.setHeaderText("🛑 Arrêt de la Caisse et Comptage Mixte");

        ButtonType btnValider = new ButtonType("Clôturer Définitivement", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValider, ButtonType.CANCEL);

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(20);
        vbox.setPadding(new javafx.geometry.Insets(20));

        // SECTION SUMMARY
        javafx.scene.layout.VBox summaryBox = new javafx.scene.layout.VBox(5);
        summaryBox.setStyle("-fx-background-color: #ECF0F1; -fx-padding: 15; -fx-background-radius: 5;");
        Label l1 = new Label(
                "Fond de Caisse initial : " + String.format("%.0f", currentSession.getFondDeCaisse()) + " FCFA");
        Label l2 = new Label("Ventes Espèces : " + String.format("%.0f", totalEspeces) + " FCFA");

        Label l4 = new Label("Ventes Mobile Money : " + String.format("%.0f", totalMobile) + " FCFA");
        l4.setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold;");

        Label l3 = new Label("MONTANT PHYSIQUE ATTENDU (Tiroir) : " + String.format("%.0f", theorieEspeces) + " FCFA");
        l3.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2980b9; -fx-padding: 10 0 5 0;");

        summaryBox.getChildren().addAll(l1, l2, l4, new javafx.scene.control.Separator(), l3);

        // SECTION INPUT
        javafx.scene.layout.GridPane inputGrid = new javafx.scene.layout.GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(15);

        Label lblEspece = new Label("Argent Physique (Tiroir) :");
        lblEspece.setStyle("-fx-font-weight: bold;");
        TextField txtDeclareEspeces = new TextField();
        txtDeclareEspeces.setPromptText("Montant Réel Tiroir");
        txtDeclareEspeces.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e67e22;");

        // Force numeric inputs
        txtDeclareEspeces.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*"))
                txtDeclareEspeces.setText(newVal.replaceAll("[^\\d]", ""));
        });

        inputGrid.add(lblEspece, 0, 0);
        inputGrid.add(txtDeclareEspeces, 1, 0);

        vbox.getChildren().addAll(summaryBox, inputGrid);

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

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Bilan de la Clôture");
            info.setHeaderText("✅ Caisse Fermée avec succès !");

            StringBuilder msg = new StringBuilder();
            msg.append("--- BILAN PHYSIQUE (TIROIR) ---\n");
            msg.append("Déclaré : ").append(String.format("%.0f", result.especes)).append(" FCFA\n");
            msg.append("Écart : ").append(String.format("%.0f", ecartEspeces)).append(" FCFA\n\n");

            msg.append("--- BILAN MOBILE MONEY ---\n");
            msg.append("Total encaissé numériquement : ").append(String.format("%.0f", totalMobile))
                    .append(" FCFA\n\n");

            msg.append("Le journal Z a été archivé dans l'historique.");
            info.setContentText(msg.toString());
            info.showAndWait();

            checkSessionStatus();
        });
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

        tableStock.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);

        tableStock.setRowFactory(tv -> new TableRow<Produit>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    int qty = calculerQteTotaleProduit(item.getId());
                    if (qty <= 0) {
                        // Utilisation du contrôle de fond interne pour préserver la sélection bleue
                        setStyle(
                                "-fx-control-inner-background: #ffcccc; -fx-control-inner-background-alt: #ffcccc; -fx-text-fill: #c0392b;");
                    } else {
                        setStyle("");
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
        colPanVenteQte
                .setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
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
        tableStock.setItems(FXCollections.observableArrayList(tousLesProduitsCache));
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

            // Re-calibration exacte pour le Z
            if (cmbModePaiement.getValue() == Vente.ModePaiement.ESPECES) {
                mEspeces = totalAPayer;
                mMobile = 0.0;
            } else if (cmbModePaiement.getValue() == Vente.ModePaiement.MOBILE_MONEY) {
                mEspeces = 0.0;
                mMobile = totalAPayer;
            } else if (cmbModePaiement.getValue() == Vente.ModePaiement.MIXTE) {
                // If they overpaid in mixte via cash, cash change is given. The base Mobile
                // sits at exact.
                if (recu > totalAPayer) {
                    double overpay = recu - totalAPayer;
                    mEspeces = Math.max(0, mEspeces - overpay);
                    // if overpay exceeds cash given, business technically refunds via mobile or
                    // it's an error. Usually cash is returned.
                }
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
            System.out.println("Vente validée et enregistrée via VenteService !");

            txtRechercheProduit.requestFocus();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void imprimerRecu(Vente vente) {
        com.pharmacie.utils.PrinterService.imprimerTicket(vente);
    }

    private void initHistoriqueColumns() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colHistVenteId.setCellValueFactory(new PropertyValueFactory<>("id"));
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

    @FXML
    public void showSelectedVenteDetail() {
        Vente selecVente = tableHistoriqueVentes.getSelectionModel().getSelectedItem();
        if (selecVente == null)
            return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Aperçu du Ticket Premium");
        dialog.setHeaderText(null);

        // Fetch Pharmacy Info
        com.pharmacie.dao.PharmacieInfoDAO infoDAO = new com.pharmacie.dao.PharmacieInfoDAO();
        com.pharmacie.models.PharmacieInfo info = infoDAO.getInfo();

        String nomPharma = info != null && info.getNom() != null ? info.getNom() : "PHARMACIE VÉTÉRINAIRE";
        String telPharma = info != null && info.getTelephone() != null ? "Tél: " + info.getTelephone() : "";
        String addrPharma = info != null && info.getAdresse() != null ? info.getAdresse() : "";
        String msgPharma = info != null && info.getMessageTicket() != null ? info.getMessageTicket() : "Merci de votre visite et à bientôt !";

        // Container principal du ticket paper visuel
        javafx.scene.layout.VBox ticketBox = new javafx.scene.layout.VBox(5);
        ticketBox.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0, 0, 5);");
        ticketBox.setPrefWidth(380);
        ticketBox.setMinWidth(380);
        ticketBox.setMaxWidth(380);
        ticketBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        String monoBold = "-fx-font-family: 'Courier New', 'Consolas', monospace; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;";
        String monoNormal = "-fx-font-family: 'Courier New', 'Consolas', monospace; -fx-font-size: 13px; -fx-text-fill: #000000;";
        String monoTitle = "-fx-font-family: 'Courier New', 'Consolas', monospace; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #000000;";

        // En-tête centré
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(3);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label lblNom = new Label(nomPharma.toUpperCase());
        lblNom.setStyle(monoTitle);
        header.getChildren().add(lblNom);
        
        if (!addrPharma.isEmpty()) {
            Label lblAddr = new Label(addrPharma); lblAddr.setStyle(monoNormal); header.getChildren().add(lblAddr);
        }
        if (!telPharma.isEmpty()) {
            Label lblTel = new Label(telPharma); lblTel.setStyle(monoNormal); header.getChildren().add(lblTel);
        }
        
        ticketBox.getChildren().addAll(header, new Label(""));

        // Metabox: Infos Date / Agent / Ticket
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = selecVente.getDateVente() != null ? selecVente.getDateVente().format(fmt) : "";
        String agent = selecVente.getUser() != null ? selecVente.getUser().getNom() : "Inconnu";
        String customRef = selecVente.getDateVente() != null ? selecVente.getDateVente().format(DateTimeFormatter.ofPattern("ddMMyy-HHmm")) + "-" + String.format("%03d", selecVente.getId()) : String.valueOf(selecVente.getId());

        Label lDate = new Label("Date     : " + dateStr); lDate.setStyle(monoNormal);
        Label lAgent = new Label("Caissier : " + agent); lAgent.setStyle(monoNormal);
        Label lTicket = new Label("Ticket N°: " + customRef); lTicket.setStyle(monoNormal);
        
        Label sep1 = new Label("----------------------------------------"); sep1.setStyle(monoNormal);

        ticketBox.getChildren().addAll(lDate, lAgent, lTicket, sep1);

        // Lignes de produits (Comme sur l'imprimante avec tirets)
        for (LigneVente lv : selecVente.getLignesVente()) {
            String nom = lv.getProduit().getNom();
            if (nom.length() > 40) nom = nom.substring(0, 40); 
            Label lblProd = new Label(nom);
            lblProd.setStyle(monoBold);
            
            String leftPart = "  " + lv.getQuantiteVendue() + " x " + String.format(java.util.Locale.FRANCE, "%.0f", lv.getPrixUnitaire()) + " F ";
            String rightPart = " " + String.format(java.util.Locale.FRANCE, "%.0f", lv.getSousTotal()) + " FCFA";
            int baseLen = leftPart.length() + rightPart.length();
            int dotsCount = 40 - baseLen; // 40 char pour la longueur repère (Ajusté pour tenir sur un papier 80mm)
            if (dotsCount < 1) dotsCount = 1;
            
            StringBuilder line = new StringBuilder(leftPart);
            for(int i=0; i < dotsCount; i++){ line.append("-"); } // Les tirets expressément demandés
            line.append(rightPart);
            
            Label lblLineInfo = new Label(line.toString());
            lblLineInfo.setStyle(monoNormal);
            
            ticketBox.getChildren().addAll(lblProd, lblLineInfo);
        }
        
        Label sep2 = new Label("----------------------------------------"); sep2.setStyle(monoNormal);
        ticketBox.getChildren().add(sep2);

        // Total et Pied de ticket
        javafx.scene.layout.VBox footer = new javafx.scene.layout.VBox(5);
        footer.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label lTotal = new Label("TOTAL A PAYER : " + String.format(java.util.Locale.FRANCE, "%,.0f FCFA", selecVente.getTotal()));
        lTotal.setStyle(monoTitle);
        footer.getChildren().add(lTotal);
        
        String mode = selecVente.getModePaiement() != null ? selecVente.getModePaiement().name() : "INCONNU";
        Label lMode = new Label("Payé par : " + mode.replace("_", " "));
        lMode.setStyle(monoNormal);
        footer.getChildren().add(lMode);
        
        if (selecVente.getModePaiement() == Vente.ModePaiement.MIXTE || selecVente.getModePaiement() == Vente.ModePaiement.ESPECES) {
            double rec = selecVente.getMontantRecu() != null ? selecVente.getMontantRecu() : selecVente.getTotal();
            double mon = selecVente.getMonnaieRendue() != null ? selecVente.getMonnaieRendue() : 0.0;
            Label lRecu = new Label("Montant Reçu : " + String.format(java.util.Locale.FRANCE, "%,.0f FCFA", rec));
            lRecu.setStyle(monoNormal);
            footer.getChildren().add(lRecu);
            Label lMonnaie = new Label("Monnaie      : " + String.format(java.util.Locale.FRANCE, "%,.0f FCFA", mon));
            lMonnaie.setStyle(monoNormal);
            footer.getChildren().add(lMonnaie);
        }

        if (selecVente.getModePaiement() == Vente.ModePaiement.MIXTE) {
            double c = selecVente.getMontantEspeces() != null ? selecVente.getMontantEspeces() : 0.0;
            double m = selecVente.getMontantMobile() != null ? selecVente.getMontantMobile() : 0.0;
            Label lMixte = new Label(String.format(java.util.Locale.FRANCE, "Espèces: %,.0f | Mobile: %,.0f", c, m));
            lMixte.setStyle(monoNormal);
            footer.getChildren().add(lMixte);
        }
        
        footer.getChildren().add(new Label(""));
        
        Label lMsg = new Label(msgPharma);
        lMsg.setStyle(monoNormal);
        lMsg.setWrapText(true);
        lMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        footer.getChildren().add(lMsg);
        
        ticketBox.getChildren().add(footer);

        // --- INTERFACE EXTERNE (ARRIERE PLAN DE LA DIALOG) ---
        javafx.scene.layout.VBox rootBox = new javafx.scene.layout.VBox(15);
        rootBox.setAlignment(javafx.geometry.Pos.CENTER);
        // Utilisation d'un fond de type "Soft dark" pour accentuer l'effet papier blanc pur du ticket
        rootBox.setStyle("-fx-padding: 35; -fx-background-color: #2C3E50;"); 
        rootBox.getChildren().add(ticketBox);

        javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button btnPrint = new Button("🖨 Imprimer le Ticket");
        btnPrint.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;");
        btnPrint.setOnAction(e -> {
            com.pharmacie.utils.PrinterService.imprimerTicket(selecVente);
        });

        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color: #BDC3C7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;");
        btnClose.setOnAction(e -> dialog.close());

        actionBox.getChildren().addAll(btnClose, btnPrint);
        rootBox.getChildren().add(actionBox);

        dialog.getDialogPane().setContent(rootBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        // Clean look
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
        if (cmbFiltreVenteProduit != null)
            cmbFiltreVenteProduit.getSelectionModel().select(0);
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
        Produit produitFiltre = cmbFiltreVenteProduit.getValue();
        com.pharmacie.models.User agentFiltre = cmbFiltreVenteAgent.getValue();
        com.pharmacie.models.Vente.ModePaiement modeFiltre = cmbFiltreVenteMode.getValue();

        // Si aucune date n'est précisée, on cherche très large
        LocalDateTime debut = d != null ? d.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime fin = f != null ? f.atTime(23, 59, 59) : LocalDateTime.of(2100, 1, 1, 23, 59);

        // Recherche optimisée directement sur le Serveur de Base de données
        List<Vente> filtered = venteDAO.findVentesByPeriode(debut, fin, produitFiltre, agentFiltre, modeFiltre);

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
