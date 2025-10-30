package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.ModePaiement;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;

import java.util.Date;
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
    @Temporal(TemporalType.DATE)
    Date dateDebut;
    @Temporal(TemporalType.DATE)
    Date dateFin;
    @Enumerated(EnumType.STRING)
    StatutReservation statutReservation;
    Double montantTotal;
    Double montantPaye;
    ModePaiement modePaiementRes;
    @Enumerated(EnumType.STRING)
    StatutLivraison statutLivraisonRes;

    //Reservation 0..* ----------- 1 Utilisateur
    @ManyToOne
    Utilisateur utilisateur;

    //Reservation 1 ------- 1..* Paiement
    @OneToMany (mappedBy = "reservation")
    Set<Paiement> paiements;

     //Reservation 1----------- 1..* LigneReservation
    @OneToMany
    Set<LigneReservation> ligneReservations;
}
