package tn.weeding.agenceevenementielle.entities.enums;

public enum StatutReservation {

    /**
     * Réservation En attente (DEVIS)
     */
    EN_ATTENTE,


    /**
     * Devis validé par l'admin et transformé en réservation confirmée
     */
    CONFIRME,


    /**
     * Réservation terminée (matériel retourné)
     */
    TERMINE,

    /**
     * Réservation annulée
     */
    ANNULE,



}
