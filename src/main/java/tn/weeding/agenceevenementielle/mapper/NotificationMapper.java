package tn.weeding.agenceevenementielle.mapper;

import org.springframework.stereotype.Component;
import tn.weeding.agenceevenementielle.dto.notification.NotificationResponseDto;
import tn.weeding.agenceevenementielle.entities.Notification;

@Component
public class NotificationMapper {

    public NotificationResponseDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponseDto.builder()
                .idNotification(notification.getIdNotification())
                .typeNotification(notification.getTypeNotification())
                .typeLibelle(notification.getTypeNotification().getLibelle())
                .typeIcone(notification.getTypeNotification().getIcone())
                .titre(notification.getTitre())
                .message(notification.getMessage())
                .dateCreation(notification.getDateCreation())
                .lue(notification.getLue())
                .dateLecture(notification.getDateLecture())
                .idReservation(notification.getIdReservation())
                .idLivraison(notification.getIdLivraison())
                .idPaiement(notification.getIdPaiement())
                .idProduit(notification.getIdProduit())
                .urlAction(notification.getUrlAction())
                .idUtilisateur(notification.getUtilisateur().getIdUtilisateur())
                .nomUtilisateur(notification.getUtilisateur().getNom())
                .prenomUtilisateur(notification.getUtilisateur().getPrenom())
                .build();
    }
}