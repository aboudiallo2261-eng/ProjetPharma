package com.pharmacie.models.dto;

public class TopProduitDTO {
    private String nom;
    private int quantite;       // Total (pour tri) = boites + unites
    private int quantiteBoites; // Boîtes entières vendues
    private int quantiteUnites; // Unités au détail vendues
    private long marge;

    public TopProduitDTO() {}

    public TopProduitDTO(String nom, int quantite, int quantiteBoites, int quantiteUnites, long marge) {
        this.nom = nom;
        this.quantite = quantite;
        this.quantiteBoites = quantiteBoites;
        this.quantiteUnites = quantiteUnites;
        this.marge = marge;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public int getQuantiteBoites() { return quantiteBoites; }
    public void setQuantiteBoites(int quantiteBoites) { this.quantiteBoites = quantiteBoites; }

    public int getQuantiteUnites() { return quantiteUnites; }
    public void setQuantiteUnites(int quantiteUnites) { this.quantiteUnites = quantiteUnites; }

    public long getMarge() { return marge; }
    public void setMarge(long marge) { this.marge = marge; }
}
