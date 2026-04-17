package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "produits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "prixAchat", nullable = false)
    private Double prixAchat = 0.0;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espece_id")
    private Espece espece;

    @Column(nullable = false)
    private Double prixVente; // Prix de la boite entière

    @Column(nullable = false)
    private Boolean estDeconditionnable = false;

    @Column(nullable = true)
    private Integer unitesParBoite;

    @Column(nullable = true)
    private Double prixVenteUnite; // Prix d'une unité au détail

    @Column(nullable = false)
    private Integer seuilAlerte = 5;
}
