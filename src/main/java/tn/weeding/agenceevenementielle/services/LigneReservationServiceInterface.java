package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.reservation.LigneModificationDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;

import java.util.List;

/**
 * ==========================================
 * INTERFACE DU SERVICE DE LIGNE DE RÉSERVATION
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 */
public interface LigneReservationServiceInterface {

    /**
     * Créer une ligne de réservation
     * (utilisé en interne par le ReservationService)
     */
    LigneReservationResponseDto creerLigneReservation(
            LigneReservationRequestDto ligneDto,
            Long idReservation,
            String username
    );

    /**
     * Récupérer toutes les lignes d'une réservation
     */
    List<LigneReservationResponseDto> getLignesByReservation(Long idReservation);

    /**
     * Récupérer une ligne par son ID
     */
    LigneReservationResponseDto getLigneById(Long idLigne);

    /**
     * Modifier une ligne de réservation (quantité, prix, dates)
     * Utilisé par l'admin lors de la modification du devis
     */
    LigneReservationResponseDto modifierLigne(
            Long idLigne,
            LigneModificationDto modificationDto,
            String username
    );

    /**
     * Supprimer une ligne de réservation
     * (si la réservation est encore en mode devis)
     */
    void supprimerLigne(Long idLigne, String username);

    /**
     * Assigner des instances spécifiques à une ligne
     * (pour produits avec référence lors de la confirmation)
     */
    LigneReservationResponseDto assignerInstances(
            Long idLigne,
            List<Long> idsInstances,
            String username
    );

    /**
     * Libérer les instances d'une ligne
     * (lors de l'annulation ou du retour)
     */
    void libererInstances(Long idLigne, String username);

    /**
     * Récupérer les lignes d'un produit
     */
    List<LigneReservationResponseDto> getLignesByProduit(Long idProduit);

    /**
     * Récupérer les lignes sans livraison assignée
     */
    List<LigneReservationResponseDto> getLignesSansLivraison();

    /**
     * Récupérer les livraisons prévues pour aujourd'hui
     */
    List<LigneReservationResponseDto> getLivraisonsAujourdhui();

    /**
     * Récupérer les retours prévus pour aujourd'hui
     */
    List<LigneReservationResponseDto> getRetoursAujourdhui();

    /**
     * Récupérer les retours en retard
     */
    List<LigneReservationResponseDto> getRetoursEnRetard();
}