package tn.weeding.agenceevenementielle.dto.authentification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Demande de réinitialisation de mot de passe")
public class PasswordResetRequestDto {
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    @Pattern(
            regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$",
            message = "Format d'email invalide"
    )
    @Schema(
            description = "Email de l'utilisateur ayant oublié son mot de passe",
            example = "mohamed.benali@gmail.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;
}
