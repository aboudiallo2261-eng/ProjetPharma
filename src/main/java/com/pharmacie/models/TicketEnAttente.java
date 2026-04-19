package com.pharmacie.models;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente un ticket (panier) suspendu temporairement en file d'attente.
 * Extraite du VenteController pour permettre une persistance de session globale.
 */
public class TicketEnAttente {
    private final int numero;
    private final LocalTime heure;
    private final List<LigneVente> lignes;
    private final double total;

    public TicketEnAttente(int num, List<LigneVente> lignes, double total) {
        this.numero = num;
        this.heure = LocalTime.now();
        this.lignes = new ArrayList<>(lignes);
        this.total = total;
    }

    public int getNumero() {
        return numero;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public List<LigneVente> getLignes() {
        return lignes;
    }

    public double getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return String.format("Ticket #%d | %s | %.0f FCFA | %d article(s)",
                numero, heure.format(DateTimeFormatter.ofPattern("HH:mm")),
                total, lignes.size());
    }
}
