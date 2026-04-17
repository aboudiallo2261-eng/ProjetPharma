package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "lignes_achat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneAchat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "achat_id")
    private Achat achat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(nullable = false)
    private Integer quantiteAchetee;

    @Column(nullable = false)
    private Double prixUnitaire;
}
