package com.pharmacie;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import com.pharmacie.utils.SyncService;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static final SyncService syncService = new SyncService();

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Pharmacie Vétérinaire - Gestion");
        
        primaryStage.setOnCloseRequest(event -> {
            if (com.pharmacie.utils.SessionManager.getCurrentUser() != null) {
                com.pharmacie.dao.SessionCaisseDAO sessionDAO = new com.pharmacie.dao.SessionCaisseDAO();
                boolean isCaisseOpen = sessionDAO.findAll().stream()
                    .anyMatch(s -> s.getUser().getId().equals(com.pharmacie.utils.SessionManager.getCurrentUser().getId()) 
                                   && s.getStatut() == com.pharmacie.models.SessionCaisse.StatutSession.OUVERTE);

                if (isCaisseOpen) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Fermeture Impossible");
                    alert.setHeaderText("🚨 Caisse logicielle toujours ouverte !");
                    alert.setContentText("La norme de sécurité interdisant la disparition des agents, vous ne pouvez pas fermer l'application sans justifier le compte du tiroir.\n\nVeuillez clôturer votre caisse (Z) d'abord.");
                    alert.showAndWait();
                    event.consume(); // Bloque la fermeture de JavaFX
                }
            }
        });

        syncService.startSyncDaemon();
        showLoginScreen();
    }

    @Override
    public void stop() throws Exception {
        syncService.stopSyncDaemon();
        super.stop();
    }

    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setMaximized(false);
            primaryStage.setWidth(600);
            primaryStage.setHeight(400);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showMainLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main_layout.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setMaximized(false); // Force le reset pour que le true suivant soit détecté
            primaryStage.setMaximized(true);
            primaryStage.show(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
