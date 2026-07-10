package com.pharmacie.utils;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;

/**
 * Utilitaires d'affichage pour les TableView.
 */
public final class TableUtils {

    private TableUtils() {
    }

    /**
     * Ajoute une infobulle (tooltip) sur chaque cellule des colonnes texte données :
     * quand une valeur est plus large que sa colonne (nom de produit long, email...),
     * le survol de la souris affiche le texte complet.
     *
     * <p>À appeler APRÈS avoir défini les cellValueFactory des colonnes.</p>
     */
    @SafeVarargs
    public static <S> void tooltipSurColonnes(TableColumn<S, String>... colonnes) {
        for (TableColumn<S, String> col : colonnes) {
            col.setCellFactory(c -> new TableCell<S, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        Tooltip infobulle = new Tooltip(item);
                        infobulle.setShowDelay(javafx.util.Duration.millis(300));
                        setTooltip(infobulle);
                    }
                }
            });
        }
    }
}
