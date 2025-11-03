package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.enums.Categorie;
import tn.weeding.agenceevenementielle.entities.Produit;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des produits
 *
 */
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // ============================================
    // RECHERCHES DE BASE
    // ============================================

    // Recherche par code produit
    Optional<Produit> findByCodeProduit(String codeProduit);

    // Vérifier si un produit existe par son code
    boolean existsByCodeProduit(String codeProduit);

    // Recherche par catégorie
    List<Produit> findByCategorieProduit(Categorie categorie);

    // Recherche par type de produit
    List<Produit> findByTypeProduit(TypeProduit typeProduit);

    // Produits nécessitant une maintenance
    List<Produit> findByMaintenanceRequise(Boolean maintenanceRequise);

    // Recherche par nom (recherche partielle, insensible à la casse)
    @Query("SELECT p FROM Produit p WHERE LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Produit> searchByNom(@Param("nom") String nom);

    // ============================================
    // DISPONIBILITÉ SANS PÉRIODE (Stock global)
    // ============================================

    /**
     * Produits disponibles en stock (quantité > 0)
     * NE PREND PAS EN COMPTE LES PÉRIODES
     * Utilisé uniquement pour vue administrative du stock global
     */
    @Query("SELECT p FROM Produit p WHERE p.quantiteDisponible > 0")
    List<Produit> findProduitsDisponibles();

    /**
     * Produits en rupture de stock
     */
    @Query("SELECT p FROM Produit p WHERE p.quantiteDisponible = 0")
    List<Produit> findProduitsEnRupture();

    /**
     * Produits avec alerte stock critique (avec seuil paramétrable)
     */
    @Query("SELECT p FROM Produit p WHERE p.quantiteDisponible <= :seuil AND p.quantiteDisponible > 0")
    List<Produit> findProduitsStockCritique(@Param("seuil") Integer seuil);

    // ============================================
    //  DISPONIBILITÉ AVEC PÉRIODE (PRODUITS DE QUANTITÉ)
    // ============================================

    /**
     * Calculer la quantité réellement disponible sur une période
     *
     * Pour produits de type QUANTITE uniquement
     * Calcule : quantiteDisponible - SUM(quantités réservées pendant la période)
     *
     * @param idProduit ID du produit
     * @param dateDebut Date de début de la période souhaitée
     * @param dateFin Date de fin de la période souhaitée
     * @return Quantité réellement disponible sur cette période
     */
    @Query("SELECT (p.quantiteDisponible - COALESCE(SUM(lr.quantite), 0)) " +
            "FROM Produit p " +
            "LEFT JOIN p.ligneReservationProduit lr " +
            "WHERE p.idProduit = :idProduit " +
            "AND p.typeProduit = tn.weeding.agenceevenementielle.entities.enums.TypeProduit.EN_QUANTITE " +
            "AND (lr IS NULL OR " +
            "     (lr.dateDebut <= :dateFin " +              // Chevauchement si début ligne < fin demandée
            "      AND lr.dateFin >= :dateDebut " +          // ET fin ligne > début demandé
            "      AND lr.reservation.statutReservation IN " +
            "         (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "          tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME))) " +
            "GROUP BY p.idProduit, p.quantiteDisponible")
    Integer calculerQuantiteDisponibleSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Vérifier si une quantité est disponible sur une période
     *
     * Vérifie que : quantiteDisponible - quantitésRéservées >= quantiteDemandée
     *
     * @param idProduit ID du produit
     * @param quantiteDemandee Quantité souhaitée
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return true si la quantité est disponible, false sinon
     */
    @Query("SELECT CASE WHEN (p.quantiteDisponible - COALESCE(SUM(lr.quantite), 0)) >= :quantiteDemandee " +
            "THEN true ELSE false END " +
            "FROM Produit p " +
            "LEFT JOIN p.ligneReservationProduit lr " +
            "WHERE p.idProduit = :idProduit " +
            "AND p.typeProduit = tn.weeding.agenceevenementielle.entities.enums.TypeProduit.EN_QUANTITE " +
            "AND (lr IS NULL OR " +
            "     (lr.dateDebut <= :dateFin " +
            "      AND lr.dateFin >= :dateDebut " +
            "      AND lr.reservation.statutReservation IN " +
            "         (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "          tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME))) " +
            "GROUP BY p.idProduit, p.quantiteDisponible")
    Boolean estDisponibleSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("quantiteDemandee") Integer quantiteDemandee,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Trouver tous les produits disponibles sur une période
     *
     * Retourne les produits de type QUANTITE qui ont au moins 1 unité disponible
     * pendant la période spécifiée
     *
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des produits disponibles
     */
    @Query("SELECT p FROM Produit p " +
            "WHERE p.typeProduit = tn.weeding.agenceevenementielle.entities.enums.TypeProduit.EN_QUANTITE " +
            "AND (p.quantiteDisponible - " +
            "     COALESCE((SELECT SUM(lr2.quantite) FROM LigneReservation lr2 " +
            "               WHERE lr2.produit.idProduit = p.idProduit " +
            "               AND lr2.dateDebut <= :dateFin " +
            "               AND lr2.dateFin >= :dateDebut " +
            "               AND lr2.reservation.statutReservation IN " +
            "                  (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "                   tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)), 0)) > 0")
    List<Produit> findProduitsDisponiblesSurPeriode(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Trouver les produits avec une quantité minimum disponible sur une période
     *
     * Utile pour filtrer les produits qui peuvent satisfaire une demande
     *
     * @param quantiteMin Quantité minimum requise
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des produits avec quantité suffisante
     */
    @Query("SELECT p FROM Produit p " +
            "WHERE p.typeProduit = tn.weeding.agenceevenementielle.entities.enums.TypeProduit.EN_QUANTITE " +
            "AND (p.quantiteDisponible - " +
            "     COALESCE((SELECT SUM(lr2.quantite) FROM LigneReservation lr2 " +
            "               WHERE lr2.produit.idProduit = p.idProduit " +
            "               AND lr2.dateDebut <= :dateFin " +
            "               AND lr2.dateFin >= :dateDebut " +
            "               AND lr2.reservation.statutReservation IN " +
            "                  (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "                   tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)), 0)) >= :quantiteMin")
    List<Produit> findProduitsAvecQuantiteMinSurPeriode(
            @Param("quantiteMin") Integer quantiteMin,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * ✅ NOUVELLE REQUÊTE : Produits en stock critique sur une période donnée
     *
     * Identifie les produits qui auront un stock faible pendant une période
     * Utile pour alertes et planification
     *
     * @param seuil Seuil critique (ex: 5)
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des produits en situation critique
     */
    @Query("SELECT p FROM Produit p " +
            "WHERE p.typeProduit = tn.weeding.agenceevenementielle.entities.enums.TypeProduit.EN_QUANTITE " +
            "AND (p.quantiteDisponible - " +
            "     COALESCE((SELECT SUM(lr2.quantite) FROM LigneReservation lr2 " +
            "               WHERE lr2.produit.idProduit = p.idProduit " +
            "               AND lr2.dateDebut <= :dateFin " +
            "               AND lr2.dateFin >= :dateDebut " +
            "               AND lr2.reservation.statutReservation IN " +
            "                  (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "                   tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)), 0)) " +
            "BETWEEN 1 AND :seuil")
    List<Produit> findProduitsStockCritiqueSurPeriode(
            @Param("seuil") Integer seuil,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    // ============================================
    // RECHERCHE MULTICRITÈRES
    // ============================================

    /**
     * Recherche multicritères (version de base, sans période)
     */
    @Query("SELECT p FROM Produit p WHERE " +
            "(:categorie IS NULL OR p.categorieProduit = :categorie) AND " +
            "(:typeProduit IS NULL OR p.typeProduit = :typeProduit) AND " +
            "(:minPrix IS NULL OR p.prixUnitaire >= :minPrix) AND " +
            "(:maxPrix IS NULL OR p.prixUnitaire <= :maxPrix) AND " +
            "(:disponible IS NULL OR " +
            " (:disponible = true AND p.quantiteDisponible > 0) OR " +
            " (:disponible = false AND p.quantiteDisponible = 0))")
    List<Produit> searchProduits(
            @Param("categorie") Categorie categorie,
            @Param("typeProduit") TypeProduit typeProduit,
            @Param("minPrix") Double minPrix,
            @Param("maxPrix") Double maxPrix,
            @Param("disponible") Boolean disponible
    );

    /**
     *  Recherche multicritères avec période
     *
     * Permet de filtrer les produits disponibles selon plusieurs critères
     * ET une période de disponibilité
     */
    @Query("SELECT DISTINCT p FROM Produit p WHERE " +
            "(:categorie IS NULL OR p.categorieProduit = :categorie) AND " +
            "(:typeProduit IS NULL OR p.typeProduit = :typeProduit) AND " +
            "(:minPrix IS NULL OR p.prixUnitaire >= :minPrix) AND " +
            "(:maxPrix IS NULL OR p.prixUnitaire <= :maxPrix) AND " +
            "(:dateDebut IS NULL OR :dateFin IS NULL OR " +
            " (p.quantiteDisponible - " +
            "  COALESCE((SELECT SUM(lr2.quantite) FROM LigneReservation lr2 " +
            "            WHERE lr2.produit.idProduit = p.idProduit " +
            "            AND lr2.dateDebut <= :dateFin " +
            "            AND lr2.dateFin >= :dateDebut " +
            "            AND lr2.reservation.statutReservation IN " +
            "               (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "                tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)), 0)) > 0)")
    List<Produit> searchProduitsAvecPeriode(
            @Param("categorie") Categorie categorie,
            @Param("typeProduit") TypeProduit typeProduit,
            @Param("minPrix") Double minPrix,
            @Param("maxPrix") Double maxPrix,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    // ============================================
    // STATISTIQUES ET ANALYTIQUES
    // ============================================

    /**
     * Obtenir les produits les plus loués
     */
    @Query("SELECT p FROM Produit p " +
            "LEFT JOIN p.ligneReservationProduit lr " +
            "GROUP BY p " +
            "ORDER BY COUNT(lr) DESC")
    List<Produit> findProduitsLesPlusLoues();

    /**
     * Obtenir les produits les mieux notés
     */
    @Query("SELECT p FROM Produit p " +
            "LEFT JOIN p.avisProduit a " +
            "GROUP BY p " +
            "HAVING AVG(a.note) >= :minNote " +
            "ORDER BY AVG(a.note) DESC")
    List<Produit> findProduitsMieuxNotes(@Param("minNote") Double minNote);

    /**
     *  Obtenir les produits avec leur taux d'occupation sur une période
     *
     * Calcule le pourcentage d'utilisation de chaque produit
     * Utile pour optimisation et décisions d'achat
     */
    @Query("SELECT p.idProduit, p.nomProduit, " +
            "CASE WHEN p.quantiteDisponible > 0 " +
            "THEN (COALESCE(SUM(lr.quantite), 0) * 100.0 / p.quantiteDisponible) " +
            "ELSE 0 END as tauxOccupation " +
            "FROM Produit p " +
            "LEFT JOIN p.ligneReservationProduit lr " +
            "WHERE p.typeProduit = tn.weeding.agenceevenementielle.entities.enums.TypeProduit.EN_QUANTITE " +
            "AND (lr IS NULL OR " +
            "     (lr.dateDebut <= :dateFin " +
            "      AND lr.dateFin >= :dateDebut " +
            "      AND lr.reservation.statutReservation IN " +
            "         (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "          tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME))) " +
            "GROUP BY p.idProduit, p.nomProduit, p.quantiteDisponible " +
            "ORDER BY CASE WHEN p.quantiteDisponible > 0 " +
            "          THEN (COALESCE(SUM(lr.quantite), 0) * 100.0 / p.quantiteDisponible) " +
            "          ELSE 0 END DESC")
    List<Object[]> findTauxOccupationProduitsParPeriode(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}