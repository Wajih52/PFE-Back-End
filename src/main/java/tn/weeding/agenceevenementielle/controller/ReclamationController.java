package tn.weeding.agenceevenementielle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.dto.reclamation.*;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;
import tn.weeding.agenceevenementielle.entities.enums.TypeReclamation;
import tn.weeding.agenceevenementielle.services.ReclamationServiceInterface;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReclamationController {

    private final ReclamationServiceInterface reclamationService;
    private final AuthenticationFacade authenticationFacade ;

    /**
     * Créer une réclamation (public - pour visiteurs ET clients et employés)
     */
    @PostMapping("create")
    public ResponseEntity<ReclamationResponseDto> creerReclamation(
            @Valid @RequestBody ReclamationRequestDto dto,
            Authentication authentication) {

        log.info("Nouvelle réclamation - Type: {}", dto.getTypeReclamation());

        // Récupérer le username si l'utilisateur est connecté
        String username = (authentication != null) ? authentication.getName() : null;

        ReclamationResponseDto created = reclamationService.creerReclamation(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Récupérer toutes les réclamations (ADMIN/EMPLOYE)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<ReclamationResponseDto>> getAllReclamations() {
        log.info("Récupération de toutes les réclamations");
        List<ReclamationResponseDto> reclamations = reclamationService.getAllReclamations();
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer une réclamation par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    public ResponseEntity<ReclamationResponseDto> getReclamationById(@PathVariable Long id) {
        log.info("Récupération réclamation ID: {}", id);
        ReclamationResponseDto reclamation = reclamationService.getReclamationById(id);
        return ResponseEntity.ok(reclamation);
    }

    /**
     * Récupérer une réclamation par code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ReclamationResponseDto> getReclamationByCode(@PathVariable String code) {
        log.info("Récupération réclamation Code: {}", code);
        ReclamationResponseDto reclamation = reclamationService.getReclamationByCode(code);
        return ResponseEntity.ok(reclamation);
    }

    /**
     * Récupérer les réclamations de l'utilisateur connecté
     */
    @GetMapping("/mes-reclamations")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYE','MANAGER')")
    public ResponseEntity<List<ReclamationResponseDto>> getMesReclamations(Authentication authentication) {
        log.info("Récupération des réclamations de l'utilisateur connecté");
        // Récupérer l'ID de l'utilisateur connecté depuis le token
        Long idUtilisateur = authenticationFacade.getCurrentUserId();
        List<ReclamationResponseDto> reclamations = reclamationService.getReclamationsByUtilisateur(idUtilisateur);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer les réclamations d'un utilisateur spécifique (ADMIN et managers )
     */
    @GetMapping("/utilisateur/{idUtilisateur}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ReclamationResponseDto>> getReclamationsByUtilisateur(
            @PathVariable Long idUtilisateur) {

        log.info("Récupération réclamations utilisateur ID: {}", idUtilisateur);
        List<ReclamationResponseDto> reclamations =
                reclamationService.getReclamationsByUtilisateur(idUtilisateur);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer les réclamations par email (pour visiteurs)
     */
    @GetMapping("/email")
    public ResponseEntity<List<ReclamationResponseDto>> getReclamationsByEmail(
            @RequestParam String email) {

        log.info("Récupération réclamations email: {}", email);
        List<ReclamationResponseDto> reclamations =
                reclamationService.getReclamationsByEmail(email);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer les réclamations par statut
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<ReclamationResponseDto>> getReclamationsByStatut(
            @PathVariable StatutReclamation statut) {

        log.info("Récupération réclamations statut: {}", statut);
        List<ReclamationResponseDto> reclamations =
                reclamationService.getReclamationsByStatut(statut);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer les réclamations par type
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<ReclamationResponseDto>> getReclamationsByType(
            @PathVariable TypeReclamation type) {

        log.info("Récupération réclamations type: {}", type);
        List<ReclamationResponseDto> reclamations =
                reclamationService.getReclamationsByType(type);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer les réclamations par priorité
     */
    @GetMapping("/priorite/{priorite}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<ReclamationResponseDto>> getReclamationsByPriorite(
            @PathVariable PrioriteReclamation priorite) {

        log.info("📋 Récupération réclamations priorité: {}", priorite);
        List<ReclamationResponseDto> reclamations =
                reclamationService.getReclamationsByPriorite(priorite);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer les réclamations liées à une réservation
     */
    @GetMapping("/reservation/{idReservation}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    public ResponseEntity<List<ReclamationResponseDto>> getReclamationsByReservation(
            @PathVariable Long idReservation) {

        log.info("Récupération réclamations réservation ID: {}", idReservation);
        List<ReclamationResponseDto> reclamations =
                reclamationService.getReclamationsByReservation(idReservation);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Classer une réclamation (priorité + statut)
     */
    @PatchMapping("/{id}/classer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<ReclamationResponseDto> classerReclamation(
            @PathVariable Long id,
            @Valid @RequestBody ClasserReclamationDto dto,
            Authentication authentication) {

        log.info("Classification réclamation ID: {}", id);
        ReclamationResponseDto updated =
                reclamationService.classerReclamation(id, dto, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * Traiter/Répondre à une réclamation
     */
    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<ReclamationResponseDto> traiterReclamation(
            @PathVariable Long id,
            @Valid @RequestBody TraiterReclamationDto dto,
            Authentication authentication) {

        log.info("Traitement réclamation ID: {}", id);
        ReclamationResponseDto updated =
                reclamationService.traiterReclamation(id, dto, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * Recherche multi-critères
     */
    @GetMapping("/rechercher")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<ReclamationResponseDto>> rechercherReclamations(
            @RequestParam(required = false) StatutReclamation statut,
            @RequestParam(required = false) TypeReclamation type,
            @RequestParam(required = false) PrioriteReclamation priorite,
            @RequestParam(required = false) Long idUtilisateur) {

        log.info("Recherche réclamations - Statut: {}, Type: {}, Priorité: {}, Utilisateur: {}",
                statut, type, priorite, idUtilisateur);

        List<ReclamationResponseDto> reclamations =
                reclamationService.rechercherReclamations(statut, type, priorite, idUtilisateur);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Récupérer les réclamations sur une période
     */
    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ReclamationResponseDto>> getReclamationsByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        log.info("Récupération réclamations période: {} - {}", debut, fin);
        List<ReclamationResponseDto> reclamations =
                reclamationService.getReclamationsByPeriode(debut, fin);
        return ResponseEntity.ok(reclamations);
    }

    /**
     * Statistiques des réclamations
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getStatistiques() {
        log.info("Récupération des statistiques des réclamations");

        Map<String, Object> stats = new HashMap<>();
        stats.put("enAttente", reclamationService.countByStatut(StatutReclamation.EN_ATTENTE));
        stats.put("enCours", reclamationService.countByStatut(StatutReclamation.EN_COURS));
        stats.put("resolu", reclamationService.countByStatut(StatutReclamation.RESOLU));
        stats.put("urgentesNonTraitees", reclamationService.countReclamationsUrgentesNonTraitees());

        return ResponseEntity.ok(stats);
    }

    /**
     * Supprimer une réclamation (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReclamation(@PathVariable Long id) {
        log.warn("Suppression réclamation ID: {}", id);
        reclamationService.deleteReclamation(id);
        return ResponseEntity.noContent().build();
    }
}