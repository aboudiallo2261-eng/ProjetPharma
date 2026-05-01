package com.pharmacie.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente un ticket (panier) suspendu temporairement en file d'attente.
 * Extraite du VenteController pour permettre une persistance de session globale.
 */
public class TicketEnAttente {
    private final int numero;
    private final LocalDateTime heure;
    private final List<LigneVente> lignes;
    private final List<LigneVente> reservedLines; // Point 9 : Pour tracer les lots exacts réservés
    private final double total;
    private final LocalDateTime expirationTime; // Point 8 : Expiration après 2 heures

    public TicketEnAttente(int num, List<LigneVente> lignes, List<LigneVente> reservedLines, double total) {
        this.numero = num;
        this.heure = LocalDateTime.now();
        this.lignes = new ArrayList<>(lignes);
        this.reservedLines = reservedLines != null ? new ArrayList<>(reservedLines) : new ArrayList<>();
        this.total = total;
        this.expirationTime = this.heure.plusHours(2);
    }

    public List<LigneVente> getReservedLines() {
        return reservedLines;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    public int getNumero() {
        return numero;
    }

    public LocalDateTime getHeure() {
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
