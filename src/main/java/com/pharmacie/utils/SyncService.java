package com.pharmacie.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class SyncService {

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
                        System.out.println("[Sync] Internet détecté. Début de la synchronisation...");
                        syncData();
                    } else {
                        System.out.println("[Sync] Pas d'accès Internet. Fonctionnement 100% hors-ligne maintenu.");
                    }
                } catch (Exception e) {
                    System.err.println("[Sync] Erreur lors du thread de synchronisation: " + e.getMessage());
                }
            }
        }, 10000, 300000);
    }

    public void stopSyncDaemon() {
        if (timer != null) {
            timer.cancel();
            System.out.println("[Sync] Service de synchronisation arrêté.");
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
        
        System.out.println("[Sync] -> Simulation Extraction des ventes locales...");
        System.out.println("[Sync] -> Simulation Extraction du statut actuel des stocks...");
        System.out.println("[Sync] -> Envoi sécurisé des données vers le Cloud : " + REMOTE_API_URL);
        
        System.out.println("[Sync] Synchronisation vers le Cloud réussie avec succès !");
    }
}
