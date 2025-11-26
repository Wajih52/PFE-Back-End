package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.dto.notification.NotificationResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;

import java.util.List;

public interface NotificationServiceInterface {

    /**
     * Créer une notification
     */
    NotificationResponseDto creerNotification(NotificationRequestDto dto);

    /**
     * Récupérer toutes les notifications d'un utilisateur
     */
    List<NotificationResponseDto> getNotificationsByUtilisateur(Long idUtilisateur);

    /**
     * Récupérer les notifications non lues d'un utilisateur
     */
    List<NotificationResponseDto> getNotificationsNonLues(Long idUtilisateur);

    /**
     * Compter les notifications non lues
     */
    Long compterNotificationsNonLues(Long idUtilisateur);

    /**
     * Marquer une notification comme lue
     */
    NotificationResponseDto marquerCommeLue(Long idNotification, Long idUtilisateur);

    /**
     * Marquer toutes les notifications comme lues
     */
    void marquerToutesCommeLues(Long idUtilisateur);

    /**
     * Supprimer une notification
     */
    void supprimerNotification(Long idNotification, Long idUtilisateur);

    /**
     * Méthodes helper pour créer rapidement des notifications
     */
    void creerNotificationReservationConfirmee(Long idUtilisateur, Long idReservation, String referenceReservation);
    void creerNotificationDevisValide(Long idUtilisateur, Long idReservation, String referenceDevis);
    void creerNotificationLivraisonPrevue(Long idUtilisateur, Long idLivraison, String dateLivraison);
    void creerNotificationPaiementRecu(Long idUtilisateur, Long idPaiement, Double montant);
    void creerNotificationStockCritique(Long idProduit, String nomProduit, Integer quantiteRestante);

    void creerNotificationAvecEmail(NotificationRequestDto dto);

    void creerNotificationPourStaff(TypeNotification type, String titre, String message,
                                    Long idReservation, String urlAction);
}