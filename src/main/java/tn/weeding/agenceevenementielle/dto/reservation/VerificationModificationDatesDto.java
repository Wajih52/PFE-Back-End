package tn.weeding.agenceevenementielle.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour la vérification de modification de dates
 *
 * Indique si la modification est possible et pourquoi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationModificationDatesDto {

    /**
     * La modification est-elle possible ?
     */
    private Boolean possible;

    /**
     * Message d'explication
     */
    private String message;

    /**
     * Nombre de jours de la nouvelle période (si possible)
     */
    private Long nombreJours;

    /**
     * Détails par produit (optionnel)
     */
    private java.util.List<DetailDisponibiliteProduitDto> detailsProduits;
}

/**
 * Détail de disponibilité pour un produit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DetailDisponibiliteProduitDto {
    private Long idProduit;
    private String nomProduit;
    private Boolean disponible;
    private String message;
}