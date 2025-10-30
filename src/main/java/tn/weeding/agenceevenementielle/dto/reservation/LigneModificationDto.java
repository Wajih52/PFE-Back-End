package tn.weeding.agenceevenementielle.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour modifier une ligne de réservation (par l'admin lors de la validation du devis)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneModificationDto {

    @NotNull(message = "L'ID de la ligne est obligatoire")
    private Long idLigneReservation;

    // Nouveau prix unitaire (si modifié)
    @Min(value = 0, message = "Le prix doit être positif")
    private Double nouveauPrixUnitaire;

    // Nouvelle quantité (si modifiée)
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer nouvelleQuantite;
}
