package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.InstanceProduit;
import tn.weeding.agenceevenementielle.entities.enums.StatutInstance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface InstanceProduitRepository extends JpaRepository<InstanceProduit, Long> {

    /**
     * Trouver une instance par son numéro de série
     */
    Optional<InstanceProduit> findByNumeroSerie(String numeroSerie);

    /**
     * Trouver une instance par son code QR
     */
    Optional<InstanceProduit> findByQrCodeToken(String qrCodeToken);

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
     * (Accepte DISPONIBLE, EN_ATTENTE, EN_LIVRAISON, EN_UTILISATION, EN_RETOUR tant qu'il n'y a pas de conflit sur la période)
     */
    @Query("SELECT i FROM InstanceProduit i " +
            "WHERE i.produit.idProduit = :idProduit " +
            "AND i.statut NOT IN (tn.weeding.agenceevenementielle.entities.enums.StatutInstance.HORS_SERVICE, " +
            "                     tn.weeding.agenceevenementielle.entities.enums.StatutInstance.EN_MAINTENANCE, " +
            "                     tn.weeding.agenceevenementielle.entities.enums.StatutInstance.PERDU, " +
            "                     tn.weeding.agenceevenementielle.entities.enums.StatutInstance.EN_PANNE) " +
            "AND NOT EXISTS (" +
            "    SELECT lr FROM LigneReservation lr " +
            "    WHERE i MEMBER OF lr.instancesReservees " +
            "    AND lr.dateDebut <= :dateFin " +
            "    AND lr.dateFin >= :dateDebut " +
            "    AND lr.reservation.statutReservation = 'CONFIRME'" +
            ") " +
            "ORDER BY i.numeroSerie")
    List<InstanceProduit> findInstancesDisponiblesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );


    /**
     * Compter les instances disponibles d'un produit sur une période
     */
    @Query("SELECT COUNT(i) FROM InstanceProduit i " +
            "WHERE i.produit.idProduit = :idProduit " +
            "AND i.statut NOT IN (tn.weeding.agenceevenementielle.entities.enums.StatutInstance.HORS_SERVICE, " +
            "                     tn.weeding.agenceevenementielle.entities.enums.StatutInstance.EN_MAINTENANCE, " +
            "                     tn.weeding.agenceevenementielle.entities.enums.StatutInstance.PERDU, " +
            "                     tn.weeding.agenceevenementielle.entities.enums.StatutInstance.EN_PANNE) " +
            "AND NOT EXISTS (" +
            "    SELECT lr FROM LigneReservation lr " +
            "    WHERE i MEMBER OF lr.instancesReservees " +
            "    AND lr.dateDebut <= :dateFin " +
            "    AND lr.dateFin >= :dateDebut " +
            "    AND lr.reservation.statutReservation = 'CONFIRME'" +
            ")")
    int countInstancesDisponiblesSurPeriode(
            @Param("idProduit") Long idProduit,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

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


    Long  countByProduit_IdProduit (Long idProduit);
}