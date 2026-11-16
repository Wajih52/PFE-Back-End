package tn.weeding.agenceevenementielle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.dto.conge.DemandeCongeRequestDto;
import tn.weeding.agenceevenementielle.dto.conge.DemandeCongeResponseDto;
import tn.weeding.agenceevenementielle.services.CongeService;

import java.util.List;

@RestController
@RequestMapping("/api/conges")
@RequiredArgsConstructor
public class CongeController {

    private final CongeService congeService;
    private final AuthenticationFacade authenticationFacade;

    // Employé : Demander un congé
    @PostMapping("/demander")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<DemandeCongeResponseDto> demanderConge(@Valid @RequestBody DemandeCongeRequestDto dto) {
        String username = authenticationFacade.getAuthentication().getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(congeService.demanderConge(dto, username));
    }

    // Employé : Voir son historique de demandes
    @GetMapping("/mes-demandes")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<DemandeCongeResponseDto>> getMesDemandes() {
        String username = authenticationFacade.getAuthentication().getName();
        return ResponseEntity.ok(congeService.getMesDemandes(username));
    }

    // Admin : Voir toutes les demandes et l'historique global
    @GetMapping("/toutes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<DemandeCongeResponseDto>> getToutesLesDemandes() {
        return ResponseEntity.ok(congeService.getToutesLesDemandes());
    }

    // Admin : Accepter ou Refuser une demande (?accepter=true ou false)
    @PatchMapping("/{idDemande}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DemandeCongeResponseDto> traiterDemande(
            @PathVariable Long idDemande,
            @RequestParam boolean accepter) {
        return ResponseEntity.ok(congeService.traiterDemande(idDemande, accepter));
    }

    // Admin : Modifier le quota annuel d'un employé
    @PatchMapping("/employe/{idEmploye}/solde")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> modifierSolde(
            @PathVariable Long idEmploye,
            @RequestParam Integer nouveauSolde) {
        congeService.modifierSoldeEmploye(idEmploye, nouveauSolde);
        return ResponseEntity.ok().build();
    }
}