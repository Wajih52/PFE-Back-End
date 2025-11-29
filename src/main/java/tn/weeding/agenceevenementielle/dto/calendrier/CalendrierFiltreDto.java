package tn.weeding.agenceevenementielle.dto.calendrier;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour filtrer les événements du calendrier
 * Sprint 7 - Gestion du calendrier
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendrierFiltreDto {

    // ============================================
    // FILTRES TEMPORELS
    // ============================================
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    // ============================================
    // FILTRES PAR TYPE D'ÉVÉNEMENT
    // ============================================
    private Boolean inclureReservations = true;
    private Boolean inclureLivraisons = true;

    // ============================================
    // FILTRES PAR STATUT
    // ============================================
    private List<String> statutsReservation; // EN_ATTENTE, CONFIRME, etc.
    private List<String> statutsLivraison; // NOT_TODAY, EN_COURS, etc.

    // ============================================
    // FILTRES PAR CLIENT
    // ============================================
    private Long idClient; // Filtrer par un client spécifique
    private String nomClient; // Recherche par nom

    // ============================================
    // FILTRES PAR EMPLOYÉ
    // ============================================
    private Long idEmploye; // Livraisons affectées à un employé

    // ============================================
    // FILTRES PAR PRODUIT
    // ============================================
    private Long idProduit; // Réservations/livraisons contenant ce produit
    private String nomProduit; // Recherche par nom de produit

    // ============================================
    // AUTRES FILTRES
    // ============================================
    private Boolean paiementCompletUniquement = false; // Seulement réservations payées
    private Boolean avecRetardPaiement = false; // Alertes paiement
}