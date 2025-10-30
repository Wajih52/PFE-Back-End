package tn.weeding.agenceevenementielle.dto.authentification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "L'Ajout d'un nouveau rôle")
public class RoleRequestDto {
    @NotBlank(message = "Le nom du rôle est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caractères")
    @Pattern(
            regexp = "^[A-Z_]+$",
            message = "Le nom du rôle doit être en majuscules et peut contenir des underscores (ex: ADMIN, SUPER_ADMIN)"
    )
    @Schema(
            description = "le nom du Role : ADMIN, EMPLOYE, CLIENT, MANAGER, ETC",
            example = "CLIENT"
    )
    private String nom;

    @Size(max = 200, message = "La description ne peut pas dépasser 200 caractères")
    @Schema(
            description = "Description Du Rôle",
            example = "Description"
    )
    private String description;


}
