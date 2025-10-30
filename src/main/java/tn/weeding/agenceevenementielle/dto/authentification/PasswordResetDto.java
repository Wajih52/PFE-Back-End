package tn.weeding.agenceevenementielle.dto.authentification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "réinitialisation de mot de passe")
public class PasswordResetDto {
    @NotBlank(message = "Le token est obligatoire")
    @Size(min = 36, max = 255, message = "Token invalide")
    private String token;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
            message = "Le mot de passe doit contenir au moins : 1 majuscule, 1 minuscule, 1 chiffre et 1 caractère spécial (@$!%*?&)"
    )
    @Schema(
            description = "Numéro de téléphone (8 à 15 chiffres)",
            example = "21612345678",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String nouveauMotDePasse;
}