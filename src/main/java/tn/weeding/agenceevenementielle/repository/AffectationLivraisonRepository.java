package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.AffectationLivraison;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;



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

    //=================================================
    // Statistiques (Pour dashboard)
    //=================================================

    /**
     * Top employés par nombre de livraisons effectuées
     * Retourne: [idEmploye, nombreLivraisons]
     */
    @Query("SELECT a.utilisateur.idUtilisateur, COUNT(a) " +
            "FROM AffectationLivraison a " +
            "GROUP BY a.utilisateur.idUtilisateur " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> findTopEmployesParNombreLivraisons(@Param("limit") int limit);
}