package tn.weeding.agenceevenementielle.services.Reservation;

import tn.weeding.agenceevenementielle.dto.produit.InstanceProduitResponseDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import  tn.weeding.agenceevenementielle.exceptions.CustomException;
import  tn.weeding.agenceevenementielle.exceptions.ReservationException;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface LigneReservationServiceInterface {

    // ============================================
    // CRÉATION ET AJOUT
    // ============================================

    /**
     * Créer une nouvelle ligne de réservation (ajouter un produit au panier)
     *
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
     */
    LigneReservationResponseDto getLigneReservationById(Long id);

    /**
     * Récupérer toutes les lignes d'une réservation (le panier)
     *
    */
    List<LigneReservationResponseDto> getLignesReservationByReservation(Long idReservation);

    /**
     * Récupérer les lignes contenant un produit spécifique
     * Utile pour voir quelles réservations utilisent un produit
     *
     */
    List<LigneReservationResponseDto> getLignesReservationByProduit(Long idProduit);

    /**
     * Récupérer les lignes par statut de livraison
     */
    List<LigneReservationResponseDto> getLignesReservationByStatut(StatutLivraison statut);

    // ============================================
    // MODIFICATION
    // ============================================

    /**
     * Modifier une ligne de réservation
     * Gère automatiquement les changements de stock si la quantité change
     *
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
     */
    LigneReservationResponseDto updateStatutLivraison(Long id, StatutLivraison nouveauStatut,String username);

    // ============================================
    // SUPPRESSION
    // ============================================

    /**
     * Supprimer une ligne de réservation
     * Libère automatiquement le stock et les instances
     *
     */
    void supprimerLigneReservation(Long id, String username);

    // ============================================
    // STATISTIQUES ET CALCULS
    // ============================================

    /**
     * Calculer le montant total d'une réservation
     * Somme: quantité × prix unitaire pour chaque ligne
     */
    Double calculerMontantTotalReservation(Long idReservation);

    /**
     * Obtenir les statistiques d'une réservation
     * Inclut: nombre de lignes, produits, montant, répartition par catégorie
     */
    Map<String, Object> getStatistiquesReservation(Long idReservation);

    Set<InstanceProduitResponseDto> getInstancesLigneReservation(Long idLigneReservation);
}