package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.LigneReservation;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;

import java.util.Date;
import java.util.List;

/**
 * Repository pour la gestion des lignes de réservation
 * Sprint 4 - Gestion des réservations (incluant devis)
 */
@Repository
public interface LigneReservationRepository extends JpaRepository<LigneReservation, Long> {

    // ============ RECHERCHES DE BASE ============

    /**
     * Trouver toutes les lignes d'une réservation
     */
    List<LigneReservation> findByReservation_IdReservation(Long idReservation);

    /**
     * Trouver toutes les lignes pour un produit donné
     */
    List<LigneReservation> findByProduit_IdProduit(Long idProduit);

    /**
     * Compter le nombre de lignes dans une réservation
     */
    long countByReservation_IdReservation(Long idReservation);

    // ============ VÉRIFICATION DE DISPONIBILITÉ ============

    /**
     * CRITIQUE: Trouver les réservations confirmées d'un produit qui se chevauchent avec une période
     * Utilisé pour vérifier la disponibilité avant de créer une réservation
     *
     * Logique: Une réservation se chevauche si:
     * - La réservation est confirmée (pas annulée)
     * - dateDebut <= dateFin_demandée ET dateFin >= dateDebut_demandée
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "AND ((lr.dateDebut <= :dateFin AND lr.dateFin >= :dateDebut))")
    List<LigneReservation> findReservationsConfirmeesChevauchantes(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

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
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    /**
     * Compter le nombre d'instances réservées pour un produit avec référence sur une période
     * (pour produits avec référence: projecteurs, caméras, etc.)
     */
    @Query("SELECT COUNT(DISTINCT ip.idInstance) FROM LigneReservation lr " +
            "JOIN lr.instancesReservees ip " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "AND ((lr.dateDebut <= :dateFin AND lr.dateFin >= :dateDebut))")
    Long countInstancesReserveesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    // ============ GESTION DES LIVRAISONS ============

    /**
     * Trouver les lignes de réservation d'une livraison
     */
    List<LigneReservation> findByLivraison_IdLivraison(Long idLivraison);

    /**
     * Trouver les lignes sans livraison assignée (réservations confirmées)
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.reservation.statutReservation = 'CONFIRME' " +
            "AND lr.livraison IS NULL " +
            "ORDER BY lr.dateDebut ASC")
    List<LigneReservation> findLignesSansLivraison();

    /**
     * Trouver les lignes par statut de livraison
     */
    List<LigneReservation> findByStatutLivraisonLigne(StatutLivraison statut);

    /**
     * Trouver les lignes dont la livraison est en attente
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.reservation.statutReservation = 'confirme' " +
            "AND lr.statutLivraisonLigne = 'EN_ATTENTE' " +
            "AND lr.dateDebut <= :dateLimit " +
            "ORDER BY lr.dateDebut ASC")
    List<LigneReservation> findLivraisonsEnAttente(@Param("dateLimit") Date dateLimit);

    // ============ STATISTIQUES PAR PRODUIT ============

    /**
     * Compter le nombre total de réservations d'un produit
     */
    @Query("SELECT COUNT(lr) FROM LigneReservation lr " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.reservation.statutReservation = 'CONFIRME'")
    Long countReservationsByProduit(@Param("idProduit") Long idProduit);

    /**
     * Calculer le chiffre d'affaires généré par un produit
     */
    @Query("SELECT SUM(lr.quantite * lr.prixUnitaire) FROM LigneReservation lr " +
            "WHERE lr.produit.idProduit = :idProduit " +
            "AND lr.reservation.statutReservation = 'CONFIRME'")
    Double calculateChiffreAffairesByProduit(@Param("idProduit") Long idProduit);

    /**
     * Trouver les produits les plus réservés (top N)
     */
    @Query("SELECT lr.produit.idProduit, lr.produit.nomProduit, COUNT(lr) as nbReservations " +
            "FROM LigneReservation lr " +
            "WHERE lr.reservation.statutReservation = 'CONFIRME' " +
            "GROUP BY lr.produit.idProduit, lr.produit.nomProduit " +
            "ORDER BY nbReservations DESC")
    List<Object[]> findProduitsLesPlusReserves();

    // ============ FILTRAGE PAR DATE ============

    /**
     * Trouver les lignes de réservation dans une période donnée
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.dateDebut >= :dateDebut AND lr.dateFin <= :dateFin " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "ORDER BY lr.dateDebut ASC")
    List<LigneReservation> findLignesInPeriode(
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    /**
     * Trouver les lignes dont la date de début est aujourd'hui
     * (livraisons à effectuer aujourd'hui)
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.dateDebut = CURRENT_DATE " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "AND lr.statutLivraisonLigne = 'EN_ATTENTE'")
    List<LigneReservation> findLivraisonsAujourdhui();

    /**
     * Trouver les lignes dont la date de fin est aujourd'hui
     * (retours prévus aujourd'hui)
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.dateFin = CURRENT_DATE " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "AND lr.statutLivraisonLigne IN (tn.weeding.agenceevenementielle.entities.enums.StatutLivraison.LIVREE, " +
            "tn.weeding.agenceevenementielle.entities.enums.StatutLivraison.EN_COURS)")
    List<LigneReservation> findRetoursAujourdhui();

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
     * Trouver les instances réservées pour une ligne de réservation
     * (pour produits avec référence uniquement)
     */
    @Query("SELECT ip.numeroSerie FROM LigneReservation lr " +
            "JOIN lr.instancesReservees ip " +
            "WHERE lr.idLigneReservation = :idLigneReservation")
    List<String> findInstancesReservees(@Param("idLigneReservation") Long idLigneReservation);

    /**
     * Vérifier si une instance est déjà réservée sur une période
     */
    @Query("SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END " +
            "FROM LigneReservation lr " +
            "JOIN lr.instancesReservees ip " +
            "WHERE ip.idInstance = :idInstance " +
            "AND lr.reservation.statutReservation = 'CONFIRME' " +
            "AND ((lr.dateDebut <= :dateFin AND lr.dateFin >= :dateDebut))")
    boolean isInstanceReserveeSurPeriode(
            @Param("idInstance") Long idInstance,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    // ============ RECHERCHE AVANCÉE ============

    /**
     * Rechercher les lignes de réservation par client
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.reservation.utilisateur.idUtilisateur = :idUtilisateur " +
            "ORDER BY lr.dateDebut DESC")
    List<LigneReservation> findByClient(@Param("idUtilisateur") Long idUtilisateur);

    /**
     * Rechercher les lignes de réservation d'un client pour un produit spécifique
     */
    @Query("SELECT lr FROM LigneReservation lr " +
            "WHERE lr.reservation.utilisateur.idUtilisateur = :idUtilisateur " +
            "AND lr.produit.idProduit = :idProduit " +
            "ORDER BY lr.dateDebut DESC")
    List<LigneReservation> findByClientAndProduit(
            @Param("idUtilisateur") Long idUtilisateur,
            @Param("idProduit") Long idProduit
    );


    // ============================================
    // RECHERCHES AVANCÉES
    // ============================================

    /**
     * Récupérer les lignes d'une réservation triées par produit
     */
    @Query("SELECT l FROM LigneReservation l " +
            "WHERE l.reservation.idReservation = :idReservation " +
            "ORDER BY l.produit.nomProduit")
    List<LigneReservation> findByReservationOrderByProduit(@Param("idReservation") Long idReservation);

    /**
     * Récupérer les lignes dont la date de début est dans une période
     * Utile pour le planning
     */
    @Query("SELECT l FROM LigneReservation l " +
            "WHERE l.dateDebut BETWEEN :dateDebut AND :dateFin")
    List<LigneReservation> findByDateDebutBetween(
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    /**
     * Récupérer les lignes avec un produit spécifique et un statut
     */
    @Query("SELECT l FROM LigneReservation l " +
            "WHERE l.produit.idProduit = :idProduit " +
            "AND l.statutLivraisonLigne = :statut")
    List<LigneReservation> findByProduitAndStatut(
            @Param("idProduit") Long idProduit,
            @Param("statut") StatutLivraison statut
    );

    /**
     * Récupérer les lignes en attente de livraison pour une période
     * Utile pour planifier les livraisons
     */
    @Query("SELECT l FROM LigneReservation l " +
            "WHERE l.statutLivraisonLigne = 'EN_ATTENTE' " +
            "AND l.dateDebut BETWEEN :dateDebut AND :dateFin")
    List<LigneReservation> findLignesEnAttenteParPeriode(
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    // ============================================
    // STATISTIQUES ET COMPTAGES
    // ============================================

    /**
     * Compter le nombre de lignes d'une réservation
     */
    @Query("SELECT COUNT(l) FROM LigneReservation l " +
            "WHERE l.reservation.idReservation = :idReservation")
    Long countByReservation(@Param("idReservation") Long idReservation);

    /**
     * Calculer la quantité totale de produits réservés pour un produit
     * Utile pour suivre la popularité d'un produit
     */
    @Query("SELECT COALESCE(SUM(l.quantite), 0) FROM LigneReservation l " +
            "WHERE l.produit.idProduit = :idProduit")
    Integer getTotalQuantiteReserveePourProduit(@Param("idProduit") Long idProduit);

    /**
     * Calculer le montant total d'une réservation
     */
    @Query("SELECT COALESCE(SUM(l.quantite * l.prixUnitaire), 0.0) FROM LigneReservation l " +
            "WHERE l.reservation.idReservation = :idReservation")
    Double calculateMontantTotalReservation(@Param("idReservation") Long idReservation);

    /**
     * Compter les lignes par statut
     * Utile pour les dashboards
     */
    @Query("SELECT COUNT(l) FROM LigneReservation l " +
            "WHERE l.statutLivraisonLigne = :statut")
    Long countByStatut(@Param("statut") StatutLivraison statut);


    // ============================================
    // VÉRIFICATIONS MÉTIER
    // ============================================

    /**
     * Vérifier si un produit est utilisé dans des réservations actives
     * Utile avant de supprimer ou désactiver un produit
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
            "FROM LigneReservation l " +
            "WHERE l.produit.idProduit = :idProduit " +
            "AND l.statutLivraisonLigne IN (tn.weeding.agenceevenementielle.entities.enums.StatutLivraison.EN_ATTENTE, " +
            "tn.weeding.agenceevenementielle.entities.enums.StatutLivraison.EN_COURS)")
    boolean isProduitUtiliseDansReservationsActives(@Param("idProduit") Long idProduit);

    /**
     * Récupérer les lignes avec des instances spécifiques
     * Utile pour le suivi des produits avec référence
     */
    @Query("SELECT l FROM LigneReservation l " +
            "JOIN l.instancesReservees i " +
            "WHERE i.idInstance = :idInstance")
    List<LigneReservation> findByInstanceReservee(@Param("idInstance") Long idInstance);

    /**
     * Récupérer les lignes nécessitant une livraison dans les N prochains jours
     * Utile pour les alertes de planning
     */
    @Query("SELECT l FROM LigneReservation l " +
            "WHERE l.statutLivraisonLigne = 'EN_ATTENTE' " +
            "AND l.dateDebut <= :dateLimite " +
            "ORDER BY l.dateDebut ASC")
    List<LigneReservation> findLignesNecessitantLivraisonAvant(@Param("dateLimite") Date dateLimite);
}