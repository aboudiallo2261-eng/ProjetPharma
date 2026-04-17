package com.pharmacie;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class TestLoad extends Application {
    @Override
    public void start(Stage stage) {
        System.out.println("====== DÉBUT DU TEST DE CHARGEMENT ======");
        try {
            System.out.println("Tentative de chargement de achats.fxml ...");
            FXMLLoader.load(getClass().getResource("/fxml/achats.fxml"));
            System.out.println(">>> SUCCÈS ACHATS");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            System.out.println("Tentative de chargement de ventes.fxml ...");
            FXMLLoader.load(getClass().getResource("/fxml/ventes.fxml"));
            System.out.println(">>> SUCCÈS VENTES");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Tentative de chargement de rapports.fxml ...");
            FXMLLoader.load(getClass().getResource("/fxml/rapports.fxml"));
            System.out.println(">>> SUCCÈS RAPPORTS");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("====== FIN DU TEST ======");
        Platform.exit();
        System.exit(0);
    }
    public static void main(String[] args) { launch(args); }
}
