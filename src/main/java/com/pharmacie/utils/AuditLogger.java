package com.pharmacie.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service de journalisation d'audit (DésIAtisation technique).
 * Génère un fichier texte brut, lisible par l'humain pour la traçabilité des opérations critiques (PRA, Synchro).
 */
public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "system_log.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Enregistre une action dans le journal d'audit.
     * Format : [DATE] [HEURE] - ACTION - STATUT
     * Exemple : 2026-05-12 18:30 - Backup USB - SUCCESS
     */
    public static void log(String action, String statut) {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File logFile = new File(dir, LOG_FILE);
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("%s - %s - %s", timestamp, action, statut);

        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            logger.error("Impossible d'écrire dans le journal d'audit : " + logFile.getAbsolutePath(), e);
        }
    }
}
