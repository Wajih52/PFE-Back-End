package tn.weeding.agenceevenementielle.dto.notification;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponseDto {
    private Long idNotification;
    private TypeNotification typeNotification;
    private String typeLibelle;
    private String typeIcone;
    private String titre;
    private String message;
    private LocalDateTime dateCreation;
    private Boolean lue;
    private LocalDateTime dateLecture;
    private Long idReservation;
    private Long idLivraison;
    private Long idPaiement;
    private Long idProduit;
    private String urlAction;

    // Informations utilisateur (pour admin)
    private Long idUtilisateur;
    private String nomUtilisateur;
    private String prenomUtilisateur;
}