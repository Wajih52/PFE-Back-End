package tn.weeding.agenceevenementielle.entities;

/**
 * Statut d'une instance de produit avec référence
 * Sprint 3 - Gestion des produits et du stock
 */
public enum StatutInstance {
    DISPONIBLE,         // Disponible pour réservation
    RESERVE,            // Réservé (dans une ligne de réservation)
    EN_LIVRAISON,       // En cours de livraison chez le client
    EN_MAINTENANCE,     // En cours de réparation/maintenance
    HORS_SERVICE,       // Défectueux, ne peut pas être utilisé
    PERDU               // Perdu ou volé
}