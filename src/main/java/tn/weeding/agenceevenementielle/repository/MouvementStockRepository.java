package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.MouvementStock;
import tn.weeding.agenceevenementielle.entities.Produit;
import tn.weeding.agenceevenementielle.entities.TypeMouvement;

import java.util.Date;
import java.util.List;

/**
 * Repository pour la gestion de l'historique des mouvements de stock
 */
@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    // Obtenir tous les mouvements d'un produit
    List<MouvementStock> findByProduitOrderByDateMouvementDesc(Produit produit);

    // Obtenir les mouvements d'un produit par ID
    @Query("SELECT m FROM MouvementStock m WHERE m.produit.idProduit = :idProduit ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(@Param("idProduit") Long idProduit);

    // Obtenir les mouvements par type
    List<MouvementStock> findByTypeMouvementOrderByDateMouvementDesc(TypeMouvement typeMouvement);

    // Obtenir les mouvements effectués par un utilisateur
    List<MouvementStock> findByEffectueParOrderByDateMouvementDesc(String effectuePar);

    // Obtenir les mouvements dans une période
    @Query("SELECT m FROM MouvementStock m WHERE m.dateMouvement BETWEEN :dateDebut AND :dateFin ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByPeriode(@Param("dateDebut") Date dateDebut, @Param("dateFin") Date dateFin);

    // Obtenir les mouvements liés à une réservation
    List<MouvementStock> findByIdReservationOrderByDateMouvementDesc(Long idReservation);

    // Obtenir les derniers mouvements (pour dashboard)
    @Query("SELECT m FROM MouvementStock m ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findRecentMouvements();

    // Statistiques : Total des entrées pour un produit
    @Query("SELECT SUM(m.quantite) FROM MouvementStock m " +
            "WHERE m.produit.idProduit = :idProduit " +
            "AND m.typeMouvement IN (tn.weeding.agenceevenementielle.entities.TypeMouvement.ENTREE_STOCK, " +
            "tn.weeding.agenceevenementielle.entities.TypeMouvement.RETOUR_RESERVATION, " +
            "tn.weeding.agenceevenementielle.entities.TypeMouvement.RETOUR_MAINTENANCE)")
    Integer getTotalEntrees(@Param("idProduit") Long idProduit);

    // Statistiques : Total des sorties pour un produit
    @Query("SELECT SUM(m.quantite) FROM MouvementStock m " +
            "WHERE m.produit.idProduit = :idProduit " +
            "AND m.typeMouvement IN (tn.weeding.agenceevenementielle.entities.TypeMouvement.SORTIE_RESERVATION, " +
            "tn.weeding.agenceevenementielle.entities.TypeMouvement.PRODUIT_ENDOMMAGE, " +
            "tn.weeding.agenceevenementielle.entities.TypeMouvement.MAINTENANCE)")
    Integer getTotalSorties(@Param("idProduit") Long idProduit);

}