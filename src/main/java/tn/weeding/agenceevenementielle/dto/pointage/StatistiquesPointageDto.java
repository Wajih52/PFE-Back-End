package tn.weeding.agenceevenementielle.dto.pointage;

import lombok.*;

/**
 * DTO pour les statistiques de pointage d'un employé
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatistiquesPointageDto {

    private Long idUtilisateur;
    private String nomComplet;
    private String poste;

    // Statistiques du mois en cours
    private Integer joursPresents;
    private Integer joursAbsents;
    private Integer joursEnRetard;
    private Integer joursEnConge;

    // Heures de travail
    private Double totalHeuresTravaillees;
    private Double moyenneHeuresParJour;

    // Taux
    private Double tauxPresence; // En pourcentage
    private Double tauxRetard; // En pourcentage

    // Retards
    private Integer totalRetards;
    private Integer totalMinutesRetard;

    // Période
    private String periodeDebut;
    private String periodeFin;
}
