package tn.weeding.agenceevenementielle.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutFacture;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Facture implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFacture;

    // Numéro de facture unique (définitif pour facture finale)
    private String numeroFacture;

    // Type de facture
    @Enumerated(EnumType.STRING)
    private TypeFacture typeFacture;

    // Statut de la facture
    @Enumerated(EnumType.STRING)
    private StatutFacture statutFacture;

    // Dates
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDate dateEcheance; // Date limite de paiement

    // Montants (calculés à partir de la réservation)
    private Double montantHT;
    private Double montantTVA;
    private Double montantTTC;
    private Double montantRemise;

    // Informations complémentaires
    @Column(length = 2000)
    private String notes;

    @Column(length = 500)
    private String conditionsPaiement;

    // Chemin du fichier PDF généré
    private String cheminPDF;

    // Lien vers la réservation
    @ManyToOne
    @JoinColumn(name = "idReservation")
    private Reservation reservation;

    // Qui a généré la facture
    private String generePar;

    @PrePersist
    public void prePersist() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        dateModification = LocalDateTime.now();
    }
}