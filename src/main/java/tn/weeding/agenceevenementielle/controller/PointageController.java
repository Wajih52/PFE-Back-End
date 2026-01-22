package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.pointage.PointageRequestDto;
import tn.weeding.agenceevenementielle.dto.pointage.PointageResponseDto;
import tn.weeding.agenceevenementielle.dto.pointage.StatistiquesPointageDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.services.PointageServiceInterface;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/pointages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pointages", description = "Gestion des pointages des employés")
public class PointageController {

    private final PointageServiceInterface pointageService;
    private final AuthenticationFacade authenticationFacade;


    /**
     * Pointer son arrivée
     */
    @PostMapping("/pointer-arrivee")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Pointer l'arrivée",
            description = "Permet à un employé de pointer son heure d'arrivée")
    public ResponseEntity<PointageResponseDto> pointerArrivee() {
        log.info("Demande de pointage arrivée");

        String username = authenticationFacade.getAuthentication().getName();
        PointageResponseDto pointage = pointageService.pointerArrivee(username);

        return ResponseEntity.status(HttpStatus.CREATED).body(pointage);
    }

    /**
     * Pointer son départ
     */
    @PutMapping("/pointer-depart")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Pointer le départ",
            description = "Permet à un employé de pointer son heure de départ")
    public ResponseEntity<PointageResponseDto> pointerDepart() {
        log.info("Demande de pointage départ");

        String username = authenticationFacade.getAuthentication().getName();
        PointageResponseDto pointage = pointageService.pointerDepart(username);

        return ResponseEntity.ok(pointage);
    }

    /**
     * Récupérer mon pointage du jour
     */
    @GetMapping("/mon-pointage-du-jour")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Mon pointage du jour",
            description = "Récupère le pointage du jour de l'utilisateur connecté")
    public ResponseEntity<PointageResponseDto> getMonPointageDuJour() {
        log.info("Récupération du pointage du jour");

        String username = authenticationFacade.getAuthentication().getName();
        PointageResponseDto pointage = pointageService.getPointageDuJour(username);

        return ResponseEntity.ok(pointage);
    }

    /**
     * Récupérer mon historique de pointages
     */
    @GetMapping("/mes-pointages")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Mon historique",
            description = "Récupère l'historique des pointages de l'utilisateur connecté")
    public ResponseEntity<List<PointageResponseDto>> getMesPointages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Récupération de l'historique personnel: {} à {}", dateDebut, dateFin);

        String username = authenticationFacade.getAuthentication().getName();
        List<PointageResponseDto> pointages = pointageService.getMesPointages(username, dateDebut, dateFin);

        return ResponseEntity.ok(pointages);
    }

    /**
     * Récupérer mes statistiques
     */
    @GetMapping("/mes-statistiques")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Mes statistiques",
            description = "Récupère les statistiques personnelles de pointage")
    public ResponseEntity<StatistiquesPointageDto> getMesStatistiques(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Récupération des statistiques personnelles");

        String username = authenticationFacade.getAuthentication().getName();
        StatistiquesPointageDto stats = pointageService.getMesStatistiques(username, dateDebut, dateFin);

        return ResponseEntity.ok(stats);
    }


    /**
     * Créer un pointage manuel
     */
    @PostMapping("/manuel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Créer pointage manuel",
            description = "Permet de créer un pointage manuellement pour un employé")
    public ResponseEntity<PointageResponseDto> creerPointageManuel(
            @Valid @RequestBody PointageRequestDto dto) {

        log.info("Création pointage manuel");

        String username = authenticationFacade.getAuthentication().getName();
        PointageResponseDto pointage = pointageService.creerPointageManuel(dto, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(pointage);
    }

    /**
     * Modifier un pointage
     */
    @PutMapping("/{idPointage}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Modifier un pointage",
            description = "Permet de modifier un pointage existant")
    public ResponseEntity<PointageResponseDto> modifierPointage(
            @PathVariable Long idPointage,
            @Valid @RequestBody PointageRequestDto dto) {

        log.info("Modification pointage ID: {}", idPointage);

        String username = authenticationFacade.getAuthentication().getName();
        PointageResponseDto pointage = pointageService.modifierPointage(idPointage, dto, username);

        return ResponseEntity.ok(pointage);
    }

    /**
     * Supprimer un pointage
     */
    @DeleteMapping("/{idPointage}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un pointage",
            description = "Permet de supprimer un pointage")
    public ResponseEntity<Void> supprimerPointage(@PathVariable Long idPointage) {
        log.info("Suppression pointage ID: {}", idPointage);

        String username = authenticationFacade.getAuthentication().getName();
        pointageService.supprimerPointage(idPointage, username);

        return ResponseEntity.noContent().build();
    }

    /**
     * Récupérer les pointages d'un employé
     */
    @GetMapping("/employe/{idEmploye}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Pointages d'un employé",
            description = "Récupère les pointages d'un employé spécifique")
    public ResponseEntity<List<PointageResponseDto>> getPointagesEmploye(
            @PathVariable Long idEmploye,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info(" Récupération pointages employé ID: {}", idEmploye);

        List<PointageResponseDto> pointages =
                pointageService.getPointagesEmploye(idEmploye, dateDebut, dateFin);

        return ResponseEntity.ok(pointages);
    }

    /**
     * Récupérer les statistiques d'un employé
     */
    @GetMapping("/employe/{idEmploye}/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Statistiques d'un employé",
            description = "Récupère les statistiques de pointage d'un employé")
    public ResponseEntity<StatistiquesPointageDto> getStatistiquesEmploye(
            @PathVariable Long idEmploye,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Récupération statistiques employé ID: {}", idEmploye);

        StatistiquesPointageDto stats =
                pointageService.getStatistiquesEmploye(idEmploye, dateDebut, dateFin);

        return ResponseEntity.ok(stats);
    }

    // ============================================
    // VUES GLOBALES
    // ============================================

    /**
     * Récupérer les pointages d'aujourd'hui
     */
    @GetMapping("/aujourd-hui")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Pointages du jour",
            description = "Récupère tous les pointages d'aujourd'hui")
    public ResponseEntity<List<PointageResponseDto>> getPointagesAujourdhui() {
        log.info("Récupération des pointages du jour");

        List<PointageResponseDto> pointages = pointageService.getPointagesAujourdhui();

        return ResponseEntity.ok(pointages);
    }

    /**
     * Récupérer tous les pointages sur une période
     */
    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Pointages par période",
            description = "Récupère tous les pointages sur une période donnée")
    public ResponseEntity<List<PointageResponseDto>> getTousLesPointages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Récupération pointages période: {} à {}", dateDebut, dateFin);

        List<PointageResponseDto> pointages =
                pointageService.getTousLesPointages(dateDebut, dateFin);

        return ResponseEntity.ok(pointages);
    }

    /**
     * Récupérer les pointages par statut
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Pointages par statut",
            description = "Récupère les pointages d'un statut spécifique")
    public ResponseEntity<List<PointageResponseDto>> getPointagesByStatut(
            @PathVariable StatutPointage statut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Récupération pointages statut: {} pour le {}", statut, date);

        List<PointageResponseDto> pointages = pointageService.getPointagesByStatut(statut, date);

        return ResponseEntity.ok(pointages);
    }

    /**
     * Récupérer les employés absents
     */
    @GetMapping("/absents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Employés absents",
            description = "Récupère la liste des employés absents pour une date")
    public ResponseEntity<List<Long>> getEmployesAbsents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Récupération des absents du {}", date);

        List<Long> idsAbsents = pointageService.getEmployesAbsents(date);

        return ResponseEntity.ok(idsAbsents);
    }

    /**
     * lancement manuellement de marquage des absents
     */
    @PostMapping("/marquer-absents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marquer les absents",
            description = "Déclenche manuellement le processus de marquage des absents")
    public ResponseEntity<Void> marquerAbsents() {
        log.info("Déclenchement manuel du marquage des absents");

        pointageService.marquerAbsentsAutomatiquement();

        return ResponseEntity.ok().build();
    }
}
