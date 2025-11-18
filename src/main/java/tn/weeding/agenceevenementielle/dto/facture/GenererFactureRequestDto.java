package tn.weeding.agenceevenementielle.dto.facture;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenererFactureRequestDto {

    @NotNull(message = "L'ID de la réservation est obligatoire")
    private Long idReservation;

    @NotNull(message = "Le type de facture est obligatoire")
    private TypeFacture typeFacture;

    private String notes;
    private String conditionsPaiement;
}