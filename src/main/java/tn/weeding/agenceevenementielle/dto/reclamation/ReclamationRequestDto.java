package tn.weeding.agenceevenementielle.dto.reclamation;

import jakarta.validation.constraints.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeReclamation;

/**
 * DTO pour créer une réclamation
 * Utilisé par les clients connectés et les visiteurs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReclamationRequestDto {

    @NotBlank(message = "L'objet de la réclamation est obligatoire")
    @Size(max = 200, message = "L'objet ne doit pas dépasser 200 caractères")
    private String objet;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String descriptionReclamation;

    @NotBlank(message = "L'email de contact est obligatoire")
    @Email(message = "L'email doit être valide")
    private String contactEmail;

    @Pattern(regexp = "^[0-9]{8,15}$", message = "Le numéro de téléphone doit contenir entre 8 et 15 chiffres")
    private String contactTelephone;

    @NotNull(message = "Le type de réclamation est obligatoire")
    private TypeReclamation typeReclamation;

    // Optionnel - ID de la réservation concernée (si applicable)
    private Long idReservation;
}