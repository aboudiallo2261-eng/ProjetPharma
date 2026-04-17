package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "achats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @Column(nullable = false)
    private LocalDateTime dateAchat;

    @Column(nullable = true)
    private String referenceFacture;

    @OneToMany(mappedBy = "achat", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LigneAchat> lignesAchat;
}
