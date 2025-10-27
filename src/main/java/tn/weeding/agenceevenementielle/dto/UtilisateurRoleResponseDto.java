package tn.weeding.agenceevenementielle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UtilisateurRoleResponseDto {

    private Long idUtilisateurRole;

    // Informations du rôle
    private Long idRole;
    private String nomRole;
    private String descriptionRole;

    // Métadonnées d'attribution
    private LocalDateTime dateAffectationRole;
    private String attribuePar;

    // Informations utilisateur
    private Long idUtilisateur;
    private String pseudoUtilisateur;
}