package tn.weeding.agenceevenementielle.entities.enums;

public enum StatutFacture {
    EN_ATTENTE_VALIDATION_CLIENT,  // Pour devis
    EN_ATTENTE_LIVRAISON,           // Pour pro-forma
    A_REGLER,                       // Pour finale (pas encore payée)
    PAYEE,                          // Pour finale (payée)
    ANNULEE                         // Si réservation annulée
}