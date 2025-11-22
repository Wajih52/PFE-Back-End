package tn.weeding.agenceevenementielle.dto.livraison;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO de réponse pour une livraison
 * Sprint 6 - Gestion des livraisons
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LivraisonResponseDto {

    private Long idLivraison;
    private String titreLivraison;
    private String adresseLivraison;
    private LocalDate dateLivraison;
    private LocalTime heureLivraison;
    private StatutLivraison statutLivraison;
    private String observations;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    /**
     * Liste des lignes de réservation associées
     */
    private List<LigneLivraisonDto> lignesReservation;

    /**
     * Liste des employés affectés à cette livraison
     */
    private List<AffectationLivraisonDto> affectations;

    /**
     * Informations sur le client (nom + référence réservation)
     */
    private String nomClient;
    private String prenomClient;
    private Long telephoneClient;
    private String emailClient;
    private String referenceReservation;

    /**
     * Nombre total d'articles à livrer
     */
    private Integer nombreTotalArticles;

    /**
     * DTO simplifié pour une ligne de réservation dans une livraison
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LigneLivraisonDto {
        private Long idLigne;
        private String nomProduit;
        private Integer quantite;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private StatutLivraison statutLivraisonLigne;
        private String typeProduit; // EN_QUANTITE ou AVEC_REFERENCE
        private List<String> instancesReservees; // Pour produits avec référence

    }
}