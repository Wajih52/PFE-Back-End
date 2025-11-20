package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class AffectationLivraison implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idAffectationLivraison;


    LocalDate dateAffectationLivraison;

    LocalTime heureDebut;

    LocalTime heureFin;

    String notes;

    //AffectationLivraison 1..* --------------- 1 Livraison
    @ManyToOne
    Livraison livraison;

    //AffectationLivraison 1..* ------------- 1 Utilisateur
    @ManyToOne
    Utilisateur utilisateur;
}
