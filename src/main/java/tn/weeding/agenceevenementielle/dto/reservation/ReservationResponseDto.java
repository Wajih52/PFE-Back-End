package tn.weeding.agenceevenementielle.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.entities.enums.ModePaiement;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * DTO de réponse complète pour une réservation/devis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDto {

    // Identifiants
    private Long idReservation;
    private String referenceReservation;  // Ex: RES-2025-001

    // Client
    private Long idUtilisateur;
    private String nomClient;
    private String prenomClient;
    private String emailClient;
    private Long telephoneClient;

    // Dates
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDateTime dateCreation;

    // Statut
    private StatutReservation statutReservation;  // EnAttente, confirme, annule
    private StatutLivraison statutLivraisonRes;

    // Montants
    private Double montantOriginal;      // Montant avant remises
    private Double remisePourcentage;    // Remise en %
    private Double remiseMontant;        // Remise en montant fixe
    private Double montantTotal;         // Montant final après remises
    private Double montantPaye;          // Montant déjà payé
    private Double montantRestant;       // Montant restant à payer

    // Paiement
    private ModePaiement modePaiementRes;

    // Lignes de réservation (produits)
    private List<LigneReservationResponseDto> lignesReservation;

    // Observations
    private String observationsClient;
    private String commentaireAdmin;

    // Indicateurs
    private Boolean estDevis;           // true si en attente (devis non confirmé)
    private Boolean paiementComplet;    // true si montantPaye >= montantTotal
    private Integer nombreProduits;     // Nombre total de produits
    private Integer joursLocation;      // Durée en jours
}
