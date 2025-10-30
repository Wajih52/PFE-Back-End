package tn.weeding.agenceevenementielle.dto.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO pour créer une ligne de réservation (item dans le panier)
 * Utilisé lors de la création du devis par le client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneReservationRequestDto {

    @NotNull(message = "L'ID du produit est obligatoire")
    private Long idProduit;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    @NotNull(message = "La date de début est obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    private Date dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Future(message = "La date de fin doit être dans le futur")
    private Date dateFin;

    // Optionnel: observations spécifiques pour cette ligne
    @Size(max = 1000, message = "Les observations ne doivent pas dépasser 1000 caractères")
    private String observations;
}
