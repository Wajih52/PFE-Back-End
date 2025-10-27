package tn.weeding.agenceevenementielle.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "DTO pour l'inscription d'un nouvel utilisateur")
public class UtilisateurInscriptionDto {


    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    @Pattern(
            regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
            message = "Le nom ne peut contenir que des lettres, espaces, apostrophes et tirets"
    )
    @Schema(
            description = "Nom de famille de l'utilisateur",
            example = "Ben Ali",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    @Pattern(
            regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
            message = "Le prénom ne peut contenir que des lettres, espaces, apostrophes et tirets"
    )
    @Schema(
            description = "Prénom de l'utilisateur",
            example = "Mohamed",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String prenom;


    @NotBlank(message = "Le pseudo est obligatoire")
    @Size(min = 3, max = 20, message = "Le pseudo doit contenir entre 3 et 20 caractères")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "Le pseudo ne peut contenir que des lettres, chiffres, tirets et underscores"
    )
    @Schema(
            description = "Pseudo unique de l'utilisateur (pour la connexion)",
            example = "mohamed_benali",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String pseudo;


    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    @Pattern(
            regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$",
            message = "Format d'email invalide"
    )
    @Schema(
            description = "Adresse email de l'utilisateur (doit être unique)",
            example = "mohamed.benali@gmail.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
            message = "Le mot de passe doit contenir au moins : 1 majuscule, 1 minuscule, 1 chiffre et 1 caractère spécial (@$!%*?&)"
    )
    @Schema(
            description = "Mot de passe (min 8 caractères, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial)",
            example = "Password123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String motDePasse;

    @NotNull(message = "Le numéro de téléphone est obligatoire")
    @Min(value = 10000000, message = "Numéro de téléphone invalide (minimum 8 chiffres)")
    @Max(value = 999999999999999L, message = "Numéro de téléphone invalide (maximum 15 chiffres)")
    @Schema(
            description = "Numéro de téléphone (8 à 15 chiffres)",
            example = "21612345678",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long telephone;


    @Pattern(
            regexp = "^(Homme|Femme|Autre)$",
            message = "Le genre doit être : Homme, Femme ou Autre"
    )
    @Schema(
            description = "Genre de l'utilisateur",
            example = "Homme",
            allowableValues = {"Homme", "Femme", "Autre"}
    )
    private String genre;


    @Size(max = 200, message = "L'adresse ne peut pas dépasser 200 caractères")
    @Schema(
            description = "Adresse postale de l'utilisateur",
            example = "Rue de la République, Tunis 1000"
    )
    private String adresse;
}
