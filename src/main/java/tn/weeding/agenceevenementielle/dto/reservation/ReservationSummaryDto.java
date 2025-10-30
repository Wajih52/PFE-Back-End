package tn.weeding.agenceevenementielle.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le résumé/statistiques d'une réservation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSummaryDto {

    private Long totalReservations;
    private Long reservationsEnAttente;
    private Long reservationsConfirmees;
    private Long reservationsAnnulees;
    private Double chiffreAffairesTotal;
    private Double chiffreAffairesEnAttente;
    private Double chiffreAffairesConfirmees;
}
