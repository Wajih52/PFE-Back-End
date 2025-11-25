package tn.weeding.agenceevenementielle.entities.enums;

public enum TypeReclamation {
    PRODUIT_ENDOMMAGE,      // Produit reçu endommagé
    QUANTITE_MANQUANTE,     // Quantité livrée inférieure à la commande
    RETARD_LIVRAISON,       // Livraison en retard
    QUALITE_SERVICE,        // Problème lié à la qualité du service
    PRODUIT_NON_CONFORME,   // Produit ne correspond pas à la description
    PROBLEME_RETOUR,        // Problème lors du retour du matériel
    FACTURATION,            // Erreur de facturation
    AUTRE                   // Autre type de réclamation
}
