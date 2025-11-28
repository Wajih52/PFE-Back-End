package tn.weeding.agenceevenementielle.dto.avis;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisCreateDto {

    @NotNull(message = "L'ID de la réservation est obligatoire")
    private Long idReservation;

    @NotNull(message = "L'ID du produit est obligatoire")
    private Long idProduit;

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    private Integer note;

    @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères")
    private String commentaire;
}