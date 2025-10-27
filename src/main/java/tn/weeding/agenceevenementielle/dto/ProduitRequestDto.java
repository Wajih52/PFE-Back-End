package tn.weeding.agenceevenementielle.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.Categorie;
import tn.weeding.agenceevenementielle.entities.TypeProduit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitRequestDto {

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 3, max = 100)
    private String nomProduit;

    @NotBlank(message = "La description est obligatoire")
    @Size(min = 10, max = 1000)
    private String descriptionProduit;

    @NotNull(message = "La catégorie est obligatoire")
    private Categorie categorieProduit;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private Double prixUnitaire;

    @NotNull(message = "La quantité initiale est obligatoire")
    @Min(value = 0)
    private Integer quantiteInitial;

    @NotNull(message = "Le type de produit est obligatoire")
    private TypeProduit typeProduit;

    private Boolean maintenanceRequise = false;
    private String imageProduit;

    @Min(value = 0)
    private Integer seuilCritique;
}