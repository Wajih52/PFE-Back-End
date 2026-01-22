package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.calendrier.CalendrierEventDto;
import tn.weeding.agenceevenementielle.dto.calendrier.CalendrierFiltreDto;
import tn.weeding.agenceevenementielle.dto.calendrier.CalendrierStatistiquesDto;
import tn.weeding.agenceevenementielle.services.Calendrier.CalendrierService;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/calendrier")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Calendrier", description = "APIs pour la vue calendrier (réservations + livraisons)")
@CrossOrigin(origins = "*")
public class CalendrierController {

    private final CalendrierService calendrierService;

    /**
     * Récupérer tous les événements avec filtres
     */
    @PostMapping("/evenements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(
            summary = "Récupérer les événements du calendrier",
            description = "Retourne les réservations et livraisons selon les filtres appliqués. "

    )
    public ResponseEntity<List<CalendrierEventDto>> getEvenements(
            @Valid @RequestBody CalendrierFiltreDto filtres
    ) {
        log.info("les événements avec filtres : {}", filtres);

        List<CalendrierEventDto> evenements = calendrierService.getEvenements(filtres);

        log.info("{} événements retournés", evenements.size());
        return ResponseEntity.ok(evenements);
    }

    /**
     * Obtenir les statistiques pour une période
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Statistiques du calendrier",
            description = "Nombre de réservations, livraisons, montant total, taux de paiement pour une période donnée"
    )
    public ResponseEntity<CalendrierStatistiquesDto> getStatistiques(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        log.info(" statistique calendrier - {} à {}", dateDebut, dateFin);

        CalendrierStatistiquesDto stats = calendrierService.getStatistiques(dateDebut, dateFin);

        return ResponseEntity.ok(stats);
    }

    /**
     *  Événements d'un mois spécifique
     */
    @GetMapping("/mois")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(
            summary = "Événements d'un mois",
            description = "Récupère tous les événements d'un mois donné (simplifié)"
    )
    public ResponseEntity<List<CalendrierEventDto>> getEvenementsMois(
            @RequestParam int annee,
            @RequestParam int mois
    ) {
        log.info("Événements d'un mois spécifique - {}/{}", mois, annee);

        // Calculer le premier et dernier jour du mois
        LocalDate dateDebut = LocalDate.of(annee, mois, 1);
        LocalDate dateFin = dateDebut.withDayOfMonth(dateDebut.lengthOfMonth());

        CalendrierFiltreDto filtres = CalendrierFiltreDto.builder()
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .inclureReservations(true)
                .inclureLivraisons(true)
                .build();

        List<CalendrierEventDto> evenements = calendrierService.getEvenements(filtres);

        log.info("{} événements pour {}/{}", evenements.size(), mois, annee);
        return ResponseEntity.ok(evenements);
    }
}