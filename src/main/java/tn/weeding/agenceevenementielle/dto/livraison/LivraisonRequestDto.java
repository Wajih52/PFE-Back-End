package tn.weeding.agenceevenementielle.dto.livraison;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO pour créer ou modifier une livraison
 * Sprint 6 - Gestion des livraisons
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LivraisonRequestDto {

    @NotBlank(message = "Le titre de la livraison est obligatoire")
    private String titreLivraison;

    @NotBlank(message = "L'adresse de livraison est obligatoire")
    private String adresseLivraison;

    @NotNull(message = "La date de livraison est obligatoire")
    private LocalDate dateLivraison;

    @NotNull(message = "L'heure de livraison est obligatoire")
    private LocalTime heureLivraison;

    /**
     * Liste des IDs des lignes de réservation à associer à cette livraison
     */
    @NotNull(message = "Au moins une ligne de réservation doit être associée")
    private List<Long> idLignesReservation;

    /**
     * Observations ou notes sur la livraison
     */
    private String observations;
}