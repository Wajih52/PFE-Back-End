package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;

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

public class Pointage implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idPointage;


    LocalDate dateTravail;


    LocalTime heureDebut;

    @Temporal(TemporalType.TIME)
    LocalTime heureFin;

    @Enumerated(EnumType.STRING)
    StatutPointage statutPointage;

    Double totalHeures;

    String description;

    //Pointage 0..* ---------------- 1 Utilisateur
    @ManyToOne
    Utilisateur utilisateur;

}
