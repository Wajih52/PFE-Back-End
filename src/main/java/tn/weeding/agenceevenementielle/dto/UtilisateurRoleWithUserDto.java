package tn.weeding.agenceevenementielle.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UtilisateurRoleWithUserDto {

    private Long idUtilisateurRole;
    private LocalDateTime dateAffectationRole;
    private String attribuePar;

    // Infos du rôle
    private Long idRole;
    private String nomRole;
    private String descriptionRole;

    // Infos de l'utilisateur
    private Long idUtilisateur;
    private String pseudo;
    private String nom;
    private String prenom;
    private String telephone ;
    private String email;
    private String codeUtilisateur;
    private String image;
    private String etatCompte;
}