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
}
