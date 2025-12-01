package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Pointage;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointageRepository extends JpaRepository<Pointage, Long> {

    /**
     * Trouver un pointage par utilisateur et date
     */
    Optional<Pointage> findByUtilisateurIdUtilisateurAndDateTravail(Long idUtilisateur, LocalDate dateTravail);

    /**
     * Vérifier si un pointage existe pour une date et un utilisateur
     */
    boolean existsByUtilisateurIdUtilisateurAndDateTravail(Long idUtilisateur, LocalDate dateTravail);

    /**
     * Récupérer tous les pointages d'un utilisateur
     */
    List<Pointage> findByUtilisateurIdUtilisateur(Long idUtilisateur);

    /**
     * Récupérer les pointages d'un utilisateur pour une période
     */
    @Query("SELECT p FROM Pointage p WHERE p.utilisateur.idUtilisateur = :idUtilisateur " +
            "AND p.dateTravail BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY p.dateTravail DESC")
    List<Pointage> findByUtilisateurAndPeriode(
            @Param("idUtilisateur") Long idUtilisateur,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Récupérer tous les pointages d'une date spécifique
     */
    List<Pointage> findByDateTravail(LocalDate dateTravail);

    /**
     * Récupérer les pointages entre deux dates
     */
    @Query("SELECT p FROM Pointage p WHERE p.dateTravail BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY p.dateTravail DESC, p.utilisateur.nom")
    List<Pointage> findByPeriode(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Récupérer les pointages par statut
     */
    List<Pointage> findByStatutPointage(StatutPointage statut);

    /**
     * Récupérer les pointages par statut pour une date
     */
    List<Pointage> findByStatutPointageAndDateTravail(StatutPointage statut, LocalDate dateTravail);

    /**
     * Compter les pointages d'un utilisateur par statut sur une période
     */
    @Query("SELECT COUNT(p) FROM Pointage p WHERE p.utilisateur.idUtilisateur = :idUtilisateur " +
            "AND p.statutPointage = :statut " +
            "AND p.dateTravail BETWEEN :dateDebut AND :dateFin")
    Long countByUtilisateurAndStatutAndPeriode(
            @Param("idUtilisateur") Long idUtilisateur,
            @Param("statut") StatutPointage statut,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Calculer le total des heures travaillées pour un utilisateur sur une période
     */
    @Query("SELECT COALESCE(SUM(p.totalHeures), 0.0) FROM Pointage p " +
            "WHERE p.utilisateur.idUtilisateur = :idUtilisateur " +
            "AND p.dateTravail BETWEEN :dateDebut AND :dateFin " +
            "AND p.statutPointage = 'present'")
    Double sumTotalHeuresByUtilisateurAndPeriode(
            @Param("idUtilisateur") Long idUtilisateur,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Récupérer les pointages d'aujourd'hui
     */
    @Query("SELECT p FROM Pointage p WHERE p.dateTravail = :today ORDER BY p.utilisateur.nom")
    List<Pointage> findPointagesAujourdhui(@Param("today") LocalDate today);

    /**
     * Récupérer les employés absents pour une date donnée
     * (Employés qui n'ont pas de pointage pour cette date)
     */
    @Query("SELECT u.idUtilisateur FROM Utilisateur u " +
            "JOIN u.utilisateurRoles ur " +
            "WHERE ur.role.nom IN ('EMPLOYE', 'MANAGER') " +
            "AND u.statutEmploye = 'EnTravail' " +
            "AND u.idUtilisateur NOT IN (" +
            "  SELECT p.utilisateur.idUtilisateur FROM Pointage p WHERE p.dateTravail = :date" +
            ")")
    List<Long> findEmployesAbsents(@Param("date") LocalDate date);

    //=================================================
    // Statistiques (Pour dashboard)
    //=================================================

    /**
     * Trouver les pointages d'un employé sur une période
     */
    List<Pointage> findByUtilisateur_IdUtilisateurAndDateTravailBetween(
            Long idUtilisateur,
            LocalDate dateDebut,
            LocalDate dateFin
    );
}
