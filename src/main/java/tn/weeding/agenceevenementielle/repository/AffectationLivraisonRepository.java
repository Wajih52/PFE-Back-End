package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.AffectationLivraison;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Repository pour gérer les affectations de livraison
 * Sprint 6 - Gestion des livraisons
 */
@Repository
public interface AffectationLivraisonRepository extends JpaRepository<AffectationLivraison, Long> {

    /**
     * Trouver toutes les affectations d'une livraison
     */
    List<AffectationLivraison> findByLivraison_IdLivraison(Long idLivraison);

    /**
     * Trouver toutes les affectations d'un employé
     */
    List<AffectationLivraison> findByUtilisateur_IdUtilisateur(Long idUtilisateur);

    /**
     * Trouver les affectations d'un employé à une date donnée
     */
    @Query("SELECT af FROM AffectationLivraison af " +
            "WHERE af.utilisateur.idUtilisateur = :idEmploye " +
            "AND af.dateAffectationLivraison = :date " +
            "ORDER BY af.heureDebut ASC")
    List<AffectationLivraison> findAffectationsByEmployeAndDate(
            @Param("idEmploye") Long idEmploye,
            @Param("date") LocalDate date
    );

    /**
     * Vérifier si un employé est déjà affecté à une livraison
     */
    @Query("SELECT COUNT(af) > 0 FROM AffectationLivraison af " +
            "WHERE af.livraison.idLivraison = :idLivraison " +
            "AND af.utilisateur.idUtilisateur = :idEmploye")
    boolean existsByLivraisonAndEmploye(
            @Param("idLivraison") Long idLivraison,
            @Param("idEmploye") Long idEmploye
    );

    /**
     * Supprimer toutes les affectations d'une livraison
     */
    void deleteByLivraison_IdLivraison(Long idLivraison);
}