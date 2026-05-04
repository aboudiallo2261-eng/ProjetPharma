package com.pharmacie.controllers;

import com.pharmacie.dao.*;
import com.pharmacie.models.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    // --- Indicateurs de chargement asynchrone (un par onglet critique) ---
    @FXML
    private ProgressIndicator progressRapport;
    @FXML
    private ProgressIndicator progressAjust;

    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private ComboBox<User> cmbCaissier;
    @FXML
    private ComboBox<Categorie> cmbCategorie;
    @FXML
    private ComboBox<Espece> cmbEspece;
    @FXML
    private ComboBox<Produit> cmbRapportProduit;
    @FXML
    private Label lblTotalFiltre;
    // Phase 5 : Boutons d'export
    @FXML
    private Button btnExportPdfVentes;
    @FXML
    private Button btnExportCsvVentes;
    @FXML
    private Button btnExportPdfAjust;
    @FXML
    private Button btnExportPdfCloture;
    @FXML
    private Button btnExportExcelCloture;
    @FXML
    private Button btnExportPdfAudit;
    @FXML
    private Button btnExportExcelAudit;

    @FXML
    private TableView<LigneVente> tableLignesVente;
    @FXML
    private TableColumn<LigneVente, String> colLvDate, colLvTicket, colLvAgent, colLvProduit, colLvCat, colLvEsp;
    @FXML
    private TableColumn<LigneVente, Integer> colLvQte;
    @FXML
    private TableColumn<LigneVente, Double> colLvPrix, colLvTotal;

    @FXML
    private DatePicker dpAjustDebut;
    @FXML
    private DatePicker dpAjustFin;
    @FXML
    private ComboBox<Produit> cmbAjustProduit;
    @FXML
    private ComboBox<User> cmbAjustUser;
    @FXML
    private ComboBox<MouvementStock.TypeMouvement> cmbAjustType;
    @FXML
    private TableView<AjustementStock> tableAjustements;
    @FXML
    private TableColumn<AjustementStock, String> colAdjDate, colAdjProduit, colAdjLot, colAdjUser, colAdjMotif,
            colAdjObs;
    @FXML
    private TableColumn<AjustementStock, Integer> colAdjQte;
    @FXML
    private Label lblTotalAjustements;
    @FXML
    private javafx.scene.control.Button btnExportExcelAjust;

    // --- AUDIT DES STOCKS ---
    @FXML
    private DatePicker dpAuditDebut;
    @FXML
    private DatePicker dpAuditFin;
    @FXML
    private ComboBox<Produit> cmbAuditProduit;
    @FXML
    private ComboBox<MouvementStock.TypeMouvement> cmbAuditType;
    @FXML
    private ComboBox<User> cmbAuditUser;
    @FXML
    private TableView<MouvementStock> tableAudit;
    @FXML
    private TableColumn<MouvementStock, String> colAuditDate, colAuditType, colAuditRapport, colAuditQte, colAuditRef,
            colAuditUser;
    @FXML
    private Label lblTotalAudit;

    // --- REGISTRE DES CLOTURES (Z) ---
    @FXML
    private DatePicker dpClotureDebut;
    @FXML
    private DatePicker dpClotureFin;
    @FXML
    private ComboBox<User> cmbClotureUser;
    @FXML
    private ComboBox<String> cmbClotureStatut;
    @FXML
    private ComboBox<String> cmbClotureBilan;
    @FXML
    private TableView<SessionCaisse> tableClotures;
    @FXML
    private TableColumn<SessionCaisse, String> colZDate, colZAgent, colZStatut, colZTheorieEsp, colZTheorieMob;
    @FXML
    private TableColumn<SessionCaisse, Double> colZEcartEsp, colZEcartMob;
    @FXML
    private Label lblTotalClotures;

    // LigneVenteDAO remplace le GenericDAO brut : fournit findAllWithDetails() avec
    // JOIN FETCH qui évite la LazyInitializationException et le problème N+1.
    private LigneVenteDAO ligneVenteDAO = new LigneVenteDAO();
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
        // Verrou global pendant toute la phase d'initialisation.
        // Empêche tout handler FXML-bound (onAction="#genererXxx") de déclencher un
        // chargement de données AVANT que tous les composants soient prêts.
        // Les verrous sont relâchés juste avant les 4 appels initiaux explicites.
        isUpdatingRapportFiltres = true;
        isUpdatingAjustFiltres = true;
        isUpdatingAuditFiltres = true;
        isUpdatingClotureFiltres = true;

        // Initialisation standardisée "Date du Jour" pour tous les modules
        // Un pharmacien consulte en priorité l'activité du jour en cours (UX
        // prioritaire)
        LocalDate today = LocalDate.now();

        dpDateDebut.setValue(today);
        dpDateFin.setValue(today);

        dpAjustDebut.setValue(today);
        dpAjustFin.setValue(today);

        dpAuditDebut.setValue(today);
        dpAuditFin.setValue(today);

        dpClotureDebut.setValue(today);
        dpClotureFin.setValue(today);

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
            @Override
            public String toString(User u) {
                return u != null ? u.getNom() : "Tous les agents";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        };
        cmbCaissier.setConverter(userCvt);
        cmbAjustUser.setConverter(userCvt);
        cmbAuditUser.setConverter(userCvt);
        cmbClotureUser.setConverter(userCvt);
        // Actions automatiques (Live Filtering) avec anti-rebond
        javafx.event.EventHandler<javafx.event.ActionEvent> actRapport = e -> {
            if (!isUpdatingRapportFiltres)
                genererRapport();
        };
        dpDateDebut.setOnAction(actRapport);
        dpDateFin.setOnAction(actRapport);
        cmbCaissier.setOnAction(actRapport);
        cmbCategorie.setOnAction(actRapport);
        cmbEspece.setOnAction(actRapport);

        javafx.event.EventHandler<javafx.event.ActionEvent> actAjust = e -> {
            if (!isUpdatingAjustFiltres)
                genererAjustements();
        };
        dpAjustDebut.setOnAction(actAjust);
        dpAjustFin.setOnAction(actAjust);
        cmbAjustUser.setOnAction(actAjust);
        if (cmbAjustProduit != null)
            cmbAjustProduit.setOnAction(actAjust);

        javafx.event.EventHandler<javafx.event.ActionEvent> actAudit = e -> {
            if (!isUpdatingAuditFiltres)
                genererAudit();
        };
        dpAuditDebut.setOnAction(actAudit);
        dpAuditFin.setOnAction(actAudit);
        cmbAuditUser.setOnAction(actAudit);
        if (cmbAuditProduit != null)
            cmbAuditProduit.setOnAction(actAudit);
        if (cmbAuditType != null)
            cmbAuditType.setOnAction(actAudit);

        javafx.event.EventHandler<javafx.event.ActionEvent> actCloture = e -> {
            if (!isUpdatingClotureFiltres)
                genererClotures();
        };
        dpClotureDebut.setOnAction(actCloture);
        dpClotureFin.setOnAction(actCloture);
        cmbClotureUser.setOnAction(actCloture);

        // Phase 4 : Colonnes adaptatives (CONSTRAINED retiré pour privilégier les
        // prefWidth FXML sur les grands tableaux)

        // Relâchement des verrous : toutes les associations inter-composants sont en
        // place.
        // Les 4 chargements initiaux se déclenchent maintenant proprement.
        isUpdatingRapportFiltres = false;
        isUpdatingAjustFiltres = false;
        isUpdatingAuditFiltres = false;
        isUpdatingClotureFiltres = false;

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
            public String toString(Categorie v) {
                return v == null ? "Toutes les Catégories" : v.getNom();
            }

            public Categorie fromString(String string) {
                return null;
            }
        };
        cmbCategorie.setConverter(catConv);

        javafx.util.StringConverter<Espece> espConv = new javafx.util.StringConverter<Espece>() {
            public String toString(Espece v) {
                return v == null ? "Toutes les Espèces" : v.getNom();
            }

            public Espece fromString(String string) {
                return null;
            }
        };
        cmbEspece.setConverter(espConv);

        if (cmbRapportProduit != null) {
            cmbRapportProduit.getItems().add(null);
            cmbRapportProduit.getItems().addAll(produitDAO.findAll());
        }

        cmbAuditProduit.getItems().add(null);
        cmbAuditProduit.getItems().addAll(produitDAO.findAll());
        cmbAuditType.getItems().add(null);
        cmbAuditType.getItems().addAll(MouvementStock.TypeMouvement.values());

        javafx.util.StringConverter<Produit> prodConv = new javafx.util.StringConverter<Produit>() {
            public String toString(Produit v) {
                return v == null ? "Tous les produits" : v.getNom();
            }

            public Produit fromString(String string) {
                return null;
            }
        };
        cmbAuditProduit.setConverter(prodConv);
        if (cmbRapportProduit != null)
            cmbRapportProduit.setConverter(prodConv);

        javafx.util.StringConverter<MouvementStock.TypeMouvement> typeConv = new javafx.util.StringConverter<>() {
            public String toString(MouvementStock.TypeMouvement v) {
                if (v == null)
                    return "Tous les mouvements";
                return switch (v) {
                    case AJUSTEMENT_POSITIF -> "Ajouts de Stock";
                    case AJUSTEMENT_NEGATIF -> "Perte / Retrait";
                    case ACHAT -> "Achat / Réception";
                    case VENTE -> "Vente";
                    default -> v.name();
                };
            }

            public MouvementStock.TypeMouvement fromString(String string) {
                return null;
            }
        };
        cmbAuditType.setConverter(typeConv);

        if (cmbAjustProduit != null) {
            cmbAjustProduit.getItems().add(null);
            cmbAjustProduit.getItems().addAll(produitDAO.findAll());
            cmbAjustProduit.setConverter(prodConv);
        }

        if (cmbAjustType != null) {
            // Bug #5 : Le ComboBox a un onAction="#genererAjustements" déclaré dans le
            // FXML.
            // Sans ce flag, chaque appel à .getItems().add() déclenche genererAjustements()
            // pendant le chargement initial, causant des appels parasites en cascade.
            isUpdatingAjustFiltres = true;
            cmbAjustType.getItems().add(null);
            cmbAjustType.getItems().add(MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF);
            cmbAjustType.getItems().add(MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF);
            cmbAjustType.setConverter(new javafx.util.StringConverter<MouvementStock.TypeMouvement>() {
                public String toString(MouvementStock.TypeMouvement v) {
                    if (v == null)
                        return "Toutes les opérations";
                    if (v == MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF)
                        return "Retraits / Pertes";
                    if (v == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF)
                        return "Ajouts de Stock";
                    return v.name();
                }

                public MouvementStock.TypeMouvement fromString(String string) {
                    return null;
                }
            });
            isUpdatingAjustFiltres = false; // Relâche le verrou après chargement complet
        }

        if (cmbClotureStatut != null) {
            isUpdatingClotureFiltres = true;
            cmbClotureStatut.getItems().addAll("Tous les statuts", "Fermées (Normal)", "Ouvertes (Anomalie)");
            cmbClotureStatut.getSelectionModel().select(0);
            isUpdatingClotureFiltres = false;
        }

        if (cmbClotureBilan != null) {
            isUpdatingClotureFiltres = true;
            cmbClotureBilan.getItems().addAll("Tous les bilans", "Avec Manquant (Écart négatif)",
                    "Avec Excédent (Écart positif)");
            cmbClotureBilan.getSelectionModel().select(0);
            isUpdatingClotureFiltres = false;
        }
    }

    private void initColumns() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colLvDate
                .setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getVente().getDateVente().format(fmt)));
        colLvTicket.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getVente().getNumeroTicketOfficiel()));
        colLvAgent.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getVente().getUser().getNom()));
        colLvProduit.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getProduit().getNom()));
        colLvCat.setCellValueFactory(v -> {
            String cat = v.getValue().getProduit().getCategorie().getNom();
            return new SimpleStringProperty(
                    cat != null && !cat.isEmpty() ? cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase()
                            : "");
        });
        colLvEsp.setCellValueFactory(v -> {
            String esp = v.getValue().getProduit().getEspece().getNom();
            return new SimpleStringProperty(
                    esp != null && !esp.isEmpty() ? esp.substring(0, 1).toUpperCase() + esp.substring(1).toLowerCase()
                            : "");
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
        // L'ancien .toLowerCase() détruisait la casse des noms composés (ex: "IVOMEC
        // 1%" → "ivomec 1%").
        colAdjProduit.setCellValueFactory(v -> {
            String nom = v.getValue().getLot().getProduit().getNom();
            return new SimpleStringProperty(nom != null ? nom : "");
        });

        colAdjLot.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getLot().getNumeroLot()));
        colAdjUser.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getUser().getNom()));
        colAdjMotif.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getMotif().getLabel()));

        colAdjQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colAdjQte.setCellFactory(column -> new TableCell<AjustementStock, Integer>() {
            {
                // Point 5 : Listener sur la selection de la ligne parente
                tableRowProperty().addListener((obs, oldRow, newRow) -> {
                    if (newRow != null) {
                        newRow.selectedProperty().addListener((obs2, was, isNow) -> {
                            if (!isEmpty())
                                updateItem(getItem(), false);
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                boolean selected = getTableRow() != null && getTableRow().isSelected();
                AjustementStock adj = getTableView().getItems().get(getIndex());
                if (adj != null && adj.getTypeAjustement() == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) {
                    setText("+ " + item);
                    setStyle(selected
                            ? "-fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;"
                            : "-fx-text-fill: #27ae60; -fx-alignment: CENTER; -fx-font-weight: bold;");
                } else {
                    setText("- " + item);
                    setStyle(selected
                            ? "-fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;"
                            : "-fx-text-fill: #c0392b; -fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });

        colAdjObs.setCellValueFactory(new PropertyValueFactory<>("observation"));

        // Bug #3 corrigé : RowFactory avec code couleur par type d'ajustement.
        // AJUSTEMENT_POSITIF (ajout) → fond vert très pâle (#eafaf1)
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
                    if (isSelected() || isHover()) {
                        // Laisse le système appliquer sa couleur de sélection (Emerald)
                        setStyle("");
                        return;
                    }
                    if (item.getTypeAjustement() == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) {
                        setStyle("-fx-background-color: #eafaf1;"); // Vert très pâle
                    } else {
                        setStyle("-fx-background-color: #fdf2f2;"); // Rouge très pâle
                    }
                };
                // Écoute les changements de sélection ET de survol (hover)
                selectedProperty().addListener((obs, wasSelected, isNowSelected) -> applyStyle.run());
                hoverProperty().addListener((obs, wasHovered, isNowHovered) -> applyStyle.run());
                applyStyle.run();
            }
        });
    }

    private void initAuditColumns() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colAuditDate.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getDateMouvement().format(fmt)));

        colAuditType.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getTypeMouvement().name()));
        colAuditType.setCellFactory(column -> new TableCell<MouvementStock, String>() {
            {
                // Point 5 : Re-render quand la ligne est (dé)sélectionnée
                tableRowProperty().addListener((obs, oldRow, newRow) -> {
                    if (newRow != null) {
                        newRow.selectedProperty().addListener((obs2, was, isNow) -> {
                            if (!isEmpty())
                                updateItem(getItem(), false);
                        });
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                boolean selected = getTableRow() != null && getTableRow().isSelected();
                MouvementStock m = getTableView().getItems().get(getIndex());
                if (m != null) {
                    if (selected) {
                        setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                        switch (m.getTypeMouvement()) {
                            case ACHAT:
                                setText("Entrée (Achat)");
                                break;
                            case VENTE:
                                setText("Sortie (Vente)");
                                break;
                            case AJUSTEMENT_POSITIF:
                                setText("Ajouts de Stock");
                                break;
                            case AJUSTEMENT_NEGATIF:
                                setText("Perte / Retrait");
                                break;
                            default:
                                setText(m.getTypeMouvement().name());
                                break;
                        }
                    } else {
                        switch (m.getTypeMouvement()) {
                            case ACHAT:
                                setText("Entrée (Achat)");
                                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                                break;
                            case VENTE:
                                setText("Sortie (Vente)");
                                setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                                break;
                            case AJUSTEMENT_POSITIF:
                                setText("Ajouts de Stock");
                                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
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
            boolean estVenteDetail = m.getReference() != null && m.getReference().contains("(Vente au Détail)");
            
            // Si c'est explicitement une vente au détail, on force l'affichage en unités.
            if (!estVenteDetail && p.getEstDeconditionnable() != null && p.getEstDeconditionnable() && p.getUnitesParBoite() != null
                    && p.getUnitesParBoite() > 0) {
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
        colZDate.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        "Du " + dtf.format(cellData.getValue().getDateOuverture()) + "\nAu " +
                                (cellData.getValue().getDateCloture() != null
                                        ? dtf.format(cellData.getValue().getDateCloture())
                                        : "En cours")));
        colZAgent.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUser().getNom()));
        colZStatut
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut().toString()));
        colZTheorieEsp.setCellValueFactory(cellData -> {
            String total = String.format("%,.0f FCFA", cellData.getValue().getTotalEspecesAttendu());
            String fond = cellData.getValue().getFondDeCaisse() > 0
                    ? " (incl. " + String.format("%,.0f", cellData.getValue().getFondDeCaisse()) + " fond)"
                    : "";
            return new SimpleStringProperty(total + fond);
        });
        colZTheorieMob.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%,.0f FCFA",
                cellData.getValue().getTotalMobileAttendu() != null ? cellData.getValue().getTotalMobileAttendu()
                        : 0.0)));
        colZEcartEsp.setCellValueFactory(new PropertyValueFactory<>("ecartEspeces"));
        configureEcartColumn(colZEcartEsp);
        colZEcartMob.setCellValueFactory(new PropertyValueFactory<>("ecartMobile"));
        configureEcartColumn(colZEcartMob);
    }

    private void configureEcartColumn(TableColumn<SessionCaisse, Double> col) {
        col.setCellFactory(column -> new TableCell<SessionCaisse, Double>() {
            {
                // Point 5 : Texte lisible sur fond bleu de selection
                tableRowProperty().addListener((obs, oldRow, newRow) -> {
                    if (newRow != null) {
                        newRow.selectedProperty().addListener((obs2, was, isNow) -> {
                            if (!isEmpty())
                                updateItem(getItem(), false);
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                    return;
                }
                boolean selected = getTableRow() != null && getTableRow().isSelected();
                double val = item != null ? item : 0.0;
                setText(String.format("%,.0f FCFA", val));
                if (selected) {
                    setTextFill(Color.web("#0F172A"));
                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
                } else if (val < 0) {
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
        });
    }

    @FXML
    public void resetRapportVente() {
        isUpdatingRapportFiltres = true;
        dpDateDebut.setValue(LocalDate.now());
        dpDateFin.setValue(LocalDate.now());
        cmbCaissier.getSelectionModel().select(0);
        cmbCategorie.getSelectionModel().select(0);
        cmbEspece.getSelectionModel().select(0);
        if (cmbRapportProduit != null)
            cmbRapportProduit.getSelectionModel().select(0);
        isUpdatingRapportFiltres = false;
        genererRapport();
    }

    @FXML
    public void resetAudit() {
        isUpdatingAuditFiltres = true;
        dpAuditDebut.setValue(LocalDate.now());
        dpAuditFin.setValue(LocalDate.now());
        if (cmbAuditProduit != null)
            cmbAuditProduit.getSelectionModel().select(0);
        if (cmbAuditType != null)
            cmbAuditType.getSelectionModel().select(0);
        if (cmbAuditUser != null)
            cmbAuditUser.getSelectionModel().select(0);
        isUpdatingAuditFiltres = false;
        genererAudit();
    }

    @FXML
    public void resetAjustements() {
        isUpdatingAjustFiltres = true;
        dpAjustDebut.setValue(LocalDate.now());
        dpAjustFin.setValue(LocalDate.now());
        if (cmbAjustProduit != null)
            cmbAjustProduit.getSelectionModel().select(0);
        if (cmbAjustUser != null)
            cmbAjustUser.getSelectionModel().select(0);
        if (cmbAjustType != null)
            cmbAjustType.getSelectionModel().select(0);
        isUpdatingAjustFiltres = false;
        genererAjustements();
    }

    @FXML
    public void resetClotures() {
        isUpdatingClotureFiltres = true;
        dpClotureDebut.setValue(LocalDate.now());
        dpClotureFin.setValue(LocalDate.now());
        if (cmbClotureUser != null)
            cmbClotureUser.getSelectionModel().select(0);
        if (cmbClotureStatut != null)
            cmbClotureStatut.getSelectionModel().select(0);
        if (cmbClotureBilan != null)
            cmbClotureBilan.getSelectionModel().select(0);
        isUpdatingClotureFiltres = false;
        genererClotures();
    }

    @FXML
    public void genererRapport() {
        // Guard : bloque les appels spurieux pendant l'initialisation (verrou global)
        // et empêche le lancement de deux Tasks concurrents si l'utilisateur clique
        // rapidement plusieurs fois sur "Filtrer".
        if (isUpdatingRapportFiltres)
            return;
        if (dpDateDebut.getValue() == null || dpDateFin.getValue() == null)
            return;

        // OBLIGATOIRE : les propriétés JavaFX ne sont accessibles que depuis l'UI
        // Thread.
        // On capture les valeurs AVANT de créer le Task background.
        final LocalDateTime start = dpDateDebut.getValue().atStartOfDay();
        final LocalDateTime end = dpDateFin.getValue().plusDays(1).atStartOfDay();
        final User selUser = cmbCaissier.getValue();
        final Categorie selCat = cmbCategorie.getValue();
        final Espece selEsp = cmbEspece.getValue();
        final Produit selProd = cmbRapportProduit != null ? cmbRapportProduit.getValue() : null;

        setRapportLoading(true);

        Task<List<LigneVente>> task = new Task<>() {
            @Override
            protected List<LigneVente> call() {
                Long userId = selUser != null ? selUser.getId() : null;
                Long catId = selCat != null ? selCat.getId() : null;
                Long espId = selEsp != null ? selEsp.getId() : null;
                Long prodId = selProd != null ? selProd.getId() : null;

                // P2.C Scalabilité : Délégation de la recherche et du filtrage à la base de
                // données via HQL.
                // On évite un OutOfMemoryError en production car la base ne retourne que les
                // objets ciblés.
                return ligneVenteDAO.rechercherLignesVente(start, end, userId, catId, espId, prodId);
            }
        };

        task.setOnSucceeded(e -> {
            currentPeriodLines = task.getValue();
            double filteredCA = currentPeriodLines.stream().mapToDouble(LigneVente::getSousTotal).sum();
            com.pharmacie.utils.AnimationUtils.animerValeurMonetaire(lblTotalFiltre, filteredCA, "");
            tableLignesVente.setItems(FXCollections.observableArrayList(currentPeriodLines));
            boolean hasData = !currentPeriodLines.isEmpty();
            if (btnExportPdfVentes != null)
                btnExportPdfVentes.setDisable(!hasData);
            if (btnExportCsvVentes != null)
                btnExportCsvVentes.setDisable(!hasData);
            setRapportLoading(false);
        });

        task.setOnFailed(e -> {
            log.error("[Rapports] Erreur chargement journal des ventes", task.getException());
            setRapportLoading(false);
        });

        Thread t = new Thread(task);
        t.setDaemon(true); // Le thread ne bloque pas la fermeture de l'application
        t.start();
    }

    /**
     * Active ou désactive le mode chargement de l'onglet Journal des Ventes.
     * Affiche le spinner et désactive tous les contrôles de filtre pendant
     * l'exécution du Task pour empêcher les requêtes concurrentes.
     */
    private void setRapportLoading(boolean loading) {
        if (progressRapport != null) {
            progressRapport.setVisible(loading);
            progressRapport.setManaged(loading);
        }
        dpDateDebut.setDisable(loading);
        dpDateFin.setDisable(loading);
        cmbCaissier.setDisable(loading);
        cmbCategorie.setDisable(loading);
        cmbEspece.setDisable(loading);
    }

    @FXML
    public void genererAjustements() {
        // Guard N°1 : déclenché par onAction FXML-bound de cmbAjustType pendant
        // loadFilters().
        // Guard N°2 : empêche deux Tasks concurrents.
        if (isUpdatingAjustFiltres)
            return;
        if (dpAjustDebut.getValue() == null || dpAjustFin.getValue() == null)
            return;

        // Capture UI Thread → Task (thread-safety JavaFX).
        final LocalDateTime start = dpAjustDebut.getValue().atStartOfDay();
        final LocalDateTime end = dpAjustFin.getValue().plusDays(1).atStartOfDay();
        final Produit selProd = cmbAjustProduit != null ? cmbAjustProduit.getValue() : null;
        final User selUser = cmbAjustUser != null ? cmbAjustUser.getValue() : null;
        final MouvementStock.TypeMouvement selType = cmbAjustType != null ? cmbAjustType.getValue() : null;

        setAjustLoading(true);

        Task<List<AjustementStock>> task = new Task<>() {
            @Override
            protected List<AjustementStock> call() {
                List<AjustementStock> allAjustements = ajustementStockDAO.findAllWithDetails();
                return allAjustements.stream()
                        .filter(a -> (!a.getDateAjustement().isBefore(start))
                                && a.getDateAjustement().isBefore(end))
                        .filter(a -> selProd == null
                                || a.getLot().getProduit().getId().equals(selProd.getId()))
                        .filter(a -> selUser == null
                                || a.getUser().getId().equals(selUser.getId()))
                        .filter(a -> {
                            if (selType == null)
                                return true;
                            MouvementStock.TypeMouvement type = a.getTypeAjustement() != null
                                    ? a.getTypeAjustement()
                                    : MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF;
                            return type == selType;
                        })
                        .collect(Collectors.toList());
            }
        };

        task.setOnSucceeded(e -> {
            currentAjustements = task.getValue();
            tableAjustements.setItems(FXCollections.observableArrayList(currentAjustements));
            // Mise à jour du compteur — même pattern que lblTotalFiltre du Journal des
            // Ventes
            if (lblTotalAjustements != null) {
                int count = currentAjustements.size();
                lblTotalAjustements.setText(count + (count > 1 ? " enregistrements" : " enregistrement"));
            }
            if (btnExportPdfAjust != null)
                btnExportPdfAjust.setDisable(currentAjustements.isEmpty());
            if (btnExportExcelAjust != null)
                btnExportExcelAjust.setDisable(currentAjustements.isEmpty());
            setAjustLoading(false);
        });

        task.setOnFailed(e -> {
            log.error("[Rapports] Erreur chargement historique ajustements", task.getException());
            setAjustLoading(false);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Active ou désactive le mode chargement de l'onglet Historique des
     * Ajustements.
     */
    private void setAjustLoading(boolean loading) {
        if (progressAjust != null) {
            progressAjust.setVisible(loading);
            progressAjust.setManaged(loading);
        }
        dpAjustDebut.setDisable(loading);
        dpAjustFin.setDisable(loading);
        if (cmbAjustProduit != null)
            cmbAjustProduit.setDisable(loading);
        if (cmbAjustUser != null)
            cmbAjustUser.setDisable(loading);
        if (cmbAjustType != null)
            cmbAjustType.setDisable(loading);
    }

    @FXML
    public void genererAudit() {
        if (dpAuditDebut.getValue() == null || dpAuditFin.getValue() == null)
            return;
        LocalDate debut = dpAuditDebut.getValue();
        LocalDate fin = dpAuditFin.getValue();
        Long prodId = cmbAuditProduit.getValue() != null ? cmbAuditProduit.getValue().getId() : null;
        MouvementStock.TypeMouvement type = cmbAuditType.getValue();
        Long userId = cmbAuditUser.getValue() != null ? cmbAuditUser.getValue().getId() : null;
        List<MouvementStock> res = mouvementDAO.rechercher(debut, fin, prodId, type, userId);
        tableAudit.setItems(FXCollections.observableArrayList(res));
        // Mise à jour du compteur dynamique — même pattern que les autres onglets
        if (lblTotalAudit != null) {
            int count = res.size();
            lblTotalAudit.setText(count + (count > 1 ? " mouvements" : " mouvement"));
        }
        // Phase 5 : Activer bouton PDF & Excel
        if (btnExportPdfAudit != null)
            btnExportPdfAudit.setDisable(res.isEmpty());
        if (btnExportExcelAudit != null)
            btnExportExcelAudit.setDisable(res.isEmpty());
    }

    @FXML
    public void genererClotures() {
        if (isUpdatingClotureFiltres)
            return;

        LocalDate debut = dpClotureDebut.getValue() != null ? dpClotureDebut.getValue() : LocalDate.of(2000, 1, 1);
        LocalDate fin = dpClotureFin.getValue() != null ? dpClotureFin.getValue() : LocalDate.of(2100, 1, 1);
        User agentSelect = cmbClotureUser.getValue();

        String statutSelect = cmbClotureStatut != null ? cmbClotureStatut.getValue() : "Tous les statuts";
        String bilanSelect = cmbClotureBilan != null ? cmbClotureBilan.getValue() : "Tous les bilans";

        List<SessionCaisse> list = sessionDAO.findAll();
        currentClotures = list.stream().filter(s -> {
            boolean dateOk = !s.getDateOuverture().toLocalDate().isBefore(debut)
                    && !s.getDateOuverture().toLocalDate().isAfter(fin);
            boolean agentOk = (agentSelect == null) || s.getUser().getId().equals(agentSelect.getId());

            boolean statutOk = true;
            if ("Fermées (Normal)".equals(statutSelect))
                statutOk = s.getStatut() == com.pharmacie.models.SessionCaisse.StatutSession.FERMEE;
            else if ("Ouvertes (Anomalie)".equals(statutSelect))
                statutOk = s.getStatut() == com.pharmacie.models.SessionCaisse.StatutSession.OUVERTE;

            boolean bilanOk = true;
            if ("Avec Manquant (Écart négatif)".equals(bilanSelect)) {
                bilanOk = (s.getEcartEspeces() != null && s.getEcartEspeces() < 0)
                        || (s.getEcartMobile() != null && s.getEcartMobile() < 0);
            } else if ("Avec Excédent (Écart positif)".equals(bilanSelect)) {
                bilanOk = (s.getEcartEspeces() != null && s.getEcartEspeces() > 0)
                        || (s.getEcartMobile() != null && s.getEcartMobile() > 0);
            }

            return dateOk && agentOk && statutOk && bilanOk;
        }).collect(Collectors.toList());

        tableClotures.setItems(FXCollections.observableArrayList(currentClotures));
        // Mise à jour du compteur de sessions — même pattern que les autres onglets
        if (lblTotalClotures != null) {
            int count = list.size();
            lblTotalClotures.setText(count + (count > 1 ? " clôtures" : " clôture"));
        }

        if (btnExportPdfCloture != null)
            btnExportPdfCloture.setDisable(list.isEmpty());
        if (btnExportExcelCloture != null)
            btnExportExcelCloture.setDisable(list.isEmpty());
    }

    @FXML
    public void exporterCSV() {
        if (currentPeriodLines == null || currentPeriodLines.isEmpty()) {
            com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.WARNING,
                    "Aucune de donnée", "Export impossible", "Aucune donnée à exporter pour cette période.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableLignesVente.getScene().getWindow();
        com.pharmacie.utils.ExcelExportService.genererJournalVentesExcel(currentPeriodLines, stage);
    }

    @FXML
    public void exporterVentesPDF() {
        if (currentPeriodLines == null || currentPeriodLines.isEmpty()) {
            com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.WARNING,
                    "Aucune de donnée", "Export impossible", "Aucune donnée de vente à exporter.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableLignesVente.getScene().getWindow();
        String periode = dpDateDebut.getValue().toString() + " au " + dpDateFin.getValue().toString();
        com.pharmacie.utils.PdfService.genererJournalVentes(currentPeriodLines, periode, stage);
    }

    @FXML
    public void exporterAjustementsPDF() {
        if (currentAjustements == null || currentAjustements.isEmpty())
            return;
        javafx.stage.Stage stage = (javafx.stage.Stage) tableAjustements.getScene().getWindow();
        String periode = dpAjustDebut.getValue().toString() + " au " + dpAjustFin.getValue().toString();
        MouvementStock.TypeMouvement operation = cmbAjustType != null ? cmbAjustType.getValue() : null;
        com.pharmacie.utils.PdfService.genererRapportAjustements(currentAjustements, periode, operation, stage);
    }

    @FXML
    public void exporterAjustementsExcel() {
        if (currentAjustements == null || currentAjustements.isEmpty())
            return;
        javafx.stage.Stage stage = (javafx.stage.Stage) tableAjustements.getScene().getWindow();
        String periode = dpAjustDebut.getValue().toString() + "_au_" + dpAjustFin.getValue().toString();
        MouvementStock.TypeMouvement operation = cmbAjustType != null ? cmbAjustType.getValue() : null;
        com.pharmacie.utils.ExcelExportService.genererRapportAjustementsExcel(currentAjustements, periode, operation,
                stage);
    }

    @FXML
    public void exporterAuditPDF() {
        if (tableAudit.getItems() == null || tableAudit.getItems().isEmpty()) {
            showError("Aucune donnée d'audit à exporter.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableAudit.getScene().getWindow();
        String periode = dpAuditDebut.getValue().toString() + " au " + dpAuditFin.getValue().toString();
        com.pharmacie.utils.PdfService.genererRapportAudit(new java.util.ArrayList<>(tableAudit.getItems()), periode,
                stage);
    }

    @FXML
    public void exporterAuditExcel() {
        if (tableAudit.getItems() == null || tableAudit.getItems().isEmpty()) {
            showError("Aucune donnée d'audit à exporter.");
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) tableAudit.getScene().getWindow();
        String periode = dpAuditDebut.getValue().toString() + "_au_" + dpAuditFin.getValue().toString();
        com.pharmacie.utils.ExcelExportService.genererAuditStocksExcel(new java.util.ArrayList<>(tableAudit.getItems()),
                periode, stage);
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
        } catch (Exception e) {
            log.error("Erreur lors de la génération PDF", e);
            showError("Erreur lors de la génération PDF : " + e.getMessage());
        }
    }

    @FXML
    public void exporterCloturesExcel() {
        if (currentClotures == null || currentClotures.isEmpty()) {
            showError("Aucune donnée de clôture à exporter.");
            return;
        }
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) tableClotures.getScene().getWindow();
            String periode = dpClotureDebut.getValue().toString() + "_au_" + dpClotureFin.getValue().toString();
            com.pharmacie.utils.ExcelExportService.genererCloturesCaisseExcel(currentClotures, periode, stage);
        } catch (Exception e) {
            log.error("Erreur lors de la génération Excel", e);
            showError("Erreur lors de la génération Excel : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                "Opération impossible", msg);
    }
}
