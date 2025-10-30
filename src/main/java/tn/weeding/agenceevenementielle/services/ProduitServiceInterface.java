package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.produit.MouvementStockResponseDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.Categorie;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;

import java.util.Date;
import java.util.List;

/**
 * Interface du service de gestion des produits
 */
public interface ProduitServiceInterface {

    // ============ GESTION DES PRODUITS ============

    /**
     * Créer un nouveau produit
     */
    ProduitResponseDto creerProduit(ProduitRequestDto produitDto, String username);

    /**
     * Modifier un produit existant
     */
    ProduitResponseDto modifierProduit(Long idProduit, ProduitRequestDto produitDto, String username);

    /**
     * Supprimer un produit (suppression logique en mettant quantité à 0)
     */
    void supprimerProduit(Long idProduit, String username);

    /**
     * Supprimer un produit (suppression logique en mettant quantité à 0)
     */
    void supprimerProduitDeBase(Long idProduit, String username);

    /**
     * Désactiver un produit (quantité disponible = 0, mais garde les données)
     */
    void desactiverProduit(Long idProduit, String username);

    /**
     * Réactiver un produit
     */
    ProduitResponseDto reactiverProduit(Long idProduit, Integer quantite, String username);

    /**
     * Obtenir un produit par son ID
     */
    ProduitResponseDto getProduitById(Long idProduit);

    /**
     * Obtenir un produit par son code
     */
    ProduitResponseDto getProduitByCode(String codeProduit);

    /**
     * Obtenir tous les produits
     */
    List<ProduitResponseDto> getAllProduits();

    /**
     * Obtenir les produits disponibles
     */
    List<ProduitResponseDto> getProduitsDisponibles();

    /**
     * Obtenir les produits en rupture de stock
     */
    List<ProduitResponseDto> getProduitsEnRupture();

    /**
     * Obtenir les produits avec stock critique
     */
    List<ProduitResponseDto> getProduitsStockCritique(Integer seuil);

    /**
     * Rechercher des produits par nom
     */
    List<ProduitResponseDto> searchProduitsByNom(String nom);

    /**
     * Rechercher des produits par catégorie
     */
    List<ProduitResponseDto> getProduitsByCategorie(Categorie categorie);

    /**
     * Rechercher des produits par type
     */
    List<ProduitResponseDto> getProduitsByType(TypeProduit typeProduit);

    /**
     * Recherche multicritères
     */
    List<ProduitResponseDto> searchProduits(
            Categorie categorie,
            TypeProduit typeProduit,
            Double minPrix,
            Double maxPrix,
            Boolean disponible
    );

    /**
     * Obtenir les produits les plus loués
     */
    List<ProduitResponseDto> getProduitsLesPlusLoues();

    /**
     * Obtenir les produits les mieux notés
     */
    List<ProduitResponseDto> getProduitsMieuxNotes(Double minNote);

    // ============ GESTION DU STOCK ============

    /**
     * Ajuster manuellement le stock d'un produit
     */
    ProduitResponseDto ajusterStock(Long idProduit, Integer quantite, String motif, String username);

    /**
     * Décrémenter le stock lors d'une réservation
     */
    void decrementerStock(Long idProduit, Integer quantite, Long idReservation, String username);

    /**
     * Incrémenter le stock lors d'un retour
     */
    void incrementerStock(Long idProduit, Integer quantite, Long idReservation, String username);

    /**
     * Marquer un produit comme endommagé
     */
    void marquerProduitEndommage(Long idProduit, Integer quantite, String motif, String username);

    /**
     * Mettre un produit en maintenance
     */
    void mettreEnMaintenance(Long idProduit, Integer quantite, String motif, String username);

    /**
     * Retourner un produit de la maintenance
     */
    void retournerDeMaintenance(Long idProduit, Integer quantite, String motif, String username);

    /**
     * Vérifier si un produit est disponible en quantité suffisante
     */
    boolean verifierDisponibilite(Long idProduit, Integer quantiteRequise);

    /**
     * Vérifier si le stock est critique
     */
    boolean verifierStockCritique(Long idProduit);

    // ============ HISTORIQUE DES MOUVEMENTS ============

    /**
     * Obtenir l'historique complet des mouvements d'un produit
     */
    List<MouvementStockResponseDto> getHistoriqueMouvements(Long idProduit);

    /**
     * Obtenir les mouvements par type
     */
    List<MouvementStockResponseDto> getMouvementsByType(TypeMouvement typeMouvement);

    /**
     * Obtenir les mouvements effectués par un utilisateur
     */
    List<MouvementStockResponseDto> getMouvementsByUser(String username);

    /**
     * Obtenir les mouvements dans une période
     */
    List<MouvementStockResponseDto> getMouvementsByPeriode(Date dateDebut, Date dateFin);

    /**
     * Obtenir les derniers mouvements (pour dashboard)
     */
    List<MouvementStockResponseDto> getRecentMouvements(Integer limit);

    /**
     * Obtenir les statistiques d'un produit (entrées, sorties, etc.)
     */
    StockStatistiquesDto getStatistiquesProduit(Long idProduit);
}

