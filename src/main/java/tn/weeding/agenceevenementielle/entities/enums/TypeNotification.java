package tn.weeding.agenceevenementielle.entities.enums;

import lombok.Getter;

@Getter
public enum TypeNotification {
    // Notifications clients
    RESERVATION_CONFIRMEE("Réservation confirmée", "✅"),
    DEVIS_VALIDE("Devis validé", "📋"),
    DEVIS_EXPIRE("Devis expiré", "⏰"),
    DEVIS_EN_ATTENTE("Devis en attente", "⚠️"),
    DEVIS_PROCHE_EXPIRATION("Devis proche expiration", "🚨"),
    LIVRAISON_PREVUE("Livraison prévue", "🚚"),
    LIVRAISON_EN_COURS("Livraison en cours", "📦"),
    LIVRAISON_EFFECTUEE("Livraison effectuée", "✅"),
    RETOUR_PREVU("Retour à prévoir", "🔄"),
    PAIEMENT_RECU("Paiement reçu", "💰"),
    PAIEMENT_EN_ATTENTE("Paiement en attente", "⏳"),
    PAIEMENT_RETARD("Paiement en retard", "⚠️"),
    PAIEMENT_REFUSE("Paiement Refusé", "❌️"),

    // Notifications admin/employés
    NOUVELLE_RESERVATION("Nouvelle réservation", "🆕"),
    NOUVEAU_DEVIS("Nouveau devis", "📝"),
    NOUVEAU_PAIEMENT("Nouveau paiement", "💳"),
    LIVRAISON_A_EFFECTUER("Livraison à effectuer", "📅"),
    RETOUR_EN_RETARD("Retour en retard", "❌"),
    STOCK_CRITIQUE("Stock critique", "⚠️"),
    NOUVELLE_RECLAMATION("Nouvelle réclamation", "📢"),

    // Notifications système
    SYSTEME_INFO("Information système", "ℹ️"),
    SYSTEME_ALERTE("Alerte système", "⚠️");

    private final String libelle;
    private final String icone;

    TypeNotification(String libelle, String icone) {
        this.libelle = libelle;
        this.icone = icone;
    }

}