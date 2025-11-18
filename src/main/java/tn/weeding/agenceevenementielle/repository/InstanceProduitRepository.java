package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.InstanceProduit;
import tn.weeding.agenceevenementielle.entities.enums.StatutInstance;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des instances de produits
 * Sprint 3 - Gestion des produits et du stock
 */
@Repository
public interface InstanceProduitRepository extends JpaRepository<InstanceProduit, Long> {

    /**
     * Trouver une instance par son numéro de série
     */
    Optional<InstanceProduit> findByNumeroSerie(String numeroSerie);

    /**
     * Vérifier si un numéro de série existe déjà
     */
    boolean existsByNumeroSerie(String numeroSerie);

    /**
     * Récupérer toutes les instances d'un produit
     */
    List<InstanceProduit> findByProduit_IdProduit(Long idProduit);

    /**
     * Récupérer les instances disponibles d'un produit (statut DISPONIBLE pour vérifier l'etat physique)
     */
    @Query("SELECT i FROM InstanceProduit i WHERE i.produit.idProduit = :idProduit " +
            "AND i.statut = tn.weeding.agenceevenementielle.entities.enums.StatutInstance.DISPONIBLE ")
    List<InstanceProduit> findInstancesDisponibles(@Param("idProduit") Long idProduit);

    /**
     * Récupérer les instances disponibles d'un produit sur une période
     * (statut DISPONIBLE et pas de ligne réservation sur une prériode donnée)
     */
    @Query("SELECT i FROM InstanceProduit i " +
            "WHERE i.produit.idProduit = :idProduit " +
            "AND i.statut = tn.weeding.agenceevenementielle.entities.enums.StatutInstance.DISPONIBLE " +
            "AND NOT EXISTS (" +
            "    SELECT lr FROM LigneReservation lr " +
            "    WHERE i MEMBER OF lr.instancesReservees " +
            "    AND lr.dateDebut <= :dateFin " +
            "    AND lr.dateFin >= :dateDebut " +
            "    AND lr.reservation.statutReservation = 'CONFIRME'" +  // ✅ UNIQUEMENT CONFIRME
            ") " +
            "ORDER BY i.numeroSerie")
    List<InstanceProduit> findInstancesDisponiblesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Récupérer les N premières instances disponibles d'un produit
     */
    @Query("SELECT i FROM InstanceProduit i WHERE i.produit.idProduit = :idProduit " +
            "AND i.statut = tn.weeding.agenceevenementielle.entities.enums.StatutInstance.DISPONIBLE " +
            "ORDER BY i.numeroSerie")
    List<InstanceProduit> findTopNInstancesDisponibles(@Param("idProduit") Long idProduit);

    /**
     * Compter les instances disponibles d'un produit
     */
    @Query("SELECT COUNT(i) FROM InstanceProduit i WHERE i.produit.idProduit = :idProduit " +
            "AND i.statut = 'DISPONIBLE'")
    int countInstancesDisponibles(@Param("idProduit") Long idProduit);


    /**
     * Compter les instances disponibles d'un produit sur une période
     */
    @Query("SELECT COUNT(i) FROM InstanceProduit i " +
            "WHERE i.produit.idProduit = :idProduit " +
            "AND i.statut = tn.weeding.agenceevenementielle.entities.enums.StatutInstance.DISPONIBLE " +
            "AND NOT EXISTS (" +
            "    SELECT lr FROM LigneReservation lr " +
            "    WHERE i MEMBER OF lr.instancesReservees " +
            "    AND lr.dateDebut <= :dateFin " +
            "    AND lr.dateFin >= :dateDebut " +
            "    AND lr.reservation.statutReservation = 'CONFIRME'" +  // ✅ UNIQUEMENT CONFIRME
            ")")
    int countInstancesDisponiblesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
    /**
     * Récupérer les instances d'un produit par statut
     */
    List<InstanceProduit> findByProduit_IdProduitAndStatut(Long idProduit, StatutInstance statut);

    /**
     * Récupérer les instances par statut (tous produits)
     */
    List<InstanceProduit> findByStatut(StatutInstance statut);


    /**
     * Récupérer les instances nécessitant une maintenance
     */
    @Query("SELECT i FROM InstanceProduit i WHERE i.dateProchaineMaintenance <= :date " +
            "AND i.statut != 'EN_MAINTENANCE'")
    List<InstanceProduit> findInstancesNecessitantMaintenance(@Param("date") LocalDate date);

    /**
     * Récupérer les instances hors service
     */
    @Query("SELECT i FROM InstanceProduit i WHERE i.statut IN (tn.weeding.agenceevenementielle.entities.enums.StatutInstance.HORS_SERVICE," +
            " tn.weeding.agenceevenementielle.entities.enums.StatutInstance.PERDU)")
    List<InstanceProduit> findInstancesHorsService();

    Long  countByProduit_IdProduit (Long idProduit);
}