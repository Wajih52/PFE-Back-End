package tn.weeding.agenceevenementielle.dto.pointage;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO pour la création/modification d'un pointage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageRequestDto {

    @NotNull(message = "La date de travail est obligatoire")
    private LocalDate dateTravail;

    private LocalTime heureDebut;

    private LocalTime heureFin;

    private StatutPointage statutPointage;

    private String description;

    // Pour l'admin/manager qui crée un pointage pour un employé
    private Long idUtilisateur;
}
