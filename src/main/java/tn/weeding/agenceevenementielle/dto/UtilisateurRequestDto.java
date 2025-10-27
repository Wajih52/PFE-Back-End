package tn.weeding.agenceevenementielle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.weeding.agenceevenementielle.entities.StatutCompte;
import tn.weeding.agenceevenementielle.entities.StatutEmp;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UtilisateurRequestDto {


    @Size(min = 5, max = 20, message = "Le code doit contenir entre 5 et 20 caractères")
    @Pattern(
            regexp = "^(CL|EM|AD)[0-9]{10,15}$",
            message = "Format invalide. Exemples : CL1234567890, EM9876543210"
    )
    @Schema(
            description = "Code utilisateur unique (généré automatiquement)",
            example = "CL1729177890123",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String codeUtilisateur;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    @Pattern(
            regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
            message = "Le nom ne peut contenir que des lettres"
    )
    @Schema(
            description = "Nom de famille",
            example = "Ben Ali",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    @Pattern(
            regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$",
            message = "Le prénom ne peut contenir que des lettres"
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

    @NotNull(message = "Le numéro de téléphone est obligatoire")
    @Min(value = 10000000, message = "Numéro invalide (minimum 8 chiffres)")
    @Max(value = 999999999999999L, message = "Numéro invalide (maximum 15 chiffres)")
    @Schema(
            description = "Numéro de téléphone (8 à 15 chiffres)",
            example = "21612345678",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long telephone;

    @Size(max = 200, message = "L'adresse ne peut pas dépasser 200 caractères")
    @Schema(
            description = "Adresse postale de l'utilisateur",
            example = "Rue de la République, Tunis 1000"
    )
    private String adresse;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email trop long")
    @Schema(
            description = "Adresse email de l'utilisateur (doit être unique)",
            example = "mohamed.benali@gmail.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
            message = "Le mot de passe doit contenir : 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial"
    )
    @Schema(
            description = "Mot de passe (min 8 caractères, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial)",
            example = "Password123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String motDePasse;

//    @Size(max = 255, message = "URL d'image trop longue")
//    @Pattern(
//            regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp)|/uploads/.*\\.(jpg|jpeg|png|gif|webp))?$",
//            message = "Format d'image invalide. Extensions : jpg, jpeg, png, gif, webp"
//    )
    private String image;

    @NotNull(message = "L'état du compte est obligatoire")
    private StatutCompte etatCompte;

    @Size(max = 100, message = "Le poste ne peut pas dépasser 100 caractères")
    private String poste;

    @PastOrPresent(message = "La date d'embauche ne peut pas être dans le futur")
    @Schema(
            description = "Date D'embauche si employé",
            example = "2025-10-18T10:22:14.046Z"
    )
    private Date dateEmbauche;

    @Future(message = "La date de fin de contrat doit être dans le futur")
    @Schema(
            description = "Date Du fin de contrat si employé",
            example = "2025-10-18T10:22:14.046Z"
    )
    private Date dateFinContrat;

    @Schema(
            description = "Statut de l'employé",
            example = "EnTravail"
    )
    private StatutEmp statutEmploye;

    @Size(max = 500, message = "La bio ne peut pas dépasser 500 caractères")
    @Schema(
            description = "à propos ...",
            example = "petite description"
    )
    private String bio;

    @Size(max = 50, message = "Le nom du rôle ne peut pas dépasser 50 caractères")
    @Schema(
            description = "Role pour l'utilisateur",
            example = "EMPLOYE",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"CLIENT", "ADMIN", "EMPLOYE"}
    )
    private String role;


}
