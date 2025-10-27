package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Time;
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
    @Temporal(TemporalType.DATE)
    Date dateAffectationLivraison;
    @Temporal(TemporalType.TIME)
    Time heureDebut;
    @Temporal(TemporalType.TIME)
    Time heureFin;

    //AffectationLivraison 1..* --------------- 1 Livraison
    @ManyToOne
    Livraison livraison;

    //AffectationLivraison 1..* ------------- 1 Utilisateur
    @ManyToOne
    Utilisateur utilisateur;
}
