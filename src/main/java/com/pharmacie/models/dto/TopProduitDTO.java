package com.pharmacie.models.dto;

public class TopProduitDTO {
    private String nom;
    private int quantite;
    private long marge;

    public TopProduitDTO() {}

    public TopProduitDTO(String nom, int quantite, long marge) {
        this.nom = nom;
        this.quantite = quantite;
        this.marge = marge;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public long getMarge() { return marge; }
    public void setMarge(long marge) { this.marge = marge; }
}
