package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Avis;
import tn.weeding.agenceevenementielle.entities.enums.StatutAvis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvisRepository extends JpaRepository<Avis, Long> {


    /**
     * Vérifier si un client a déjà évalué un produit pour une réservation donnée
     */
    boolean existsByClient_IdUtilisateurAndReservation_IdReservationAndProduit_IdProduit(
            Long clientId,
            Long reservationId,
            Long produitId
    );


    /**
     * Tous les avis d'un produit (peu importe le statut)
     */
    List<Avis> findByProduit_IdProduitOrderByDateAvisDesc(Long produitId);

    /**
     * Avis approuvés et visibles d'un produit
     */
    @Query("SELECT a FROM Avis a WHERE a.produit.idProduit = :produitId " +
            "AND a.statut = 'APPROUVE' AND a.visible = true " +
            "ORDER BY a.dateAvis DESC")
    List<Avis> findAvisApprouvesByProduit(@Param("produitId") Long produitId);


    /**
     * Tous les avis d'un client
     */
    List<Avis> findByClient_IdUtilisateurOrderByDateAvisDesc(Long clientId);


    /**
     * Tous les avis avec un statut donné
     */
    List<Avis> findByStatutOrderByDateAvisDesc(StatutAvis statut);

    /**
     * Avis en attente de modération
     */
    @Query("SELECT a FROM Avis a WHERE a.statut = 'EN_ATTENTE' " +
            "ORDER BY a.dateAvis ASC")
    List<Avis> findAvisEnAttente();

    /**
     * Compter les avis en attente
     */
    long countByStatut(StatutAvis statut);


    // STATISTIQUES

    /**
     * Moyenne des notes approuvées pour un produit
     */
    @Query("SELECT AVG(a.note) FROM Avis a WHERE a.produit.idProduit = :produitId " +
            "AND a.statut = 'APPROUVE' AND a.visible = true")
    Double getMoyenneNotesByProduit(@Param("produitId") Long produitId);


    /**
     * Produits avec les meilleures notes (minimum X avis)
     */
    @Query("SELECT a.produit.idProduit, AVG(a.note) as moyenne, COUNT(a) as nombre " +
            "FROM Avis a WHERE a.statut = 'APPROUVE' AND a.visible = true " +
            "GROUP BY a.produit.idProduit " +
            "HAVING COUNT(a) >= :minAvis " +
            "ORDER BY moyenne DESC")
    List<Object[]> getTopProduitsByNote(@Param("minAvis") Long minAvis);


    // RECHERCHE AVANCÉE


    /**
     * Rechercher des avis par note
     */
    List<Avis> findByNoteAndStatutOrderByDateAvisDesc(Integer note, StatutAvis statut);

    /**
     * Avis créés dans une période
     */
    @Query("SELECT a FROM Avis a WHERE a.dateAvis BETWEEN :debut AND :fin " +
            "ORDER BY a.dateAvis DESC")
    List<Avis> findAvisByPeriode(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );

    /**
     * Recherche textuelle dans les commentaires
     */
    @Query("SELECT a FROM Avis a WHERE LOWER(a.commentaire) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY a.dateAvis DESC")
    List<Avis> searchByCommentaire(@Param("keyword") String keyword);



    //  Dashbooard et analyse


    /**
     * Moyenne des notes par catégorie de produits
     * Retourne: [categorie, moyenneNote]
     */
    @Query("SELECT p.categorieProduit, AVG(a.note) " +
            "FROM Avis a " +
            "JOIN a.produit p " +
            "WHERE a.statut = 'APPROUVE' " +
            "GROUP BY p.categorieProduit " +
            "ORDER BY AVG(a.note) DESC")
    List<Object[]> findMoyenneNotesParCategorie();

    /**
     * Trouver les avis approuvés d'un produit
     */
    @Query("SELECT a FROM Avis a WHERE a.produit.idProduit = :produitId " +
            "AND a.statut = 'APPROUVE' AND a.visible = true")
    List<Avis> getAvisApprouvesByProduit(@Param("produitId") Long produitId);

}