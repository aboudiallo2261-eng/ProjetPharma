package com.pharmacie.models.dto;

/**
 * Data Transfer Object (DTO) pour simplifier les données des lots périmés.
 * Allège considérablement le flux JSON vers le Web.
 */
public class ProduitPerimeDTO {
    private String nom;
    private String numeroLot;
    private String dateExpiration;
    private int stockRestant;

    public ProduitPerimeDTO(String nom, String numeroLot, String dateExpiration, int stockRestant) {
        this.nom = nom;
        this.numeroLot = numeroLot;
        this.dateExpiration = dateExpiration;
        this.stockRestant = stockRestant;
    }

    // Getters
    public String getNom() { return nom; }
    public String getNumeroLot() { return numeroLot; }
    public String getDateExpiration() { return dateExpiration; }
    public int getStockRestant() { return stockRestant; }
}
