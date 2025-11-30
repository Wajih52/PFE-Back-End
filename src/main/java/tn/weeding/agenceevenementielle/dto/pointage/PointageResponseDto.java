package tn.weeding.agenceevenementielle.dto.pointage;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de réponse pour un pointage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageResponseDto {

    private Long idPointage;
    private LocalDate dateTravail;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private StatutPointage statutPointage;
    private Double totalHeures;
    private String description;

    // Informations utilisateur
    private Long idUtilisateur;
    private String nomUtilisateur;
    private String prenomUtilisateur;
    private String pseudoUtilisateur;
    private String poste;

    // Indicateurs
    private Boolean estEnRetard;
    private Integer minutesRetard;
    private Boolean estComplet; // heureDebut et heureFin présents
}
