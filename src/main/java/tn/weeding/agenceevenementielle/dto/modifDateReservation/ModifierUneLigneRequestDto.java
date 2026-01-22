package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO POUR MODIFIER UNE SEULE LIGNE DE RÉSERVATION
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierUneLigneRequestDto {

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate nouvelleDateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate nouvelleDateFin;

    /**
     * Motif optionnel pour l'historique
     */
    private String motif;
}