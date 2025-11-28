package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutAvis;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Avis implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAvis;

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer note;

    @Column(length = 1000)
    private String commentaire;

    @Column(nullable = false)
    private LocalDateTime dateAvis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAvis statut;

    @Column(length = 500)
    private String commentaireModeration;

    private LocalDateTime dateModeration;

    @Column(nullable = false)
    private Boolean visible;

    // ====== RELATIONS ======

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idUtilisateur", nullable = false)
    private Utilisateur client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idProduit", nullable = false)
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idReservation", nullable = false)
    private Reservation reservation;

    @PrePersist
    protected void onCreate() {
        dateAvis = LocalDateTime.now();
        if (statut == null) {
            statut = StatutAvis.EN_ATTENTE;
        }
        if (visible == null) {
            visible = true;
        }
    }
}