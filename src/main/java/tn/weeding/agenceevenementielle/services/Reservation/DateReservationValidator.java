package tn.weeding.agenceevenementielle.services.Reservation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.weeding.agenceevenementielle.exceptions.DateValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Validateur de dates pour les réservations
 */
@Component
@Slf4j
public class DateReservationValidator {

    // ============ CONFIGURATION - À ADAPTER SELON NOS BESOINS ============

    /**
     * Durée maximale d'une location (en jours)
     */
    private static final long DUREE_MAX_LOCATION_JOURS = 20;

    /**
     * Combien de temps à l'avance on peut réserver (en mois)
     */
    private static final long RESERVATION_AVANCE_MAX_MOIS = 12;

    /**
     * Permettre les réservations pour aujourd'hui ?
     */
    private static final boolean PERMETTRE_RESERVATION_AUJOURDHUI = true;

    /**
     * Durée minimale d'une location (en jours)
     */
    private static final long DUREE_MIN_LOCATION_JOURS = 1;

    // ============ MÉTHODE PRINCIPALE DE VALIDATION ============

    /**
     * Valider une période de réservation complète
     */
    public void validerPeriodeReservation(LocalDate dateDebut, LocalDate dateFin, String contexte) {
        log.debug("Validation période réservation - Contexte: {}, Dates: {} -> {}",
                contexte, dateDebut, dateFin);

        // 1. Vérifier que les dates ne sont pas nulles
        validerDatesNonNulles(dateDebut, dateFin, contexte);

        // 2. Vérifier que la date de début n'est pas dans le passé
        validerDateDebutNonPassee(dateDebut, contexte);

        // 3. Vérifier que la date de fin est après la date de début
        validerDateFinApresDebut(dateDebut, dateFin, contexte);

        // 4. Vérifier que la période n'est pas trop longue
        validerDureeMaximale(dateDebut, dateFin, contexte);

        // 5. Vérifier que la réservation n'est pas trop loin dans le futur
        validerReservationNonTropAvance(dateDebut, contexte);

        // 6. Vérifier la durée minimale
        validerDureeMinimale(dateDebut, dateFin, contexte);

        log.debug("Validation période réussie - {} jours",
                ChronoUnit.DAYS.between(dateDebut, dateFin) + 1);
    }

    /**
     * Valider une période de réservation
     */
    public void validerPeriodeReservation(LocalDate dateDebut, LocalDate dateFin) {
        validerPeriodeReservation(dateDebut, dateFin, "réservation");
    }

    // ============ VALIDATIONS INDIVIDUELLES ============

    /**
     * Vérifier que les dates ne sont pas nulles
     */
    private void validerDatesNonNulles(LocalDate dateDebut, LocalDate dateFin, String contexte) {
        if (dateDebut == null || dateFin == null) {
            String message = String.format(
                    "Les dates de début et de fin sont obligatoires pour %s", contexte
            );
            log.error("{}", message);
            throw new DateValidationException(message);
        }
    }

    /**
     * Vérifier que la date de début n'est pas dans le passé
     */
    private void validerDateDebutNonPassee(LocalDate dateDebut, String contexte) {
        LocalDate aujourdhui = LocalDate.now();

        if (PERMETTRE_RESERVATION_AUJOURDHUI) {
            // Mode: Réservation possible pour aujourd'hui
            if (dateDebut.isBefore(aujourdhui)) {
                String message = String.format(
                        "La date de début (%s) ne peut pas être dans le passé pour %s. " +
                                "Date minimale: aujourd'hui (%s)",
                        dateDebut, contexte, aujourdhui
                );
                log.error("{}", message);
                throw new DateValidationException(message);
            }
        } else {
            // Mode: Réservation possible à partir de demain
            LocalDate demain = aujourdhui.plusDays(1);
            if (dateDebut.isBefore(demain)) {
                String message = String.format(
                        "La date de début (%s) doit être au minimum demain pour %s. " +
                                "Date minimale: %s",
                        dateDebut, contexte, demain
                );
                log.error("{}", message);
                throw new DateValidationException(message);
            }
        }
    }

    /**
     * Vérifier que la date de fin est après la date de début
     */
    private void validerDateFinApresDebut(LocalDate dateDebut, LocalDate dateFin, String contexte) {
        if (dateFin.isBefore(dateDebut)) {
            String message = String.format(
                    "La date de fin (%s) doit être après la date de début (%s) pour %s",
                    dateFin, dateDebut, contexte
            );
            log.error("{}", message);
            throw new DateValidationException(message);
        }
    }

    /**
     * Vérifier que la période n'est pas trop longue
     */
    private void validerDureeMaximale(LocalDate dateDebut, LocalDate dateFin, String contexte) {
        long nbJours = ChronoUnit.DAYS.between(dateDebut, dateFin) + 1; // +1 pour inclure le dernier jour

        if (nbJours > DUREE_MAX_LOCATION_JOURS) {
            String message = String.format(
                    "La durée de location (%d jours) dépasse la durée maximale autorisée (%d jours) pour %s. " +
                            "Période: %s -> %s",
                    nbJours, DUREE_MAX_LOCATION_JOURS, contexte, dateDebut, dateFin
            );
            log.error("{}", message);
            throw new DateValidationException(message);
        }
    }

    /**
     * Vérifier que la réservation n'est pas trop loin dans le futur
     */
    private void validerReservationNonTropAvance(LocalDate dateDebut, String contexte) {
        LocalDate dateMaxReservation = LocalDate.now().plusMonths(RESERVATION_AVANCE_MAX_MOIS);

        if (dateDebut.isAfter(dateMaxReservation)) {
            String message = String.format(
                    "La date de début (%s) est trop éloignée pour %s. " +
                            "Réservation possible jusqu'au %s maximum (%d mois à l'avance)",
                    dateDebut, contexte, dateMaxReservation, RESERVATION_AVANCE_MAX_MOIS
            );
            log.error("{}", message);
            throw new DateValidationException(message);
        }
    }

    /**
     * Vérifier la durée minimale de location
     */
    private void validerDureeMinimale(LocalDate dateDebut, LocalDate dateFin, String contexte) {
        long nbJours = ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;

        if (nbJours < DUREE_MIN_LOCATION_JOURS) {
            String message = String.format(
                    "La durée de location (%d jour(s)) est inférieure à la durée minimale (%d jour(s)) pour %s. " +
                            "Période: %s -> %s",
                    nbJours, DUREE_MIN_LOCATION_JOURS, contexte, dateDebut, dateFin
            );
            log.error("{}", message);
            throw new DateValidationException(message);
        }
    }

    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Calculer le nombre de jours d'une période (inclus)
     */
    public long calculerNombreJours(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
    }


    /**
     * Obtenir la date minimale pour une réservation
     */
    public LocalDate getDateMinimaleReservation() {
        return PERMETTRE_RESERVATION_AUJOURDHUI ? LocalDate.now() : LocalDate.now().plusDays(1);
    }

    /**
     * Obtenir la date maximale pour une réservation
     */
    public LocalDate getDateMaximaleReservation() {
        return LocalDate.now().plusMonths(RESERVATION_AVANCE_MAX_MOIS);
    }

    /**
     * Obtenir la durée maximale de location
     */
    public long getDureeMaxLocation() {
        return DUREE_MAX_LOCATION_JOURS;
    }

    /**
     * Obtenir la durée minimale de location
     */
    public long getDureeMinLocation() {
        return DUREE_MIN_LOCATION_JOURS;
    }

    // ============ VALIDATIONS SPÉCIFIQUES MÉTIER ============

    /**
     * Calculer le nombre de jours ouvrables (Lundi-Vendredi)
     */
    public long calculerJoursOuvrables(LocalDate dateDebut, LocalDate dateFin) {
        long joursOuvrables = 0;
        LocalDate current = dateDebut;

        while (!current.isAfter(dateFin)) {
            if (current.getDayOfWeek().getValue() <= 5) { // Lundi à Vendredi
                joursOuvrables++;
            }
            current = current.plusDays(1);
        }

        return joursOuvrables;
    }

}