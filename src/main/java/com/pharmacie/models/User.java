package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String identifiant;

    @Column(nullable = false)
    private String motDePasseHash;

    @Column(name = "role", nullable = false)
    private String role = "AGENT";

    @Column(nullable = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "profil_id")
    private Profil profil;

    /**
     * Force le changement de mot de passe à la prochaine connexion.
     * Positionné à true pour le compte admin par défaut (admin/admin) et
     * lors d'une réinitialisation de mot de passe par un administrateur.
     */
    @Column(name = "must_change_password", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean mustChangePassword = false;
}
