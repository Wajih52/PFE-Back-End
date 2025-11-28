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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.avis.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutAvis;
import tn.weeding.agenceevenementielle.services.AvisServiceInterface;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/avis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Évaluations", description = "APIs pour gérer les évaluations des produits")
@CrossOrigin(origins = "*")
public class AvisController {

    private final AvisServiceInterface avisService;

    // ============================================
    // ENDPOINTS CLIENT
    // ============================================

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Créer un avis",
            description = "Le client crée un avis pour un produit d'une réservation terminée")
    public ResponseEntity<AvisResponseDto> creerAvis(
            @Valid @RequestBody AvisCreateDto dto,
            Authentication authentication) {
        log.info("📝 Requête de création d'avis par {}", authentication.getName());
        AvisResponseDto response = avisService.creerAvis(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Modifier un avis",
            description = "Le client modifie son avis (uniquement si EN_ATTENTE)")
    public ResponseEntity<AvisResponseDto> modifierAvis(
            @Valid @RequestBody AvisUpdateDto dto,
            Authentication authentication) {
        log.info("✏️ Requête de modification d'avis {} par {}",
                dto.getIdAvis(), authentication.getName());
        AvisResponseDto response = avisService.modifierAvis(dto, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Supprimer un avis",
            description = "Le client supprime son avis (soft delete)")
    public ResponseEntity<Void> supprimerAvis(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("🗑️ Requête de suppression d'avis {} par {}", id, authentication.getName());
        avisService.supprimerAvis(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mes-avis")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Mes avis",
            description = "Le client consulte tous ses avis")
    public ResponseEntity<List<AvisResponseDto>> getMesAvis(Authentication authentication) {
        log.info("📋 Requête des avis de {}", authentication.getName());
        log.info("authentication get name == > {}",authentication.getName());
        List<AvisResponseDto> avis = avisService.getMesAvis(authentication.getName());
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/peut-evaluer")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Vérifier si peut évaluer",
            description = "Vérifier si le client peut évaluer un produit d'une réservation")
    public ResponseEntity<Boolean> peutEvaluerProduit(
            @RequestParam Long idReservation,
            @RequestParam Long idProduit,
            Authentication authentication) {
        log.info("🔍 Vérification si {} peut évaluer le produit {}",
                authentication.getName(), idProduit);
        Boolean peut = avisService.peutEvaluerProduit(
                idReservation, idProduit, authentication.getName()
        );
        return ResponseEntity.ok(peut);
    }

    // ============================================
    // ENDPOINTS PUBLICS
    // ============================================

    @GetMapping("/produit/{idProduit}")
    @Operation(summary = "Avis d'un produit",
            description = "Obtenir tous les avis approuvés d'un produit (public)")
    public ResponseEntity<List<AvisResponseDto>> getAvisProduit(@PathVariable Long idProduit) {
        log.info("📋 Requête des avis du produit {}", idProduit);
        List<AvisResponseDto> avis = avisService.getAvisApprouvesByProduit(idProduit);
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/produit/{idProduit}/statistiques")
    @Operation(summary = "Statistiques d'un produit",
            description = "Obtenir les statistiques d'avis d'un produit")
    public ResponseEntity<StatistiquesAvisDto> getStatistiquesProduit(@PathVariable Long idProduit) {
        log.info("📊 Requête des statistiques d'avis du produit {}", idProduit);
        StatistiquesAvisDto stats = avisService.getStatistiquesAvisProduit(idProduit);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/note/{note}")
    @Operation(summary = "Rechercher par note",
            description = "Rechercher les avis avec une note spécifique")
    public ResponseEntity<List<AvisResponseDto>> getAvisByNote(@PathVariable Integer note) {
        log.info("🔍 Recherche des avis avec la note {}", note);
        List<AvisResponseDto> avis = avisService.getAvisByNote(note);
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/recherche")
    @Operation(summary = "Rechercher par mot-clé",
            description = "Rechercher des avis par mot-clé dans les commentaires")
    public ResponseEntity<List<AvisResponseDto>> rechercherAvis(@RequestParam String keyword) {
        log.info("🔍 Recherche d'avis avec le mot-clé : {}", keyword);
        List<AvisResponseDto> avis = avisService.searchAvisByKeyword(keyword);
        return ResponseEntity.ok(avis);
    }

    // ============================================
    // ENDPOINTS ADMIN - MODÉRATION
    // ============================================

    @GetMapping("/admin/tous")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Tous les avis",
            description = "Obtenir tous les avis (tous statuts)")
    public ResponseEntity<List<AvisResponseDto>> getAllAvis() {
        log.info("📋 [ADMIN] Requête de tous les avis");
        List<AvisResponseDto> avis = avisService.getAllAvis();
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/admin/en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Avis en attente",
            description = "Obtenir les avis en attente de modération")
    public ResponseEntity<List<AvisResponseDto>> getAvisEnAttente() {
        log.info("📋 [ADMIN] Requête des avis en attente");
        List<AvisResponseDto> avis = avisService.getAvisEnAttente();
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/admin/count-en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Nombre d'avis en attente",
            description = "Obtenir le nombre d'avis en attente de modération")
    public ResponseEntity<Long> getNombreAvisEnAttente() {
        log.info("📊 [ADMIN] Requête du nombre d'avis en attente");
        Long nombre = avisService.getNombreAvisEnAttente();
        return ResponseEntity.ok(nombre);
    }

    @PutMapping("/admin/moderer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Modérer un avis",
            description = "Approuver ou rejeter un avis")
    public ResponseEntity<AvisResponseDto> modererAvis(
            @Valid @RequestBody AvisModerationDto dto,
            Authentication authentication) {
        log.info("⚖️ [ADMIN] Modération de l'avis {} par {}",
                dto.getIdAvis(), authentication.getName());
        AvisResponseDto response = avisService.modererAvis(dto, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Supprimer définitivement",
            description = "Supprimer définitivement un avis (hard delete)")
    public ResponseEntity<Void> supprimerAvisDefinitivement(@PathVariable Long id) {
        log.info("🗑️ [ADMIN] Suppression définitive de l'avis {}", id);
        avisService.supprimerAvisDefinitivement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Avis par statut",
            description = "Obtenir les avis avec un statut spécifique")
    public ResponseEntity<List<AvisResponseDto>> getAvisByStatut(@PathVariable StatutAvis statut) {
        log.info("📋 [ADMIN] Requête des avis avec le statut {}", statut);
        List<AvisResponseDto> avis = avisService.getAvisByStatut(statut);
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/admin/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Avis d'un client",
            description = "Obtenir tous les avis d'un client spécifique")
    public ResponseEntity<List<AvisResponseDto>> getAvisByClient(@PathVariable Long clientId) {
        log.info("📋 [ADMIN] Requête des avis du client {}", clientId);
        List<AvisResponseDto> avis = avisService.getAvisByClient(clientId);
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/admin/produit/{idProduit}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Tous les avis d'un produit",
            description = "Obtenir tous les avis d'un produit (tous statuts)")
    public ResponseEntity<List<AvisResponseDto>> getAllAvisByProduit(@PathVariable Long idProduit) {
        log.info("📋 [ADMIN] Requête de tous les avis du produit {}", idProduit);
        List<AvisResponseDto> avis = avisService.getAllAvisByProduit(idProduit);
        return ResponseEntity.ok(avis);
    }

    @GetMapping("/admin/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Avis par période",
            description = "Obtenir les avis créés dans une période donnée")
    public ResponseEntity<List<AvisResponseDto>> getAvisByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        log.info("📋 [ADMIN] Requête des avis entre {} et {}", debut, fin);
        List<AvisResponseDto> avis = avisService.getAvisByPeriode(debut, fin);
        return ResponseEntity.ok(avis);
    }


    @GetMapping("/admin/top-produits")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "[ADMIN] Top produits par note",
            description = "Obtenir les produits les mieux notés avec un minimum d'avis")
    public ResponseEntity<List<Object[]>> getTopProduitsParNote(
            @RequestParam(defaultValue = "3") Long minAvis) {
        log.info("🏆 [ADMIN] Requête des top produits (min {} avis)", minAvis);
        List<Object[]> topProduits = avisService.getTopProduitsParNote(minAvis);
        return ResponseEntity.ok(topProduits);
    }
}