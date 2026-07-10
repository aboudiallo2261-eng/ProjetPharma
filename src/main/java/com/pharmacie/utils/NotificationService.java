package com.pharmacie.utils;

import com.pharmacie.dao.LotDAO;
import com.pharmacie.dao.StatistiquesDAO;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Notifications Windows natives pour les alertes critiques du stock.
 *
 * <p>Canal principal : <b>toasts Windows natifs</b> (via PowerShell/WinRT) — ils
 * s'affichent en bannière ET restent archivés dans le centre de notifications,
 * même si l'utilisateur était absent (contrairement aux bulles AWT, fugaces et
 * jamais conservées). Une icône de zone de notification (System Tray) assure la
 * présence visuelle de l'application. Repli AWT si PowerShell est indisponible.</p>
 *
 * <p>Déclencheurs :</p>
 * <ul>
 *   <li>~15 s après la connexion, puis rappel toutes les 4 h (bilan global) ;</li>
 *   <li><b>immédiatement après chaque vente</b> qui fait passer un produit
 *       en rupture ou sous son seuil d'alerte ({@link #verifierProduitsApresVente}).</li>
 * </ul>
 *
 * <p><b>Note Windows</b> : si le mode « Ne pas déranger » est actif, Windows
 * n'affiche pas la bannière mais la notification reste consultable dans le
 * centre de notifications (Win+N).</p>
 */
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final long INTERVALLE_RAPPEL_MS = 4L * 60 * 60 * 1000; // 4 heures
    private static final long DELAI_INITIAL_MS = 15_000;                  // 15 s après login
    private static final String APP_ID = "VetPharma";

    private static TrayIcon trayIcon;
    private static Timer timer;
    private static boolean demarre = false;
    private static boolean appIdEnregistre = false;
    private static String nomPharmacie = null;

    /**
     * Nom de l'enseigne configuré dans Paramètres (PharmacieInfo), utilisé comme
     * marque des notifications. Chaque pharmacie voit SON nom, pas celui du logiciel.
     * Chargé une fois, repli sur "VetPharma" si indisponible.
     */
    private static synchronized String nomPharmacie() {
        if (nomPharmacie == null) {
            try {
                com.pharmacie.models.PharmacieInfo info = new com.pharmacie.dao.PharmacieInfoDAO().getInfo();
                nomPharmacie = (info != null && info.getNom() != null && !info.getNom().isBlank())
                        ? info.getNom().trim()
                        : "VetPharma";
            } catch (Exception e) {
                return "VetPharma"; // pas de cache : on retentera au prochain appel
            }
        }
        return nomPharmacie;
    }

    /** Produits déjà notifiés (anti-spam) — purgé à chaque bilan périodique. */
    private static final Set<String> dejaNotifies = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Démarre le service (idempotent — un seul démarrage par session applicative). */
    public static synchronized void demarrer() {
        if (demarre) {
            return;
        }
        enregistrerAppId();
        try {
            if (SystemTray.isSupported()) {
                var imageUrl = NotificationService.class.getResource("/images/logo_32.png");
                java.awt.Image image = (imageUrl != null)
                        ? Toolkit.getDefaultToolkit().getImage(imageUrl)
                        : new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                trayIcon = new TrayIcon(image);
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip(nomPharmacie() + " — Gestion de Pharmacie Vétérinaire");
                SystemTray.getSystemTray().add(trayIcon);
            }

            timer = new Timer("vetpharma-notifications", true); // daemon : meurt avec l'app
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    dejaNotifies.clear(); // le bilan périodique ré-autorise les alertes unitaires
                    verifierEtNotifier();
                }
            }, DELAI_INITIAL_MS, INTERVALLE_RAPPEL_MS);

            demarre = true;
            logger.info("Notifications Windows activées (toasts natifs, vérification initiale dans 15 s, rappel toutes les 4 h).");
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
     * À appeler juste après la validation d'une vente : vérifie IMMÉDIATEMENT
     * si les produits vendus sont passés en rupture ou sous leur seuil d'alerte,
     * et notifie le cas échéant. Anti-spam : chaque produit n'est notifié qu'une
     * fois par palier (rupture / seuil) jusqu'au prochain bilan périodique.
     *
     * <p>S'exécute sur un thread dédié — ne bloque jamais la caisse.</p>
     */
    public static void verifierProduitsApresVente(Collection<Long> produitIds) {
        if (produitIds == null || produitIds.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>(produitIds);
        Thread t = new Thread(() -> {
            try {
                Map<Long, Integer> stocks = new LotDAO().getStockDisponibleParProduit();
                List<Object[]> produits;
                try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                    produits = session.createQuery(
                            "SELECT p.id, p.nom, p.seuilAlerte FROM Produit p WHERE p.id IN :ids",
                            Object[].class)
                        .setParameter("ids", ids)
                        .list();
                }
                List<String> lignes = new ArrayList<>();
                boolean rupture = false;
                for (Object[] row : produits) {
                    Long id = (Long) row[0];
                    String nom = (String) row[1];
                    int seuil = row[2] != null ? ((Number) row[2]).intValue() : 0;
                    int stock = stocks.getOrDefault(id, 0);
                    if (stock <= 0 && dejaNotifies.add(id + "_RUPTURE")) {
                        lignes.add("RUPTURE : " + nom);
                        rupture = true;
                    } else if (stock > 0 && stock <= seuil && dejaNotifies.add(id + "_SEUIL")) {
                        lignes.add(nom + " — reste " + stock + " (seuil " + seuil + ")");
                    }
                }
                if (!lignes.isEmpty()) {
                    notifier(nomPharmacie() + " — Alerte stock après vente",
                            String.join("\n", lignes),
                            rupture ? TrayIcon.MessageType.ERROR : TrayIcon.MessageType.WARNING);
                    logger.info("Notification post-vente émise : {}", String.join(" | ", lignes));
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification post-vente des seuils", e);
            }
        }, "vetpharma-notif-vente");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Bilan global (mêmes requêtes que le dashboard) : périmés, ruptures, seuils.
     * Tourne sur le thread du Timer — jamais sur le thread JavaFX.
     */
    private static void verifierEtNotifier() {
        try {
            StatistiquesDAO statsDAO = new StatistiquesDAO();
            long[] kpi = statsDAO.getDashboardWebAlertesKPI(LocalDate.now());
            long ruptures = kpi[0];
            long alertesStock = kpi[1];
            long perimes = kpi[2];

            List<String> lignes = new ArrayList<>();
            if (perimes > 0)      lignes.add(perimes + " lot(s) PÉRIMÉ(S) en stock");
            if (ruptures > 0)     lignes.add(ruptures + " produit(s) en RUPTURE");
            if (alertesStock > 0) lignes.add(alertesStock + " produit(s) sous le seuil d'alerte");

            if (!lignes.isEmpty()) {
                notifier(nomPharmacie() + " — Alertes Stock", String.join("\n", lignes),
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

    /**
     * Émet une notification Windows.
     * Canal principal : toast natif WinRT (persistant dans le centre de notifications).
     * Repli : bulle AWT du System Tray.
     */
    public static void notifier(String titre, String message, TrayIcon.MessageType type) {
        if (!toastNatif(titre, message)) {
            if (trayIcon != null) {
                trayIcon.displayMessage(titre, message, type);
            }
        }
    }

    // ─────────────────────── Toasts natifs (WinRT via PowerShell) ───────────────────────

    /**
     * Affiche un toast Windows natif via PowerShell/WinRT.
     * Le script est passé encodé en Base64 UTF-16LE (-EncodedCommand) :
     * aucun problème d'échappement ni d'accents.
     *
     * @return true si la commande a été lancée, false pour utiliser le repli AWT.
     */
    private static boolean toastNatif(String titre, String message) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return false;
        }
        try {
            String script =
                "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null\n" +
                "$xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)\n" +
                "$t = $xml.GetElementsByTagName('text')\n" +
                "$t.Item(0).AppendChild($xml.CreateTextNode('" + echapperPs(titre) + "')) | Out-Null\n" +
                "$t.Item(1).AppendChild($xml.CreateTextNode('" + echapperPs(message) + "')) | Out-Null\n" +
                "$toast = [Windows.UI.Notifications.ToastNotification]::new($xml)\n" +
                "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('" + APP_ID + "').Show($toast)\n";
            String encode = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-EncodedCommand", encode)
                .redirectErrorStream(true)
                .start();
            return true;
        } catch (Exception e) {
            logger.warn("Toast natif indisponible ({}) — repli sur la bulle AWT.", e.getMessage());
            return false;
        }
    }

    /** Doublage des apostrophes pour les littéraux PowerShell entre quotes simples. */
    private static String echapperPs(String s) {
        return s == null ? "" : s.replace("'", "''").replace("\r", " ").replace("\n", " — ");
    }

    /**
     * Enregistre l'identité applicative « VetPharma » (AppUserModelID) dans le
     * registre utilisateur (HKCU) : les toasts affichent alors le nom et le logo
     * de l'application au lieu de « Windows PowerShell ». Opération locale à
     * l'utilisateur, idempotente, sans droits administrateur.
     */
    private static void enregistrerAppId() {
        if (appIdEnregistre) {
            return;
        }
        try {
            // Extraire le logo vers un chemin stable pour l'IconUri du toast
            Path dossier = Path.of(System.getProperty("user.home"), ".vetpharma");
            Files.createDirectories(dossier);
            Path logo = dossier.resolve("logo_64.png");
            if (!Files.exists(logo)) {
                try (var in = NotificationService.class.getResourceAsStream("/images/logo_64.png")) {
                    if (in != null) {
                        Files.copy(in, logo);
                    }
                }
            }
            String cle = "HKCU\\Software\\Classes\\AppUserModelId\\" + APP_ID;
            new ProcessBuilder("reg", "add", cle, "/v", "DisplayName", "/t", "REG_SZ",
                    "/d", nomPharmacie(), "/f").start().waitFor();
            new ProcessBuilder("reg", "add", cle, "/v", "IconUri", "/t", "REG_SZ",
                    "/d", logo.toAbsolutePath().toString(), "/f").start().waitFor();
            appIdEnregistre = true;
            logger.info("Identité de notification '{}' enregistrée (HKCU).", APP_ID);
        } catch (Exception e) {
            logger.warn("Enregistrement de l'identité de notification impossible : {}", e.getMessage());
        }
    }
}
