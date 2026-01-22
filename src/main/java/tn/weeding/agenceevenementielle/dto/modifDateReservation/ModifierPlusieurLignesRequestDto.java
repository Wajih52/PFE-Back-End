package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO POUR MODIFIER PLUSIEURS LIGNES SPÉCIFIQUES
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierPlusieurLignesRequestDto {

    @NotEmpty(message = "La liste des modifications ne peut pas être vide")
    @Valid
    private List<ModificationLigneDto> modifications;

    /**
     * Motif global optionnel
     */
    private String motif;

    /**
     * DTO interne : Une modification de ligne
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModificationLigneDto {

        @NotNull(message = "L'ID de la ligne est obligatoire")
        private Long idLigne;

        @NotNull(message = "La date de début est obligatoire")
        private LocalDate nouvelleDateDebut;

        @NotNull(message = "La date de fin est obligatoire")
        private LocalDate nouvelleDateFin;
    }
}