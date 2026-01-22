package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.dto.reservation.ReservationResponseDto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO DE RÉPONSE APRÈS MODIFICATION DE DATES
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModificationDatesResponseDto {

    /**
     * Succès de l'opération
     */
    private Boolean succes;

    /**
     * Message de confirmation
     */
    private String message;

    /**
     * Réservation mise à jour (avec dates recalculées)
     */
    private ReservationResponseDto reservationMiseAJour;

    /**
     * Anciennes dates de la réservation
     */
    private LocalDate ancienneDateDebutReservation;
    private LocalDate ancienneDateFinReservation;

    /**
     * Nouvelles dates de la réservation (recalculées)
     */
    private LocalDate nouvelleDateDebutReservation;
    private LocalDate nouvelleDateFinReservation;

    /**
     * Détails des lignes modifiées
     */
    private List<DetailLigneModifiee> lignesModifiees;

    /**
     * Montants
     */
    private Double ancienMontantTotal;
    private Double nouveauMontantTotal;
    private Double differenceMontant;


    /**
     * Détails d'une ligne modifiée
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailLigneModifiee {
        private Long idLigne;
        private String nomProduit;
        private LocalDate ancienneDateDebut;
        private LocalDate ancienneDateFin;
        private LocalDate nouvelleDateDebut;
        private LocalDate nouvelleDateFin;
        private Integer joursDifferenceDebut;
        private Integer joursDifferenceF;
    }
}