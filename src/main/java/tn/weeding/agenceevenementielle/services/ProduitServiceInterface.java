package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.produit.MouvementStockResponseDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.Categorie;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ✅ VERSION CORRIGÉE : Interface du service de gestion des produits
 *
 * Nouvelles méthodes ajoutées pour gérer la disponibilité avec périodes
 * Sprint 3 : Gestion des produits et du stock
 */
public interface ProduitServiceInterface {

    // ============================================
    // GESTION DES PRODUITS (CRUD)
    // ============================================

    /**
     * Créer un nouveau produit
     */
    ProduitResponseDto creerProduit(ProduitRequestDto produitDto, String username);

    /**
     * Modifier un produit existant
     */
    ProduitResponseDto modifierProduit(Long id, ProduitRequestDto produitDto, String username);

    /**
     * Supprimer/désactiver un produit
     */
    void supprimerProduit(Long id, String username);

    /**
     * Supprimer un produit (de la base des données)
     */
    void supprimerProduitDeBase(Long idProduit, String username);
    /**
     * Réactiver un produit désactivé
     */
    ProduitResponseDto reactiverProduit(Long id, Integer quantite, String username);

    /**
     * Obtenir un produit par son ID
     */
    ProduitResponseDto getProduitById(Long id);

    /**
     * Obtenir un produit par son code
     */
    ProduitResponseDto getProduitByCode(String code);

    /**
     * Obtenir tous les produits
     */
    List<ProduitResponseDto> getAllProduits();

    // ============================================
    // RECHERCHE ET FILTRAGE (SANS PÉRIODE)
    // ============================================

    /**
     * Obtenir les produits disponibles (stock > 0)
     * NE PREND PAS EN COMPTE LES PÉRIODES
     * Utilisé uniquement pour la vue administrative du stock global
     */
    List<ProduitResponseDto> getProduitsDisponibles();

    /**
     * Obtenir les produits en rupture de stock
     */
    List<ProduitResponseDto> getProduitsEnRupture();

    /**
     * Obtenir les produits avec stock critique
     * NE PREND PAS EN COMPTE LES PÉRIODES
     */
    List<ProduitResponseDto> getProduitsStockCritique(Integer seuil);

    /**
     * Rechercher des produits par nom
     */
    List<ProduitResponseDto> searchProduitsByNom(String nom);

    /**
     * Filtrer par catégorie
     */
    List<ProduitResponseDto> getProduitsByCategorie(Categorie categorie);

    /**
     * Filtrer par type de produit
     */
    List<ProduitResponseDto> getProduitsByType(TypeProduit typeProduit);

    /**
     * Recherche multicritères (sans période)
     */
    List<ProduitResponseDto> searchProduits(
            Categorie categorie,
            TypeProduit typeProduit,
            Double minPrix,
            Double maxPrix,
            Boolean disponible
    );

    // ============================================
    // DISPONIBILITÉ AVEC PÉRIODE
    // ============================================

    /**
     * Calculer la quantité disponible sur une période
     *
     * Pour produits de type EN_QUANTITE uniquement
     * Retourne : quantiteDisponible - quantités réservées pendant la période
     *
     * @param idProduit ID du produit
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @return Quantité réellement disponible
     */
    Integer calculerQuantiteDisponibleSurPeriode(Long idProduit, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Vérifier si une quantité est disponible sur une période
     *
     * Remplace l'ancienne méthode verifierDisponibilite(id, quantite)
     *
     * @param idProduit ID du produit
     * @param quantiteDemandee Quantité souhaitée
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return true si disponible, false sinon
     */
    Boolean verifierDisponibiliteSurPeriode(
            Long idProduit,
            Integer quantiteDemandee,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    /**
     * Obtenir le catalogue disponible sur une période
     *
     * Retourne uniquement les produits réellement disponibles pendant la période
     * Utilisé pour l'affichage du catalogue client avec dates
     *
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des produits disponibles
     */
    List<ProduitResponseDto> getCatalogueDisponibleSurPeriode(LocalDate dateDebut, LocalDate dateFin);

    /**
     * Recherche multicritères avec période
     *
     * Permet de filtrer les produits disponibles selon plusieurs critères
     * ET une période de disponibilité
     *
     * @param categorie Catégorie (optionnel)
     * @param typeProduit Type de produit (optionnel)
     * @param minPrix Prix minimum (optionnel)
     * @param maxPrix Prix maximum (optionnel)
     * @param dateDebut Date de début (optionnel, mais si fourni dateFin obligatoire)
     * @param dateFin Date de fin (optionnel, mais si fourni dateDebut obligatoire)
     * @return Liste des produits correspondants
     */
    List<ProduitResponseDto> searchProduitsAvecPeriode(
            Categorie categorie,
            TypeProduit typeProduit,
            Double minPrix,
            Double maxPrix,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    /**
     * Produits avec quantité minimum disponible sur période
     *
     * Utilisé pour suggérer des alternatives ou filtrer par quantité minimale
     *
     * @param quantiteMin Quantité minimum requise
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des produits avec quantité suffisante
     */
    List<ProduitResponseDto> getProduitsAvecQuantiteMinSurPeriode(
            Integer quantiteMin,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    /**
     *  Produits en stock critique sur une période
     *
     * Identifie les produits qui auront un stock faible pendant une période
     * Utile pour alertes et planification
     *
     * @param seuil Seuil critique (ex: 5)
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des produits en situation critique
     */
    List<ProduitResponseDto> getProduitsStockCritiqueSurPeriode(
            Integer seuil,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    /**
     * Taux d'occupation des produits sur une période
     *
     * Retourne le pourcentage d'utilisation de chaque produit
     * Utilisé pour statistiques et optimisation
     *
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Map avec idProduit -> Map(nomProduit, tauxOccupation)
     */
    List<Map<String, Object>> getTauxOccupationSurPeriode(LocalDate dateDebut, LocalDate dateFin);

    // ============================================
    // GESTION DU STOCK (PRODUITS EN_QUANTITE)
    // ============================================

    /**
     * Ajouter du stock à un produit EN_QUANTITE
     */
    ProduitResponseDto ajouterStock(Long id, Integer quantite, String motif, String username);

    /**
     * Retirer du stock d'un produit EN_QUANTITE
     */
    ProduitResponseDto retirerStock(Long id, Integer quantite, String motif, String username);

    /**
     * Ajuster le stock d'un produit EN_QUANTITE
     */
    ProduitResponseDto ajusterStock(Long id, Integer nouvelleQuantite, String motif, String username);

    /**
     * Vérifier si le stock est critique
     */
    Boolean verifierStockCritique(Long id);

    // ============================================
    // STATISTIQUES ET RAPPORTS
    // ============================================

    /**
     * Obtenir les statistiques d'un produit
     */
    StockStatistiquesDto getStatistiquesProduit(Long id);

    /**
     * Obtenir les produits les plus loués
     */
    List<ProduitResponseDto> getProduitsLesPlusLoues();

    /**
     * Obtenir les produits les mieux notés
     */
    List<ProduitResponseDto> getProduitsMieuxNotes(Double minNote);

    // ============================================
    // HISTORIQUE DES MOUVEMENTS
    // ============================================

    /**
     * Obtenir l'historique complet des mouvements d'un produit
     */
    List<MouvementStockResponseDto> getHistoriqueMouvements(Long idProduit);

    /**
     * Filtrer les mouvements par type
     */
    List<MouvementStockResponseDto> getMouvementsByType(TypeMouvement type);

    /**
     * Filtrer les mouvements par utilisateur
     */
    List<MouvementStockResponseDto> getMouvementsByUser(String username);

    /**
     * Filtrer les mouvements par période
     */
    List<MouvementStockResponseDto> getMouvementsByPeriode(Date dateDebut, Date dateFin);

    /**
     * Obtenir les mouvements d'un produit sur une période
     */
    List<MouvementStockResponseDto> getMouvementsProduitParPeriode(
            Long idProduit,
            Date dateDebut,
            Date dateFin
    );
}