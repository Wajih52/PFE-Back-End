package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.weeding.agenceevenementielle.entities.Paiement;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.enums.StatutPaiement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Optional<Paiement> findByCodePaiement(String codePaiement);
    boolean existsByCodePaiement(String codePaiement);
    List<Paiement> findByReservationIdReservationOrderByDatePaiementDesc(Long idReservation);
    List<Paiement> findByStatutPaiementOrderByDatePaiementDesc(StatutPaiement statut);

    @Query("SELECT p FROM Paiement p WHERE p.reservation.utilisateur.idUtilisateur = :idClient ORDER BY p.datePaiement DESC")
    List<Paiement> findByClientIdOrderByDatePaiementDesc(@Param("idClient") Long idClient);

    @Query("SELECT p FROM Paiement p WHERE p.statutPaiement = 'EN_ATTENTE' ORDER BY p.datePaiement ASC")
    List<Paiement> findPaiementsEnAttente();

    @Query("SELECT COALESCE(SUM(p.montantPaiement), 0.0) FROM Paiement p WHERE p.reservation.idReservation = :idReservation AND p.statutPaiement = 'VALIDE'")
    Double calculerMontantPayeValidePourReservation(@Param("idReservation") Long idReservation);

    @Query("SELECT p FROM Paiement p WHERE p.reservation.idReservation = :idReservation AND p.statutPaiement = 'VALIDE' ORDER BY p.datePaiement DESC")
    List<Paiement> findPaiementsValidesByReservation(@Param("idReservation") Long idReservation);

    @Query("SELECT p FROM Paiement p WHERE p.datePaiement BETWEEN :dateDebut AND :dateFin ORDER BY p.datePaiement DESC")
    List<Paiement> findPaiementsByPeriode(@Param("dateDebut") LocalDateTime dateDebut, @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT COUNT(p) FROM Paiement p WHERE p.statutPaiement = 'EN_ATTENTE'")
    Long countPaiementsEnAttente();

    @Query(value = "SELECT codePaiement FROM paiement WHERE codePaiement LIKE CONCAT(:yearPrefix, '%') ORDER BY codePaiement DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLastCodePaiementByYear(@Param("yearPrefix") String yearPrefix);


    //===================================
    //  Dashbooard et analyse
    //==================================

    /**
     * Trouver les paiements validés
     */
    List<Paiement> findByValidePar_IsNotNull();

    /**
     * Trouver les paiements sur une période (validés uniquement)
     */
    List<Paiement> findByDatePaiementBetweenAndValidePar_IsNotNull(
            LocalDateTime debut,
            LocalDateTime fin
    );

}
