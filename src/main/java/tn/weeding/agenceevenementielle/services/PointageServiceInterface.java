package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.pointage.PointageRequestDto;
import tn.weeding.agenceevenementielle.dto.pointage.PointageResponseDto;
import tn.weeding.agenceevenementielle.dto.pointage.StatistiquesPointageDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface du service de gestion des pointages
 */
public interface PointageServiceInterface {

    // ============ POINTAGE EMPLOYÉ ============

    /**
     * Pointer l'arrivée (heureDebut)
     */
    PointageResponseDto pointerArrivee(String username);

    /**
     * Pointer le départ (heureFin)
     */
    PointageResponseDto pointerDepart(String username);

    /**
     * Récupérer le pointage du jour de l'utilisateur connecté
     */
    PointageResponseDto getPointageDuJour(String username);

    /**
     * Récupérer l'historique des pointages de l'utilisateur connecté
     */
    List<PointageResponseDto> getMesPointages(String username, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupérer les statistiques personnelles
     */
    StatistiquesPointageDto getMesStatistiques(String username, LocalDate dateDebut, LocalDate dateFin);

    // ============ GESTION ADMIN/MANAGER ============

    /**
     * Créer/Modifier un pointage manuellement (admin/manager)
     */
    PointageResponseDto creerPointageManuel(PointageRequestDto dto, String username);

    /**
     * Modifier un pointage existant
     */
    PointageResponseDto modifierPointage(Long idPointage, PointageRequestDto dto, String username);

    /**
     * Supprimer un pointage
     */
    void supprimerPointage(Long idPointage, String username);

    /**
     * Récupérer tous les pointages d'un employé
     */
    List<PointageResponseDto> getPointagesEmploye(Long idEmploye, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupérer les statistiques d'un employé
     */
    StatistiquesPointageDto getStatistiquesEmploye(Long idEmploye, LocalDate dateDebut, LocalDate dateFin);

    // ============ VUES GLOBALES ============

    /**
     * Récupérer tous les pointages du jour
     */
    List<PointageResponseDto> getPointagesAujourdhui();

    /**
     * Récupérer tous les pointages d'une période
     */
    List<PointageResponseDto> getTousLesPointages(LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupérer les pointages par statut
     */
    List<PointageResponseDto> getPointagesByStatut(StatutPointage statut, LocalDate date);

    /**
     * Récupérer les employés absents du jour
     */
    List<Long> getEmployesAbsents(LocalDate date);

    /**
     * Marquer automatiquement les absents (tâche programmée)
     */
    void marquerAbsentsAutomatiquement();
}
