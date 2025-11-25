package tn.weeding.agenceevenementielle.dto.reclamation;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;

/**
 * DTO pour classer une réclamation (priorité et statut)
 * Utilisé pour mise à jour rapide sans réponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClasserReclamationDto {

    @NotNull(message = "Le statut est obligatoire")
    private StatutReclamation statutReclamation;

    @NotNull(message = "La priorité est obligatoire")
    private PrioriteReclamation prioriteReclamation;
}