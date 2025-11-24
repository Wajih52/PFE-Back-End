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
     * Correction de stock (alias pour compatibilité)
     * Même fonction que CORRECTION
     */
    CORRECTION_STOCK;

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Vérifie si le mouvement est une entrée (augmente le stock)
     * @return true si c'est une entrée, false sinon
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
     * @return true si c'est une sortie, false sinon
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

    /**
     * Vérifie si le mouvement est un ajustement (ne change pas forcément le stock)
     * @return true si c'est un ajustement, false sinon
     */
    public boolean isAjustement() {
        return this == AJUSTEMENT_INVENTAIRE ||
                this == CORRECTION ||
                this == CORRECTION_STOCK;
    }

    /**
     * Obtient le libellé français du type de mouvement
     * @return Libellé descriptif
     */
    public String getLibelle() {
        return switch (this) {
            case CREATION -> "Création du produit";
            case REACTIVATION -> "Réactivation";
            case DESACTIVATION -> "Désactivation";
            case AJOUT_STOCK -> "Ajout de stock";
            case ENTREE_STOCK -> "Entrée de stock";
            case RETRAIT_STOCK -> "Retrait de stock";
            case MAINTENANCE -> "Envoi en maintenance";
            case RETOUR_MAINTENANCE -> "Retour de maintenance";
            case PRODUIT_ENDOMMAGE -> "Produit endommagé";
            case AJOUT_INSTANCE -> "Ajout d'instance";
            case SUPPRESSION_INSTANCE -> "Suppression d'instance";
            case RESERVATION -> "Réservation";
            case ANNULATION_RESERVATION -> "Annulation de réservation";
            case LIVRAISON -> "Livraison";
            case RETOUR -> "Retour";
            case AJUSTEMENT_INVENTAIRE -> "Ajustement inventaire";
            case CORRECTION -> "Correction";
            case CORRECTION_STOCK -> "Correction de stock";
        };
    }

    /**
     * Obtient l'icône associée au type de mouvement
     * @return Emoji représentant le type
     */
    public String getIcon() {
        return switch (this) {
            case CREATION -> "🆕";
            case REACTIVATION -> "♻️";
            case DESACTIVATION -> "🗑️";
            case AJOUT_STOCK, ENTREE_STOCK -> "📦";
            case RETRAIT_STOCK -> "📤";
            case  LIVRAISON -> "🚚";
            case  RETOUR -> "🔙";
            case MAINTENANCE -> "🔧";
            case RETOUR_MAINTENANCE -> "✅";
            case PRODUIT_ENDOMMAGE -> "💔";
            case AJOUT_INSTANCE -> "➕";
            case SUPPRESSION_INSTANCE -> "➖";
            case RESERVATION -> "📅";
            case ANNULATION_RESERVATION -> "❌";
            case AJUSTEMENT_INVENTAIRE, CORRECTION, CORRECTION_STOCK -> "📝";
        };
    }

    /**
     * Obtient la classe CSS pour le style visuel
     * @return Nom de la classe CSS
     */
    public String getCssClass() {
        if (isEntree()) {
            return "mouvement-entree";
        } else if (isSortie()) {
            return "mouvement-sortie";
        } else {
            return "mouvement-ajustement";
        }
    }
}