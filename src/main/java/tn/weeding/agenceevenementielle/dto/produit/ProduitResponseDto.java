package tn.weeding.agenceevenementielle.dto.produit;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.Categorie;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitResponseDto {

    private Long idProduit;
    private String codeProduit;
    private String nomProduit;
    private String descriptionProduit;
    private String imageProduit;
    private Categorie categorieProduit;
    private Double prixUnitaire;
    private Integer quantiteInitial;
    private Integer quantiteDisponible;
    private Boolean maintenanceRequise;
    private TypeProduit typeProduit;
    private Integer seuilCritique;

    // Indicateurs
    private Boolean enStock;
    private Boolean alerteStockCritique;

    // Statistiques
    private Integer nombreReservations;
    private Double moyenneNotes;
    private Integer nombreAvis;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereModification;
}