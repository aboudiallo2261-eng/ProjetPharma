package com.pharmacie.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;
import java.util.Optional;

public class AlertUtils {

    public static void showPremiumAlert(AlertType type, String title, String headerText, String contentText) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);

        String typeColor = type == AlertType.ERROR ? "#E74C3C" : (type == AlertType.WARNING ? "#F39C12" : "#18BC9C");
        
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;" +
            "-fx-border-color: " + typeColor + ";" +
            "-fx-border-width: 0 0 0 5;" +
            "-fx-font-size: 14px;"
        );

        dialogPane.lookup(".header-panel").setStyle(
            "-fx-background-color: #F8F9FA;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 16px;" +
            "-fx-text-fill: #2C3E50;"
        );

        alert.showAndWait();
    }

    public static boolean showPremiumConfirmation(String title, String headerText, String contentText) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        
        // Custom style labels inside text
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;" +
            "-fx-border-color: #3498DB;" +
            "-fx-border-width: 0 0 0 5;" +
            "-fx-font-size: 14px;"
        );
        dialogPane.lookup(".header-panel").setStyle(
            "-fx-background-color: #F8F9FA;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 16px;"
        );

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
