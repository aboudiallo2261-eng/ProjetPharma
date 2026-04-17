package com.pharmacie.models;

import jakarta.persistence.*;

@Entity
@Table(name = "pharmacie_info")
public class PharmacieInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 255)
    private String adresse;

    @Column(length = 50)
    private String telephone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String messageTicket;

    public PharmacieInfo() {}

    public PharmacieInfo(String nom, String adresse, String telephone, String email, String messageTicket) {
        this.nom = nom;
        this.adresse = adresse;
        this.telephone = telephone;
        this.email = email;
        this.messageTicket = messageTicket;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMessageTicket() { return messageTicket; }
    public void setMessageTicket(String messageTicket) { this.messageTicket = messageTicket; }
}
