package com.pharmacie.utils;

import java.io.*;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service gérant la configuration locale de l'application (DésIAtisation).
 * Sauvegarde les paramètres comme le chemin USB dans un fichier config.properties.
 */
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String CONFIG_FILE = "config.properties";
    private static final String KEY_BACKUP_PATH = "backup.usb.path";

    /**
     * Lit le chemin de sauvegarde USB configuré.
     * @return Le chemin absolu, ou null si non configuré.
     */
    public static String getBackupPath() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            prop.load(input);
            return prop.getProperty(KEY_BACKUP_PATH);
        } catch (IOException ex) {
            logger.warn("Fichier de configuration introuvable ou illisible, chemin USB non défini.");
            return null;
        }
    }

    public static String getSupabaseUrl() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            prop.load(input);
            return prop.getProperty("supabase.url");
        } catch (IOException ex) {
            return null;
        }
    }

    public static String getSupabaseKey() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            prop.load(input);
            return prop.getProperty("supabase.key");
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Enregistre le chemin de sauvegarde USB.
     * @param path Le chemin absolu du dossier de sauvegarde.
     */
    public static void saveBackupPath(String path) {
        Properties prop = new Properties();
        // Charger les propriétés existantes pour ne pas les écraser
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            prop.load(input);
        } catch (IOException ex) {
            // Fichier inexistant, on le créera
        }

        prop.setProperty(KEY_BACKUP_PATH, path);

        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            prop.store(output, "Configuration Pharmacie Vet");
            AuditLogger.log("Config USB", "Nouveau chemin défini : " + path);
        } catch (IOException io) {
            logger.error("Impossible de sauvegarder la configuration", io);
        }
    }
}
