package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.livraison.*;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface du service de gestion des livraisons
 * Sprint 6 - Gestion des livraisons
 */
public interface LivraisonServiceInterface {

    // ============================================
    // CRUD LIVRAISONS
    // ============================================

    /**
     * Créer une nouvelle livraison
     */
    LivraisonResponseDto creerLivraison(LivraisonRequestDto dto, String username);

    /**
     * Modifier une livraison existante
     */
    LivraisonResponseDto modifierLivraison(Long idLivraison, LivraisonRequestDto dto, String username);

    /**
     * Récupérer une livraison par ID
     */
    LivraisonResponseDto getLivraisonById(Long idLivraison);

    /**
     * Récupérer toutes les livraisons
     */
    List<LivraisonResponseDto> getAllLivraisons();

    /**
     * Récupérer les livraisons par statut
     */
    List<LivraisonResponseDto> getLivraisonsByStatut(StatutLivraison statut);

    /**
     * Récupérer les livraisons d'une date spécifique
     */
    List<LivraisonResponseDto> getLivraisonsByDate(LocalDate date);

    /**
     * Récupérer les livraisons entre deux dates
     */
    List<LivraisonResponseDto> getLivraisonsBetweenDates(LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupérer les livraisons d'aujourd'hui
     */
    List<LivraisonResponseDto> getLivraisonsAujourdhui();

    /**
     * Récupérer les livraisons affectées à un employé
     */
    List<LivraisonResponseDto> getLivraisonsByEmploye(Long idEmploye);

    /**
     * Récupérer les livraisons d'une réservation
     */
    List<LivraisonResponseDto> getLivraisonsByReservation(Long idReservation);

    /**
     * Obtenir toutes les lignes de réservation d'une livraison
     *
     * @param idLivraison ID de la livraison
     * @return Liste des lignes de réservation
     */
    List<LigneReservationResponseDto> getLignesLivraison(Long idLivraison);



    /**
     * Supprimer une livraison
     */
    void supprimerLivraison(Long idLivraison, String username);

    /**
     * Marquer une ligne de réservation spécifique comme LIVREE
     *
     * @param idLigne ID de la ligne de réservation
     * @param username Nom d'utilisateur de l'employé
     * @return LigneReservationResponseDto mise à jour
     */
    LigneReservationResponseDto marquerLigneLivree(Long idLigne, String username);



    // ============================================
    // GESTION DES STATUTS
    // ============================================

    /**
     * Changer le statut d'une livraison
     */
    LivraisonResponseDto changerStatutLivraison(Long idLivraison, StatutLivraison nouveauStatut, String username);

    /**
     * Marquer une livraison comme "En cours"
     */
    LivraisonResponseDto marquerLivraisonEnCours(Long idLivraison, String username);

    /**
     * Marquer une livraison comme "Livrée"
     */
    LivraisonResponseDto marquerLivraisonLivree(Long idLivraison, String username);

    // ============================================
    // AFFECTATION D'EMPLOYÉS
    // ============================================

    /**
     * Affecter un employé à une livraison
     */
    AffectationLivraisonDto affecterEmploye(AffectationLivraisonRequestDto dto, String username);

    /**
     * Retirer un employé d'une livraison
     */
    void retirerEmploye(Long idAffectation, String username);

    /**
     * Récupérer les affectations d'une livraison
     */
    List<AffectationLivraisonDto> getAffectationsByLivraison(Long idLivraison);

    /**
     * Récupérer les affectations d'un employé
     */
    List<AffectationLivraisonDto> getAffectationsByEmploye(Long idEmploye);

    // ============================================
    // BON DE LIVRAISON
    // ============================================

    /**
     * Générer un bon de livraison (PDF)
     */
    byte[] genererBonLivraison(Long idLivraison);

    // ============================================
    // STATISTIQUES
    // ============================================

    /**
     * Compter les livraisons par statut
     */
    Long countByStatut(StatutLivraison statut);
}