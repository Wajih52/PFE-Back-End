package tn.weeding.agenceevenementielle.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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
    private Long reservationsTermine;

    private Double chiffreAffairesTotal;
    private Double chiffreAffairesEnAttente;
    private Double chiffreAffairesConfirmees;

    // Montants
    private Double montantTotal;
    private Double montantPaye;
    private Double montantMoyen;

    // Informations client (pour stats client)
    private String nomClient;
    private String emailClient;

    // Produits préférés
    private List<String> produitsPreferences;

    // Période (pour stats sur période)
    private LocalDate dateDebut;
    private LocalDate dateFin;
}
