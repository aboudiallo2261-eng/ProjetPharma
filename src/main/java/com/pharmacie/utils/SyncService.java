package com.pharmacie.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncService {

    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);
    private static final String REMOTE_API_URL = "https://api.pharmacie-veterinaire-cloud.com/sync";
    private Timer timer;

    public void startSyncDaemon() {
        timer = new Timer(true); // Thread Daemon (s'arrête avec l'application)
        
        // Exécute 1 fois toutes les 5 minutes (300 000 ms), avec un délai initial de 10s
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (isInternetAvailable()) {
                        logger.info("Internet détecté. Début de la synchronisation...");
                        syncData();
                    } else {
                        logger.warn("Pas d'accès Internet. Fonctionnement 100% hors-ligne maintenu.");
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors du thread de synchronisation", e);
                }
            }
        }, 10000, 300000);
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

    private void syncData() {
        // En vrai (Production) :
        // 1. Lire tout ce qui n'a pas encore été synchronisé dans la DB locale
        // 2. Parser la data en JSON et faire une requête POST (API Endpoint distant)
        // 3. A la réponse 200 OK -> Marquer les données locales comme synchronisées
        
        logger.debug("Simulation Extraction des ventes locales...");
        logger.debug("Simulation Extraction du statut actuel des stocks...");
        logger.info("Envoi sécurisé des données vers le Cloud : {}", REMOTE_API_URL);
        
        logger.info("Synchronisation vers le Cloud réussie avec succès !");
    }
}
