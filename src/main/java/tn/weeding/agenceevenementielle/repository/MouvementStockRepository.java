package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.MouvementStock;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;

import java.util.Date;
import java.util.List;


@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    // ============================================
    // RECHERCHE PAR PRODUIT ou par instance
    // ============================================

    /**
     * Trouve tous les mouvements pour un produit
     */
    List<MouvementStock> findByProduit_IdProduit(Long idProduit);

    /**
     * Trouve tous les mouvements pour une instance
     */
    List<MouvementStock> findByIdInstance(Long idInstance);

    /**
     * Récupérer tous les mouvements d'un produit, triés par date décroissante
     * @param idProduit ID du produit
     * @return Liste des mouvements, du plus récent au plus ancien
     */
    @Query("SELECT m FROM MouvementStock m WHERE m.produit.idProduit = :idProduit ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByProduit_IdProduitOrderByDateMouvementDesc(@Param("idProduit") Long idProduit);

    
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
    // RECHERCHE PAR INSTANCE
    // ============================================

    /**
     * Récupérer les mouvements d'une instance, triés par date
     * @param codeInstance Code de l'instance
     * @return Liste des mouvements, du plus récent au plus ancien
     */
    List<MouvementStock> findByCodeInstanceOrderByDateMouvementDesc(String codeInstance);


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
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.MAINTENANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.PRODUIT_ENDOMMAGE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.SUPPRESSION_INSTANCE, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.RESERVATION, " +
            "    tn.weeding.agenceevenementielle.entities.enums.TypeMouvement.LIVRAISON" +
            ")")
    Integer getTotalSorties(@Param("idProduit") Long idProduit);

}