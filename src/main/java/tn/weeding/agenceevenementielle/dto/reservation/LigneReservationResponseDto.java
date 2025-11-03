package tn.weeding.agenceevenementielle.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * DTO de réponse pour une ligne de réservation
 * Contient les infos complètes du produit associé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneReservationResponseDto {

    private Long idLigneReservation;
    private Long idProduit;
    private String nomProduit;
    private String codeProduit;
    private String imageProduit;
    private Integer quantite;
    private Double prixUnitaire;
    private Double sousTotal;  // quantite * prixUnitaire
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutLivraison statutLivraisonLigne;
    private String observations;

    // Pour les produits avec référence: liste des instances réservées
    private List<String> numerosSeries;  // Ex: ["PROJ-2025-001", "PROJ-2025-002"]

    // Infos de livraison si assignée
    private Long idLivraison;
    private String titreLivraison;
}
