package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.produit.InstanceProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.produit.InstanceProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutInstance;
import tn.weeding.agenceevenementielle.exceptions.ProduitException;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface du service pour la gestion des instances de produits
 *
 */
public interface InstanceProduitServiceInterface {

    // ============ CRUD DE BASE ============

    /**
     * Créer une nouvelle instance de produit
     */
    InstanceProduitResponseDto creerInstance(InstanceProduitRequestDto dto, String username);

    /**
     * Modifier une instance existante
     */
    InstanceProduitResponseDto modifierInstance(Long idInstance, InstanceProduitRequestDto dto, String username);

    /**
     * Supprimer une instance
     */
    void supprimerInstance(Long idInstance);

    /**
     * Récupérer une instance par son ID
     */
    InstanceProduitResponseDto getInstanceById(Long idInstance);

    /**
     * Récupérer toutes les instances
     */
    List<InstanceProduitResponseDto> getInstances();
    /**
     * Récupérer une instance par son numéro de série
     */
    InstanceProduitResponseDto getInstanceByNumeroSerie(String numeroSerie);

    // ============ CONSULTATION ============

    /**
     * Récupérer toutes les instances d'un produit
     */
    List<InstanceProduitResponseDto> getInstancesByProduit(Long idProduit);

    /**
     * Récupérer les instances disponibles d'un produit
     */
    List<InstanceProduitResponseDto> getInstancesDisponibles(Long idProduit);

    /**
     * Récupérer les instances par statut
     */
    List<InstanceProduitResponseDto> getInstancesByStatut(StatutInstance statut);

    /**
     * Récupérer les instances d'une ligne de réservation
     */
    List<InstanceProduitResponseDto> getInstancesByLigneReservation(Long idLigneReservation);

    // ============ GESTION DES STATUTS ============

    /**
     * Changer le statut d'une instance
     */
    InstanceProduitResponseDto changerStatut(Long idInstance, StatutInstance nouveauStatut, String username);

    // ============ GESTION DE LA MAINTENANCE ============

    /**
     * Marquer une instance comme en maintenance
     */
    InstanceProduitResponseDto envoyerEnMaintenance(Long idInstance, String motif, String username);

    /**
     * Marquer une instance comme retournée de maintenance
     */
    InstanceProduitResponseDto retournerDeMaintenance(Long idInstance,LocalDate dateProchainMaintenance , String username);

    /**
     * Récupérer les instances nécessitant une maintenance
     */
    List<InstanceProduitResponseDto> getInstancesNecessitantMaintenance();

    // ============ RÉSERVATION (Utilisé par ReservationService) ============

    /**
     * Réserver des instances pour une ligne de réservation
     * Sélectionne automatiquement N instances disponibles
     */
    List<InstanceProduitResponseDto> reserverInstances(Long idProduit, int quantite, Long idLigneReservation, String username);

    /**
     * Libérer les instances d'une ligne de réservation
     */
    void libererInstances(Long idLigneReservation, String username);

    /**
     * Libérer une instance spécifique d'une réservation
     * Remet l'instance à DISPONIBLE et supprime la référence à la ligne de réservation
     *
     * @param idInstance ID de l'instance à libérer
     * @param username Utilisateur effectuant l'action
     * @return DTO de l'instance libérée
     * @throws ProduitException si l'instance n'existe pas
     */
    InstanceProduitResponseDto libererInstance(Long idInstance, String username);

    // ============ CRÉATION EN LOT ============

    /**
     * Créer plusieurs instances en lot pour un produit
     */
    List<InstanceProduitResponseDto> creerInstancesEnLot(Long idProduit, int quantite, String prefixeNumeroSerie, String username);
}