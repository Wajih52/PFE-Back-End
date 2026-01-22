package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO POUR DÉCALER TOUTES LES LIGNES D'UNE RÉSERVATION
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecalerToutesLignesRequestDto {

    @NotNull(message = "Le nombre de jours est obligatoire")
    private Integer nombreJours;  // +7 pour avancer, -7 pour reculer

    @NotNull(message = "Le motif est obligatoire")
    private String motif;  // Ex: "Mariage reporté d'une semaine"
}