package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ventes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime dateVente;

    @Column(nullable = false)
    private Double total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModePaiement modePaiement;

    @ManyToOne(optional = true)
    @JoinColumn(name = "session_caisse_id")
    private SessionCaisse sessionCaisse;

    @Column(nullable = true)
    private Double montantEspeces;

    @Column(nullable = true)
    private Double montantMobile;

    @Column(nullable = true)
    private Double montantRecu;

    @Column(nullable = true)
    private Double monnaieRendue;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LigneVente> lignesVente;

    public enum ModePaiement {
        ESPECES, MOBILE_MONEY, MIXTE
    }

    public String getNumeroTicketOfficiel() {
        if (id == null) return "NON_SAUVEGARDE";
        if (dateVente == null) return String.valueOf(id);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("ddMMyy-HHmm");
        return dateVente.format(formatter) + "-" + String.format("%03d", id);
    }
}
