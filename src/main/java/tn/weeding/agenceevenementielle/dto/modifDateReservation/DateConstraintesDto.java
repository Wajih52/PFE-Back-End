package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


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

}