package com.pharmacie.models.dto;

/**
 * Data Transfer Object (DTO) pour simplifier les données des alertes de stock.
 * Allège considérablement le flux JSON vers le Web.
 */
public class ProduitRuptureDTO {
    private Long id;
    private String nom;
    private int stockPhysique;
    private int seuilAlerte;

    public ProduitRuptureDTO(Long id, String nom, int stockPhysique, int seuilAlerte) {
        this.id = id;
        this.nom = nom;
        this.stockPhysique = stockPhysique;
        this.seuilAlerte = seuilAlerte;
    }

    // Getters
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public int getStockPhysique() { return stockPhysique; }
    public int getSeuilAlerte() { return seuilAlerte; }
}
