package tn.weeding.agenceevenementielle.dto.avis;

import jakarta.validation.constraints.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutAvis;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisModerationDto {

    @NotNull(message = "L'ID de l'avis est obligatoire")
    private Long idAvis;

    @NotNull(message = "Le statut est obligatoire")
    private StatutAvis statut;

    @Size(max = 500, message = "Le commentaire ne doit pas dépasser 500 caractères")
    private String commentaireModeration;
}