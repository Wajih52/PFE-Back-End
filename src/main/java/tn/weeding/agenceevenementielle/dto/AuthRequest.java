package tn.weeding.agenceevenementielle.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Requête d'authentification (login)")
public class AuthRequest {


    @NotBlank(message = "L'identifiant est obligatoire")
    @Size(min = 3, max = 100, message = "L'identifiant doit contenir entre 3 et 100 caractères")
    @Schema(
            description = "Identifiant de connexion (pseudo ou email)",
            example = "mohamed_benali",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String identifiant;


    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Schema(
            description = "Mot de passe de l'utilisateur",
            example = "Password123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String motDePasse;
}
