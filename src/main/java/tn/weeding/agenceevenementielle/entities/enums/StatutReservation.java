package tn.weeding.agenceevenementielle.entities.enums;

public enum StatutReservation {
    /**
     * État initial - Devis créé mais pas encore validé
     */
    @Deprecated
    DEVIS,

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
     * Devis rejeté par l'admin
     */
    @Deprecated
    REJETE,

    /**
     * En attente de paiement
     */
    EN_ATTENTE_PAIEMENT,

    /**
     * Partiellement payé (acompte versé)
     */
    PARTIELLEMENT_PAYE,

    EN_ATTENTE,
}
