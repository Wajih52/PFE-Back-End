package tn.weeding.agenceevenementielle.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO pour vérifier la disponibilité des produits avant réservation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDisponibiliteDto {

    @NotNull(message = "L'ID du produit est obligatoire")
    private Long idProduit;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    @NotNull(message = "La date de début est obligatoire")
    private Date dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private Date dateFin;
}
