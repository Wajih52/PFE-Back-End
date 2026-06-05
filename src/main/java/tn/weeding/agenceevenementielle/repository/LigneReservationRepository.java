package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.LigneReservation;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Repository pour la gestion des lignes de réservation
 *
 */
@Repository
public interface LigneReservationRepository extends JpaRepository<LigneReservation, Long>
{

    // ============ RECHERCHES DE BASE ============

    /**
     * Trouver toutes les lignes d'une réservation
     */
    List<LigneReservation> findByReservation_IdReservation(Long idReservation);

    /**
     * Trouver toutes les lignes pour un produit donné
     */
    List<LigneReservation> findByProduit_IdProduit(Long idProduit);



    // ============ VÉRIFICATION DE DISPONIBILITÉ ============

    /**
     * Calculer la quantité totale réservée pour un produit sur une période
     * (pour produits en quantité: chaises, assiettes, etc.)
     */
    @Query("SELECT COALESCE(SUM(lr.quantite), 0) FROM LigneReservation lr " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "AND ((lr.dateDebut <= :dateFin AND lr.dateFin >= :dateDebut))")
    Integer calculateQuantiteReserveeSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Compter le nombre d'instances réservées pour un produit avec référence sur une période
     * (pour produits avec référence : projecteurs, caméras, etc.)
     */
    @Query("SELECT COUNT(DISTINCT ip.idInstance) FROM LigneReservation lr " +
            "JOIN lr.instancesReservees ip " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.reservation.statutReservation IN (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME," +
            "tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE) " +
            "AND ((lr.dateDebut <= :dateFin AND lr.dateFin >= :dateDebut))")
    Long countInstancesReserveesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    // ============ GESTION DES LIVRAISONS ============

    /**
     * Trouver les lignes de réservation d'une livraison
     */
    List<LigneReservation> findByLivraison_IdLivraison(Long idLivraison);

    /**
     * Trouver les lignes de réservation par liste d'IDs de livraison
     */
    List<LigneReservation> findByLivraison_IdLivraisonIn(List<Long> idsLivraisons);


    /**
     * Trouver les lignes par statut de livraison
     */
    List<LigneReservation> findByStatutLivraisonLigne(StatutLivraison statut);


    /**
     * Trouver les retours en retard (date de fin passée mais pas encore retournés)
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.dateFin < CURRENT_DATE " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "AND lr.statutLivraisonLigne NOT IN (tn.weeding.agenceevenementielle.entities.enums.StatutLivraison.RETOUR," +
            " tn.weeding.agenceevenementielle.entities.enums.StatutLivraison.RETOUR_PARTIEL) " +
            "ORDER BY lr.dateFin ASC")
    List<LigneReservation> findRetoursEnRetard();

    // ============ INSTANCES DE PRODUITS ============


    /**
     * Vérifier si une instance est dans des réservations actives
     * (utilise l'ID de l'instance)
     */
    @Query("SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END " +
            "FROM LigneReservation lr " +
            "JOIN lr.instancesReservees ip " +
            "WHERE ip.idInstance = :idInstance " +
            "AND lr.dateFin >= :dateActuelle " +
            "AND lr.reservation.statutReservation IN " +
            "   (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)")
    boolean existsActiveReservationForInstance(
            @Param("idInstance") Long idInstance,
            @Param("dateActuelle") LocalDate dateActuelle
    );

    /**
     * Vérifier si un produit est dans des réservations actives
     * (Pour vérifier avant suppression)
     */
    @Query("SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END " +
            "FROM LigneReservation lr " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.dateFin >= :dateActuelle " +
            "AND lr.reservation.statutReservation IN " +
            "   (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)")
    boolean existsActiveReservationForProduit(
            @Param("idProduit") Long idProduit,
            @Param("dateActuelle") LocalDate dateActuelle
    );


    // ============================================
    // VÉRIFICATIONS
    // ============================================

    /**
     * Trouver la quantité réservée d'un produit sur une période
     * EN EXCLUANT une réservation spécifique (pour permettre la modification)
     *
     * @param idProduit ID du produit
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @param reservationExclue ID de la réservation à exclure du calcul
     * @return Quantité totale réservée
     */
    @Query("SELECT COALESCE(SUM(lr.quantite), 0) " +
            "FROM LigneReservation lr " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.reservation.idReservation != :reservationExclue " +
            "AND lr.reservation.statutReservation IN (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME" +
            ", tn.weeding.agenceevenementielle.entities.enums.StatutReservation.EN_ATTENTE) " +
            "AND ((lr.dateDebut <= :dateFin) AND (lr.dateFin >= :dateDebut))")
    int findQuantiteReserveeForProduitInPeriodExcludingReservation(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("reservationExclue") Long reservationExclue
    );

    /**
     * Compter le nombre de réservations d'une instance sur une période
     * EN EXCLUANT une réservation spécifique
     *
     * @param idInstance ID de l'instance
     * @param dateDebut Date de début de la période
     * @param dateFin Date de fin de la période
     * @param reservationExclue ID de la réservation à exclure
     * @return Nombre de réservations actives pour cette instance
     */
    @Query("SELECT COUNT(lr) " +
            "FROM LigneReservation lr " +
            "JOIN lr.instancesReservees inst " +
            "WHERE inst.idInstance = :idInstance " +
            "AND lr.reservation.idReservation != :reservationExclue " +
            "AND lr.reservation.statutReservation IN (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME) " +
            "AND ((lr.dateDebut <= :dateFin) AND (lr.dateFin >= :dateDebut))")
    long countReservationsForInstanceInPeriodExcludingReservation(
            @Param("idInstance") Long idInstance,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("reservationExclue") Long reservationExclue
    );



    /**
     * Trouve la quantité maximale réservée pour un produit à une date donnée
     * @param idProduit ID du produit
     * @param dateReference Date de référence (généralement aujourd'hui)
     * @return La quantité maximale réservée
     */
    @Query("""
    SELECT MAX(lr.quantite)
    FROM LigneReservation lr
    WHERE lr.produit.idProduit = :idProduit
    AND lr.reservation.statutReservation IN (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)
    AND lr.dateDebut <= :dateReference
    AND lr.dateFin >= :dateReference
    """)
    Integer findMaxQuantiteReserveeForProduit(
            @Param("idProduit") Long idProduit,
            @Param("dateReference") LocalDate dateReference
    );

    /**
     * Calculer la quantité réservée pour un produit sur une période donnée
     */
    @Query("""
    SELECT COALESCE(MAX(lr.quantite), 0)
    FROM LigneReservation lr
    WHERE lr.produit.idProduit = :idProduit
    AND lr.reservation.statutReservation IN (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)
    AND (
        (lr.dateDebut <= :dateFin AND lr.dateFin >= :dateDebut)
    )
    """)
    Integer calculerQuantiteReserveePourPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Compter les instances réservées pour un produit sur une période
     */
    @Query("""
    SELECT COUNT(DISTINCT i.idInstance)
    FROM LigneReservation lr
    JOIN lr.instancesReservees i
    WHERE lr.produit.idProduit = :idProduit
    AND lr.reservation.statutReservation IN (tn.weeding.agenceevenementielle.entities.enums.StatutReservation.CONFIRME)
    AND (
        (lr.dateDebut <= :dateFin AND lr.dateFin >= :dateDebut)
    )
    """)
    Long countInstancesReserveesPourPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    //=================================================
    // Statistiques (Pour dashboard)
    //=================================================

    /**
     * Top produits les plus loués (par nombre de locations)
     * Retourne: [idProduit, nomProduit, nombreLocations, chiffreAffairesGenere]
     */
    @Query(value = "SELECT p.idProduit, p.nomProduit, COUNT(lr.idLigneReservation) as nb_locations, " +
            "COALESCE(SUM(lr.quantite * (DATEDIFF(lr.dateFin, lr.dateDebut) + 1) * lr.prixUnitaire), 0) as ca_genere " +
            "FROM LigneReservation lr " +
            "JOIN Produit p ON lr.produit_idProduit = p.idProduit " +
            "JOIN Reservation r ON lr.reservation_idReservation = r.idReservation " +
            "WHERE r.statutReservation IN ('CONFIRME', 'TERMINE') " +
            "GROUP BY p.idProduit, p.nomProduit " +
            "ORDER BY nb_locations DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopProduitsLoues(@Param("limit") int limit);
    /**
     * Top produits par CA généré
     * Retourne: [idProduit, nomProduit, chiffreAffairesGenere]
     */
    @Query(value = "SELECT p.idProduit, p.nomProduit, " +
            "SUM(lr.quantite * (DATEDIFF(lr.dateFin, lr.dateDebut) + 1) * lr.prixUnitaire) as ca_genere " +
            "FROM LigneReservation lr " +
            "JOIN Produit p ON lr.produit_idProduit = p.idProduit " +
            "JOIN Reservation r ON lr.reservation_idReservation = r.idReservation " +
            "WHERE r.statutReservation IN ('CONFIRME', 'TERMINE') " +
            "GROUP BY p.idProduit, p.nomProduit " +
            "ORDER BY ca_genere DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopProduitsParCA(@Param("limit") int limit);

    /**
     * CA par catégorie de produits
     * Retourne: [categorie, chiffreAffaires]
     */
    @Query(value = "SELECT p.categorieProduit, " +
            "SUM(lr.quantite * (DATEDIFF(lr.dateFin, lr.dateDebut) + 1) * lr.prixUnitaire) as ca_total " +
            "FROM LigneReservation lr " +
            "JOIN Produit p ON lr.produit_idProduit = p.idProduit " +
            "JOIN Reservation r ON lr.reservation_idReservation = r.idReservation " +
            "WHERE r.statutReservation IN ('CONFIRME', 'TERMINE') " +
            "GROUP BY p.categorieProduit " +
            "ORDER BY ca_total DESC",
            nativeQuery = true)
    List<Object[]> findCAParCategorie();

    /**
     * Top produits loués sur une période spécifique
     * Retourne: [idProduit, nomProduit, nombreLocations, chiffreAffairesGenere]
     */
    @Query(value = "SELECT p.idProduit, p.nomProduit, COUNT(lr.idLigneReservation) as nb_locations, " +
            "COALESCE(SUM(lr.quantite * (DATEDIFF(lr.dateFin, lr.dateDebut) + 1) * lr.prixUnitaire), 0) as ca_genere " +
            "FROM LigneReservation lr " +
            "JOIN Produit p ON lr.produit_idProduit = p.idProduit " +
            "JOIN Reservation r ON lr.reservation_idReservation = r.idReservation " +
            "WHERE r.statutReservation IN ('CONFIRME', 'TERMINE') " +
            "AND lr.dateDebut >= :debut " +
            "AND lr.dateFin <= :fin " +
            "GROUP BY p.idProduit, p.nomProduit " +
            "ORDER BY nb_locations DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findTopProduitsLouesPeriode(@Param("debut") LocalDate debut,
                                               @Param("fin") LocalDate fin,
                                               @Param("limit") int limit);

    /**
     * CA par catégorie sur une période
     * Retourne: [categorie, chiffreAffaires]
     */
    @Query(value = "SELECT p.categorieProduit, " +
            "SUM(lr.quantite * (DATEDIFF(lr.dateFin, lr.dateDebut) + 1) * lr.prixUnitaire) as ca_total " +
            "FROM LigneReservation lr " +
            "JOIN Produit p ON lr.produit_idProduit = p.idProduit " +
            "JOIN Reservation r ON lr.reservation_idReservation = r.idReservation " +
            "WHERE r.statutReservation IN ('CONFIRME', 'TERMINE') " +
            "AND lr.dateDebut >= :debut " +
            "AND lr.dateFin <= :fin " +
            "GROUP BY p.categorieProduit " +
            "ORDER BY ca_total DESC",
            nativeQuery = true)
    List<Object[]> findCAParCategoriePeriode(@Param("debut") LocalDate debut,
                                             @Param("fin") LocalDate fin);
    /**
     * Compter le nombre de locations d'un produit
     */
    Long countByProduit_IdProduitAndReservation_StatutReservation(
            Long idProduit,
            StatutReservation statut
    );

    /**
     * Compter les retours attendus pour une date donnée
     */
    Long countByDateFinAndStatutLivraisonLigneIn(
            LocalDate dateFin,
            List<StatutLivraison> statuts
    );

}