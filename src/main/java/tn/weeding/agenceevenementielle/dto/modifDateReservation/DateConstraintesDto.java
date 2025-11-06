package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour communiquer les contraintes de dates au frontend
 *
 * Permet au client Angular de:
 * - Désactiver les dates invalides dans le datepicker
 * - Afficher des messages d'aide
 * - Valider côté client avant envoi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateConstraintesDto {

    /**
     * Date minimale pour une réservation (aujourd'hui ou demain)
     */
    private LocalDate dateMinimale;

    /**
     * Date maximale pour une réservation (X mois à l'avance)
     */
    private LocalDate dateMaximale;

    /**
     * Durée minimale de location (en jours)
     */
    private Long dureeMinJours;

    /**
     * Durée maximale de location (en jours)
     */
    private Long dureeMaxJours;

    /**
     * Est-ce qu'on peut réserver pour aujourd'hui ?
     */
    private Boolean reservationAujourdhuiAutorisee;

    /**
     * Message d'aide pour le client
     */
    private String messageAide;

    /**
     * Builder avec message d'aide automatique
     */
    public static DateConstraintesDto createWithHelp(
            LocalDate dateMin,
            LocalDate dateMax,
            Long dureeMin,
            Long dureeMax,
            Boolean aujourdhuiAutorise) {

        String message = String.format(
                "Réservation possible du %s au %s. " +
                        "Durée: minimum %d jour(s), maximum %d jour(s).",
                dateMin, dateMax, dureeMin, dureeMax
        );

        return DateConstraintesDto.builder()
                .dateMinimale(dateMin)
                .dateMaximale(dateMax)
                .dureeMinJours(dureeMin)
                .dureeMaxJours(dureeMax)
                .reservationAujourdhuiAutorisee(aujourdhuiAutorise)
                .messageAide(message)
                .build();
    }
}