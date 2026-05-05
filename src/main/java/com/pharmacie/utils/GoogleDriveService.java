package com.pharmacie.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class GoogleDriveService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final String APPLICATION_NAME = "VetPharma Backup System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String ROOT_FOLDER_NAME = "Backups_Pharmacy";

    /**
     * Initialise et retourne l'instance de l'API Google Drive via OAuth 2.0.
     */
    private static Drive getDriveService() throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        InputStream in = GoogleDriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Fichier " + CREDENTIALS_FILE_PATH + " introuvable dans resources.");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Le scope DRIVE_FILE permet à l'application de gérer les fichiers qu'elle a elle-même créés
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, Collections.singletonList(DriveScopes.DRIVE_FILE))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        // Utilisation d'un port dynamique (-1) pour éviter l'erreur "Address already in use"
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(-1).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Upload le fichier ZIP dans l'arborescence Backups_Pharmacy/YYYY/MM
     * @param zipFile Le fichier ZIP à envoyer.
     * @return true si succès, false si échec.
     */
    public static boolean uploadBackup(java.io.File zipFile) {
        try {
            if (zipFile == null || !zipFile.exists() || zipFile.length() == 0) {
                logger.error("Fichier ZIP invalide ou vide.");
                return false;
            }

            Drive service = getDriveService();

            // 1. Gérer l'arborescence des dossiers
            LocalDate now = LocalDate.now();
            String yearFolder = now.format(DateTimeFormatter.ofPattern("yyyy"));
            String monthFolder = now.format(DateTimeFormatter.ofPattern("MM"));

            String rootFolderId = getOrCreateFolder(service, ROOT_FOLDER_NAME, null);
            String yearFolderId = getOrCreateFolder(service, yearFolder, rootFolderId);
            String monthFolderId = getOrCreateFolder(service, monthFolder, yearFolderId);

            // 2. Préparer les métadonnées du fichier
            File fileMetadata = new File();
            fileMetadata.setName(zipFile.getName());
            fileMetadata.setParents(Collections.singletonList(monthFolderId));

            // 3. Préparer le contenu du fichier
            FileContent mediaContent = new FileContent("application/zip", zipFile);

            // 4. Exécuter l'upload
            logger.info("Début de l'upload vers Google Drive : {}", zipFile.getName());
            File uploadedFile = service.files().create(fileMetadata, mediaContent)
                    .setFields("id, name")
                    .execute();

            logger.info("Fichier uploadé avec succès sur Drive. File ID: {}", uploadedFile.getId());
            return true;

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("Erreur API Google Drive (Détails) : {}", e.getContent() != null ? e.getContent() : e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Erreur lors de l'upload sur Google Drive", e);
            return false;
        }
    }

    /**
     * Récupère l'ID d'un dossier, ou le crée s'il n'existe pas.
     */
    private static String getOrCreateFolder(Drive service, String folderName, String parentId) throws IOException {
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";
        if (parentId != null) {
            query += " and '" + parentId + "' in parents";
        }

        FileList result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId(); // Dossier existant
        }

        // Création du dossier
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        if (parentId != null) {
            folderMetadata.setParents(Collections.singletonList(parentId));
        }

        File folder = service.files().create(folderMetadata)
                .setFields("id")
                .execute();
        
        logger.info("Dossier Drive créé : {}", folderName);
        return folder.getId();
    }
}
