package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.statistiques.DashboardStatistiquesDto;
import tn.weeding.agenceevenementielle.services.DashboardStatistiquesService;

import java.time.LocalDate;



@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistiques & Analytics", description = "APIs pour le dashboard de statistiques")
@CrossOrigin(origins = "*")
public class StatistiquesController {

    private final DashboardStatistiquesService dashboardStatistiquesService;

    /**
     * Obtenir toutes les statistiques du dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Statistiques complètes du dashboard",
            description = "Retourne tous les KPIs, graphiques et métriques pour le dashboard principal"
    )
    public ResponseEntity<DashboardStatistiquesDto> getDashboardStatistiques() {
        log.info("statistiques dashboard globales");

        DashboardStatistiquesDto stats = dashboardStatistiquesService.getDashboardStatistiques();

        log.info("Statistiques calculées: CA total = {}, {} réservations",
                stats.getChiffreAffairesTotal(),
                stats.getNombreTotalReservations());

        return ResponseEntity.ok(stats);
    }

    /**
     * Obtenir les statistiques pour une période spécifique
     */
    @GetMapping("/dashboard/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Statistiques par période",
            description = "Retourne les statistiques pour une période personnalisée"
    )
    public ResponseEntity<DashboardStatistiquesDto> getDashboardStatistiquesPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        log.info("statistiques pour période {} - {}", dateDebut, dateFin);

        DashboardStatistiquesDto stats = dashboardStatistiquesService
                .getDashboardStatistiquesPeriode(dateDebut, dateFin);

        log.info("Statistiques période calculées: CA = {}",
                stats.getChiffreAffairesTotal());

        return ResponseEntity.ok(stats);
    }

    /**
     * Vérifier la santé de l'API statistiques
     * GET /api/statistiques/health
     */
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Santé de l'API statistiques",
            description = "Endpoint de test pour vérifier que l'API statistiques fonctionne"
    )
    public ResponseEntity<String> healthCheck() {
        log.info("Health check statistiques");
        return ResponseEntity.ok("API Statistiques opérationnelle ");
    }
}