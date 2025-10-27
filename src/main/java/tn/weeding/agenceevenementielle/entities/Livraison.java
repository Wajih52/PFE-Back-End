package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class Livraison implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idLivraison;
    String titreLivraison;
    String adresserLivraison;
    @Temporal(TemporalType.DATE)
    Date dateLivraison;
    @Temporal(TemporalType.TIME)
    Time heureLivraison;
    @Enumerated(EnumType.STRING)
    StatutLivraison statutLivraison;

    //Livraison 0..1 ------------ 1..* LigneReservation
    @OneToMany(mappedBy = "livraison")
    Set<LigneReservation> ligneReservations;

    //Livraison 1 ----------- 1..* AffectationLivraison
    @OneToMany
    Set<AffectationLivraison> affectationLivraisons;
}
