package com.pharmacie.utils;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;

public class ComboBoxAutoComplete {

    /**
     * Applique une logique de filtrage (auto-completion) dynamique sur une ComboBox classique.
     * Idéal pour gérer des listes de milliers de Produits/Fournisseurs sans souris.
     */
    public static <T> void setup(ComboBox<T> comboBox) {
        final ObservableList<T> originalItems = FXCollections.observableArrayList(comboBox.getItems());

        // Synchroniser au cas où la BDD charge des items APRÈS le setup (ex: loadProduits)
        comboBox.getItems().addListener((ListChangeListener.Change<? extends T> c) -> {
            // Mettre à jour la base d'origine si on vient de recharger complètement la combobox 
            // (on ignore si c'est notre propre filtrage qui réduit la liste)
            if (comboBox.getItems().size() > originalItems.size() || 
               (comboBox.getItems().size() > 0 && originalItems.isEmpty())) {
                originalItems.setAll(comboBox.getItems());
            }
        });

        final StringBuilder filter = new StringBuilder();

        comboBox.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            // Autoriser les lettres, les chiffres et les espaces
            if (code.isLetterKey() || code.isDigitKey() || code == KeyCode.SPACE) {
                filter.append(event.getText());
            } else if (code == KeyCode.BACK_SPACE && filter.length() > 0) {
                filter.deleteCharAt(filter.length() - 1);
            } else if (code == KeyCode.ESCAPE) {
                filter.setLength(0);
            } else {
                // Touches de navigation : on laisse JavaFX faire
                return;
            }

            if (filter.length() == 0) {
                T selected = comboBox.getSelectionModel().getSelectedItem();
                comboBox.getItems().setAll(originalItems);
                comboBox.getSelectionModel().select(selected);
            } else {
                ObservableList<T> filteredItems = FXCollections.observableArrayList();
                String filterStr = filter.toString().toLowerCase();
                for (T item : originalItems) {
                    if (item != null && item.toString().toLowerCase().contains(filterStr)) {
                        filteredItems.add(item);
                    }
                }
                T selected = comboBox.getSelectionModel().getSelectedItem();
                comboBox.getItems().setAll(filteredItems);
                if (filteredItems.contains(selected)) {
                    comboBox.getSelectionModel().select(selected);
                }
                comboBox.show(); // Ouvrir la popup filtrée
            }
        });

        // Réinitialiser le filtre lorsque la liste déroulante se ferme
        comboBox.setOnHidden(event -> {
            filter.setLength(0);
            T selected = comboBox.getSelectionModel().getSelectedItem();
            comboBox.getItems().setAll(originalItems);
            if (selected != null) {
                comboBox.getSelectionModel().select(selected);
            }
        });
    }
}
