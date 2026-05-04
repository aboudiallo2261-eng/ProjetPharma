package com.pharmacie.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pharmacie.dao.StatistiquesDAO;
import com.pharmacie.models.dto.DashboardSyncDTO;
import com.pharmacie.models.dto.ProduitRuptureDTO;
import com.pharmacie.models.dto.ProduitPerimeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Phase 2 — Moteur de Synchronisation du Jumeau Numérique (DésIAtisation).
 *
 * Ce service remplit deux responsabilités :
 *  1. Un démon de fond (Timer) qui tente la synchro Cloud toutes les 5 min si Internet est disponible.
 *  2. Une méthode statique `synchroniser()` appelée à chaque clôture de caisse pour
 *     écrire le snapshot JSON local dans le dossier "sync/".
 *
 * Architecture : UNIDIRECTIONNELLE (Local MySQL → JSON → Cloud).
 * Aucune donnée sensible n'est exposée (noms de patients, montants individuels).
 */
public class SyncService {

    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);
    private static final String SYNC_DIR = "sync";
    private static final String SYNC_FILE = "dashboard_snapshot.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final StatistiquesDAO statsDAO = new StatistiquesDAO();

    // La configuration Supabase est désormais lue de manière sécurisée via ConfigService

    // --- Démon de synchronisation Cloud (inchangé) ---
    private Timer timer;

    public void startSyncDaemon() {
        timer = new Timer(true); // Thread Daemon (s'arrête avec l'application)
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (isInternetAvailable()) {
                        logger.info("Internet détecté. Début de la synchronisation Cloud...");
                        synchroniser(); // Génère le JSON local + tentative upload
                    } else {
                        logger.warn("Pas d'accès Internet. Fonctionnement 100% hors-ligne maintenu.");
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors du thread de synchronisation", e);
                }
            }
        }, 10000, 300000); // Délai initial 10s, puis toutes les 5 min
    }

    public void stopSyncDaemon() {
        if (timer != null) {
            timer.cancel();
            logger.info("Service de synchronisation arrêté.");
        }
    }

    private boolean isInternetAvailable() {
        try {
            URL url = new URL("http://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // --- Moteur de Synchronisation Locale (Phase 2 V2) ---

    /**
     * Point d'entrée principal.
     * Construit le DashboardSyncDTO depuis la DB locale et l'écrit en JSON dans sync/.
     * Conçu pour être appelé dans un Thread/Task JavaFX (jamais sur le Thread UI).
     */
    public static void synchroniser() {
        logger.info("Démarrage extraction agrégats Jumeau Numérique...");
        AuditLogger.log("Synchro Dashboard", "STARTED");

        try {
            DashboardSyncDTO dto = construireSnapshot();
            ecrireJson(dto);
            
            // Phase 3 : Envoi silencieux vers le miroir Cloud
            envoyerVersCloud(dto);
            
            AuditLogger.log("Synchro Dashboard", "SUCCESS");
            logger.info("Snapshot JSON écrit dans {}/{}", SYNC_DIR, SYNC_FILE);
        } catch (Exception e) {
            AuditLogger.log("Synchro Dashboard", "ERROR: " + e.getMessage());
            logger.error("Erreur lors de la synchronisation du dashboard", e);
        }
    }

    /**
     * Collecte les KPI depuis les DAOs existants. Aucune requête dupliquée :
     * on réutilise à 100% les méthodes déjà codées dans StatistiquesDAO.
     */
    private static DashboardSyncDTO construireSnapshot() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // Périodes actuelles
        LocalDateTime debutJour = today.atStartOfDay();
        LocalDateTime debutMois = today.withDayOfMonth(1).atStartOfDay();

        // Périodes précédentes (pour la comparaison)
        LocalDateTime debutHier = today.minusDays(1).atStartOfDay();
        LocalDateTime finHier = today.minusDays(1).atTime(23, 59, 59);
        LocalDateTime debutMoisDernier = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMoisDernier = today.withDayOfMonth(1).minusDays(1).atTime(23, 59, 59);

        // Période annuelle : 1er jan de l'année en cours → maintenant
        LocalDateTime debutAnnee = today.withDayOfYear(1).atStartOfDay();
        // Même période l'année précédente (1er jan N-1 → même jour/mois N-1)
        LocalDateTime debutAnneePrecedente = today.minusYears(1).withDayOfYear(1).atStartOfDay();
        LocalDateTime finAnneePrecedente = today.minusYears(1).atTime(23, 59, 59);

        DashboardSyncDTO dto = new DashboardSyncDTO();
        dto.setDateSynchro(maintenant.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // --- 1. Construction des KPIs ---
        DashboardSyncDTO.KpisDTO kpis = new DashboardSyncDTO.KpisDTO();

        // KPI Jour
        DashboardSyncDTO.KpiFinancierDTO kpiJour = new DashboardSyncDTO.KpiFinancierDTO();
        double caJour = statsDAO.getChiffreAffairesTotal(debutJour, maintenant);
        double caHier = statsDAO.getChiffreAffairesTotal(debutHier, finHier);
        
        kpiJour.setChiffreAffaire(Math.round(caJour));
        kpiJour.setBenefice(Math.round(statsDAO.getBeneficeNet(debutJour, maintenant)));
        kpiJour.setVentesRealisees(statsDAO.getNombreVentes(debutJour, maintenant).intValue());
        kpiJour.setEvolutionCA(calculerEvolution(caJour, caHier));
        kpis.setJour(kpiJour);

        // KPI Mois
        DashboardSyncDTO.KpiFinancierDTO kpiMois = new DashboardSyncDTO.KpiFinancierDTO();
        double caMois = statsDAO.getChiffreAffairesTotal(debutMois, maintenant);
        double caMoisDernier = statsDAO.getChiffreAffairesTotal(debutMoisDernier, finMoisDernier);

        kpiMois.setChiffreAffaire(Math.round(caMois));
        kpiMois.setBenefice(Math.round(statsDAO.getBeneficeNet(debutMois, maintenant)));
        kpiMois.setVentesRealisees(statsDAO.getNombreVentes(debutMois, maintenant).intValue());
        kpiMois.setEvolutionCA(calculerEvolution(caMois, caMoisDernier));
        kpis.setMois(kpiMois);

        // KPI Année (1er jan → aujourd'hui, comparé à la même période l'an dernier)
        DashboardSyncDTO.KpiFinancierDTO kpiAnnee = new DashboardSyncDTO.KpiFinancierDTO();
        double caAnnee = statsDAO.getChiffreAffairesTotal(debutAnnee, maintenant);
        double caAnneePrecedente = statsDAO.getChiffreAffairesTotal(debutAnneePrecedente, finAnneePrecedente);
        kpiAnnee.setChiffreAffaire(Math.round(caAnnee));
        kpiAnnee.setBenefice(Math.round(statsDAO.getBeneficeNet(debutAnnee, maintenant)));
        kpiAnnee.setVentesRealisees(statsDAO.getNombreVentes(debutAnnee, maintenant).intValue());
        kpiAnnee.setEvolutionCA(calculerEvolution(caAnnee, caAnneePrecedente));
        kpis.setAnnee(kpiAnnee);

        // KPI Stock
        DashboardSyncDTO.KpiStockDTO kpiStock = new DashboardSyncDTO.KpiStockDTO();
        kpiStock.setValeurTotale(Math.round(statsDAO.getValeurTotaleStock(today)));
        long[] alertesKpi = statsDAO.getDashboardWebAlertesKPI(today);
        kpiStock.setNombreRuptures((int) alertesKpi[0]);
        kpiStock.setNombreAlerteStock((int) alertesKpi[1]);
        kpiStock.setNombrePerimes((int) alertesKpi[2]);
        kpis.setStock(kpiStock);

        dto.setKpis(kpis);

        // --- 2. Construction des Alertes détaillées ---
        DashboardSyncDTO.AlertesDTO alertes = new DashboardSyncDTO.AlertesDTO();

        // Pertes du jour
        List<Object[]> pertesData = statsDAO.getPertesDuJourDetails(today);
        List<com.pharmacie.models.dto.PerteDetailDTO> pertesList = new ArrayList<>();
        long pertesValeurJour = 0L;
        for (Object[] row : pertesData) {
            String produit = (String) row[0];
            String numeroLot = (String) row[1];
            int quantite = ((Number) row[2]).intValue();
            double prixAchat = (row[3] != null) ? ((Number) row[3]).doubleValue() : 0.0;
            String motif = (String) row[4];
            Boolean decond = (Boolean) row[5];
            Integer unitesParBoite = (row[6] != null) ? ((Number) row[6]).intValue() : 1;
            
            double valeurLigne = 0.0;
            if (Boolean.TRUE.equals(decond) && unitesParBoite != null && unitesParBoite > 0) {
                valeurLigne = quantite * (prixAchat / unitesParBoite);
            } else {
                valeurLigne = quantite * prixAchat;
            }
            long valeur = Math.round(valeurLigne);

            pertesValeurJour += valeur;
            pertesList.add(new com.pharmacie.models.dto.PerteDetailDTO(produit, numeroLot, quantite, valeur, motif));
        }
        alertes.setPertes(pertesList);
        kpiJour.setPertesValeur(pertesValeurJour);

        // Top Produits (Volume + Marge Réelle) - Jour
        dto.setTopProduitsJour(construireTopProduits(statsDAO.getTopProduitsVolume(debutJour, maintenant, 5)));

        // Top Produits (Volume + Marge Réelle) - Mois
        dto.setTopProduitsMois(construireTopProduits(statsDAO.getTopProduitsVolume(debutMois, maintenant, 5)));

        // Top Produits (Volume + Marge Réelle) - Annee
        dto.setTopProduitsAnnee(construireTopProduits(statsDAO.getTopProduitsVolume(debutAnnee, maintenant, 5)));

        // Historique 7 jours
        List<Object[]> hist7jData = statsDAO.getEvolutionCA(debutJour.minusDays(6), finHier);
        List<com.pharmacie.models.dto.HistoriqueCADTO> hist7jList = new ArrayList<>();
        for (Object[] row : hist7jData) {
            String dateStr = row[0].toString();
            long ca = (row[1] != null) ? ((Number) row[1]).longValue() : 0L;
            hist7jList.add(new com.pharmacie.models.dto.HistoriqueCADTO(dateStr, ca));
        }
        hist7jList.add(new com.pharmacie.models.dto.HistoriqueCADTO(today.toString(), kpiJour.getChiffreAffaire()));
        dto.setHistorique7Jours(hist7jList);

        // Historique 3 mois
        List<Object[]> hist3mData = statsDAO.getEvolutionCAMensuelle(debutMois.minusMonths(2), maintenant);
        List<com.pharmacie.models.dto.HistoriqueCADTO> hist3mList = new ArrayList<>();
        for (Object[] row : hist3mData) {
            String monthStr = row[0].toString() + "-" + String.format("%02d", ((Number) row[1]).intValue());
            long ca = (row[2] != null) ? ((Number) row[2]).longValue() : 0L;
            hist3mList.add(new com.pharmacie.models.dto.HistoriqueCADTO(monthStr, ca));
        }
        dto.setHistorique3Mois(hist3mList);

        // Historique 3 ans
        List<Object[]> hist3aData = statsDAO.getHistoriqueCA3Ans(today);
        List<com.pharmacie.models.dto.HistoriqueCADTO> hist3aList = new ArrayList<>();
        for (Object[] row : hist3aData) {
            String yearStr = row[0].toString();
            long ca = (row[1] != null) ? ((Number) row[1]).longValue() : 0L;
            hist3aList.add(new com.pharmacie.models.dto.HistoriqueCADTO(yearStr, ca));
        }
        dto.setHistorique3Ans(hist3aList);

        List<Object[]> ruptures = statsDAO.getProduitsEnRuptureTotale(10);
        List<ProduitRuptureDTO> rupturesList = new ArrayList<>();
        for (Object[] row : ruptures) {
            Long id = (Long) row[0];
            String nom = (String) row[1];
            int stock = ((Number) row[2]).intValue();
            rupturesList.add(new ProduitRuptureDTO(id, nom, stock));
        }
        alertes.setRuptures(rupturesList);

        // Alertes stock
        List<Object[]> alertesStockData = statsDAO.getProduitsEnAlerte(10);
        List<ProduitRuptureDTO> alertesStockList = new ArrayList<>();
        for (Object[] row : alertesStockData) {
            Long id = (Long) row[0];
            String nom = (String) row[1];
            int stock = ((Number) row[2]).intValue();
            alertesStockList.add(new ProduitRuptureDTO(id, nom, stock));
        }
        alertes.setAlertesStock(alertesStockList);

        List<Object[]> perimes = statsDAO.getLotsPerimes(today);
        List<ProduitPerimeDTO> perimesList = new ArrayList<>();
        double valeurPerimesTotale = 0.0;
        for (Object[] row : perimes) {
            String nom = (String) row[0];
            String numeroLot = (String) row[1];
            String dateExp = (row[2] != null) ? row[2].toString() : "";
            int stock = ((Number) row[3]).intValue();
            double prixAchat = (row[4] != null) ? ((Number) row[4]).doubleValue() : 0.0;
            Boolean decond = (Boolean) row[5];
            Integer unitesParBoite = (row[6] != null) ? ((Number) row[6]).intValue() : 1;
            
            double valeurLigne = 0.0;
            if (Boolean.TRUE.equals(decond) && unitesParBoite != null && unitesParBoite > 0) {
                valeurLigne = stock * (prixAchat / unitesParBoite);
            } else {
                valeurLigne = stock * prixAchat;
            }
            
            valeurPerimesTotale += valeurLigne;
            perimesList.add(new ProduitPerimeDTO(nom, numeroLot, dateExp, stock));
        }
        alertes.setPerimes(perimesList);
        kpiStock.setValeurPerimes(Math.round(valeurPerimesTotale));

        // Proches de la péremption (Anticipation : <= 60 jours selon demande utilisateur)
        List<Object[]> prochePeremptions = statsDAO.getLotsProchePeremption(today, 60);
        List<ProduitPerimeDTO> prochePeremptionsList = new ArrayList<>();
        double valeurARisqueTotale = 0.0;
        for (Object[] row : prochePeremptions) {
            String nom = (String) row[0];
            String numeroLot = (String) row[1];
            String dateExp = (row[2] != null) ? row[2].toString() : "";
            int stock = ((Number) row[3]).intValue();
            double prixAchat = (row[4] != null) ? ((Number) row[4]).doubleValue() : 0.0;
            Boolean decond = (Boolean) row[5];
            Integer unitesParBoite = (row[6] != null) ? ((Number) row[6]).intValue() : 1;
            
            double valeurLigne = 0.0;
            if (Boolean.TRUE.equals(decond) && unitesParBoite != null && unitesParBoite > 0) {
                valeurLigne = stock * (prixAchat / unitesParBoite);
            } else {
                valeurLigne = stock * prixAchat;
            }
            valeurARisqueTotale += valeurLigne;
            
            prochePeremptionsList.add(new ProduitPerimeDTO(nom, numeroLot, dateExp, stock));
        }
        alertes.setProchePeremptions(prochePeremptionsList);
        kpiStock.setNombreProchePeremption(prochePeremptionsList.size());
        kpiStock.setValeurARisque(Math.round(valeurARisqueTotale));

        dto.setAlertes(alertes);

        return dto;
    }

    /**
     * Calcule le pourcentage d'évolution entre deux périodes.
     * Arrondi à une décimale (ex: 15.2 ou -5.0).
     */
    private static double calculerEvolution(double actuel, double precedent) {
        if (precedent == 0) {
            return actuel > 0 ? 100.0 : 0.0;
        }
        double evolution = ((actuel - precedent) / precedent) * 100;
        return Math.round(evolution * 10.0) / 10.0;
    }

    /**
     * Construit une liste de TopProduitDTO avec marge réelle.
     *
     * Un produit déconditionnable peut apparaître 2 fois (BOITE_ENTIERE + DETAIL).
     * On les fusionne ici en UNE seule ligne par produit en gardant :
     *   - quantiteBoites  : nombre de boîtes entières vendues
     *   - quantiteUnites  : nombre d'unités au détail vendues
     *   - quantite        : total pour le tri (boites + unites)
     *   - marge           : CA - coût (identique à getBeneficeNet)
     */
    private static List<com.pharmacie.models.dto.TopProduitDTO> construireTopProduits(List<Object[]> rows) {
        // long[0] = quantite totale, long[1] = ca total, long[2] = cout total,
        // long[3] = quantite boites, long[4] = quantite unites
        java.util.LinkedHashMap<String, long[]> aggregation = new java.util.LinkedHashMap<>();

        for (Object[] row : rows) {
            String nom         = (String) row[0];
            double quantite    = (row[1] != null) ? ((Number) row[1]).doubleValue() : 0.0;
            double sousTotal   = (row[2] != null) ? ((Number) row[2]).doubleValue() : 0.0;
            double coutRevient = (row[3] != null) ? ((Number) row[3]).doubleValue() : 0.0;
            String typeUnite   = (row[4] != null) ? row[4].toString() : "BOITE_ENTIERE";

            long[] acc = aggregation.computeIfAbsent(nom, k -> new long[5]);
            acc[0] += Math.round(quantite);
            acc[1] += Math.round(sousTotal);
            acc[2] += Math.round(coutRevient);
            if ("DETAIL".equals(typeUnite)) {
                acc[4] += Math.round(quantite); // unités au détail
            } else {
                acc[3] += Math.round(quantite); // boîtes entières
            }
        }

        List<com.pharmacie.models.dto.TopProduitDTO> result = new ArrayList<>();
        for (java.util.Map.Entry<String, long[]> entry : aggregation.entrySet()) {
            String nom          = entry.getKey();
            long   qte          = entry.getValue()[0];
            long   ca           = entry.getValue()[1];
            long   cout         = entry.getValue()[2];
            long   qteBoites    = entry.getValue()[3];
            long   qteUnites    = entry.getValue()[4];
            long   marge        = ca - cout;
            result.add(new com.pharmacie.models.dto.TopProduitDTO(
                    nom, (int) qte, (int) qteBoites, (int) qteUnites, marge));
        }
        // Trier par quantité totale décroissante et limiter à 5
        result.sort((a, b) -> Integer.compare(b.getQuantite(), a.getQuantite()));
        return result.stream().limit(5).collect(java.util.stream.Collectors.toList());
    }

    /**
     * Sérialise le DTO en JSON (pretty-printed) et l'écrit dans sync/dashboard_snapshot.json.
     * L'écriture est atomique : on écrase le fichier complet à chaque synchro.
     */
    private static void ecrireJson(DashboardSyncDTO dto) throws IOException {
        File dir = new File(SYNC_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File fichier = new File(dir, SYNC_FILE);
        try (FileWriter writer = new FileWriter(fichier, false)) {
            GSON.toJson(dto, writer);
        }
        logger.info("Fichier JSON écrit : {} ({} octets)", fichier.getAbsolutePath(), fichier.length());
    }

    /**
     * Envoie le JSON généré vers la base de données Supabase via l'API REST.
     * Utilise un Upsert (merge-duplicates) basé sur l'identifiant de la pharmacie.
     */
    private static void envoyerVersCloud(DashboardSyncDTO dto) {
        if (!new SyncService().isInternetAvailable()) {
            logger.warn("Pas d'accès Internet. L'envoi vers le Cloud est reporté.");
            return;
        }

        String supabaseUrl = ConfigService.getSupabaseUrl();
        String supabaseKey = ConfigService.getSupabaseKey();

        if (supabaseUrl == null || supabaseKey == null || supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            logger.error("Configuration Supabase manquante dans config.properties. Annulation de la synchronisation.");
            return;
        }

        try {
            URL url = new URL(supabaseUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", supabaseKey);
            conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates"); // Upsert
            conn.setDoOutput(true);

            // Construction du payload attendu par Supabase (avec timestamp explicite pour forcer la mise à jour)
            String jsonPayload = GSON.toJson(dto);
            String nowUtc = java.time.Instant.now().toString(); // Format ISO-8601 attendu par Supabase
            String requestBody = "{\"pharmacy_id\": \"MAIN_PHARMACY\", \"updated_at\": \"" + nowUtc + "\", \"payload\": " + jsonPayload + "}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                logger.info("Synchro Cloud Supabase réussie ! (Code: " + code + ")");
            } else {
                logger.error("Échec Synchro Cloud Supabase. (Code HTTP: " + code + ")");
            }
        } catch (Exception e) {
            logger.error("Erreur de connexion à Supabase: " + e.getMessage());
        }
    }
}
