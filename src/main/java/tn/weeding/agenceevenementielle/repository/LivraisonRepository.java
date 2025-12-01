package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Livraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Repository pour gérer les livraisons
 * Sprint 6 - Gestion des livraisons
 */
@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long> {

    /**
     * Trouver toutes les livraisons par statut
     */
    List<Livraison> findByStatutLivraison(StatutLivraison statut);

    /**
     * Trouver les livraisons d'une date spécifique
     */
    @Query("SELECT l FROM Livraison l WHERE l.dateLivraison = :date ORDER BY l.heureLivraison ASC")
    List<Livraison> findByDateLivraison(@Param("date") LocalDate date);

    /**
     * Trouver les livraisons entre deux dates
     */
    @Query("SELECT l FROM Livraison l WHERE l.dateLivraison BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY l.dateLivraison ASC, l.heureLivraison ASC")
    List<Livraison> findLivraisonsBetweenDates(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Trouver les livraisons en attente
     */
    @Query("SELECT l FROM Livraison l WHERE l.statutLivraison = 'EN_ATTENTE' " +
            "ORDER BY l.dateLivraison ASC, l.heureLivraison ASC")
    List<Livraison> findLivraisonsEnAttente();

    /**
     * Trouver les livraisons en cours
     */
    @Query("SELECT l FROM Livraison l WHERE l.statutLivraison = 'EN_COURS' " +
            "ORDER BY l.dateLivraison ASC, l.heureLivraison ASC")
    List<Livraison> findLivraisonsEnCours();

    /**
     * Trouver les livraisons d'aujourd'hui
     */
    @Query("SELECT l FROM Livraison l WHERE l.dateLivraison = CURRENT_DATE " +
            "ORDER BY l.heureLivraison ASC")
    List<Livraison> findLivraisonsAujourdhui();

    /**
     * Trouver les livraisons affectées à un employé
     */
    @Query("SELECT DISTINCT l FROM Livraison l " +
            "JOIN l.affectationLivraisons af " +
            "WHERE af.utilisateur.idUtilisateur = :idEmploye " +
            "ORDER BY l.dateLivraison DESC")
    List<Livraison> findLivraisonsByEmploye(@Param("idEmploye") Long idEmploye);

    /**
     * Compter les livraisons par statut
     */
    @Query("SELECT COUNT(l) FROM Livraison l WHERE l.statutLivraison = :statut")
    Long countByStatut(@Param("statut") StatutLivraison statut);

    /**
     * Trouver les livraisons d'une réservation spécifique
     */
    @Query("SELECT DISTINCT l FROM Livraison l " +
            "JOIN l.ligneReservations lr " +
            "WHERE lr.reservation.idReservation = :idReservation")
    List<Livraison> findLivraisonsByReservation(@Param("idReservation") Long idReservation);

    /**
     * 📅 Récupérer les livraisons entre deux dates
     */
    List<Livraison> findByDateLivraisonBetween(LocalDate dateDebut, LocalDate dateFin);

    /**
     * 📊 Compter les livraisons entre deux dates
     */
    long countByDateLivraisonBetween(LocalDate dateDebut, LocalDate dateFin);
    //=================================================
    // Statistiques (Pour dashboard)
    //=================================================

    /**
     * Compter les livraisons d'une date spécifique
     */
    Long countByDateLivraison(LocalDate date);
}