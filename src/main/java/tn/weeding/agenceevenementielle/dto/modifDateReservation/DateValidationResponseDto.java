package tn.weeding.agenceevenementielle.dto.modifDateReservation;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de validation de dates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateValidationResponseDto {

    /**
     * La période est-elle valide ?
     */
    private Boolean valide;

    /**
     * Message d'erreur ou de succès
     */
    private String message;

    /**
     * Nombre de jours de la période (si valide)
     */
    private Long nombreJours;
}