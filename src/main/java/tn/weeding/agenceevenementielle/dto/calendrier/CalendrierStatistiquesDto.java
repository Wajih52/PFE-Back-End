package tn.weeding.agenceevenementielle.dto.calendrier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendrierStatistiquesDto {

    private Long nombreReservations;
    private Long nombreLivraisons;
    private Double montantTotalPeriode;
    private Double tauxPaiement; // Pourcentage
}