package tn.weeding.agenceevenementielle.entities.enums;

/**
 * Statut d'une instance de produit avec référence
 * Sprint 3 - Gestion des produits et du stock
 */
public enum StatutInstance {
    DISPONIBLE,         // Disponible pour réservation
    EN_ATTENTE,      //le Jour de la livraison où la livraison est pas encore affecté
    EN_LIVRAISON,       // En cours de livraison chez le client
    EN_RETOUR,
    EN_MAINTENANCE,     // En cours de réparation/maintenance
    HORS_SERVICE,       // Défectueux, ne peut pas être utilisé
    PERDU ,// Perdu ou volé
    EN_UTILISATION,
    EN_PANNE,

}