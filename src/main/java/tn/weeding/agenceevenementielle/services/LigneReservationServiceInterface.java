package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import  tn.weeding.agenceevenementielle.exceptions.CustomException;

import java.util.List;
import java.util.Map;

/**
 * ==========================================
 * INTERFACE DU SERVICE DE GESTION DES LIGNES DE RÉSERVATION
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 *
 * Contrat de service pour:
 * - Gérer les produits dans le panier/réservation
 * - Vérifier la disponibilité automatiquement
 * - Affecter les instances pour produits avec référence
 * - Calculer les montants
 * - Gérer le stock (décrémentation/libération)
 */
public interface LigneReservationServiceInterface {

    // ============================================
    // CRÉATION ET AJOUT
    // ============================================

    /**
     * Créer une nouvelle ligne de réservation (ajouter un produit au panier)
     *
     * @param dto DTO contenant les informations de la ligne
     * @param idReservation ID de la réservation parente
     * @param username Utilisateur effectuant l'action
     * @return DTO de la ligne créée
     * @throws CustomException si le produit ou la réservation n'existe pas
     * @throws CustomException si le stock est insuffisant
     */
    LigneReservationResponseDto creerLigneReservation(
            LigneReservationRequestDto dto,
            Long idReservation,
            String username
    );

    // ============================================
    // CONSULTATION
    // ============================================

    /**
     * Récupérer une ligne par son ID
     *
     * @param id ID de la ligne
     * @return DTO de la ligne
     * @throws ReservationException si la ligne n'existe pas
     */
    LigneReservationResponseDto getLigneReservationById(Long id);

    /**
     * Récupérer toutes les lignes d'une réservation (le panier)
     *
     * @param idReservation ID de la réservation
     * @return Liste des lignes
     * @throws ReservationException si la réservation n'existe pas
     */
    List<LigneReservationResponseDto> getLignesReservationByReservation(Long idReservation);

    /**
     * Récupérer les lignes contenant un produit spécifique
     * Utile pour voir quelles réservations utilisent un produit
     *
     * @param idProduit ID du produit
     * @return Liste des lignes contenant ce produit
     */
    List<LigneReservationResponseDto> getLignesReservationByProduit(Long idProduit);

    /**
     * Récupérer les lignes par statut de livraison
     *
     * @param statut Statut recherché (EN_ATTENTE, EN_LIVRAISON, LIVRE, etc.)
     * @return Liste des lignes ayant ce statut
     */
    List<LigneReservationResponseDto> getLignesReservationByStatut(StatutLivraison statut);

    // ============================================
    // MODIFICATION
    // ============================================

    /**
     * Modifier une ligne de réservation
     * Gère automatiquement les changements de stock si la quantité change
     *
     * @param id ID de la ligne à modifier
     * @param dto Nouvelles données
     * @param username Utilisateur effectuant l'action
     * @return DTO de la ligne modifiée
     * @throws ReservationException si la ligne n'existe pas
     * @throws CustomException si le stock est insuffisant pour augmentation
     */
    LigneReservationResponseDto modifierLigneReservation(
            Long id,
            LigneReservationRequestDto dto,
            String username
    );

    /**
     * Mettre à jour le statut de livraison d'une ligne
     * Met à jour automatiquement le statut des instances associées
     *
     * @param id ID de la ligne
     * @param nouveauStatut Nouveau statut
     * @return DTO de la ligne mise à jour
     * @throws ReservationException si la ligne n'existe pas
     */
    LigneReservationResponseDto updateStatutLivraison(Long id, StatutLivraison nouveauStatut);

    // ============================================
    // SUPPRESSION
    // ============================================

    /**
     * Supprimer une ligne de réservation
     * Libère automatiquement le stock et les instances
     *
     * @param id ID de la ligne à supprimer
     * @param username Utilisateur effectuant l'action
     * @throws ReservationException si la ligne n'existe pas
     */
    void supprimerLigneReservation(Long id, String username);

    // ============================================
    // STATISTIQUES ET CALCULS
    // ============================================

    /**
     * Calculer le montant total d'une réservation
     * Somme: quantité × prix unitaire pour chaque ligne
     *
     * @param idReservation ID de la réservation
     * @return Montant total en TND
     */
    Double calculerMontantTotalReservation(Long idReservation);

    /**
     * Obtenir les statistiques d'une réservation
     * Inclut: nombre de lignes, produits, montant, répartition par catégorie
     *
     * @param idReservation ID de la réservation
     * @return Map contenant les statistiques
     */
    Map<String, Object> getStatistiquesReservation(Long idReservation);
}