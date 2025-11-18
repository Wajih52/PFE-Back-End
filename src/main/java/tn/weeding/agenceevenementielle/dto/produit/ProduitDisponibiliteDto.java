package tn.weeding.agenceevenementielle.dto.produit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;

/**
 * DTO pour afficher la disponibilité d'un produit sur une période
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitDisponibiliteDto {
    private Long idProduit;
    private String codeProduit;
    private String nomProduit;
    private TypeProduit typeProduit;

    private Integer quantiteTotale;        // Stock total du produit
    private Integer quantiteReservee;      // Quantité réservée sur la période
    private Integer quantiteDisponible;    // quantiteTotale - quantiteReservee
}