package com.pharmacie.controllers;

import com.pharmacie.dao.StatistiquesDAO;
import com.pharmacie.dao.LotDAO;
import com.pharmacie.dao.ProduitDAO;
import com.pharmacie.models.Lot;
import com.pharmacie.models.Produit;
import com.pharmacie.models.Vente;
import javafx.animation.Transition;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class DashboardController {

    // ── Header ────────────────────────────────────────────────────────
    @FXML private ProgressIndicator progressIndicator;
    @FXML private ComboBox<String>  cmbPeriode;
    @FXML private DatePicker        dpDebut;
    @FXML private DatePicker        dpFin;

    // ── KPI Row 1 ─────────────────────────────────────────────────────
    @FXML private Label lblCA;
    @FXML private Label lblBenefice;
    @FXML private Label lblVolume;
    @FXML private Label lblAlertes;

    // ── KPI Row 2 ─────────────────────────────────────────────────────
    @FXML private Label lblValeurStock;
    @FXML private Label lblEspeces;
    @FXML private Label lblMobile;

    // ── Charts ────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> chartEvolution;
    @FXML private BarChart<String, Number>  barCategorie;
    @FXML private BarChart<String, Number>  barEspece;
    @FXML private BarChart<String, Number>  barPertes;
    @FXML private PieChart                  pieTopProduits;

    // ── Top Produits Table ────────────────────────────────────────────
    @FXML private TableView<TopProduitDTO>       tableTopProduits;
    @FXML private TableColumn<TopProduitDTO, String> colTopNom;
    @FXML private TableColumn<TopProduitDTO, Double> colTopQte;
    @FXML private Label lblDerniereSynchro;

    // ── DAOs ──────────────────────────────────────────────────────────
    private final StatistiquesDAO statsDAO   = new StatistiquesDAO();
    private final LotDAO          lotDAO     = new LotDAO();
    private final ProduitDAO      produitDAO = new ProduitDAO();

    // =================================================================
    // Initialisation
    // =================================================================
    @FXML
    public void initialize() {
        cmbPeriode.setItems(FXCollections.observableArrayList(
                "Aujourd'hui", "7 Derniers Jours", "30 Derniers Jours",
                "Ce Mois", "Cette Année", "Tout"
        ));
        cmbPeriode.getSelectionModel().select("Ce Mois");

        colTopNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colTopQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));

        com.pharmacie.utils.DateUtils.bindDateFilters(dpDebut, dpFin);

        javafx.event.EventHandler<javafx.event.ActionEvent> loadEvent = e -> chargerDonnees();
        dpDebut.setOnAction(loadEvent);
        dpFin.setOnAction(loadEvent);

        // Correction chirurgicale du bug JavaFX (chevauchement des étiquettes Catégorie/Espèce/Motif) :
        // On désactive l'animation sur l'axe X (qui ne gère pas asynchronement les tailles de texte long)
        // tout en conservant animated="true" dans le FXML pour la montée fluide des barres de statistiques.
        barCategorie.getXAxis().setAnimated(false);
        barEspece.getXAxis().setAnimated(false);
        barPertes.getXAxis().setAnimated(false);
        chartEvolution.getXAxis().setAnimated(false);

        chargerDonnees();
    }

    // =================================================================
    // Actions header
    // =================================================================

    /** Quand l'utlisateur change la période prédéfinie → réinitialise les date pickers */
    @FXML
    public void onPeriodeChange() {
        dpDebut.setValue(null);
        dpFin.setValue(null);
        chargerDonnees();
    }

    /** Remet le sélecteur de plage libre à vide et revient à la période du ComboBox */
    @FXML
    public void reinitialiserPlage() {
        dpDebut.setValue(null);
        dpFin.setValue(null);
        chargerDonnees();
    }

    /** Déclenche une synchronisation manuelle vers le cloud (Supabase) */
    @FXML
    public void forcerSynchronisation() {
        if (progressIndicator != null) progressIndicator.setVisible(true);
        if (lblDerniereSynchro != null) {
            lblDerniereSynchro.setText("Synchro en cours...");
            lblDerniereSynchro.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        }
        
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() {
                com.pharmacie.utils.SyncService.synchroniser();
                return null;
            }
        };
        
        task.setOnSucceeded(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            if (lblDerniereSynchro != null) {
                lblDerniereSynchro.setText("Synchro réussie à " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                lblDerniereSynchro.setStyle("-fx-font-size: 11px; -fx-text-fill: #059669;"); // Emerald 600
            }
        });
        
        task.setOnFailed(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            if (lblDerniereSynchro != null) {
                lblDerniereSynchro.setText("Échec de la synchro");
                lblDerniereSynchro.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;"); // Red 600
            }
        });
        
        new Thread(task).start();
    }

    // =================================================================
    // Calcul de la plage temporelle (plage libre prioritaire sur ComboBox)
    // =================================================================
    private LocalDateTime[] getPlageDates() {
        // --- Plage libre : si les deux DatePickers sont renseignés, ils ont la priorité ---
        if (dpDebut.getValue() != null && dpFin.getValue() != null) {
            return new LocalDateTime[]{
                dpDebut.getValue().atStartOfDay(),
                dpFin.getValue().atTime(LocalTime.MAX)
            };
        }

        // --- Sinon : logique du ComboBox ---
        LocalDate today = LocalDate.now();
        LocalDateTime fin = today.atTime(LocalTime.MAX);
        String periode = cmbPeriode.getValue();
        if (periode == null) periode = "Ce Mois";

        LocalDateTime debut = switch (periode) {
            case "Aujourd'hui"       -> today.atStartOfDay();
            case "7 Derniers Jours"  -> today.minusDays(7).atStartOfDay();
            case "30 Derniers Jours" -> today.minusDays(30).atStartOfDay();
            case "Ce Mois"           -> today.withDayOfMonth(1).atStartOfDay();
            case "Cette Année"       -> today.withDayOfYear(1).atStartOfDay();
            default                  -> LocalDateTime.of(2000, 1, 1, 0, 0);
        };
        return new LocalDateTime[]{debut, fin};
    }

    /** Retourne true si la plage couvre plus d'1 mois (→ affichage mensuel sur LineChart) */
    private boolean isMensuel() {
        LocalDateTime[] plage = getPlageDates();
        return plage[1].toLocalDate().isAfter(plage[0].toLocalDate().plusDays(31));
    }

    // =================================================================
    // Point d'entrée — Task asynchrone
    // =================================================================
    @FXML
    public void chargerDonnees() {
        LocalDateTime[] plage  = getPlageDates();
        LocalDateTime   debut  = plage[0];
        LocalDateTime   fin    = plage[1];
        LocalDate       today  = LocalDate.now();
        boolean         mensuel = isMensuel();

        setChargement(true);

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                DashboardData data = new DashboardData();

                // ── KPI Row 1 ──────────────────────────────────────
                data.ca           = statsDAO.getChiffreAffairesTotal(debut, fin);
                data.benefice     = statsDAO.getBeneficeNet(debut, fin);
                data.nbVentes     = statsDAO.getNombreVentes(debut, fin);
                data.alertesKPI   = statsDAO.getAlertesKPI(today);

                // ── KPI Row 2 (nouveaux) ────────────────────────────
                data.valeurStock  = statsDAO.getValeurTotaleStock(today);
                data.ventilationPaiement = statsDAO.getCAParModePaiement(debut, fin);

                // ── Charts ──────────────────────────────────────────
                data.mensuel = mensuel;
                data.horaire = !mensuel && debut.toLocalDate().isEqual(fin.toLocalDate());
                
                if (data.mensuel) {
                    data.evolutionCA     = statsDAO.getEvolutionCAMensuelle(debut, fin);
                    data.evolutionAchats = statsDAO.getEvolutionCoutsAchatsMensuelle(debut, fin);
                } else if (data.horaire) {
                    data.evolutionCA     = statsDAO.getEvolutionCAHoraire(debut, fin);
                    data.evolutionAchats = statsDAO.getEvolutionCoutsAchatsHoraire(debut, fin);
                } else {
                    data.evolutionCA     = statsDAO.getEvolutionCA(debut, fin);
                    data.evolutionAchats = statsDAO.getEvolutionCoutsAchats(debut, fin);
                }
                data.catData   = statsDAO.getCAByCategorie(debut, fin);
                data.espData   = statsDAO.getCAByEspece(debut, fin);
                data.topData   = statsDAO.getTopProduitsVolume(debut, fin, 10);
                data.pertesData = statsDAO.getPertesParMotif(debut, fin);

                return data;
            }
        };

        task.setOnSucceeded(e -> {
            DashboardData data = task.getValue();
            appliquerKPIs(data);
            appliquerGraphiques(data);
            setChargement(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("[Dashboard] Erreur : " + ex.getMessage());
            ex.printStackTrace();
            // --- Feedback visible pour le diagnostic ---
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur Tableau de Bord");
                alert.setHeaderText("Impossible de charger les données");
                alert.setContentText("Cause : " + ex.getMessage()
                    + "\n\nConsultez la console pour le détail.");
                alert.showAndWait();
            });
            setChargement(false);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // =================================================================
    // Application KPIs
    // =================================================================
    private void appliquerKPIs(DashboardData data) {
        // Row 1 — Appliquer avec Animation
        animerValeurMonetaire(lblCA, data.ca);
        animerValeurMonetaire(lblBenefice, data.benefice);
        animerValeurEntiere(lblVolume, data.nbVentes);

        // Row 1 — Alertes (pas d'animation sur les alertes critiques)
        long nbAlertes = data.alertesKPI[0];
        long nbExpires = data.alertesKPI[1];
        lblAlertes.setText(nbAlertes + " / " + nbExpires);
        lblAlertes.setTooltip(new Tooltip("Cliquer pour le détail des alertes"));
        lblAlertes.setCursor(javafx.scene.Cursor.HAND);
        lblAlertes.setOnMouseClicked(ev -> afficherDetailAlertesAsync());

        // Row 2 — Valeur Stock (Animation)
        animerValeurMonetaire(lblValeurStock, data.valeurStock);

        // Row 2 — Ventilation Espèces / Mobile
        double especes = 0.0, mobile = 0.0;
        for (Object[] row : data.ventilationPaiement) {
            if (row[0] == null) continue;
            String mode = row[0].toString();
            double val  = ((Number) row[1]).doubleValue();
            if (mode.equals(Vente.ModePaiement.ESPECES.name()))       especes = val;
            if (mode.equals(Vente.ModePaiement.MOBILE_MONEY.name()))  mobile  = val;
        }
        animerValeurMonetaire(lblEspeces, especes);
        animerValeurMonetaire(lblMobile, mobile);
    }

    // =================================================================
    // Utilitaires UX Premium — Animations Numériques (Compteurs)
    // =================================================================
    private void animerValeurMonetaire(Label lbl, double cible) {
        Transition animation = new Transition() {
            { setCycleDuration(Duration.millis(1200)); } // 1.2 sec
            @Override
            protected void interpolate(double frac) {
                // Interpolation ease-out simple : Math.pow(frac, 0.5) pour un ralentissement à la fin
                double currentFrac = Math.pow(frac, 0.5);
                lbl.setText(String.format("%,.0f FCFA", cible * currentFrac));
            }
        };
        animation.play();
    }

    private void animerValeurEntiere(Label lbl, long cible) {
        Transition animation = new Transition() {
            { setCycleDuration(Duration.millis(1200)); }
            @Override
            protected void interpolate(double frac) {
                double currentFrac = Math.pow(frac, 0.5);
                lbl.setText(String.valueOf(Math.round(cible * currentFrac)));
            }
        };
        animation.play();
    }

    // =================================================================
    // Application des Graphiques
    // =================================================================
    private void appliquerGraphiques(DashboardData data) {

        // ── 1. LineChart : CA + Achats superposés ───────────────────
        chartEvolution.getData().clear();

        XYChart.Series<String, Number> seriesCA = new XYChart.Series<>();
        seriesCA.setName("Chiffre d'Affaires");
        XYChart.Series<String, Number> seriesAchats = new XYChart.Series<>();
        seriesAchats.setName("Coût des Achats");

        if (data.mensuel) {
            for (Object[] row : data.evolutionCA) {
                String label = String.format("%02d/%d",
                    ((Number) row[1]).intValue(), ((Number) row[0]).intValue());
                seriesCA.getData().add(new XYChart.Data<>(label, (Number) row[2]));
            }
            for (Object[] row : data.evolutionAchats) {
                String label = String.format("%02d/%d",
                    ((Number) row[1]).intValue(), ((Number) row[0]).intValue());
                seriesAchats.getData().add(new XYChart.Data<>(label, (Number) row[2]));
            }
        } else if (data.horaire) {
            for (Object[] row : data.evolutionCA) {
                String label = String.format("%02dh", ((Number) row[0]).intValue());
                seriesCA.getData().add(new XYChart.Data<>(label, (Number) row[1]));
            }
            for (Object[] row : data.evolutionAchats) {
                String label = String.format("%02dh", ((Number) row[0]).intValue());
                seriesAchats.getData().add(new XYChart.Data<>(label, (Number) row[1]));
            }
        } else {
            for (Object[] row : data.evolutionCA)
                seriesCA.getData().add(new XYChart.Data<>(row[0].toString(), (Number) row[1]));
            for (Object[] row : data.evolutionAchats)
                seriesAchats.getData().add(new XYChart.Data<>(row[0].toString(), (Number) row[1]));
        }
        chartEvolution.getData().addAll(seriesCA, seriesAchats);

        // ── 2. BarChart Catégorie ────────────────────────────────────
        barCategorie.getData().clear();
        XYChart.Series<String, Number> seriesCat = new XYChart.Series<>();
        seriesCat.setName("CA");
        for (Object[] row : data.catData)
            seriesCat.getData().add(new XYChart.Data<>(
                row[0] != null ? row[0].toString() : "Inconnu", (Number) row[1]));
        barCategorie.getData().add(seriesCat);

        // ── 3. BarChart Espèce ───────────────────────────────────────
        barEspece.getData().clear();
        XYChart.Series<String, Number> seriesEsp = new XYChart.Series<>();
        seriesEsp.setName("CA");
        for (Object[] row : data.espData)
            seriesEsp.getData().add(new XYChart.Data<>(
                row[0] != null ? row[0].toString() : "Inconnu", (Number) row[1]));
        barEspece.getData().add(seriesEsp);

        // ── 4. Top 10 Table + Top 5 PieChart ────────────────────────
        ObservableList<TopProduitDTO> topList = FXCollections.observableArrayList();
        pieTopProduits.getData().clear();
        int count = 0;
        for (Object[] row : data.topData) {
            String name = row[0] != null ? row[0].toString() : "Inconnu";
            Double qte  = ((Number) row[1]).doubleValue();
            topList.add(new TopProduitDTO(name, qte));
            if (count < 5) pieTopProduits.getData().add(new PieChart.Data(name, qte));
            count++;
        }
        tableTopProduits.setItems(topList);

        // ── 5. BarChart Pertes par Motif ─────────────────────────────
        barPertes.getData().clear();
        XYChart.Series<String, Number> seriesPertes = new XYChart.Series<>();
        seriesPertes.setName("Unités retirées");
        for (Object[] row : data.pertesData) {
            String motifLabel = row[0] != null ? row[0].toString() : "Autre";
            // row[0] = enum name → on traduit en label lisible
            motifLabel = traduireMotif(motifLabel);
            Long totalUnites = ((Number) row[2]).longValue();
            seriesPertes.getData().add(new XYChart.Data<>(motifLabel, totalUnites));
        }
        barPertes.getData().add(seriesPertes);
    }

    /** Traduit le nom de l'enum MotifAjustement en libellé lisible */
    private String traduireMotif(String enumName) {
        return switch (enumName) {
            case "CASSE"            -> "Casse";
            case "PEREMPTION"       -> "Péremption";
            case "ERREUR_INVENTAIRE"-> "Erreur inventaire";
            case "USAGE_INTERNE"    -> "Usage interne";
            default                 -> "Autre";
        };
    }

    // =================================================================
    // Détail alertes — Ouverture de la modale Premium
    // =================================================================
    private void afficherDetailAlertesAsync() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/alertes_stock.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Centre d'Alertes - Pharmacie");
            modalStage.setScene(new javafx.scene.Scene(root, 950, 650));
            
            // Bloque l'application principale tant que la modale est ouverte
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            // Empêche le redimensionnement pour conserver le design Premium
            modalStage.setResizable(false);
            
            modalStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[Dashboard] Erreur lors de l'ouverture de la modale d'alertes.");
        }
    }

    // =================================================================
    // Spinner visible/invisible
    // =================================================================
    private void setChargement(boolean enCours) {
        progressIndicator.setVisible(enCours);
        progressIndicator.setManaged(enCours);
        cmbPeriode.setDisable(enCours);
        dpDebut.setDisable(enCours);
        dpFin.setDisable(enCours);
    }

    // =================================================================
    // DTO de transport Task → UI thread
    // =================================================================
    private static class DashboardData {
        double          ca, benefice, valeurStock;
        long            nbVentes;
        long[]          alertesKPI           = {0, 0};
        List<Object[]>  ventilationPaiement;
        boolean         mensuel;
        boolean         horaire;
        List<Object[]>  evolutionCA, evolutionAchats;
        List<Object[]>  catData, espData, topData, pertesData;
    }

    // =================================================================
    // DTO d'affichage — TableView Top Produits
    // =================================================================
    public static class TopProduitDTO {
        private final String nom;
        private final Double quantite;
        public TopProduitDTO(String n, Double q) { this.nom = n; this.quantite = q; }
        public String getNom()      { return nom;      }
        public Double getQuantite() { return quantite; }
    }
}
