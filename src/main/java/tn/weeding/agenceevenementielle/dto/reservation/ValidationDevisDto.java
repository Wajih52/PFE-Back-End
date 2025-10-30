package tn.weeding.agenceevenementielle.dto.reservation;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO pour la validation du devis par le CLIENT
 * Le client accepte ou refuse le devis modifié par l'admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationDevisDto {

    @NotNull(message = "L'ID de la réservation est obligatoire")
    private Long idReservation;

    @NotNull(message = "La décision (accepter/refuser) est obligatoire")
    private Boolean accepter;  // true = confirmer, false = annuler

    // Commentaire du client (optionnel)
    @Size(max = 500, message = "Le commentaire ne doit pas dépasser 500 caractères")
    private String commentaireClient;
}

