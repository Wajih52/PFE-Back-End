package tn.weeding.agenceevenementielle.dto.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la modification du devis par l'ADMIN
 * L'admin peut modifier les prix et ajouter des remises
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevisModificationDto {

    @NotNull(message = "L'ID de la réservation est obligatoire")
    private Long idReservation;

    // Liste des modifications de lignes (prix unitaire modifié)
    private List<LigneModificationDto> lignesModifiees;

    // Remise globale en pourcentage (0-100)
    @Min(value = 0, message = "La remise doit être positive")
    @Max(value = 100, message = "La remise ne peut pas dépasser 100%")
    private Double remisePourcentage;

    // Remise fixe en montant
    @Min(value = 0, message = "La remise doit être positive")
    private Double remiseMontant;

    // Commentaire de l'admin
    @Size(max = 1000, message = "Les commentaires ne doivent pas dépasser 1000 caractères")
    private String commentaireAdmin;
}
