package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ajustements_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjustementStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime dateAjustement;

    @Column(name = "quantiteRetiree", nullable = false)
    private Integer quantite;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_ajustement", length = 32)
    private MouvementStock.TypeMouvement typeAjustement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotifAjustement motif;

    @Column(length = 255)
    private String observation;

    // Rétrocompatibilité : on initialise typeAjustement à NEGATIF par défaut si null
    @PrePersist
    @PreUpdate
    private void ensureTypeAjustement() {
        if (typeAjustement == null) {
            typeAjustement = MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF;
        }
    }

    public enum MotifAjustement {
        CASSE("Casse / Produit endommagé"),
        PEREMPTION("Produit périmé"),
        ERREUR_INVENTAIRE("Erreur d'inventaire"),
        USAGE_INTERNE("Usage interne"),
        EXCEDENT_INVENTAIRE("Excédent physique constaté"),
        RETOUR_INTERNE("Retour Usage Interne"),
        AUTRE("Autre (Préciser en observation)");

        private final String label;
        MotifAjustement(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }
}
