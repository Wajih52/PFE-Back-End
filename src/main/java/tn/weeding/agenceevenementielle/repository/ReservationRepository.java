package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des réservations
 * Sprint 4 - Gestion des réservations (incluant devis)
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // ============ RECHERCHES DE BASE ============

    /**
     * Trouver une réservation par sa référence
     */
    Optional<Reservation> findByReferenceReservation(String referenceReservation);

    /**
     * Vérifier si une référence existe
     */
    boolean existsByReferenceReservation(String referenceReservation);

    /**
     * Trouver toutes les réservations d'un utilisateur
     */
    List<Reservation> findByUtilisateur_IdUtilisateur(Long idUtilisateur);

    /**
     * Trouver les réservations d'un utilisateur triées par date (plus récentes en premier)
     */
    List<Reservation> findByUtilisateur_IdUtilisateurOrderByDateDebutDesc(Long idUtilisateur);

    List<Reservation> findByDateExpirationDevis (LocalDateTime dateExpirationDevis);

    // ============ FILTRAGE PAR STATUT ============

    /**
     * Trouver toutes les réservations par statut
     */
    List<Reservation> findByStatutReservation(StatutReservation statut);

    /**
     * Trouver les réservations d'un client par statut
     */
    List<Reservation> findByUtilisateur_IdUtilisateurAndStatutReservation(
            Long idUtilisateur,
            StatutReservation statut
    );

    /**
     * Compter les réservations par statut
     */
    long countByStatutReservation(StatutReservation statut);

    /**
     * Trouver tous les DEVIS en attente (StatutReservation = EnAttente)
     */
    @Query("SELECT r FROM Reservation r WHERE r.statutReservation = 'EN_ATTENTE' ORDER BY r.dateDebut DESC")
    List<Reservation> findAllDevisEnAttente();

    /**
     * Trouver tous les devis en attente d'un client
     */
    @Query("SELECT r FROM Reservation r WHERE r.utilisateur.idUtilisateur = :idUtilisateur " +
            "AND r.statutReservation = 'EN_ATTENTE' ORDER BY r.dateDebut DESC")
    List<Reservation> findDevisEnAttenteByClient(@Param("idUtilisateur") Long idUtilisateur);

    // ============ FILTRAGE PAR DATE ============

    /**
     * Trouver les réservations dans une période donnée
     */
    @Query("SELECT r FROM Reservation r WHERE r.dateDebut >= :dateDebut AND r.dateFin <= :dateFin")
    List<Reservation> findReservationsBetweenDates(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Trouver les réservations qui se chevauchent avec une période donnée
     * Utile pour vérifier les conflits de disponibilité
     */
    @Query("SELECT r FROM Reservation r WHERE " +
            "r.statutReservation = 'CONFIRME' AND " +
            "((r.dateDebut <= :dateFin AND r.dateFin >= :dateDebut))")
    List<Reservation> findReservationsOverlapping(
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    /**
     * Trouver les réservations confirmées à venir (futures)
     */
    @Query("SELECT r FROM Reservation r WHERE r.statutReservation = 'CONFIRME' " +
            "AND r.dateDebut > CURRENT_DATE ORDER BY r.dateDebut ASC")
    List<Reservation> findReservationsConfirmeesAVenir();

    /**
     * Trouver les réservations en cours (date actuelle entre dateDebut et dateFin)
     */
    @Query("SELECT r FROM Reservation r WHERE r.statutReservation = 'CONFIRME' " +
            "AND CURRENT_DATE BETWEEN r.dateDebut AND r.dateFin")
    List<Reservation> findReservationsEnCours();

    /**
     * Trouver les réservations passées
     */
    @Query("SELECT r FROM Reservation r WHERE r.dateFin < CURRENT_DATE " +
            "ORDER BY r.dateFin DESC")
    List<Reservation> findReservationsPassees();

    // ============ STATISTIQUES ET MONTANTS ============

    /**
     * Calculer le chiffre d'affaires total (réservations confirmées)
     */
    @Query("SELECT SUM(r.montantTotal) FROM Reservation r " +
            "WHERE r.statutReservation = 'CONFIRME'")
    Double calculateChiffreAffairesTotal();

    /**
     * Calculer le chiffre d'affaires pour une période
     */
    @Query("SELECT SUM(r.montantTotal) FROM Reservation r " +
            "WHERE r.statutReservation = 'CONFIRME' " +
            "AND r.dateDebut >= :dateDebut AND r.dateFin <= :dateFin")
    Double calculateChiffreAffairesPeriode(
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );

    /**
     * Trouver les réservations avec paiement incomplet
     */
    @Query("SELECT r FROM Reservation r WHERE r.statutReservation = 'CONFIRME' " +
            "AND r.montantPaye < r.montantTotal")
    List<Reservation> findReservationsAvecPaiementIncomplet();

    /**
     * Compter le nombre de réservations d'un client
     */
    long countByUtilisateur_IdUtilisateur(Long idUtilisateur);




    // ============ RECHERCHE MULTICRITÈRES ============

    /**
     * Recherche avancée avec plusieurs critères
     */
    @Query("SELECT r FROM Reservation r WHERE " +
            "(:idUtilisateur IS NULL OR r.utilisateur.idUtilisateur = :idUtilisateur) AND " +
            "(:statut IS NULL OR r.statutReservation = :statut) AND " +
            "(:dateDebutMin IS NULL OR r.dateDebut >= :dateDebutMin) AND " +
            "(:dateDebutMax IS NULL OR r.dateDebut <= :dateDebutMax) AND " +
            "(:reference IS NULL OR LOWER(r.referenceReservation) LIKE LOWER(CONCAT('%', :reference, '%')))")
    List<Reservation> searchReservations(
            @Param("idUtilisateur") Long idUtilisateur,
            @Param("statut") StatutReservation statut,
            @Param("dateDebutMin") LocalDate dateDebutMin,
            @Param("dateDebutMax") LocalDate dateDebutMax,
            @Param("reference") String reference
    );

    /**
     * Rechercher par nom ou prénom du client
     */
    @Query("SELECT r FROM Reservation r WHERE " +
            "LOWER(r.utilisateur.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.utilisateur.prenom) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Reservation> searchByClientName(@Param("search") String search);

    // ============ ALERTES ET NOTIFICATIONS ============

    /**
     * Trouver les réservations qui commencent bientôt (dans les N jours)
     */
    @Query("SELECT r FROM Reservation r WHERE r.statutReservation = 'CONFIRME' " +
            "AND r.dateDebut BETWEEN CURRENT_DATE AND :dateLimit")
    List<Reservation> findReservationsCommencantDansNJours(@Param("dateLimit") Date dateLimit);

    /**
     * Trouver les réservations dont la date de fin approche
     */
    @Query("SELECT r FROM Reservation r WHERE r.statutReservation = 'CONFIRME' " +
            "AND r.dateFin BETWEEN CURRENT_DATE AND :dateLimit")
    List<Reservation> findReservationsFinissantDansNJours(@Param("dateLimit") Date dateLimit);

    /**
     * Trouver les devis en attente depuis plus de N jours
     * (pour relance client)
     */
    @Query(value = "SELECT r FROM Reservation r WHERE r.statutReservation = 'EN_ATTENTE' " +
            "AND DATEDIFF(CURRENT_DATE, r.dateCreation) > :nbreJours")
    List<Reservation> findDevisExpires(@Param("nbreJours") int nbreJours);

    /**
     * Trouver les devis (réservations EN_ATTENTE) expirés
     *
     * @param statut Statut de la réservation (EN_ATTENTE)
     * @param dateExpiration Date limite d'expiration
     * @return Liste des devis expirés
     */
    List<Reservation> findByStatutReservationAndDateExpirationDevisBefore(
            StatutReservation statut,
            LocalDateTime dateExpiration
    );



}