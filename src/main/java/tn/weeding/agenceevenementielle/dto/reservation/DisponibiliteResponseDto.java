package tn.weeding.agenceevenementielle.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de réponse pour la vérification de disponibilité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibiliteResponseDto {

    private Long idProduit;
    private String nomProduit;
    private Boolean disponible;
    private Integer quantiteDemandee;
    private Integer quantiteDisponible;
    private String message;

    // Pour les produits avec référence: liste des instances disponibles
    private List<String> instancesDisponibles;
}
