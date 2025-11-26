package tn.weeding.agenceevenementielle.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequestDto {

    @NotNull(message = "Le type de notification est obligatoire")
    private TypeNotification typeNotification;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères")
    private String titre;

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 1000, message = "Le message ne peut pas dépasser 1000 caractères")
    private String message;

    @NotNull(message = "L'ID utilisateur est obligatoire")
    private Long idUtilisateur;

    // Optionnels
    private Long idReservation;
    private Long idLivraison;
    private Long idPaiement;
    private Long idProduit;
    private String urlAction;
}