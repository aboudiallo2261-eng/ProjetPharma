package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @Column(nullable = false)
    private String numeroLot;

    @Column(nullable = true)
    private LocalDate dateExpiration;

    // Stock tracké dans la plus petite unité de vente (unité de détail ou boîte selon le produit)
    @Column(nullable = false)
    private Integer quantiteStock;

    /**
     * Archivage logique : true quand le stock atteint 0.
     * Le lot reste en base pour l'intégrité de l'Audit Trail et des rapports financiers.
     * Il disparaît simplement de l'affichage "État du Stock" actif.
     */
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean estArchive = false;

    /**
     * Prix d'achat unitaire (par boîte) au moment de la commande de CE lot.
     * Chaque lot conserve son propre prix d'achat, indépendamment des achats ultérieurs.
     * Cela garantit que le calcul de bénéfice et la valorisation du stock
     * utilisent le coût réel d'acquisition, pas le dernier prix fournisseur.
     *
     * <p>Nullable pour rétrocompatibilité avec les lots créés avant cette migration.
     * Dans ce cas, le système utilise {@code Produit.prixAchat} comme fallback.</p>
     */
    @Column(name = "prix_achat")
    private Double prixAchat;
}
