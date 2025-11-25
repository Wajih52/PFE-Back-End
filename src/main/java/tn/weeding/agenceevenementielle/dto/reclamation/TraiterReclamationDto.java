package tn.weeding.agenceevenementielle.dto.reclamation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;

/**
 * DTO pour traiter/répondre à une réclamation
 * Utilisé par les admins et employés
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraiterReclamationDto {

    @NotNull(message = "Le statut est obligatoire")
    private StatutReclamation statutReclamation;

    @NotNull(message = "La priorité est obligatoire")
    private PrioriteReclamation prioriteReclamation;

    @NotBlank(message = "La réponse est obligatoire")
    @Size(max = 1000, message = "La réponse ne doit pas dépasser 1000 caractères")
    private String reponse;
}