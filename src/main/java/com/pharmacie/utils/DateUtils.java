package com.pharmacie.utils;

import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;

public class DateUtils {

    /**
     * Lie deux filtres DatePicker pour garantir que la date de fin ne puisse jamais 
     * précéder la date de début.
     * 
     * @param dpDebut Le sélecteur de Date de Début
     * @param dpFin   Le sélecteur de Date de Fin
     */
    public static void bindDateFilters(DatePicker dpDebut, DatePicker dpFin) {
        if (dpDebut == null || dpFin == null) return;

        // Factrique de cellules pour griser les dates inaccessibles
        javafx.util.Callback<DatePicker, DateCell> dayCellFactory = picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate debutDate = dpDebut.getValue();
                if (debutDate != null && date.isBefore(debutDate)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ECF0F1; -fx-text-fill: #BDC3C7;"); // Apparence grisée "disabled"
                }
            }
        };

        dpFin.setDayCellFactory(dayCellFactory);

        // Si la date début change, on force la date fin à s'aligner si elle la précède.
        // Et on rafraichit la factory pour recalculer les grisés.
        dpDebut.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                if (dpFin.getValue() != null && dpFin.getValue().isBefore(newDate)) {
                    dpFin.setValue(newDate);
                }
            }
            // Forcer le composant à se redessiner
            dpFin.setDayCellFactory(dayCellFactory);
        });
    }
}
