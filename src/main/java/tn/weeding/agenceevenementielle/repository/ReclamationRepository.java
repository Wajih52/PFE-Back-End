package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Reclamation;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;
import tn.weeding.agenceevenementielle.entities.enums.TypeReclamation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des réclamations
 */
@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    /**
     * Trouver une réclamation par son code
     */
    Optional<Reclamation> findByCodeReclamation(String codeReclamation);

    /**
     * Vérifier si un code existe
     */
    boolean existsByCodeReclamation(String codeReclamation);

    /**
     * Récupérer toutes les réclamations d'un utilisateur
     */
    List<Reclamation> findByUtilisateurIdUtilisateur(Long idUtilisateur);

    /**
     * Récupérer toutes les réclamations par email (pour visiteurs)
     */
    List<Reclamation> findByContactEmail(String email);

    /**
     * Récupérer les réclamations par statut
     */
    List<Reclamation> findByStatutReclamation(StatutReclamation statut);

    /**
     * Récupérer les réclamations par type
     */
    List<Reclamation> findByTypeReclamation(TypeReclamation type);

    /**
     * Récupérer les réclamations par priorité
     */
    List<Reclamation> findByPrioriteReclamation(PrioriteReclamation priorite);

    /**
     * Récupérer les réclamations par statut ET priorité
     */
    List<Reclamation> findByStatutReclamationAndPrioriteReclamation(
            StatutReclamation statut,
            PrioriteReclamation priorite
    );

    /**
     * Récupérer les réclamations liées à une réservation
     */
    List<Reclamation> findByReservationIdReservation(Long idReservation);

    /**
     * Récupérer les réclamations dans une période donnée
     */
    List<Reclamation> findByDateReclamationBetween(LocalDateTime debut, LocalDateTime fin);

    /**
     * Récupérer les réclamations traitées par un employé/admin
     */
    List<Reclamation> findByTraitePar(String codeUtilisateur);

    /**
     * Recherche multi-critères
     */
    @Query("SELECT r FROM Reclamation r WHERE " +
            "(:statut IS NULL OR r.statutReclamation = :statut) AND " +
            "(:type IS NULL OR r.typeReclamation = :type) AND " +
            "(:priorite IS NULL OR r.prioriteReclamation = :priorite) AND " +
            "(:idUtilisateur IS NULL OR r.utilisateur.idUtilisateur = :idUtilisateur)")
    List<Reclamation> rechercherReclamations(
            @Param("statut") StatutReclamation statut,
            @Param("type") TypeReclamation type,
            @Param("priorite") PrioriteReclamation priorite,
            @Param("idUtilisateur") Long idUtilisateur
    );

    /**
     * Compter les réclamations en attente
     */
    long countByStatutReclamation(StatutReclamation statut);

    /**
     * Compter les réclamations urgentes non traitées
     */
    @Query("SELECT COUNT(r) FROM Reclamation r WHERE " +
            "r.prioriteReclamation = 'URGENTE' AND " +
            "r.statutReclamation IN (tn.weeding.agenceevenementielle.entities.enums.StatutReclamation.EN_ATTENTE," +
            " tn.weeding.agenceevenementielle.entities.enums.StatutReclamation.EN_COURS)")
    long countReclamationsUrgentesNonTraitees();
}