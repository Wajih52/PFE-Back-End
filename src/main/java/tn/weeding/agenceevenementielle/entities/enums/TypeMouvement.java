package tn.weeding.agenceevenementielle.entities.enums;

public enum TypeMouvement {
    // Entrées
    ENTREE_STOCK,           // Achat/Réception de produits
    RETOUR_RESERVATION,     // Retour après réservation
    RETOUR_MAINTENANCE,     // Retour de maintenance
    AJOUT_INSTANCE,         // Ajout d'une instance (produit avecReference)

    // Sorties
    SORTIE_RESERVATION,     // Sortie pour réservation
    MAINTENANCE,            // Envoi en maintenance
    PRODUIT_ENDOMMAGE,      // Produit endommagé/perdu
    SUPPRESSION_INSTANCE,   // Suppression d'une instance

    // Ajustements
    AJUSTEMENT_INVENTAIRE,  // Correction manuelle du stock
    CORRECTION,              // Correction d'erreur
    ANNULATION_RESERVATION
}