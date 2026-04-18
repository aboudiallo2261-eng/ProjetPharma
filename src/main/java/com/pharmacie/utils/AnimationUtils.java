package com.pharmacie.utils;

import javafx.animation.TranslateTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Utilitaires d'Animation (UI) pour l'ensemble du projet.
 */
public class AnimationUtils {

    /**
     * Applique un effet "shake" (secousse rouge) à un noeud JavaFX pour indiquer une erreur.
     * Cette animation est un standard de l'UX design moderne (ex: mot de passe incorrect).
     * 
     * @param node Le composant à faire trembler (TextField, Box, etc.)
     */
    public static void shake(Node node) {
        if (node == null) return;
        
        TranslateTransition t1 = new TranslateTransition(Duration.millis(50), node);
        t1.setByX(10);
        
        TranslateTransition t2 = new TranslateTransition(Duration.millis(50), node);
        t2.setByX(-20);
        
        TranslateTransition t3 = new TranslateTransition(Duration.millis(50), node);
        t3.setByX(20);
        
        TranslateTransition t4 = new TranslateTransition(Duration.millis(50), node);
        t4.setByX(-20);
        
        TranslateTransition t5 = new TranslateTransition(Duration.millis(50), node);
        t5.setByX(10);
        
        SequentialTransition seq = new SequentialTransition(node, t1, t2, t3, t4, t5);
        seq.play();
        
        // Flash visuel : on stocke le style original pour pouvoir le remettre
        String oldStyle = node.getStyle();
        // On modifie l'apparence avec une bordure rouge marquée mais sans écraser la largeur
        node.setStyle(oldStyle + "; -fx-border-color: #E74C3C; -fx-border-width: 2px; -fx-border-radius: 4px;");
        
        seq.setOnFinished(e -> {
            node.setStyle(oldStyle);
        });
    }
}
