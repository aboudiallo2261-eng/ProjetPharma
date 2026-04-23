package com.pharmacie.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Service utilitaire pour afficher des notifications non-bloquantes (Toasts).
 * Remplace les Alert bloquantes pour les succès et les informations de flux continu.
 */
public class ToastService {

    private static final int TOAST_DELAY_MS = 3000; // 3 secondes
    private static final java.util.Map<Window, java.util.List<Popup>> activeToasts = new java.util.HashMap<>();

    public enum ToastType {
        SUCCESS("#2ecc71", "✅"),
        INFO("#3498db", "ℹ️"),
        WARNING("#f39c12", "⚠️"),
        ERROR("#e74c3c", "❌");

        private final String color;
        private final String icon;

        ToastType(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }

        public String getColor() { return color; }
        public String getIcon() { return icon; }
    }

    public static void showSuccess(Window owner, String title, String message) {
        show(owner, title, message, ToastType.SUCCESS);
    }

    public static void showInfo(Window owner, String title, String message) {
        show(owner, title, message, ToastType.INFO);
    }

    public static void showWarning(Window owner, String title, String message) {
        show(owner, title, message, ToastType.WARNING);
    }

    public static void showError(Window owner, String title, String message) {
        show(owner, title, message, ToastType.ERROR);
    }

    private static void show(Window owner, String title, String message, ToastType type) {
        if (owner == null) return;

        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            Label iconLabel = new Label(type.getIcon());
            iconLabel.setStyle("-fx-font-size: 24px;");

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

            Label msgLabel = new Label(message);
            msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-wrap-text: true;");
            msgLabel.setMaxWidth(300);

            javafx.scene.layout.VBox textContainer = new javafx.scene.layout.VBox(3, titleLabel, msgLabel);
            
            HBox toastContent = new HBox(15, iconLabel, textContainer);
            toastContent.setAlignment(Pos.CENTER_LEFT);
            toastContent.setStyle(
                "-fx-background-color: " + type.getColor() + ";" +
                "-fx-padding: 15px 20px;" +
                "-fx-background-radius: 8px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);"
            );

            StackPane pane = new StackPane(toastContent);
            pane.setStyle("-fx-padding: 10px;"); // Marge invisible pour l'ombre

            popup.getContent().add(pane);

            // Calcul du positionnement en bas à droite avec empilement
            popup.setOnShown(e -> {
                java.util.List<Popup> toasts = activeToasts.computeIfAbsent(owner, k -> new java.util.ArrayList<>());
                toasts.removeIf(p -> !p.isShowing());
                
                double yOffset = 0;
                for (Popup p : toasts) {
                    yOffset += p.getHeight() + 5; // Espacement de 5px entre les toasts
                }
                toasts.add(popup);

                double x = owner.getX() + owner.getWidth() - toastContent.getWidth() - 30;
                double y = owner.getY() + owner.getHeight() - toastContent.getHeight() - 40 - yOffset;
                popup.setX(x);
                popup.setY(y);

                // Animations d'apparition
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), pane);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), pane);
                slideIn.setFromY(50);
                slideIn.setToY(0);

                // Disparition automatique
                PauseTransition delay = new PauseTransition(Duration.millis(TOAST_DELAY_MS));

                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), pane);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), pane);
                slideOut.setFromY(0);
                slideOut.setToY(20);

                fadeIn.play();
                slideIn.play();

                delay.setOnFinished(event -> {
                    fadeOut.play();
                    slideOut.play();
                });

                fadeOut.setOnFinished(event -> popup.hide());

                delay.play();
            });

            popup.setOnHidden(e -> {
                java.util.List<Popup> list = activeToasts.get(owner);
                if (list != null) {
                    list.remove(popup);
                }
            });

            // En cas de clic on le ferme direct
            pane.setOnMouseClicked(event -> {
                popup.hide();
            });

            popup.show(owner);
        });
    }
}
