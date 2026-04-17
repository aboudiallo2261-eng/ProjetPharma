package com.pharmacie.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions_caisse")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCaisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime dateOuverture;

    private LocalDateTime dateCloture;

    @Column(nullable = false)
    private Double fondDeCaisse = 0.0;

    private Double totalEspecesAttendu = 0.0;
    
    @Column(name = "especes_declare")
    private Double especesDeclare = 0.0;
    
    @Column(name = "ecart_especes")
    private Double ecartEspeces = 0.0;

    // --- MOBILE MONEY ---
    private Double totalMobileAttendu = 0.0;
    
    private Double mobileDeclare = 0.0;
    
    private Double ecartMobile = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutSession statut = StatutSession.OUVERTE;

    public enum StatutSession {
        OUVERTE, FERMEE
    }
}
