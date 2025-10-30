package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;

import java.sql.Time;
import java.util.Date;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class Pointage implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idPointage;
    @Temporal(TemporalType.DATE)
    Date dateTravail;
    @Temporal(TemporalType.TIME)
    Time heureDebut;
    @Temporal(TemporalType.TIME)
    Time heureFin;
    @Enumerated(EnumType.STRING)
    StatutPointage statutPointage;
    Double totalHeures;
    String description;

    //Pointage 0..* ---------------- 1 Utilisateur
    @ManyToOne
    Utilisateur utilisateur;

}
