package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.avis.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutAvis;

import java.time.LocalDateTime;
import java.util.List;

public interface AvisServiceInterface {

    // ============================================
    // CRUD CLIENT
    // ============================================

    /**
     * Client crée un avis (après avoir loué un produit)
     */
    AvisResponseDto creerAvis(AvisCreateDto dto, String username);

    /**
     * Client modifie son avis (uniquement si EN_ATTENTE)
     */
    AvisResponseDto modifierAvis(AvisUpdateDto dto, String username);

    /**
     * Client supprime son avis (soft delete)
     */
    void supprimerAvis(Long idAvis, String username);

    /**
     * Client consulte ses propres avis
     */
    List<AvisResponseDto> getMesAvis(String username);

    /**
     * Vérifier si un client peut évaluer un produit d'une réservation
     */
    Boolean peutEvaluerProduit(Long idReservation, Long idProduit, String username);

    // ============================================
    // CONSULTATION PUBLIQUE
    // ============================================

    /**
     * Obtenir tous les avis approuvés d'un produit (visible par tous)
     */
    List<AvisResponseDto> getAvisApprouvesByProduit(Long idProduit);

    /**
     * Obtenir les statistiques d'un produit
     */
    StatistiquesAvisDto getStatistiquesAvisProduit(Long idProduit);

    // ============================================
    // MODÉRATION ADMIN
    // ============================================

    /**
     * Admin consulte tous les avis (peu importe le statut)
     */
    List<AvisResponseDto> getAllAvis();

    /**
     * Admin consulte les avis en attente de modération
     */
    List<AvisResponseDto> getAvisEnAttente();

    /**
     * Admin modère un avis (approuver/rejeter)
     */
    AvisResponseDto modererAvis(AvisModerationDto dto, String adminUsername);

    /**
     * Admin supprime définitivement un avis
     */
    void supprimerAvisDefinitivement(Long idAvis);

    /**
     * Admin consulte les avis par statut
     */
    List<AvisResponseDto> getAvisByStatut(StatutAvis statut);

    /**
     * Admin consulte les avis d'un client spécifique
     */
    List<AvisResponseDto> getAvisByClient(Long clientId);

    /**
     * Admin consulte tous les avis d'un produit (tous statuts)
     */
    List<AvisResponseDto> getAllAvisByProduit(Long idProduit);

    // ============================================
    // RECHERCHE ET FILTRAGE
    // ============================================

    /**
     * Rechercher des avis par note
     */
    List<AvisResponseDto> getAvisByNote(Integer note);

    /**
     * Rechercher des avis par période
     */
    List<AvisResponseDto> getAvisByPeriode(LocalDateTime debut, LocalDateTime fin);

    /**
     * Rechercher des avis par mot-clé dans le commentaire
     */
    List<AvisResponseDto> searchAvisByKeyword(String keyword);

    // ============================================
    // STATISTIQUES GLOBALES
    // ============================================

    /**
     * Obtenir le nombre d'avis en attente
     */
    Long getNombreAvisEnAttente();

    /**
     * Obtenir les produits les mieux notés
     */
    List<Object[]> getTopProduitsParNote(Long minAvis);
}