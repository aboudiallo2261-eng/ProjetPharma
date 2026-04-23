package com.pharmacie.utils;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);

    public static boolean exportDatabase(File destination) {
        String dbUser = "root";
        String dbPass = "";
        String dbName = "pharmacie_vet_db";

        try {
            // Recherche de mysqldump
            String mysqldumpPath = findMysqldumpPath();
            if (mysqldumpPath == null) {
                logger.error("mysqldump.exe introuvable. Veuillez l'ajouter à votre variable PATH.");
                return false;
            }

            ProcessBuilder pb;
            if (dbPass == null || dbPass.isEmpty()) {
                pb = new ProcessBuilder(mysqldumpPath, "-u", dbUser, dbName, "-r", destination.getAbsolutePath());
            } else {
                pb = new ProcessBuilder(mysqldumpPath, "-u", dbUser, "-p" + dbPass, dbName, "-r", destination.getAbsolutePath());
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int processComplete = process.waitFor();
            
            return processComplete == 0;
        } catch (IOException | InterruptedException e) {
            logger.error("Erreur lors de la sauvegarde de la base", e);
            return false;
        }
    }

    private static String findMysqldumpPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            return "mysqldump"; // Sur Linux/Mac, c'est généralement dans le PATH
        }

        // Test de la commande mysqldump directement (si dans le PATH)
        try {
            Process p = new ProcessBuilder("mysqldump", "--version").start();
            if (p.waitFor() == 0) return "mysqldump";
        } catch (Exception e) {
            // Ignorer, on va chercher dans les chemins courants
        }

        // Liste des chemins courants de XAMPP, WAMP, Laragon, MySQL Server
        String[] commonPaths = {
            "C:\\xampp\\mysql\\bin\\mysqldump.exe",
            "C:\\laragon\\bin\\mysql\\mysql-8.0.30-winx64\\bin\\mysqldump.exe", // Laragon default
            "C:\\wamp64\\bin\\mysql\\mysql8.0.31\\bin\\mysqldump.exe", // WAMP default
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe"
        };

        for (String path : commonPaths) {
            if (new File(path).exists()) {
                return path;
            }
        }

        // Recherche générique basique dans les dossiers parents
        File[] searchDirs = {
            new File("C:\\wamp64\\bin\\mysql"),
            new File("C:\\laragon\\bin\\mysql"),
            new File("C:\\Program Files\\MySQL")
        };

        for (File baseDir : searchDirs) {
            if (baseDir.exists() && baseDir.isDirectory()) {
                File[] subDirs = baseDir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        File dump = new File(subDir, "bin\\mysqldump.exe");
                        if (dump.exists()) return dump.getAbsolutePath();
                    }
                }
            }
        }

        return null; // Introuvable
    }
}
