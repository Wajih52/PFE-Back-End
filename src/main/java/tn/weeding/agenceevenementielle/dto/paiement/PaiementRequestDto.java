package tn.weeding.agenceevenementielle.dto.paiement;

import jakarta.validation.constraints.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.ModePaiement;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaiementRequestDto {

    @NotNull(message = "L'ID de la réservation est obligatoire")
    private Long idReservation;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private Double montantPaiement;

    @NotNull(message = "Le mode de paiement est obligatoire")
    private ModePaiement modePaiement;

    @Size(max = 1000)
    private String descriptionPaiement;

    @Size(max = 255)
    private String referenceExterne;
}