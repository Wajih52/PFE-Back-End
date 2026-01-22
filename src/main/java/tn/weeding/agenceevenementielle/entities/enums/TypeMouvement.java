package tn.weeding.agenceevenementielle.entities.enums;

public enum TypeMouvement {
    // ============================================
    // CRÉATION ET INITIALISATION
    // ============================================

    /**
     * Création initiale d'un produit avec stock
     * Utilisé lors de l'ajout d'un nouveau produit au catalogue
     */
    CREATION,

    /**
     * Réactivation d'un produit désactivé
     * Utilisé lors de la remise en service d'un produit
     */
    REACTIVATION,

    /**
     * Désactivation d'un produit
     * Soft delete - le produit est retiré du catalogue mais conservé en base
     */
    DESACTIVATION,

    // ============================================
    // ENTRÉES DE STOCK (PRODUITS EN_QUANTITE)
    // ============================================

    /**
     * Ajout de stock pour un produit EN_QUANTITE
     * Ex: Achat de 50 nouvelles chaises
     */
    AJOUT_STOCK,

    /**
     * Réception de stock commandé
     * Ex: Livraison fournisseur
     */
    ENTREE_STOCK,

    /**
     * Retour de produit après maintenance
     * Le produit redevient disponible
     */
    RETOUR_MAINTENANCE,

    // ============================================
    // SORTIES DE STOCK (PRODUITS EN_QUANTITE)
    // ============================================

    /**
     * Retrait de stock pour un produit EN_QUANTITE
     * Ex: Retrait manuel de 10 assiettes cassées
     */
    RETRAIT_STOCK,



    /**
     * Envoi en maintenance
     * Le produit n'est temporairement plus disponible
     */
    MAINTENANCE,

    /**
     * Produit endommagé ou perdu
     * Retrait définitif du stock
     */
    PRODUIT_ENDOMMAGE,

    // ============================================
    // INSTANCES (PRODUITS avecReference)
    // ============================================

    /**
     * Ajout d'une nouvelle instance de produit avec référence
     * Ex: Ajout d'un nouveau projecteur avec numéro de série
     */
    AJOUT_INSTANCE,

    /**
     * Suppression d'une instance de produit avec référence
     * Ex: Retrait définitif d'un projecteur cassé
     */
    SUPPRESSION_INSTANCE,

    // ============================================
    // RÉSERVATIONS ET LIVRAISONS
    // ============================================

    /**
     * Réservation de produit
     * Allocation temporaire du stock pour un événement
     */
    RESERVATION,

    /**
     * Annulation d'une réservation
     * Le stock redevient disponible
     */
    ANNULATION_RESERVATION,

    /**
     * Livraison de produits pour un événement
     * Les produits sont physiquement sortis
     */
    LIVRAISON,

    /**
     * Retour de produits après événement
     * Les produits reviennent dans l'inventaire
     */
    RETOUR,

    // ============================================
    // AJUSTEMENTS ET CORRECTIONS
    // ============================================

    /**
     * Ajustement manuel du stock après inventaire
     * Ex: Correction suite à un comptage physique
     */
    AJUSTEMENT_INVENTAIRE,

    /**
     * Correction d'une erreur de saisie
     * Rectification d'un mouvement incorrect
     */
    CORRECTION,

    /**
     * Correction de stock
     * Même fonction que CORRECTION
     */
    CORRECTION_STOCK;

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Vérifie si le mouvement est une entrée (augmente le stock)
     */
    public boolean isEntree() {
        return switch (this) {
            case CREATION,
                 REACTIVATION,
                 AJOUT_STOCK,
                 ENTREE_STOCK,
                 RETOUR_MAINTENANCE,
                 AJOUT_INSTANCE,
                 ANNULATION_RESERVATION,
                 RETOUR -> true;
            default -> false;
        };
    }

    /**
     * Vérifie si le mouvement est une sortie (diminue le stock)
     */
    public boolean isSortie() {
        return switch (this) {
            case DESACTIVATION,
                 RETRAIT_STOCK,
                 MAINTENANCE,
                 PRODUIT_ENDOMMAGE,
                 SUPPRESSION_INSTANCE,
                 RESERVATION,
                 LIVRAISON -> true;
            default -> false;
        };
    }
}