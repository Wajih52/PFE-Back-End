package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;


import java.util.Date;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Paiement implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idPaiement;
    String codePaiement;
    @Temporal(TemporalType.DATE)
    Date datePaiement;
    @Enumerated(EnumType.STRING)
    ModePaiement modePaiement;
    String descriptionPaiement;


    // Paiement 1..* -------------- 1 Reservation
    @ManyToOne
    Reservation reservation;


}
