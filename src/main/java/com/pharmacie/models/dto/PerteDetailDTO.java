package com.pharmacie.models.dto;

public class PerteDetailDTO {
    private String produit;
    private String numeroLot;
    private int quantite;
    private long valeur;
    private String motif;

    public PerteDetailDTO() {}

    public PerteDetailDTO(String produit, String numeroLot, int quantite, long valeur, String motif) {
        this.produit = produit;
        this.numeroLot = numeroLot;
        this.quantite = quantite;
        this.valeur = valeur;
        this.motif = motif;
    }

    public String getProduit() { return produit; }
    public void setProduit(String produit) { this.produit = produit; }

    public String getNumeroLot() { return numeroLot; }
    public void setNumeroLot(String numeroLot) { this.numeroLot = numeroLot; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public long getValeur() { return valeur; }
    public void setValeur(long valeur) { this.valeur = valeur; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}
