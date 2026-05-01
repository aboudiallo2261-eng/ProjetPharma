package com.pharmacie.models.dto;

/**
 * Data Transfer Object (DTO) pour simplifier les données des alertes de stock.
 * Allège considérablement le flux JSON vers le Web.
 */
public class ProduitRuptureDTO {
    private Long id;
    private String nom;
    private int stockPhysique;

    public ProduitRuptureDTO(Long id, String nom, int stockPhysique) {
        this.id = id;
        this.nom = nom;
        this.stockPhysique = stockPhysique;
    }

    // Getters
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public int getStockPhysique() { return stockPhysique; }
}
