package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.MouvementStock;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;

import java.util.Date;
import java.util.List;

/**
 * ✅ VERSION CORRIGÉE : Repository pour la gestion des mouvements de stock
 *
 * Contient toutes les méthodes nécessaires pour le suivi de l'historique
 * Sprint 3 : Gestion des produits et du stock
 */
@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    // ============================================
    // RECHERCHE PAR PRODUIT
    // ============================================

    /**
     * Récupérer tous les mouvements d'un produit, triés par date décroissante
     * @param idProduit ID du produit
     * @return Liste des mouvements, du plus récent au plus ancien
     */
    @Query("SELECT m FROM MouvementStock m WHERE m.produit.idProduit = :idProduit ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByProduit_IdProduitOrderByDateMouvementDesc(@Param("idProduit") Long idProduit);

    /**
     * Alias pour compatibilité avec l'ancien code
     */
    @Query("SELECT m FROM MouvementStock m WHERE m.produit.idProduit = :idProduit ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(@Param("idProduit") Long idProduit);

    // ============================================
    // RECHERCHE PAR TYPE DE MOUVEMENT
    // ============================================

    /**
     * Récupérer tous les mouvements d'un type donné
     * @param typeMouvement Type de mouvement
     * @return Liste des mouvements de ce type
     */
    List<MouvementStock> findByTypeMouvement(TypeMouvement typeMouvement);

    /**
     * Récupérer tous les mouvements d'un type, triés par date décroissante
     * @param typeMouvement Type de mouvement
     * @return Liste des mouvements, du plus récent au plus ancien
     */
    List<MouvementStock> findByTypeMouvementOrderByDateMouvementDesc(TypeMouvement typeMouvement);

    // ============================================
    // RECHERCHE PAR UTILISATEUR
    // ============================================

    /**
     * Récupérer tous les mouvements effectués par un utilisateur
     * @param username Nom d'utilisateur
     * @return Liste des mouvements de cet utilisateur
     */
    List<MouvementStock> findByEffectuePar(String username);

    /**
     * Récupérer tous les mouvements d'un utilisateur, triés par date décroissante
     * @param username Nom d'utilisateur
     * @return Liste des mouvements, du plus récent au plus ancien
     */
    List<MouvementStock> findByEffectueParOrderByDateMouvementDesc(String username);

    // ============================================
    // RECHERCHE PAR PÉRIODE
    // ============================================

    /**
     * Récupérer tous les mouvements entre deux dates
     * @param dateDebut Date de début (incluse)
     * @param dateFin Date de fin (incluse)
     * @return Liste des mouvements dans cette période
     */
    List<MouvementStock> findByDateMouvementBetween(Date dateDebut, Date dateFin);

    /**
     * Alias avec requête JPQL explicite
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des mouvements dans la période
     */
    @Query("SELECT m FROM MouvementStock m WHERE m.dateMouvement >= :dateDebut AND m.dateMouvement <= :dateFin ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByPeriode(@Param("dateDebut") Date dateDebut, @Param("dateFin") Date dateFin);

    /**
     * Récupérer les mouvements d'un produit sur une période
     * @param idProduit ID du produit
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Liste des mouvements
     */
    @Query("SELECT m FROM MouvementStock m WHERE m.produit.idProduit = :idProduit " +
            "AND m.dateMouvement >= :dateDebut AND m.dateMouvement <= :dateFin " +
            "ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByProduitAndPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin);

    // ============================================
    // RECHERCHE PAR RÉSERVATION
    // ============================================

    /**
     * Récupérer tous les mouvements liés à une réservation
     * @param idReservation ID de la réservation
     * @return Liste des mouvements de cette réservation
     */
    List<MouvementStock> findByIdReservation(Long idReservation);

    /**
     * Récupérer les mouvements d'une réservation, triés par date
     * @param idReservation ID de la réservation
     * @return Liste des mouvements, du plus récent au plus ancien
     */
    List<MouvementStock> findByIdReservationOrderByDateMouvementDesc(Long idReservation);

    // ============================================
    // RECHERCHE PAR INSTANCE
    // ============================================

    /**
     * Récupérer les mouvements liés à une instance spécifique
     * @param codeInstance Code de l'instance
     * @return Liste des mouvements de cette instance
     */
    List<MouvementStock> findByCodeInstance(String codeInstance);

    /**
     * Récupérer les mouvements d'une instance, triés par date
     * @param codeInstance Code de l'instance
     * @return Liste des mouvements, du plus récent au plus ancien
     */
    List<MouvementStock> findByCodeInstanceOrderByDateMouvementDesc(String codeInstance);

    // ============================================
    // RÉCUPÉRATION DES DERNIERS MOUVEMENTS
    // ============================================

    /**
     * Récupérer les N derniers mouvements (tous produits confondus)
     * @return Liste des derniers mouvements, triés par date décroissante
     */
    @Query("SELECT m FROM MouvementStock m ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findRecentMouvements();

    /**
     * Récupérer les N derniers mouvements avec limite
     * @param limit Nombre maximum de résultats
     * @return Liste des derniers mouvements
     */
    @Query(value = "SELECT * FROM mouvement_stock ORDER BY date_mouvement DESC LIMIT :limit", nativeQuery = true)
    List<MouvementStock> findTopNRecentMouvements(@Param("limit") int limit);

    // ============================================
    // STATISTIQUES - CALCULS D'ENTRÉES/SORTIES
    // ============================================

    /**
     * Calculer le total des entrées de stock pour un produit
     * Compte les mouvements de type ENTREE
     *
     * @param idProduit ID du produit
     * @return Total des entrées (somme des quantités)
     */
    @Query("SELECT COALESCE(SUM(m.quantite), 0) FROM MouvementStock m " +
            "WHERE m.produit.idProduit = :idProduit " +
            "AND m.typeMouvement IN (" +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.CREATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.REACTIVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.AJOUT_STOCK, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.ENTREE_STOCK, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETOUR_RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETOUR_MAINTENANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.AJOUT_INSTANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.ANNULATION_RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETOUR" +
            ")")
    Integer getTotalEntrees(@Param("idProduit") Long idProduit);

    /**
     * Calculer le total des sorties de stock pour un produit
     * Compte les mouvements de type SORTIE
     *
     * @param idProduit ID du produit
     * @return Total des sorties (somme des quantités)
     */
    @Query("SELECT COALESCE(SUM(m.quantite), 0) FROM MouvementStock m " +
            "WHERE m.produit.idProduit = :idProduit " +
            "AND m.typeMouvement IN (" +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.DESACTIVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETRAIT_STOCK, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.SORTIE_RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.MAINTENANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.PRODUIT_ENDOMMAGE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.SUPPRESSION_INSTANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.LIVRAISON" +
            ")")
    Integer getTotalSorties(@Param("idProduit") Long idProduit);

    /**
     * Calculer le total des entrées sur une période
     * @param idProduit ID du produit
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Total des entrées sur la période
     */
    @Query("SELECT COALESCE(SUM(m.quantite), 0) FROM MouvementStock m " +
            "WHERE m.produit.idProduit = :idProduit " +
            "AND m.dateMouvement >= :dateDebut AND m.dateMouvement <= :dateFin " +
            "AND m.typeMouvement IN (" +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.CREATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.AJOUT_STOCK, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.ENTREE_STOCK, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETOUR_RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETOUR_MAINTENANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.AJOUT_INSTANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETOUR" +
            ")")
    Integer getTotalEntreesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin);

    /**
     * Calculer le total des sorties sur une période
     * @param idProduit ID du produit
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Total des sorties sur la période
     */
    @Query("SELECT COALESCE(SUM(m.quantite), 0) FROM MouvementStock m " +
            "WHERE m.produit.idProduit = :idProduit " +
            "AND m.dateMouvement >= :dateDebut AND m.dateMouvement <= :dateFin " +
            "AND m.typeMouvement IN (" +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RETRAIT_STOCK, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.SORTIE_RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.MAINTENANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.PRODUIT_ENDOMMAGE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.SUPPRESSION_INSTANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.LIVRAISON" +
            ")")
    Integer getTotalSortiesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin);

    // ============================================
    // STATISTIQUES - COMPTAGES
    // ============================================

    /**
     * Compter le nombre total de mouvements pour un produit
     * @param idProduit ID du produit
     * @return Nombre de mouvements
     */
    @Query("SELECT COUNT(m) FROM MouvementStock m WHERE m.produit.idProduit = :idProduit")
    Long countByProduit(@Param("idProduit") Long idProduit);

    /**
     * Compter les mouvements d'un type spécifique pour un produit
     * @param idProduit ID du produit
     * @param typeMouvement Type de mouvement
     * @return Nombre de mouvements de ce type
     */
    @Query("SELECT COUNT(m) FROM MouvementStock m WHERE m.produit.idProduit = :idProduit AND m.typeMouvement = :type")
    Long countByProduitAndType(@Param("idProduit") Long idProduit, @Param("type") TypeMouvement typeMouvement);

    /**
     * Compter les mouvements sur une période
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Nombre de mouvements
     */
    @Query("SELECT COUNT(m) FROM MouvementStock m WHERE m.dateMouvement >= :dateDebut AND m.dateMouvement <= :dateFin")
    Long countByPeriode(@Param("dateDebut") Date dateDebut, @Param("dateFin") Date dateFin);

    // ============================================
    // RECHERCHE AVANCÉE
    // ============================================

    /**
     * Récupérer les mouvements d'un produit et d'un type spécifique
     * @param idProduit ID du produit
     * @param typeMouvement Type de mouvement
     * @return Liste des mouvements
     */
    @Query("SELECT m FROM MouvementStock m WHERE m.produit.idProduit = :idProduit " +
            "AND m.typeMouvement = :type ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByProduitAndType(
            @Param("idProduit") Long idProduit,
            @Param("type") TypeMouvement typeMouvement);

    /**
     * Rechercher des mouvements par motif (recherche partielle)
     * @param motif Mot-clé à rechercher dans le motif
     * @return Liste des mouvements correspondants
     */
    @Query("SELECT m FROM MouvementStock m WHERE LOWER(m.motif) LIKE LOWER(CONCAT('%', :motif, '%')) " +
            "ORDER BY m.dateMouvement DESC")
    List<MouvementStock> searchByMotif(@Param("motif") String motif);

    // ============================================
    // VÉRIFICATIONS ET VALIDATIONS
    // ============================================

    /**
     * Vérifier si un produit a des mouvements
     * @param idProduit ID du produit
     * @return true si le produit a au moins un mouvement
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MouvementStock m " +
            "WHERE m.produit.idProduit = :idProduit")
    Boolean existsByProduit(@Param("idProduit") Long idProduit);

    /**
     * Obtenir le dernier mouvement d'un produit
     * @param idProduit ID du produit
     * @return Le mouvement le plus récent (ou null si aucun)
     */
    @Query("SELECT m FROM MouvementStock m WHERE m.produit.idProduit = :idProduit " +
            "ORDER BY m.dateMouvement DESC LIMIT 1")
    MouvementStock findLatestByProduit(@Param("idProduit") Long idProduit);
}