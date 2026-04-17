package com.pharmacie.models;

import jakarta.persistence.*;

@Entity
@Table(name = "profils")
public class Profil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(length = 255)
    private String description;

    // --- Droits d'accès (Booléens) ---
    
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canAccessDashboard;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canAccessVentes;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canAccessAchats;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canAccessStock;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canAccessFournisseurs;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canAccessRapports;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean canAccessParametres;

    // --- Constructeurs ---

    public Profil() {}

    public Profil(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCanAccessDashboard() { return canAccessDashboard; }
    public void setCanAccessDashboard(boolean canAccessDashboard) { this.canAccessDashboard = canAccessDashboard; }

    public boolean isCanAccessVentes() { return canAccessVentes; }
    public void setCanAccessVentes(boolean canAccessVentes) { this.canAccessVentes = canAccessVentes; }

    public boolean isCanAccessAchats() { return canAccessAchats; }
    public void setCanAccessAchats(boolean canAccessAchats) { this.canAccessAchats = canAccessAchats; }

    public boolean isCanAccessStock() { return canAccessStock; }
    public void setCanAccessStock(boolean canAccessStock) { this.canAccessStock = canAccessStock; }

    public boolean isCanAccessFournisseurs() { return canAccessFournisseurs; }
    public void setCanAccessFournisseurs(boolean canAccessFournisseurs) { this.canAccessFournisseurs = canAccessFournisseurs; }

    public boolean isCanAccessRapports() { return canAccessRapports; }
    public void setCanAccessRapports(boolean canAccessRapports) { this.canAccessRapports = canAccessRapports; }

    public boolean isCanAccessParametres() { return canAccessParametres; }
    public void setCanAccessParametres(boolean canAccessParametres) { this.canAccessParametres = canAccessParametres; }

    @Override
    public String toString() {
        return this.nom;
    }
}
