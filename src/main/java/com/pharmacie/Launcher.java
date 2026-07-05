package com.pharmacie;

/**
 * Point d'entrée pour le packaging (jpackage).
 *
 * <p>JavaFX refuse de démarrer si la classe main étend {@code Application}
 * alors que JavaFX est sur le classpath (et non le module-path). Ce lanceur
 * ne fait que déléguer à {@link MainApp#main(String[])} — contournement
 * standard pour distribuer une application JavaFX non modulaire.</p>
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
