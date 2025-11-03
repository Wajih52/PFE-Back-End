package tn.weeding.agenceevenementielle.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;

import java.time.LocalDate;
import java.util.Date;

/**
 * DTO pour les filtres de recherche de réservations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationSearchDto {

    private Long idUtilisateur;           // Filtrer par client
    private StatutReservation statut;     // Filtrer par statut
    private LocalDate dateDebutMin;            // Réservations à partir de cette date
    private LocalDate dateDebutMax;            // Réservations avant cette date
    private String referenceReservation;  // Recherche par référence
    private String nomClient;             // Recherche par nom du client
}
