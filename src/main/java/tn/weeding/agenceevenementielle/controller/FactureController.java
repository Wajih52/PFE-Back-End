package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.facture.FactureResponseDto;
import tn.weeding.agenceevenementielle.dto.facture.GenererFactureRequestDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutFacture;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;
import tn.weeding.agenceevenementielle.services.FactureServiceInterface;

import java.util.List;

@RestController
@RequestMapping("/factures")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Factures", description = "Gestion des factures (Devis, Pro-forma, Finale)")
public class FactureController {

    private final FactureServiceInterface factureService;

    // ===== GÉNÉRATION =====

    @PostMapping("/generer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Générer une facture")
    public ResponseEntity<FactureResponseDto> genererFacture(
            @Valid @RequestBody GenererFactureRequestDto request,
            Authentication authentication) {
        String username = authentication.getName();
        FactureResponseDto facture = factureService.genererFacture(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(facture);
    }

    @PostMapping("/generer-auto/{idReservation}/{typeFacture}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Générer automatiquement une facture")
    public ResponseEntity<FactureResponseDto> genererFactureAutomatique(
            @PathVariable Long idReservation,
            @PathVariable TypeFacture typeFacture,
            Authentication authentication) {
        String username = authentication.getName();
        FactureResponseDto facture = factureService.genererFactureAutomatique(idReservation, typeFacture, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(facture);
    }

    // ===== CONSULTATION =====

    @GetMapping("/{idFacture}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Obtenir une facture par ID")
    public ResponseEntity<FactureResponseDto> getFactureById(@PathVariable Long idFacture) {
        return ResponseEntity.ok(factureService.getFactureById(idFacture));
    }

    @GetMapping("/numero/{numeroFacture}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Obtenir une facture par numéro")
    public ResponseEntity<FactureResponseDto> getFactureByNumero(@PathVariable String numeroFacture) {
        return ResponseEntity.ok(factureService.getFactureByNumero(numeroFacture));
    }

    @GetMapping("/reservation/{idReservation}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Obtenir toutes les factures d'une réservation")
    public ResponseEntity<List<FactureResponseDto>> getFacturesByReservation(@PathVariable Long idReservation) {
        return ResponseEntity.ok(factureService.getFacturesByReservation(idReservation));
    }

    @GetMapping("/client/{idClient}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE','CLIENT')")
    @Operation(summary = "Obtenir toutes les factures d'un client")
    public ResponseEntity<List<FactureResponseDto>> getFacturesByClient(@PathVariable Long idClient) {
        return ResponseEntity.ok(factureService.getFacturesByClient(idClient));
    }

    @GetMapping("/toutes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir toutes les factures")
    public ResponseEntity<List<FactureResponseDto>> getAllFactures() {
        return ResponseEntity.ok(factureService.getAllFactures());
    }

    // ===== FILTRES =====

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Filtrer par statut")
    public ResponseEntity<List<FactureResponseDto>> getFacturesByStatut(@PathVariable StatutFacture statut) {
        return ResponseEntity.ok(factureService.getFacturesByStatut(statut));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Filtrer par type")
    public ResponseEntity<List<FactureResponseDto>> getFacturesByType(@PathVariable TypeFacture type) {
        return ResponseEntity.ok(factureService.getFacturesByType(type));
    }

    // ===== TÉLÉCHARGEMENT PDF =====

    @GetMapping("/{idFacture}/telecharger")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Télécharger le PDF d'une facture")
    public ResponseEntity<byte[]> telechargerPdfFacture(@PathVariable Long idFacture) {
        byte[] pdfBytes = factureService.telechargerPdfFacture(idFacture);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "facture_" + idFacture + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ===== MISE À JOUR =====

    @PutMapping("/{idFacture}/statut/{nouveauStatut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Mettre à jour le statut d'une facture")
    public ResponseEntity<FactureResponseDto> updateStatutFacture(
            @PathVariable Long idFacture,
            @PathVariable StatutFacture nouveauStatut,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(factureService.updateStatutFacture(idFacture, nouveauStatut, username));
    }

    @PostMapping("/{idFacture}/regenerer-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Régénérer le PDF d'une facture")
    public ResponseEntity<FactureResponseDto> regenererPdfFacture(
            @PathVariable Long idFacture,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(factureService.regenererPdfFacture(idFacture, username));
    }
}