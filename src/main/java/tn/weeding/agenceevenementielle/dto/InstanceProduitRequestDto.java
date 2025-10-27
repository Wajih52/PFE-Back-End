package tn.weeding.agenceevenementielle.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.EtatPhysique;
import tn.weeding.agenceevenementielle.entities.StatutInstance;
import java.time.LocalDate;

/**
 * DTO pour la création/modification d'instances de produits avec référence
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanceProduitRequestDto {

    @NotBlank(message = "Le numéro de série est obligatoire")
    @Size(min = 3, max = 50, message = "Le numéro de série doit contenir entre 3 et 50 caractères")
    private String numeroSerie;

    @NotNull(message = "L'ID du produit est obligatoire")
    private Long idProduit;

    @NotNull(message = "Le statut est obligatoire")
    private StatutInstance statut;

    private EtatPhysique etatPhysique;

    @Size(max = 1000, message = "Les observations ne doivent pas dépasser 1000 caractères")
    private String observations;

    @PastOrPresent(message = "La date d'acquisition ne peut pas être dans le futur")
    private LocalDate dateAcquisition;

    @PastOrPresent(message = "La date de dernière maintenance ne peut pas être dans le futur")
    private LocalDate dateDerniereMaintenance;

    @Future(message = "La date de prochaine maintenance doit être dans le futur")
    private LocalDate dateProchaineMaintenance;
}