package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class Reservation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idReservation;

    @Column(name = "referenceReservation")
    String referenceReservation;

    LocalDateTime dateCreation;
    LocalDateTime dateModification;

    LocalDate dateDebut;
    LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    StatutReservation statutReservation;

    Double montantOriginal;
    Double remisePourcentage;
    Double remiseMontant;
    Double montantTotal;

    Double montantPaye;

    @Enumerated(EnumType.STRING)
    ModePaiement modePaiementRes;

    @Enumerated(EnumType.STRING)
    StatutPaiementRes statutPaiement;


    @Enumerated(EnumType.STRING)
    StatutLivraison statutLivraisonRes;

    @Column(length = 2000)
    String commentaireAdmin;

    @Column(length = 2000)
    String commentaireClient;

    boolean validationAutomatique = false;

    private LocalDateTime dateExpirationDevis;

    private Boolean stockReserve = false;

    //Reservation 0..* ----------- 1 Utilisateur
    @ManyToOne
    Utilisateur utilisateur;

    //Reservation 1 ------- 1..* Paiement
    @OneToMany(mappedBy = "reservation")
    Set<Paiement> paiements;

    //Reservation 1----------- 1..* LigneReservation
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<LigneReservation> ligneReservations;

    //Reservation 1 ---------- 1..* Facture
    @OneToMany(mappedBy = "reservation",cascade = CascadeType.ALL)
    Set<Facture> factures ;

    //Rservation 1------------------1..* Reclamation
    @OneToMany(mappedBy = "reservation",cascade = CascadeType.ALL)
    Set<Reclamation> reclamations;

    @PrePersist
    public void prePersist() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        dateModification = LocalDateTime.now();
    }

    public boolean isStockReserve() {
        return stockReserve;
    }

    public boolean isPaiementComplet(){
        return Objects.equals(montantTotal, montantPaye);
    }
}
