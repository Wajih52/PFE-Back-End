package tn.weeding.agenceevenementielle.entities.enums;

/**
 * État physique d'une instance de produit
 * Sprint 3 - Gestion des produits et du stock
 */
public enum EtatPhysique {
    NEUF,               // État neuf
    BON_ETAT,           // Bon état général
    ETAT_MOYEN,         // Quelques traces d'usure
    USAGE,              // Usé mais fonctionnel
    ENDOMMAGE           // Endommagé, nécessite réparation
}