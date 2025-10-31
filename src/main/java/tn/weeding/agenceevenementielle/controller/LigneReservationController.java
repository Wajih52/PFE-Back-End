package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.services.LigneReservationServiceInterface;

import java.util.List;
import java.util.Map;

/**
 * ==========================================
 * CONTRÔLEUR REST POUR LA GESTION DES LIGNES DE RÉSERVATION
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 *
 * Endpoints disponibles:
 * - POST   /api/lignes-reservation/{idReservation}           : Ajouter une ligne à une réservation
 * - GET    /api/lignes-reservation/{id}                      : Récupérer une ligne par ID
 * - GET    /api/lignes-reservation/reservation/{idRes}       : Lignes d'une réservation
 * - GET    /api/lignes-reservation/produit/{idProduit}       : Lignes contenant un produit
 * - GET    /api/lignes-reservation/statut/{statut}           : Lignes par statut de livraison
 * - PUT    /api/lignes-reservation/{id}                      : Modifier une ligne
 * - PATCH  /api/lignes-reservation/{id}/statut               : Changer le statut
 * - DELETE /api/lignes-reservation/{id}                      : Supprimer une ligne
 * - GET    /api/lignes-reservation/reservation/{id}/montant  : Montant total
 * - GET    /api/lignes-reservation/reservation/{id}/stats    : Statistiques
 */
@RestController
@RequestMapping("/api/lignes-reservation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Lignes de Réservation", description = "APIs pour gérer les produits dans le panier/réservation")
@CrossOrigin(origins = "*")
public class LigneReservationController {

    private final LigneReservationServiceInterface ligneReservationService;
    private final AuthenticationFacade authenticationFacade;

    // ============================================
    // PARTIE 1: CRÉATION DE LIGNES
    // ============================================

    /**
     * 🛒 Ajouter un produit au panier/réservation
     * Le CLIENT ou l'ADMIN peut ajouter des produits à une réservation
     */
    @PostMapping("/{idReservation}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Ajouter une ligne à une réservation",
            description = "Ajouter un produit au panier avec vérification automatique de disponibilité")
    public ResponseEntity<LigneReservationResponseDto> ajouterLigneReservation(
            @PathVariable Long idReservation,
            @Valid @RequestBody LigneReservationRequestDto ligneRequest) {

        log.info("🛒 Nouvelle demande d'ajout de ligne à la réservation ID: {}", idReservation);

        String username = authenticationFacade.getAuthentication().getName();
        LigneReservationResponseDto ligne = ligneReservationService.creerLigneReservation(
                ligneRequest, idReservation, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(ligne);
    }

    // ============================================
    // PARTIE 2: CONSULTATION DES LIGNES
    // ============================================

    /**
     * 📋 Récupérer une ligne par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer une ligne par ID",
            description = "Obtenir les détails d'une ligne de réservation spécifique")
    public ResponseEntity<LigneReservationResponseDto> getLigneById(@PathVariable Long id) {
        log.info("🔍 Recherche de la ligne ID: {}", id);
        LigneReservationResponseDto ligne = ligneReservationService.getLigneReservationById(id);
        return ResponseEntity.ok(ligne);
    }

    /**
     * 📋 Récupérer toutes les lignes d'une réservation
     */
    @GetMapping("/reservation/{idReservation}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Lignes d'une réservation",
            description = "Récupérer tous les produits d'une réservation (le panier)")
    public ResponseEntity<List<LigneReservationResponseDto>> getLignesByReservation(
            @PathVariable Long idReservation) {

        log.info("📋 Récupération des lignes de la réservation ID: {}", idReservation);
        List<LigneReservationResponseDto> lignes =
                ligneReservationService.getLignesReservationByReservation(idReservation);

        return ResponseEntity.ok(lignes);
    }

    /**
     * 📋 Récupérer les lignes contenant un produit spécifique
     */
    @GetMapping("/produit/{idProduit}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Lignes par produit (ADMIN)",
            description = "Voir toutes les réservations contenant un produit spécifique")
    public ResponseEntity<List<LigneReservationResponseDto>> getLignesByProduit(
            @PathVariable Long idProduit) {

        log.info("📋 Recherche des lignes du produit ID: {}", idProduit);
        List<LigneReservationResponseDto> lignes =
                ligneReservationService.getLignesReservationByProduit(idProduit);

        return ResponseEntity.ok(lignes);
    }

    /**
     * 📋 Récupérer les lignes par statut de livraison
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Lignes par statut (ADMIN)",
            description = "Filtrer les lignes par statut de livraison (EN_ATTENTE, EN_LIVRAISON, LIVRE, etc.)")
    public ResponseEntity<List<LigneReservationResponseDto>> getLignesByStatut(
            @PathVariable StatutLivraison statut) {

        log.info("📋 Recherche des lignes avec statut: {}", statut);
        List<LigneReservationResponseDto> lignes =
                ligneReservationService.getLignesReservationByStatut(statut);

        return ResponseEntity.ok(lignes);
    }

    // ============================================
    // PARTIE 3: MODIFICATION DES LIGNES
    // ============================================

    /**
     * ✏️ Modifier une ligne de réservation
     * Permet de changer la quantité, les dates ou les observations
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Modifier une ligne",
            description = "Modifier la quantité, les dates ou les observations d'une ligne")
    public ResponseEntity<LigneReservationResponseDto> modifierLigne(
            @PathVariable Long id,
            @Valid @RequestBody LigneReservationRequestDto ligneRequest) {

        log.info("✏️ Modification de la ligne ID: {}", id);

        String username = authenticationFacade.getAuthentication().getName();
        LigneReservationResponseDto ligne = ligneReservationService.modifierLigneReservation(
                id, ligneRequest, username);

        return ResponseEntity.ok(ligne);
    }

    /**
     * 🔄 Mettre à jour le statut de livraison d'une ligne
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Changer le statut de livraison (ADMIN)",
            description = "Mettre à jour le statut de livraison d'une ligne (EN_ATTENTE, EN_LIVRAISON, LIVRE, etc.)")
    public ResponseEntity<LigneReservationResponseDto> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutLivraison nouveauStatut) {

        log.info("🔄 Changement de statut pour la ligne ID: {} -> {}", id, nouveauStatut);

        LigneReservationResponseDto ligne = ligneReservationService.updateStatutLivraison(id, nouveauStatut);

        return ResponseEntity.ok(ligne);
    }

    // ============================================
    // PARTIE 4: SUPPRESSION DES LIGNES
    // ============================================

    /**
     * ❌ Supprimer une ligne de réservation
     * Libère automatiquement le stock et les instances
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Supprimer une ligne",
            description = "Retirer un produit du panier avec libération automatique du stock")
    public ResponseEntity<Map<String, String>> supprimerLigne(@PathVariable Long id) {
        log.info("🗑️ Suppression de la ligne ID: {}", id);

        String username = authenticationFacade.getAuthentication().getName();
        ligneReservationService.supprimerLigneReservation(id, username);

        return ResponseEntity.ok(Map.of(
                "message", "Ligne supprimée avec succès",
                "detail", "Le stock a été automatiquement libéré"
        ));
    }

    // ============================================
    // PARTIE 5: STATISTIQUES ET CALCULS
    // ============================================

    /**
     * 💰 Calculer le montant total d'une réservation
     */
    @GetMapping("/reservation/{idReservation}/montant")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Calculer le montant total",
            description = "Obtenir le montant total de tous les produits d'une réservation")
    public ResponseEntity<Map<String, Double>> calculerMontantTotal(
            @PathVariable Long idReservation) {

        log.info("💰 Calcul du montant pour la réservation ID: {}", idReservation);

        Double montantTotal = ligneReservationService.calculerMontantTotalReservation(idReservation);

        return ResponseEntity.ok(Map.of("montantTotal", montantTotal));
    }

    /**
     * 📊 Obtenir les statistiques d'une réservation
     */
    @GetMapping("/reservation/{idReservation}/statistiques")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Statistiques de la réservation",
            description = "Obtenir les stats: nombre de lignes, produits, montant, catégories, etc.")
    public ResponseEntity<Map<String, Object>> getStatistiques(
            @PathVariable Long idReservation) {

        log.info("📊 Statistiques pour la réservation ID: {}", idReservation);

        Map<String, Object> stats = ligneReservationService.getStatistiquesReservation(idReservation);

        return ResponseEntity.ok(stats);
    }

    // ============================================
    // PARTIE 6: ENDPOINTS DE VÉRIFICATION (UTILITAIRES)
    // ============================================

    /**
     * ✅ Vérifier si une ligne existe
     */
    @GetMapping("/{id}/exists")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Vérifier l'existence d'une ligne",
            description = "Endpoint utilitaire pour vérifier si une ligne existe")
    public ResponseEntity<Map<String, Boolean>> verifierExistence(@PathVariable Long id) {
        try {
            ligneReservationService.getLigneReservationById(id);
            return ResponseEntity.ok(Map.of("exists", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
    }
}