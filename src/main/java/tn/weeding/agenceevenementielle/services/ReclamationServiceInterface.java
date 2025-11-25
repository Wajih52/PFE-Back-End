package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.reclamation.*;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;
import tn.weeding.agenceevenementielle.entities.enums.TypeReclamation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface du service de gestion des réclamations
 */
public interface ReclamationServiceInterface {

    /**
     * Créer une réclamation (visiteur ou client connecté)
     * @param dto DTO de création
     * @param username Username de l'utilisateur connecté (null pour visiteur)
     * @return DTO de la réclamation créée
     */
    ReclamationResponseDto creerReclamation(ReclamationRequestDto dto, String username);

    /**
     * Récupérer toutes les réclamations (ADMIN)
     */
    List<ReclamationResponseDto> getAllReclamations();

    /**
     * Récupérer une réclamation par ID
     */
    ReclamationResponseDto getReclamationById(Long id);

    /**
     * Récupérer une réclamation par code
     */
    ReclamationResponseDto getReclamationByCode(String code);

    /**
     * Récupérer les réclamations d'un utilisateur
     */
    List<ReclamationResponseDto> getReclamationsByUtilisateur(Long idUtilisateur);

    /**
     * Récupérer les réclamations par email (pour visiteurs)
     */
    List<ReclamationResponseDto> getReclamationsByEmail(String email);

    /**
     * Récupérer les réclamations par statut
     */
    List<ReclamationResponseDto> getReclamationsByStatut(StatutReclamation statut);

    /**
     * Récupérer les réclamations par type
     */
    List<ReclamationResponseDto> getReclamationsByType(TypeReclamation type);

    /**
     * Récupérer les réclamations par priorité
     */
    List<ReclamationResponseDto> getReclamationsByPriorite(PrioriteReclamation priorite);

    /**
     * Récupérer les réclamations liées à une réservation
     */
    List<ReclamationResponseDto> getReclamationsByReservation(Long idReservation);

    /**
     * Classer une réclamation (priorité et statut) - ADMIN/EMPLOYE
     */
    ReclamationResponseDto classerReclamation(Long id, ClasserReclamationDto dto, String username);

    /**
     * Traiter/Répondre à une réclamation - ADMIN/EMPLOYE
     */
    ReclamationResponseDto traiterReclamation(Long id, TraiterReclamationDto dto, String username);

    /**
     * Recherche multi-critères
     */
    List<ReclamationResponseDto> rechercherReclamations(
            StatutReclamation statut,
            TypeReclamation type,
            PrioriteReclamation priorite,
            Long idUtilisateur
    );

    /**
     * Récupérer les réclamations dans une période
     */
    List<ReclamationResponseDto> getReclamationsByPeriode(LocalDateTime debut, LocalDateTime fin);

    /**
     * Compter les réclamations par statut
     */
    long countByStatut(StatutReclamation statut);

    /**
     * Compter les réclamations urgentes non traitées
     */
    long countReclamationsUrgentesNonTraitees();

    /**
     * Supprimer une réclamation (ADMIN uniquement)
     */
    void deleteReclamation(Long id);
}