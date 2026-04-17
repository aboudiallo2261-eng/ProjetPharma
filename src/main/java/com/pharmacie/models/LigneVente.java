package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "lignes_vente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneVente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vente_id")
    private Vente vente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(nullable = false)
    private Integer quantiteVendue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeUnite typeUnite;

    @Column(nullable = false)
    private Double prixUnitaire;

    @Column(nullable = false)
    private Double sousTotal;

    public enum TypeUnite {
        BOITE_ENTIERE, DETAIL
    }
}
