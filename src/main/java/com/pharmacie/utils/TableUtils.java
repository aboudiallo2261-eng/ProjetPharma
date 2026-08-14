package com.pharmacie.utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Ajustement des colonnes de tableau à la largeur réelle de leur contenu.
 *
 * <p>JavaFX ne propose aucun équivalent public du « ajuster à la sélection » d'Excel :
 * les largeurs restent celles écrites dans le FXML, quel que soit le texte affiché.
 * Un nom de produit plus long que prévu se retrouve donc tronqué. Cette classe mesure
 * le texte réellement rendu (police comprise) et dimensionne chaque colonne d'après
 * son propre besoin, sans empiéter sur les voisines.</p>
 *
 * <p>Deux modes, selon l'usage de l'écran :</p>
 * <ul>
 *   <li>{@link #ajusterAvecDefilement(TableView)} — écrans de consultation et d'analyse.
 *       Chaque colonne obtient exactement la largeur qu'il lui faut ; si le total dépasse
 *       la fenêtre, une barre de défilement horizontale apparaît (comportement d'un tableur).</li>
 *   <li>{@link #ajusterSansDefilement(TableView)} — écrans d'action rapide (caisse).
 *       Tout reste visible d'un coup d'œil : les largeurs deviennent des proportions
 *       fondées sur le contenu, et les textes trop longs sont abrégés avec une infobulle.</li>
 * </ul>
 */
public final class TableUtils {

    /** Marge intérieure d'une cellule (padding gauche + droite + bordure). */
    private static final double MARGE_CELLULE = 22;
    /** Marge supplémentaire d'un en-tête (place de la flèche de tri). */
    private static final double MARGE_ENTETE = 34;
    /** Au-delà, on échantillonne : mesurer 50 000 lignes bloquerait l'interface. */
    private static final int MAX_LIGNES_MESUREES = 400;
    /** Garde-fou : une colonne ne dépasse jamais cette largeur, sinon une seule mange l'écran. */
    private static final double LARGEUR_MAX_COLONNE = 420;

    private TableUtils() { }

    /**
     * Mode consultation : chaque colonne prend la largeur de son contenu le plus long,
     * quitte à faire apparaître une barre de défilement horizontale.
     */
    public static void ajusterAvecDefilement(TableView<?> table) {
        appliquer(table, true);
    }

    /**
     * Mode action rapide : tout reste visible sans défilement horizontal.
     * Les largeurs mesurées servent de proportions ; les textes trop longs sont
     * abrégés et complétés par une infobulle au survol.
     */
    public static void ajusterSansDefilement(TableView<?> table) {
        appliquer(table, false);
    }

    private static void appliquer(TableView<?> table, boolean autoriserDefilement) {
        if (table == null) {
            return;
        }
        // Différé : les cellules doivent être rendues pour que leur texte soit mesurable.
        Platform.runLater(() -> {
            try {
                // Au tout premier affichage, le tableau n'est pas encore dimensionné
                // (getWidth() == 0) : impossible de savoir s'il y aura débordement.
                // On réessaie dès que sa largeur réelle est connue.
                if (table.getWidth() <= 0) {
                    table.widthProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
                        @Override
                        public void changed(javafx.beans.value.ObservableValue<? extends Number> obs,
                                            Number ancienne, Number nouvelle) {
                            if (nouvelle.doubleValue() > 0) {
                                table.widthProperty().removeListener(this);
                                appliquer(table, autoriserDefilement);
                            }
                        }
                    });
                    return;
                }

                double totalRequis = 0;
                double[] largeurs = new double[table.getColumns().size()];

                for (int i = 0; i < table.getColumns().size(); i++) {
                    TableColumn<?, ?> colonne = table.getColumns().get(i);
                    if (!colonne.isVisible()) {
                        continue;
                    }
                    double requise = mesurerColonne(table, colonne);
                    // On respecte les bornes déjà posées dans le FXML (min/max métier).
                    // Le minWidth d'origine est mémorisé au premier passage : comme on le
                    // réécrit ensuite, s'en resservir tel quel empêcherait toute réduction
                    // après un filtre qui ne laisse que des valeurs courtes.
                    requise = Math.max(requise, minWidthInitial(colonne));
                    requise = Math.min(requise, Math.min(colonne.getMaxWidth(), LARGEUR_MAX_COLONNE));
                    largeurs[i] = requise;
                    totalRequis += requise;
                }

                double largeurDisponible = table.getWidth() - 18; // barre de défilement verticale
                boolean debordement = totalRequis > largeurDisponible && largeurDisponible > 0;

                if (debordement && autoriserDefilement) {
                    // Chaque colonne garde sa largeur idéale → défilement horizontal.
                    // minWidth est fixé en plus de prefWidth : sans cela, le recalcul de
                    // mise en page peut recomprimer la colonne et retronquer le texte.
                    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
                    for (int i = 0; i < table.getColumns().size(); i++) {
                        if (largeurs[i] > 0) {
                            TableColumn<?, ?> colonne = table.getColumns().get(i);
                            colonne.setMinWidth(largeurs[i]);
                            colonne.setPrefWidth(largeurs[i]);
                        }
                    }
                } else {
                    // Pas de débordement, ou défilement interdit : les largeurs mesurées
                    // deviennent des proportions et la politique contrainte remplit la table.
                    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                    for (int i = 0; i < table.getColumns().size(); i++) {
                        if (largeurs[i] > 0) {
                            table.getColumns().get(i).setPrefWidth(largeurs[i]);
                        }
                    }
                }
            } catch (Exception e) {
                // Un défaut d'ajustement visuel ne doit jamais empêcher d'utiliser l'écran.
                org.slf4j.LoggerFactory.getLogger(TableUtils.class)
                        .warn("Ajustement des colonnes ignoré : {}", e.getMessage());
            }
        });
    }

    /** Clé de mémorisation de la contrainte de largeur minimale définie dans le FXML. */
    private static final String CLE_MIN_INITIAL = "vetpharma.minWidthInitial";

    /**
     * Largeur minimale voulue par le FXML, mémorisée au premier ajustement.
     * Indispensable car {@link #appliquer} réécrit ensuite {@code minWidth}.
     */
    private static double minWidthInitial(TableColumn<?, ?> colonne) {
        Object memorise = colonne.getProperties().get(CLE_MIN_INITIAL);
        if (memorise instanceof Double valeur) {
            return valeur;
        }
        double initial = colonne.getMinWidth();
        colonne.getProperties().put(CLE_MIN_INITIAL, initial);
        return initial;
    }

    /** Largeur nécessaire à une colonne : le plus large entre son en-tête et ses cellules. */
    private static double mesurerColonne(TableView<?> table, TableColumn<?, ?> colonne) {
        Font police = policeDeReference(table);
        double largeur = mesurerTexte(colonne.getText(), police) + MARGE_ENTETE;

        // 1. Cellules réellement rendues : c'est le texte FORMATÉ (« 12 500 FCFA »,
        //    « 11 bte + 0 unit. »), donc la mesure la plus fidèle.
        for (Node noeud : table.lookupAll(".table-cell")) {
            if (noeud instanceof TableCell<?, ?> cellule
                    && cellule.getTableColumn() == colonne
                    && cellule.getText() != null && !cellule.getText().isEmpty()) {
                largeur = Math.max(largeur,
                        mesurerTexte(cellule.getText(), cellule.getFont() != null ? cellule.getFont() : police)
                                + MARGE_CELLULE);
            }
        }

        // 2. Données hors écran (virtualisation) : on mesure la valeur brute. Elle peut
        //    différer du rendu final, d'où la marge — mais elle évite qu'un nom présent
        //    en ligne 300 soit tronqué dès qu'on y défile.
        int lignes = Math.min(table.getItems().size(), MAX_LIGNES_MESUREES);
        for (int i = 0; i < lignes; i++) {
            try {
                Object valeur = colonne.getCellData(i);
                if (valeur != null) {
                    largeur = Math.max(largeur, mesurerTexte(valeur.toString(), police) + MARGE_CELLULE);
                }
            } catch (Exception ignore) {
                // Colonne calculée non lisible hors rendu : la mesure du point 1 suffit.
            }
        }
        return largeur;
    }

    private static double mesurerTexte(String texte, Font police) {
        if (texte == null || texte.isEmpty()) {
            return 0;
        }
        Text mesure = new Text(texte);
        if (police != null) {
            mesure.setFont(police);
        }
        return mesure.getLayoutBounds().getWidth();
    }

    /** Police effective du tableau (à défaut, la police par défaut du système). */
    private static Font policeDeReference(TableView<?> table) {
        for (Node noeud : table.lookupAll(".table-cell")) {
            if (noeud instanceof TableCell<?, ?> cellule && cellule.getFont() != null) {
                return cellule.getFont();
            }
        }
        return Font.getDefault();
    }

    /**
     * Ajoute une infobulle sur chaque cellule des colonnes texte données : quand une
     * valeur est plus large que sa colonne (nom de produit long, email...), le survol
     * de la souris affiche le texte complet.
     *
     * <p>À appeler APRÈS avoir défini les cellValueFactory des colonnes.</p>
     */
    @SafeVarargs
    public static <S> void tooltipSurColonnes(TableColumn<S, String>... colonnes) {
        for (TableColumn<S, String> colonne : colonnes) {
            colonne.setCellFactory(c -> new TableCell<S, String>() {
                @Override
                protected void updateItem(String item, boolean vide) {
                    super.updateItem(item, vide);
                    if (vide || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        Tooltip infobulle = new Tooltip(item);
                        infobulle.setShowDelay(Duration.millis(300));
                        infobulle.setWrapText(true);
                        infobulle.setMaxWidth(400);
                        setTooltip(infobulle);
                    }
                }
            });
        }
    }

    /** Variante pour un libellé simple dont le texte peut être abrégé. */
    public static void infobulle(Label label, String texteComplet) {
        if (label != null && texteComplet != null && !texteComplet.isBlank()) {
            Tooltip infobulle = new Tooltip(texteComplet);
            infobulle.setShowDelay(Duration.millis(300));
            label.setTooltip(infobulle);
        }
    }
}
