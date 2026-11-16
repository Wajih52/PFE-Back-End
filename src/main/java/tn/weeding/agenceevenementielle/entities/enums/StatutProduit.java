package tn.weeding.agenceevenementielle.entities.enums;

public enum StatutProduit {
    ACTIF,      // Produit publié et visible dans le catalogue client
    DESACTIVE,  // Produit masqué temporairement du catalogue (stock et instances préservés)
    ARCHIVE     // Produit retiré ou obsolète
}
