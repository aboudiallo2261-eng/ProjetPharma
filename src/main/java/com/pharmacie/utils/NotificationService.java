package com.pharmacie.utils;

import com.pharmacie.dao.StatistiquesDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Notifications Windows natives (zone de notification / System Tray).
 *
 * <p>Affiche des notifications système pour les alertes critiques du stock
 * (ruptures, seuils d'alerte, lots périmés) :</p>
 * <ul>
 *   <li>une première vérification ~15 s après la connexion (le temps que l'UI se pose),</li>
 *   <li>puis un rappel toutes les 4 heures tant que l'application tourne.</li>
 * </ul>
 *
 * <p><b>Limite assumée</b> : les notifications ne sont émises que quand
 * l'application est ouverte (pas de service Windows séparé). Dans une pharmacie,
 * l'application reste ouverte toute la journée — la notification du matin est
 * le moment utile pour traiter les alertes.</p>
 *
 * <p>Implémentation via AWT SystemTray : API du JDK, aucune dépendance externe,
 * compatible Windows 10/11 (les bulles sont rendues comme des toasts natifs).</p>
 */
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final long INTERVALLE_RAPPEL_MS = 4L * 60 * 60 * 1000; // 4 heures
    private static final long DELAI_INITIAL_MS = 15_000;                  // 15 s après login

    private static TrayIcon trayIcon;
    private static Timer timer;
    private static boolean demarre = false;

    /** Démarre le service (idempotent — un seul démarrage par session applicative). */
    public static synchronized void demarrer() {
        if (demarre) {
            return;
        }
        if (!SystemTray.isSupported()) {
            logger.warn("SystemTray non supporté sur ce système — notifications désactivées.");
            return;
        }
        try {
            var imageUrl = NotificationService.class.getResource("/images/logo_32.png");
            java.awt.Image image = (imageUrl != null)
                    ? Toolkit.getDefaultToolkit().getImage(imageUrl)
                    : null;
            trayIcon = new TrayIcon(image != null ? image
                    : new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB));
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("VetPharma — Gestion de Pharmacie Vétérinaire");
            SystemTray.getSystemTray().add(trayIcon);

            timer = new Timer("vetpharma-notifications", true); // daemon : meurt avec l'app
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    verifierEtNotifier();
                }
            }, DELAI_INITIAL_MS, INTERVALLE_RAPPEL_MS);

            demarre = true;
            logger.info("Notifications Windows activées (vérification initiale dans 15 s, rappel toutes les 4 h).");
        } catch (Exception e) {
            logger.error("Impossible d'initialiser les notifications Windows", e);
        }
    }

    /** Arrête le service et retire l'icône de la zone de notification. */
    public static synchronized void arreter() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
        demarre = false;
    }

    /**
     * Interroge la base (mêmes requêtes que le dashboard) et notifie si nécessaire.
     * Tourne sur le thread du Timer — jamais sur le thread JavaFX.
     */
    private static void verifierEtNotifier() {
        try {
            StatistiquesDAO statsDAO = new StatistiquesDAO();
            long[] kpi = statsDAO.getDashboardWebAlertesKPI(LocalDate.now());
            long ruptures = kpi[0];
            long alertesStock = kpi[1];
            long perimes = kpi[2];

            StringBuilder message = new StringBuilder();
            if (perimes > 0) {
                message.append("⛔ ").append(perimes).append(" lot(s) PÉRIMÉ(S) en stock\n");
            }
            if (ruptures > 0) {
                message.append("🚨 ").append(ruptures).append(" produit(s) en RUPTURE\n");
            }
            if (alertesStock > 0) {
                message.append("⚠ ").append(alertesStock).append(" produit(s) sous le seuil d'alerte");
            }

            if (message.length() > 0) {
                notifier("VetPharma — Alertes Stock", message.toString().trim(),
                        (perimes > 0 || ruptures > 0) ? TrayIcon.MessageType.ERROR
                                                      : TrayIcon.MessageType.WARNING);
                logger.info("Notification Windows émise : {} périmés, {} ruptures, {} alertes.",
                        perimes, ruptures, alertesStock);
            } else {
                logger.info("Vérification des alertes : rien à notifier.");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des alertes pour notification", e);
        }
    }

    /** Émet une notification Windows native. Sans effet si le service n'est pas démarré. */
    public static void notifier(String titre, String message, TrayIcon.MessageType type) {
        if (trayIcon != null) {
            trayIcon.displayMessage(titre, message, type);
        }
    }
}
