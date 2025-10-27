package tn.weeding.agenceevenementielle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDto {

    @NotBlank(message = "Ancien mot de passe obligatoire")
    private String ancienMotDePasse;

    @NotBlank(message = "Nouveau mot de passe obligatoire")
    @Size(min = 8, message = "Minimum 8 caractères")
    private String nouveauMotDePasse;
}