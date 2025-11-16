package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.ModePaiement;
import tn.weeding.agenceevenementielle.entities.enums.StatutPaiement;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Paiement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPaiement;

    @Column(unique = true, nullable = false)
    private String codePaiement;

    @Column(nullable = false)
    private LocalDateTime datePaiement;

    @Column(nullable = false)
    private Double montantPaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statutPaiement;

    @Column(length = 1000)
    private String descriptionPaiement;

    @Column(length = 1000)
    private String motifRefus;

    private String referenceExterne;
    private String validePar;
    private LocalDateTime dateValidation;


    @ManyToOne
    @JoinColumn(nullable = false)
    private Reservation reservation;

    @PrePersist
    public void prePersist() {
        if (datePaiement == null) {
            datePaiement = LocalDateTime.now();
        }
        if (statutPaiement == null) {
            statutPaiement = StatutPaiement.EN_ATTENTE;
        }
    }
}