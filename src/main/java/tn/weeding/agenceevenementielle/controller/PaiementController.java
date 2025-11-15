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
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.dto.paiement.PaiementRequestDto;
import tn.weeding.agenceevenementielle.dto.paiement.PaiementResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutPaiement;
import tn.weeding.agenceevenementielle.services.PaiementServiceInterface;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Paiements")
@CrossOrigin(origins = "http://localhost:4200")
public class PaiementController {

    private final PaiementServiceInterface paiementService;
    private final AuthenticationFacade authenticationFacade;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLIENT')")
    @Operation(summary = "Créer un paiement")
    public ResponseEntity<PaiementResponseDto> creerPaiement(@Valid @RequestBody PaiementRequestDto paiementDto) {
        String username = authenticationFacade.getCurrentUserPseudo();
        log.info("📝 Requête de création de paiement par: {}", username);
        PaiementResponseDto paiement = paiementService.creerPaiement(paiementDto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(paiement);
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Valider un paiement")
    public ResponseEntity<PaiementResponseDto> validerPaiement(@PathVariable Long id) {
        String username = authenticationFacade.getCurrentUserPseudo();
        log.info("✅ Validation du paiement ID: {} par {}", id, username);
        PaiementResponseDto paiement = paiementService.validerPaiement(id, username);
        return ResponseEntity.ok(paiement);
    }

    @PutMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Refuser un paiement")
    public ResponseEntity<PaiementResponseDto> refuserPaiement(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String motifRefus = body.get("motifRefus");
        if (motifRefus == null || motifRefus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String username = authenticationFacade.getCurrentUserPseudo();
        log.info("❌ Refus du paiement ID: {} par {}", id, username);
        PaiementResponseDto paiement = paiementService.refuserPaiement(id, motifRefus, username);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Récupérer un paiement par ID")
    public ResponseEntity<PaiementResponseDto> getPaiementById(@PathVariable Long id) {
        PaiementResponseDto paiement = paiementService.getPaiementById(id);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Récupérer un paiement par code")
    public ResponseEntity<PaiementResponseDto> getPaiementByCode(@PathVariable String code) {
        PaiementResponseDto paiement = paiementService.getPaiementByCode(code);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping("/reservation/{idReservation}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Paiements d'une réservation")
    public ResponseEntity<List<PaiementResponseDto>> getPaiementsByReservation(@PathVariable Long idReservation) {
        List<PaiementResponseDto> paiements = paiementService.getPaiementsByReservation(idReservation);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/mes-paiements")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Mes paiements (CLIENT)")
    public ResponseEntity<List<PaiementResponseDto>> getMesPaiements() {
        Long idUtilisateur = authenticationFacade.getCurrentUserId();
        List<PaiementResponseDto> paiements = paiementService.getPaiementsByClient(idUtilisateur);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tous les paiements (ADMIN)")
    public ResponseEntity<List<PaiementResponseDto>> getAllPaiements() {
        List<PaiementResponseDto> paiements = paiementService.getAllPaiements();
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Filtrer par statut")
    public ResponseEntity<List<PaiementResponseDto>> getPaiementsByStatut(@PathVariable StatutPaiement statut) {
        List<PaiementResponseDto> paiements = paiementService.getPaiementsByStatut(statut);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Paiements en attente")
    public ResponseEntity<List<PaiementResponseDto>> getPaiementsEnAttente() {
        List<PaiementResponseDto> paiements = paiementService.getPaiementsEnAttente();
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Paiements par période")
    public ResponseEntity<List<PaiementResponseDto>> getPaiementsByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        List<PaiementResponseDto> paiements = paiementService.getPaiementsByPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/reservation/{idReservation}/montant-paye")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Montant payé pour une réservation")
    public ResponseEntity<Map<String, Object>> getMontantPaye(@PathVariable Long idReservation) {
        Double montantPaye = paiementService.calculerMontantPaye(idReservation);
        Boolean paiementComplet = paiementService.isReservationPayeeCompletement(idReservation);
        Map<String, Object> response = Map.of("idReservation", idReservation, "montantPaye", montantPaye, "paiementComplet", paiementComplet);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservation/{idReservation}/est-complet")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Vérifier paiement complet")
    public ResponseEntity<Map<String, Boolean>> isReservationPayeeCompletement(@PathVariable Long idReservation) {
        Boolean paiementComplet = paiementService.isReservationPayeeCompletement(idReservation);
        Map<String, Boolean> response = Map.of("paiementComplet", paiementComplet);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Supprimer un paiement")
    public ResponseEntity<Map<String, String>> supprimerPaiement(@PathVariable Long id) {
        String username = authenticationFacade.getCurrentUserPseudo();
        paiementService.supprimerPaiement(id, username);
        Map<String, String> response = Map.of("message", "Paiement supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Statistiques des paiements")
    public ResponseEntity<Map<String, Object>> getStatistiquesPaiements() {
        List<PaiementResponseDto> tousLesPaiements = paiementService.getAllPaiements();

        long nbTotal = tousLesPaiements.size();
        long nbEnAttente = tousLesPaiements.stream().filter(p -> p.getStatutPaiement() == StatutPaiement.EN_ATTENTE).count();
        long nbValides = tousLesPaiements.stream().filter(p -> p.getStatutPaiement() == StatutPaiement.VALIDE).count();
        long nbRefuses = tousLesPaiements.stream().filter(p -> p.getStatutPaiement() == StatutPaiement.REFUSE).count();

        Double montantTotalValide = tousLesPaiements.stream()
                .filter(p -> p.getStatutPaiement() == StatutPaiement.VALIDE)
                .mapToDouble(PaiementResponseDto::getMontantPaiement)
                .sum();

        Map<String, Object> stats = Map.of(
                "nombreTotal", nbTotal,
                "nombreEnAttente", nbEnAttente,
                "nombreValides", nbValides,
                "nombreRefuses", nbRefuses,
                "montantTotalValide", montantTotalValide
        );

        return ResponseEntity.ok(stats);
    }
}