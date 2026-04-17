package com.pharmacie.controllers;

import com.pharmacie.dao.*;
import com.pharmacie.models.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

public class ReportController {

    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private ComboBox<User> cmbCaissier;
    @FXML private ComboBox<Categorie> cmbCategorie;
    @FXML private ComboBox<Espece> cmbEspece;
    @FXML private Label lblTotalFiltre;
    // Phase 5 : Boutons d'export
    @FXML private Button btnExportPdfVentes;
    @FXML private Button btnExportCsvVentes;
    @FXML private Button btnExportPdfAjust;
    @FXML private Button btnExportPdfCloture;
    @FXML private Button btnExportPdfAudit;

    @FXML private TableView<LigneVente> tableLignesVente;
    @FXML private TableColumn<LigneVente, String> colLvDate, colLvTicket, colLvAgent, colLvProduit, colLvCat, colLvEsp;
    @FXML private TableColumn<LigneVente, Integer> colLvQte;
    @FXML private TableColumn<LigneVente, Double> colLvPrix, colLvTotal;

    @FXML private DatePicker dpAjustDebut;
    @FXML private DatePicker dpAjustFin;
    @FXML private ComboBox<Produit> cmbAjustProduit;
    @FXML private ComboBox<User> cmbAjustUser;
    @FXML private ComboBox<MouvementStock.TypeMouvement> cmbAjustType;
    @FXML private TableView<AjustementStock> tableAjustements;
    @FXML private TableColumn<AjustementStock, String> colAdjDate, colAdjProduit, colAdjLot, colAdjUser, colAdjMotif, colAdjObs;
    @FXML private TableColumn<AjustementStock, Integer> colAdjQte;

    // --- AUDIT DES STOCKS ---
    @FXML private DatePicker dpAuditDebut;
    @FXML private DatePicker dpAuditFin;
    @FXML private ComboBox<Produit> cmbAuditProduit;
    @FXML private ComboBox<MouvementStock.TypeMouvement> cmbAuditType;
    @FXML private ComboBox<User> cmbAuditUser;
    @FXML private TableView<MouvementStock> tableAudit;
    @FXML private TableColumn<MouvementStock, String> colAuditDate, colAuditType, colAuditRapport, colAuditQte, colAuditRef, colAuditUser;

    // --- REGISTRE DES CLOTURES (Z) ---
    @FXML private DatePicker dpClotureDebut;
    @FXML private DatePicker dpClotureFin;
    @FXML private ComboBox<User> cmbClotureUser;
    @FXML private TableView<SessionCaisse> tableClotures;
    @FXML private TableColumn<SessionCaisse, String> colZDate, colZAgent, colZStatut, colZTheorieEsp, colZTheorieMob;
    @FXML private TableColumn<SessionCaisse, Double> colZEcartEsp, colZEcartMob;

    private GenericDAO<LigneVente> ligneVenteDAO = new GenericDAO<>(LigneVente.class);
    private AjustementStockDAO ajustementStockDAO = new AjustementStockDAO();
    private UserDAO userDAO = new UserDAO();
    private CategorieDAO catDAO = new CategorieDAO();
    private EspeceDAO espDAO = new EspeceDAO();
    private MouvementDAO mouvementDAO = new MouvementDAO();
    private GenericDAO<Produit> produitDAO = new GenericDAO<>(Produit.class);
    private SessionCaisseDAO sessionDAO = new SessionCaisseDAO();

    private List<LigneVente> currentPeriodLines;
    private List<AjustementStock> currentAjustements;
    private List<SessionCaisse> currentClotures;

    private boolean isUpdatingRapportFiltres = false;
    private boolean isUpdatingAjustFiltres = false;
    private boolean isUpdatingAuditFiltres = false;
    private boolean isUpdatingClotureFiltres = false;

    @FXML
    public void initialize() {
        dpDateDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpDateFin.setValue(LocalDate.now());
        
        dpAjustDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpAjustFin.setValue(LocalDate.now());
        
        dpAuditDebut.setValue(LocalDate.now().minusDays(7));
        dpAuditFin.setValue(LocalDate.now());

        com.pharmacie.utils.DateUtils.bindDateFilters(dpDateDebut, dpDateFin);
        com.pharmacie.utils.DateUtils.bindDateFilters(dpAjustDebut, dpAjustFin);
        com.pharmacie.utils.DateUtils.bindDateFilters(dpAuditDebut, dpAuditFin);
        com.pharmacie.utils.DateUtils.bindDateFilters(dpClotureDebut, dpClotureFin);

        loadFilters();
        initColumns();
        
        initAuditColumns();
        initAjustementColumns();
        initClotureColumns();

        List<User> users = userDAO.findAll();
        cmbCaissier.setItems(FXCollections.observableArrayList(users));
        cmbCaissier.getItems().add(0, null);
        cmbAjustUser.setItems(FXCollections.observableArrayList(users));
        cmbAjustUser.getItems().add(0, null);
        cmbAuditUser.setItems(FXCollections.observableArrayList(users));
        cmbAuditUser.getItems().add(0, null);
        cmbClotureUser.setItems(FXCollections.observableArrayList(users));
        cmbClotureUser.getItems().add(0, null);
        
        StringConverter<User> userCvt = new StringConverter<User>() {
            @Override public String toString(User u) { return u != null ? u.getNom() : "Tous les agents"; }
            @Override public User fromString(String string) { return null; }
        };
        cmbCaissier.setConverter(userCvt);
        cmbAjustUser.setConverter(userCvt);
        cmbAuditUser.setConverter(userCvt);
        cmbClotureUser.setConverter(userCvt);
        // Actions automatiques (Live Filtering) avec anti-rebond
        javafx.event.EventHandler<javafx.event.ActionEvent> actRapport = e -> { if (!isUpdatingRapportFiltres) genererRapport(); };
        dpDateDebut.setOnAction(actRapport);
        dpDateFin.setOnAction(actRapport);
        cmbCaissier.setOnAction(actRapport);
        cmbCategorie.setOnAction(actRapport);
        cmbEspece.setOnAction(actRapport);

        javafx.event.EventHandler<javafx.event.ActionEvent> actAjust = e -> { if (!isUpdatingAjustFiltres) genererAjustements(); };
        dpAjustDebut.setOnAction(actAjust);
        dpAjustFin.setOnAction(actAjust);
        cmbAjustUser.setOnAction(actAjust);
        if (cmbAjustProduit != null) cmbAjustProduit.setOnAction(actAjust);

        javafx.event.EventHandler<javafx.event.ActionEvent> actAudit = e -> { if (!isUpdatingAuditFiltres) genererAudit(); };
        dpAuditDebut.setOnAction(actAudit);
        dpAuditFin.setOnAction(actAudit);
        cmbAuditUser.setOnAction(actAudit);
        if (cmbAuditProduit != null) cmbAuditProduit.setOnAction(actAudit);
        if (cmbAuditType != null) cmbAuditType.setOnAction(actAudit);

        javafx.event.EventHandler<javafx.event.ActionEvent> actCloture = e -> { if (!isUpdatingClotureFiltres) genererClotures(); };
        dpClotureDebut.setOnAction(actCloture);
        dpClotureFin.setOnAction(actCloture);
        cmbClotureUser.setOnAction(actCloture);
        
        // Phase 4 : Colonnes adaptatives (CONSTRAINED retiré pour privilégier les prefWidth FXML sur les grands tableaux)

        genererRapport();
        genererAjustements();
        genererAudit();
        genererClotures();
    }

    private void loadFilters() {
        cmbCategorie.getItems().add(null);
        cmbCategorie.getItems().addAll(catDAO.findAll());
        
        cmbEspece.getItems().add(null);
        cmbEspece.getItems().addAll(espDAO.findAll());
        
        javafx.util.StringConverter<Categorie> catConv = new javafx.util.StringConverter<Categorie>() {
            public String toString(Categorie v) { return v == null ? "Toutes les Catégories" : v.getNom(); }
            public Categorie fromString(String string) { return null; }
        };
        cmbCategorie.setConverter(catConv);
        
        javafx.util.StringConverter<Espece> espConv = new javafx.util.StringConverter<Espece>() {
            public String toString(Espece v) { return v == null ? "Toutes les Espèces" : v.getNom(); }
            public Espece fromString(String string) { return null; }
        };
        cmbEspece.setConverter(espConv);
        
        cmbAuditProduit.getItems().add(null);
        cmbAuditProduit.getItems().addAll(produitDAO.findAll());
        cmbAuditType.getItems().add(null);
        cmbAuditType.getItems().addAll(MouvementStock.TypeMouvement.values());

        javafx.util.StringConverter<Produit> prodConv = new javafx.util.StringConverter<Produit>() {
            public String toString(Produit v) { return v == null ? "Tous les produits" : v.getNom(); }
            public Produit fromString(String string) { return null; }
        };
        cmbAuditProduit.setConverter(prodConv);

        javafx.util.StringConverter<MouvementStock.TypeMouvement> typeConv = new javafx.util.StringConverter<>() {
            public String toString(MouvementStock.TypeMouvement v) { return v == null ? "Tous les mouvements" : v.name(); }
            public MouvementStock.TypeMouvement fromString(String string) { return null; }
        };
        cmbAuditType.setConverter(typeConv);
        
        if (cmbAjustProduit != null) {
            cmbAjustProduit.getItems().add(null);
            cmbAjustProduit.getItems().addAll(produitDAO.findAll());
            cmbAjustProduit.setConverter(prodConv);
        }
        
        if (cmbAjustType != null) {
            // Bug #5 : Le ComboBox a un onAction="#genererAjustements" déclaré dans le FXML.
            // Sans ce flag, chaque appel à .getItems().add() déclenche genererAjustements()
            // pendant le chargement initial, causant des appels parasites en cascade.
            isUpdatingAjustFiltres = true;
            cmbAjustType.getItems().add(null);
            cmbAjustType.getItems().add(MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF);
            cmbAjustType.getItems().add(MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF);
            cmbAjustType.setConverter(new javafx.util.StringConverter<MouvementStock.TypeMouvement>() {
                public String toString(MouvementStock.TypeMouvement v) {
                    if (v == null) return "Toutes les opérations";
                    if (v == MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF) return "Retraits / Pertes";
                    if (v == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) return "Ajouts de Stock";
                    return v.name();
                }
                public MouvementStock.TypeMouvement fromString(String string) { return null; }
            });
            isUpdatingAjustFiltres = false; // Relâche le verrou après chargement complet
        }
    }

    private void initColumns() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colLvDate.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getVente().getDateVente().format(fmt)));
        colLvTicket.setCellValueFactory(v -> new SimpleStringProperty("TCK-" + v.getValue().getVente().getId()));
        colLvAgent.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getVente().getUser().getNom()));
        colLvProduit.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getProduit().getNom()));
        colLvCat.setCellValueFactory(v -> {
            String cat = v.getValue().getProduit().getCategorie().getNom();
            return new SimpleStringProperty(cat != null && !cat.isEmpty() ? cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase() : "");
        });
        colLvEsp.setCellValueFactory(v -> {
            String esp = v.getValue().getProduit().getEspece().getNom();
            return new SimpleStringProperty(esp != null && !esp.isEmpty() ? esp.substring(0, 1).toUpperCase() + esp.substring(1).toLowerCase() : "");
        });
        colLvQte.setCellValueFactory(new PropertyValueFactory<>("quantiteVendue"));
        colLvPrix.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        colLvTotal.setCellValueFactory(new PropertyValueFactory<>("sousTotal"));

        // Formatage Premium (Alignement, Monnaie)
        javafx.util.Callback<TableColumn<LigneVente, Double>, TableCell<LigneVente, Double>> cellFactoryCurrency = column -> new TableCell<LigneVente, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%,.0f FCFA", item));
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
                }
            }
        };
        colLvPrix.setCellFactory(cellFactoryCurrency);
        colLvTotal.setCellFactory(cellFactoryCurrency);

        colLvQte.setCellFactory(column -> new TableCell<LigneVente, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    private void initAjustementColumns() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colAdjDate.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getDateAjustement().format(fmt)));

        // Bug #4 corrigé : on affiche le nom tel quel depuis la base.
        // L'ancien .toLowerCase() détruisait la casse des noms composés (ex: "IVOMEC 1%" → "ivomec 1%").
        colAdjProduit.setCellValueFactory(v -> {
            String nom = v.getValue().getLot().getProduit().getNom();
            return new SimpleStringProperty(nom != null ? nom : "");
        });

        colAdjLot.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getLot().getNumeroLot()));
        colAdjUser.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getUser().getNom()));
        colAdjMotif.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getMotif().getLabel()));

        colAdjQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colAdjQte.setCellFactory(column -> new TableCell<AjustementStock, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    AjustementStock adj = getTableView().getItems().get(getIndex());
                    if (adj != null && adj.getTypeAjustement() == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) {
                        setText("+ " + item);
                        setStyle("-fx-text-fill: #27ae60; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    } else {
                        setText("- " + item);
                        setStyle("-fx-text-fill: #c0392b; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colAdjObs.setCellValueFactory(new PropertyValueFactory<>("observation"));

        // Bug #3 corrigé : RowFactory avec code couleur par type d'ajustement.
        // AJUSTEMENT_POSITIF (ajout)  → fond vert très pâle (#eafaf1)
        // AJUSTEMENT_NEGATIF (retrait) → fond rouge très pâle (#fdf2f2)
        // Le listener sur selectedProperty() garantit que la sélection système (bleu)
        // reprend le dessus immédiatement, sans laisser la couleur métier interférer.
        tableAjustements.setRowFactory(tv -> new TableRow<AjustementStock>() {
            @Override
            protected void updateItem(AjustementStock item, boolean empty) {
                super.updateItem(item, empty);
                Runnable applyStyle = () -> {
                    if (item == null || empty) {
                        setStyle("");
                        return;
                    }
                    if (isSelected()) {
                        // On laisse le système appliquer sa couleur de sélection (bleu).
                        // Forcer un style ici écraserait le bleu par la couleur métier.
                        setStyle("");
                        return;
                    }
                    if (item.getTypeAjustement() == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) {
                        setStyle("-fx-background-color: #eafaf1;"); // Vert très pâle
                    } else {
                        setStyle("-fx-background-color: #fdf2f2;"); // Rouge très pâle
                    }
                };
                // Ce listener est critique : sans lui, déselectionner une ligne
                // ne déclenche pas updateItem() et la couleur de sélection reste bloquée.
                selectedProperty().addListener((obs, wasSelected, isNowSelected) -> applyStyle.run());
                applyStyle.run();
            }
        });
    }

    private void initAuditColumns() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colAuditDate.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getDateMouvement().format(fmt)));
        
        colAuditType.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getTypeMouvement().name()));
        colAuditType.setCellFactory(column -> new TableCell<MouvementStock, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    MouvementStock m = getTableView().getItems().get(getIndex());
                    if (m != null) {
                        switch (m.getTypeMouvement()) {
                            case ACHAT:
                                setText("Entrée (Achat)");
                                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                                break;
                            case VENTE:
                                setText("Sortie (Vente)");
                                setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                                break;
                            case AJUSTEMENT_NEGATIF:
                                setText("Perte / Retrait");
                                setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                                break;
                            default:
                                setText(m.getTypeMouvement().name());
                                setStyle("-fx-text-fill: #34495e;");
                                break;
                        }
                    }
                }
            }
        });

        colAuditRapport.setCellValueFactory(v -> {
            String nom = v.getValue().getProduit().getNom();
            if (nom != null && !nom.isEmpty()) {
                nom = nom.substring(0, 1).toUpperCase() + nom.substring(1).toLowerCase();
            }
            String lot = v.getValue().getLot() != null ? " (Lot: " + v.getValue().getLot().getNumeroLot() + ")" : "";
            return new SimpleStringProperty(nom + lot);
        });
        colAuditRef.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colAuditUser.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getUser().getNom()));
        
        colAuditQte.setCellValueFactory(v -> {
            MouvementStock m = v.getValue();
            Produit p = m.getProduit();
            String prefix = m.getQuantite() > 0 ? "+ " : (m.getQuantite() < 0 ? "- " : "");
            int qteAbs = Math.abs(m.getQuantite());
            String formatee = qteAbs + " Unité(s)";
            if (p.getEstDeconditionnable() != null && p.getEstDeconditionnable() && p.getUnitesParBoite() != null && p.getUnitesParBoite() > 0) {
                int boites = qteAbs / p.getUnitesParBoite();
                int unites = qteAbs % p.getUnitesParBoite();
                if (boites == 0) {
                    formatee = unites + " Unité(s)";
                } else if (unites == 0) {
                    formatee = boites + " Bte(s)";
                } else {
                    formatee = boites + " Bte(s) et " + unites + " Unité(s)";
                }
            }
            return new SimpleStringProperty(prefix + formatee);
        });
        
        // Remove aggressive row factory to keep zebra striping
        tableAudit.setRowFactory(null);
    }

    private void initClotureColumns() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colZDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty("Du " + dtf.format(cellData.getValue().getDateOuverture()) + "\nAu " + 
                (cellData.getValue().getDateCloture() != null ? dtf.format(cellData.getValue().getDateCloture()) : "En cours")));
        colZAgent.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUser().getNom()));
        colZStatut.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut().toString()));
        colZTheorieEsp.setCellValueFactory(cellData -> {
            String total = String.format("%,.0f FCFA", cellData.getValue().getTotalEspecesAttendu());
            String fond = cellData.getValue().getFondDeCaisse() > 0 ? " (incl. " + String.format("%,.0f", cellData.getValue().getFondDeCaisse()) + " fond)" : "";
            return new SimpleStringProperty(total + fond);
        });
        colZTheorieMob.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%,.0f FCFA", cellData.getValue().getTotalMobileAttendu() != null ? cellData.getValue().getTotalMobileAttendu() : 0.0)));
        colZEcartEsp.setCellValueFactory(new PropertyValueFactory<>("ecartEspeces"));
        configureEcartColumn(colZEcartEsp);
        colZEcartMob.setCellValueFactory(new PropertyValueFactory<>("ecartMobile"));
        configureEcartColumn(colZEcartMob);
    }
    
    private void configureEcartColumn(TableColumn<SessionCaisse, Double> col) {
        col.setCellFactory(column -> new TableCell<SessionCaisse, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    double val = item != null ? item : 0.0;
                    setText(String.format("%,.0f FCFA", val));
                    if (val < 0) {
                        setTextFill(Color.web("#c0392b"));
                        setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
                    } else if (val > 0) {
                        setTextFill(Color.web("#27ae60"));
                        setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
                    } else {
                        setTextFill(Color.BLACK);
                        setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
                    }
                }
            }
        });
    }
    @FXML
    public void resetRapportVente() {
        isUpdatingRapportFiltres = true;
        dpDateDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpDateFin.setValue(LocalDate.now());
        cmbCaissier.getSelectionModel().select(0);
        cmbCategorie.getSelectionModel().select(0);
        cmbEspece.getSelectionModel().select(0);
        isUpdatingRapportFiltres = false;
        genererRapport();
    }

    @FXML
    public void resetAudit() {
        isUpdatingAuditFiltres = true;
        dpAuditDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpAuditFin.setValue(LocalDate.now());
        if (cmbAuditProduit != null) cmbAuditProduit.getSelectionModel().select(0);
        if (cmbAuditType != null) cmbAuditType.getSelectionModel().select(0);
        if (cmbAuditUser != null) cmbAuditUser.getSelectionModel().select(0);
        isUpdatingAuditFiltres = false;
        genererAudit();
    }

    @FXML
    public void resetAjustements() {
        isUpdatingAjustFiltres = true;
        dpAjustDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpAjustFin.setValue(LocalDate.now());
        if (cmbAjustProduit != null) cmbAjustProduit.getSelectionModel().select(0);
        if (cmbAjustUser != null) cmbAjustUser.getSelectionModel().select(0);
        if (cmbAjustType != null) cmbAjustType.getSelectionModel().select(0);
        isUpdatingAjustFiltres = false;
        genererAjustements();
    }

    @FXML
    public void resetClotures() {
        isUpdatingClotureFiltres = true;
        dpClotureDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpClotureFin.setValue(LocalDate.now());
        cmbClotureUser.getSelectionModel().select(0);
        isUpdatingClotureFiltres = false;
        genererClotures();
    }

    @FXML
    public void genererRapport() {
        if (dpDateDebut.getValue() == null || dpDateFin.getValue() == null) return;
        LocalDateTime start = dpDateDebut.getValue().atStartOfDay();
        LocalDateTime end = dpDateFin.getValue().plusDays(1).atStartOfDay();
        User selUser = cmbCaissier.getValue();
        Categorie selCat = cmbCategorie.getValue();
        Espece selEsp = cmbEspece.getValue();
        List<LigneVente> all = ligneVenteDAO.findAll();
        currentPeriodLines = all.stream().filter(lv -> {
            if(lv.getVente().getDateVente() == null) return false;
            boolean qDate = !lv.getVente().getDateVente().isBefore(start) && lv.getVente().getDateVente().isBefore(end);
            boolean qUser = selUser == null || lv.getVente().getUser().getId().equals(selUser.getId());
            boolean qCat = selCat == null || lv.getProduit().getCategorie().getId().equals(selCat.getId());
            boolean qEsp = selEsp == null || lv.getProduit().getEspece().getId().equals(selEsp.getId());
            return qDate && qUser && qCat && qEsp;
        }).collect(Collectors.toList());
        double filteredCA = currentPeriodLines.stream().mapToDouble(LigneVente::getSousTotal).sum();
        lblTotalFiltre.setText(String.format("%,.0f FCFA", filteredCA));
        tableLignesVente.setItems(FXCollections.observableArrayList(currentPeriodLines));
        // Phase 5 : Activer/désactiver les boutons export selon les données
        boolean hasData = !currentPeriodLines.isEmpty();
        if (btnExportPdfVentes != null) btnExportPdfVentes.setDisable(!hasData);
        if (btnExportCsvVentes != null) btnExportCsvVentes.setDisable(!hasData);
    }
    
    @FXML
    public void genererAjustements() {
        // Bug #5 : Guard against spurious triggers fired by the FXML-bound onAction
        // when items are added to cmbAjustType during loadFilters() initialization.
        if (isUpdatingAjustFiltres) return;
        if (dpAjustDebut.getValue() == null || dpAjustFin.getValue() == null) return;
        LocalDateTime start = dpAjustDebut.getValue().atStartOfDay();
        LocalDateTime end = dpAjustFin.getValue().plusDays(1).atStartOfDay();
        Produit selProd = cmbAjustProduit != null ? cmbAjustProduit.getValue() : null;
        User selUser = cmbAjustUser != null ? cmbAjustUser.getValue() : null;
        MouvementStock.TypeMouvement selType = cmbAjustType != null ? cmbAjustType.getValue() : null;
        
        List<AjustementStock> allAjustements = ajustementStockDAO.findAllWithDetails();
        currentAjustements = allAjustements.stream()
                .filter(a -> (!a.getDateAjustement().isBefore(start)) && a.getDateAjustement().isBefore(end))
                .filter(a -> selProd == null || a.getLot().getProduit().getId().equals(selProd.getId()))
                .filter(a -> selUser == null || a.getUser().getId().equals(selUser.getId()))
                .filter(a -> {
                    if (selType == null) return true;
                    MouvementStock.TypeMouvement type = a.getTypeAjustement() != null ? a.getTypeAjustement() : MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF;
                    return type == selType;
                })
                .collect(Collectors.toList());
        tableAjustements.setItems(FXCollections.observableArrayList(currentAjustements));
        // Phase 5 : Activer bouton PDF
        if (btnExportPdfAjust != null) btnExportPdfAjust.setDisable(currentAjustements.isEmpty());
    }

    @FXML
    public void genererAudit() {
        if (dpAuditDebut.getValue() == null || dpAuditFin.getValue() == null) return;
        LocalDate debut = dpAuditDebut.getValue();
        LocalDate fin = dpAuditFin.getValue();
        Long prodId = cmbAuditProduit.getValue() != null ? cmbAuditProduit.getValue().getId() : null;
        MouvementStock.TypeMouvement type = cmbAuditType.getValue();
        Long userId = cmbAuditUser.getValue() != null ? cmbAuditUser.getValue().getId() : null;
        List<MouvementStock> res = mouvementDAO.rechercher(debut, fin, prodId, type, userId);
        tableAudit.setItems(FXCollections.observableArrayList(res));
        // Phase 5 : Activer bouton PDF
        if (btnExportPdfAudit != null) btnExportPdfAudit.setDisable(res.isEmpty());
    }

    @FXML
    public void genererClotures() {
        LocalDate debut = dpClotureDebut.getValue() != null ? dpClotureDebut.getValue() : LocalDate.of(2000, 1, 1);
        LocalDate fin = dpClotureFin.getValue() != null ? dpClotureFin.getValue() : LocalDate.of(2100, 1, 1);
        User agentSelect = cmbClotureUser.getValue();
        List<SessionCaisse> list = sessionDAO.findAll();
        currentClotures = list.stream().filter(s -> {
            boolean dateOk = !s.getDateOuverture().toLocalDate().isBefore(debut) && !s.getDateOuverture().toLocalDate().isAfter(fin);
            boolean agentOk = (agentSelect == null) || s.getUser().getId().equals(agentSelect.getId());
            return dateOk && agentOk;
        }).collect(Collectors.toList());
        tableClotures.setItems(FXCollections.observableArrayList(currentClotures));
        // Phase 5 : Activer bouton PDF
        if (btnExportPdfCloture != null) btnExportPdfCloture.setDisable(currentClotures.isEmpty());
    }

    @FXML
    public void exporterCSV() {
        if (currentPeriodLines == null || currentPeriodLines.isEmpty()) {
            com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.WARNING, "Aucune de donnée", "Export impossible", "Aucune donnée à exporter pour cette période.");
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le Journal des Ventes (CSV)");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        fileChooser.setInitialFileName("Journal_Ventes_" + LocalDate.now().toString() + ".csv");
        javafx.stage.Stage stage = (javafx.stage.Stage) tableLignesVente.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("Date,Ticket,Caissier,Produit,Categorie,Espece,Qte,PrixUnitaire,SousTotal");
                for (LigneVente lv : currentPeriodLines) {
                    pw.println(
                        lv.getVente().getDateVente().toLocalDate() + "," +
                        "TCK-" + lv.getVente().getId() + "," +
                        lv.getVente().getUser().getNom() + "," +
                        lv.getProduit().getNom() + "," +
                        lv.getProduit().getCategorie().getNom() + "," +
                        lv.getProduit().getEspece().getNom() + "," +
                        lv.getQuantiteVendue() + "," +
                        lv.getPrixUnitaire() + "," +
                        lv.getSousTotal()
                    );
                }
            }
            com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Succès", "Export terminé", "Export Brut Excel (CSV) généré avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'export CSV : " + e.getMessage());
        }
    }
    
    @FXML
    public void exporterVentesPDF() {
        if (currentPeriodLines == null || currentPeriodLines.isEmpty()) {
            com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.WARNING, "Aucune de donnée", "Export impossible", "Aucune donnée de vente à exporter.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableLignesVente.getScene().getWindow();
        String periode = dpDateDebut.getValue().toString() + " au " + dpDateFin.getValue().toString();
        com.pharmacie.utils.PdfService.genererJournalVentes(currentPeriodLines, periode, stage);
    }

    @FXML
    public void exporterAjustementsPDF() {
        if (currentAjustements == null || currentAjustements.isEmpty()) return;
        javafx.stage.Stage stage = (javafx.stage.Stage) tableAjustements.getScene().getWindow();
        String periode = dpAjustDebut.getValue().toString() + " au " + dpAjustFin.getValue().toString();
        com.pharmacie.utils.PdfService.genererRapportAjustements(currentAjustements, periode, stage);
    }
    
    @FXML
    public void exporterAuditPDF() {
        if (tableAudit.getItems() == null || tableAudit.getItems().isEmpty()) {
            showError("Aucune donnée d'audit à exporter.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableAudit.getScene().getWindow();
        String periode = dpAuditDebut.getValue().toString() + " au " + dpAuditFin.getValue().toString();
        com.pharmacie.utils.PdfService.genererRapportAudit(new java.util.ArrayList<>(tableAudit.getItems()), periode, stage);
    }
    
    @FXML 
    public void exporterCloturesPDF() {
        if (currentClotures == null || currentClotures.isEmpty()) {
            showError("Aucune donnée de clôture à exporter.");
            return;
        }
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) tableClotures.getScene().getWindow();
            String periode = dpClotureDebut.getValue().toString() + " au " + dpClotureFin.getValue().toString();
            com.pharmacie.utils.PdfService.genererRapportClotures(currentClotures, periode, stage);
        } catch(Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la génération PDF : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur", "Opération impossible", msg);
    }
}
