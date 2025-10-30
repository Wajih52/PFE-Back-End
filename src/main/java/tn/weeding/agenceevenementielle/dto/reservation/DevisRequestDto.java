package tn.weeding.agenceevenementielle.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour créer un DEVIS (demande initiale du client)
 * Le client sélectionne des produits avec dates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevisRequestDto {

    @NotNull(message = "Les lignes de réservation sont obligatoires")
    @Size(min = 1, message = "Au moins un produit doit être sélectionné")
    private List<LigneReservationRequestDto> lignesReservation;

    // Observations générales du client
    @Size(max = 1000, message = "Les observations ne doivent pas dépasser 1000 caractères")
    private String observationsClient;
}
