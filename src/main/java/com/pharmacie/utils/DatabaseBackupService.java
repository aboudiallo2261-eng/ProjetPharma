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
            ProcessBuilder pb;
            if (dbPass.isEmpty()) {
                pb = new ProcessBuilder("mysqldump", "-u", dbUser, dbName, "-r", destination.getAbsolutePath());
            } else {
                pb = new ProcessBuilder("mysqldump", "-u", dbUser, "-p" + dbPass, dbName, "-r", destination.getAbsolutePath());
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
}
