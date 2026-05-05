package com.pharmacie.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);

    /**
     * Compresse un fichier SQL en ZIP.
     * @param sourceFile Le fichier SQL source (ex: backups/pharmacie_backup.sql)
     * @return Le fichier ZIP généré, ou null en cas d'erreur.
     */
    public static File compressSqlToZip(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) {
            logger.error("Le fichier source n'existe pas : {}", sourceFile);
            return null;
        }

        String sourcePath = sourceFile.getAbsolutePath();
        String zipPath = sourcePath.replace(".sql", ".zip");
        File zipFile = new File(zipPath);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sourceFile)) {

            ZipEntry zipEntry = new ZipEntry(sourceFile.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            logger.info("Fichier compressé avec succès : {}", zipPath);
            return zipFile;

        } catch (IOException e) {
            logger.error("Erreur lors de la compression du fichier : {}", sourceFile.getName(), e);
            return null;
        }
    }
}
