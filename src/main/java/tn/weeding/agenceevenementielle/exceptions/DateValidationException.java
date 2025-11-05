package tn.weeding.agenceevenementielle.exceptions;

/**
 * Exception levée quand les dates d'une réservation sont invalides
 *
 * Cas d'utilisation:
 * - Date de début dans le passé
 * - Date de fin avant date de début
 * - Période trop longue
 * - Dates null ou invalides
 */
public class DateValidationException extends RuntimeException {

    public DateValidationException(String message) {
        super(message);
    }

    public DateValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}