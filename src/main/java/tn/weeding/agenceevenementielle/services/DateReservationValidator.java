package tn.weeding.agenceevenementielle.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.weeding.agenceevenementielle.exceptions.DateValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ✅ Validateur de dates pour les réservations
 *
 * Règles métier implémentées:
 * 1. Date de début >= aujourd'hui (ou aujourd'hui pour réservation immédiate)
 * 2. Date de fin > date de début
 * 3. Période maximale: 30 jours (configurable)
 * 4. Réservation maximale à l'avance: 1 an (configurable)
 * 5. Dates non nulles
 *
 * Sprint 4 - Gestion des réservations
 */
@Component
@Slf4j
public class DateReservationValidator {

    // ============ CONFIGURATION - À ADAPTER SELON VOS BESOINS ============

    /**
     * Durée maximale d'une location (en jours)
     * Exemple: Pour un mariage, rarement plus de 3-4 jours
     */
    private static final long DUREE_MAX_LOCATION_JOURS = 20;

    /**
     * Combien de temps à l'avance on peut réserver (en mois)
     * Exemple: Pas de réservation plus de 12 mois à l'avance
     */
    private static final long RESERVATION_AVANCE_MAX_MOIS = 12;

    /**
     * Permettre les réservations pour aujourd'hui ?
     * true = Oui (pour réservations urgentes)
     * false = Non (minimum demain)
     */
    private static final boolean PERMETTRE_RESERVATION_AUJOURDHUI = true;

    /**
     * Durée minimale d'une location (en jours)
     * Exemple: Minimum 1 jour
     */
    private static final long DUREE_MIN_LOCATION_JOURS = 1;

    // ============ MÉTHODE PRINCIPALE DE VALIDATION ============

    /**
     * ✅ Valider une période de réservation complète
     *
     * @param dateDebut Date de début de la réservation
     * @param dateFin Date de fin de la réservation
     * @param contexte Contexte pour les messages d'erreur (ex: "devis", "modification")
     * @throws DateValidationException si les dates sont invalides
     */
    public void validerPeriodeReservation(LocalDate dateDebut, LocalDate dateFin, String contexte) {
        log.debug("🔍 Validation période réservation - Contexte: {}, Dates: {} -> {}",
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

        log.debug("✅ Validation période réussie - {} jours",
                ChronoUnit.DAYS.between(dateDebut, dateFin) + 1);
    }

    /**
     * ✅ Valider une période de réservation (version simplifiée sans contexte)
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
            log.error("❌ {}", message);
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
                log.error("❌ {}", message);
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
                log.error("❌ {}", message);
                throw new DateValidationException(message);
            }
        }
    }

    /**
     * Vérifier que la date de fin est après la date de début
     */
    private void validerDateFinApresDebut(LocalDate dateDebut, LocalDate dateFin, String contexte) {
        if (dateFin.isBefore(dateDebut) || dateFin.isEqual(dateDebut)) {
            String message = String.format(
                    "La date de fin (%s) doit être après la date de début (%s) pour %s",
                    dateFin, dateDebut, contexte
            );
            log.error("❌ {}", message);
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
            log.error("❌ {}", message);
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
            log.error("❌ {}", message);
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
            log.error("❌ {}", message);
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
     * Vérifier si une date est valide pour une réservation
     */
    public boolean estDateValide(LocalDate date) {
        if (date == null) {
            return false;
        }

        LocalDate aujourdhui = LocalDate.now();
        LocalDate dateMinimale = PERMETTRE_RESERVATION_AUJOURDHUI ? aujourdhui : aujourdhui.plusDays(1);
        LocalDate dateMaximale = aujourdhui.plusMonths(RESERVATION_AVANCE_MAX_MOIS);

        return !date.isBefore(dateMinimale) && !date.isAfter(dateMaximale);
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
     * Vérifier si une période chevauche un weekend
     * (Utile si vous avez des règles spéciales pour les weekends)
     */
    public boolean chevauchWeekend(LocalDate dateDebut, LocalDate dateFin) {
        LocalDate current = dateDebut;
        while (!current.isAfter(dateFin)) {
            if (current.getDayOfWeek().getValue() >= 6) { // Samedi ou Dimanche
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }

    /**
     * Vérifier si la période inclut des jours fériés
     * (À implémenter selon votre pays/région)
     */
    public boolean inclutJoursFeries(LocalDate dateDebut, LocalDate dateFin) {
        // TODO: Implémenter la vérification des jours fériés tunisiens
        // Exemple: Aid el-Fitr, Aid el-Adha, 1er Mai, 14 Janvier, etc.
        return false;
    }

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

    /**
     * Calculer le nombre de jours de weekend (Samedi-Dimanche)
     */
    public long calculerJoursWeekend(LocalDate dateDebut, LocalDate dateFin) {
        long totalJours = calculerNombreJours(dateDebut, dateFin);
        long joursOuvrables = calculerJoursOuvrables(dateDebut, dateFin);
        return totalJours - joursOuvrables;
    }
}