package tn.weeding.agenceevenementielle.dto;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.EtatPhysique;
import tn.weeding.agenceevenementielle.entities.StatutInstance;
import java.time.LocalDate;

/**
 * DTO de réponse pour les instances de produits
 * Sprint 3 - Gestion des produits et du stock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanceProduitResponseDto {

    private Long idInstance;
    private String numeroSerie;
    private StatutInstance statut;
    private EtatPhysique etatPhysique;

    // Informations du produit parent
    private Long idProduit;
    private String nomProduit;
    private String codeProduit;

    // Informations de la ligne de réservation (si réservée)
    private Long idLigneReservation;
    private Long idReservation;
    private String clientNom;
    private String clientPrenom;

    private String observations;
    private LocalDate dateAcquisition;
    private LocalDate dateDerniereMaintenance;
    private LocalDate dateProchaineMaintenance;

    // Indicateurs
    private Boolean disponible;
    private Boolean maintenanceRequise;
    private Integer joursAvantMaintenance; // Nombre de jours avant la prochaine maintenance
}