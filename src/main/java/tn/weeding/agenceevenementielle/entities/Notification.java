package tn.weeding.agenceevenementielle.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Notification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotification;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeNotification typeNotification;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private Boolean lue = false;

    private LocalDateTime dateLecture;

    // Lien vers l'entité concernée (optionnel)
    private Long idReservation;
    private Long idLivraison;
    private Long idPaiement;
    private Long idProduit;

    // URL pour action directe (optionnel)
    @Column(length = 500)
    private String urlAction;

    // Notification pour quel utilisateur
    @ManyToOne
    @JoinColumn(name = "idUtilisateur", nullable = false)
    private Utilisateur utilisateur;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}