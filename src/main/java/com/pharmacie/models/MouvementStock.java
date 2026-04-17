package com.pharmacie.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements_stock", indexes = {
    @Index(name = "idx_mvt_date", columnList = "date_mouvement"),
    @Index(name = "idx_mvt_produit", columnList = "produit_id"),
    @Index(name = "idx_mvt_user", columnList = "user_id")
})
public class MouvementStock {

    public enum TypeMouvement {
        ACHAT,
        VENTE,
        AJUSTEMENT_POSITIF,
        AJUSTEMENT_NEGATIF
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 30)
    private TypeMouvement typeMouvement;

    @Column(nullable = false)
    private Integer quantite; // Toujours relatif à l'opération de la base. Ex: 50 ou -2

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @Column(length = 255)
    private String reference; // Motif ou #Ticket, #Facture

    public MouvementStock() {
    }

    public MouvementStock(Produit produit, Lot lot, User user, TypeMouvement typeMouvement, Integer quantite, LocalDateTime dateMouvement, String reference) {
        this.produit = produit;
        this.lot = lot;
        this.user = user;
        this.typeMouvement = typeMouvement;
        this.quantite = quantite;
        this.dateMouvement = dateMouvement;
        this.reference = reference;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public Lot getLot() { return lot; }
    public void setLot(Lot lot) { this.lot = lot; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TypeMouvement getTypeMouvement() { return typeMouvement; }
    public void setTypeMouvement(TypeMouvement typeMouvement) { this.typeMouvement = typeMouvement; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public LocalDateTime getDateMouvement() { return dateMouvement; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
