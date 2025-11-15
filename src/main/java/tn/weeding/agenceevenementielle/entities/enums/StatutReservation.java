package tn.weeding.agenceevenementielle.entities.enums;

public enum StatutReservation {

    /**
     * Devis validé par l'admin et transformé en réservation confirmée
     */
    CONFIRME,

    /**
     * Réservation en cours (matériel livré)
     */
    EN_COURS,

    /**
     * Réservation terminée (matériel retourné)
     */
    TERMINE,

    /**
     * Réservation annulée
     */
    ANNULE,


    /**
     * Réservation En attente
     */
    EN_ATTENTE,
}
