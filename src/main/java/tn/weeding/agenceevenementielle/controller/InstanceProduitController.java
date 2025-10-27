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
import tn.weeding.agenceevenementielle.dto.InstanceProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.InstanceProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.StatutInstance;
import tn.weeding.agenceevenementielle.services.InstanceProduitServiceInterface;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur REST pour la gestion des instances de produits avec référence
 * Sprint 3 - Gestion des produits et du stock
 */
@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Instances", description = "APIs pour la gestion des instances de produits avec référence")
@CrossOrigin(origins = "*")
public class InstanceProduitController {

    private final InstanceProduitServiceInterface instanceService;
    private final AuthenticationFacade authenticationFacade;

    // ============ CRUD DE BASE ============

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Créer une instance", description = "Ajoute une nouvelle instance pour un produit avec référence")
    public ResponseEntity<InstanceProduitResponseDto> creerInstance(
            @Valid @RequestBody InstanceProduitRequestDto dto) {
        log.info("Création d'une nouvelle instance pour le produit ID: {}", dto.getIdProduit());
        String username = authenticationFacade.getAuthentication().getName();
        InstanceProduitResponseDto instance = instanceService.creerInstance(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(instance);
    }

    @PostMapping("/lot")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Créer des instances en lot",
            description = "Crée plusieurs instances avec numérotation automatique")
    public ResponseEntity<List<InstanceProduitResponseDto>> creerInstancesEnLot(
            @RequestParam Long idProduit,
            @RequestParam Integer quantite,
            @RequestParam(required = false, defaultValue = "INST") String prefixeNumeroSerie) {
        log.info("Création de {} instances en lot pour le produit ID: {}", quantite, idProduit);
        String username = authenticationFacade.getAuthentication().getName();
        List<InstanceProduitResponseDto> instances =
                instanceService.creerInstancesEnLot(idProduit, quantite, prefixeNumeroSerie, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(instances);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Modifier une instance", description = "Modifie les informations d'une instance existante")
    public ResponseEntity<InstanceProduitResponseDto> modifierInstance(
            @PathVariable Long id,
            @Valid @RequestBody InstanceProduitRequestDto dto) {
        log.info("Modification de l'instance ID: {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        InstanceProduitResponseDto instance = instanceService.modifierInstance(id, dto, username);
        return ResponseEntity.ok(instance);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une instance", description = "Supprime définitivement une instance")
    public ResponseEntity<Void> supprimerInstance(@PathVariable Long id) {
        log.info("Suppression de l'instance ID: {}", id);
        instanceService.supprimerInstance(id);
        return ResponseEntity.noContent().build();
    }

    // ============ CONSULTATION ============

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une instance par ID", description = "Récupère les détails d'une instance")
    public ResponseEntity<InstanceProduitResponseDto> getInstanceById(@PathVariable Long id) {
        log.info("Récupération de l'instance ID: {}", id);
        InstanceProduitResponseDto instance = instanceService.getInstanceById(id);
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/numero-serie/{numeroSerie}")
    @Operation(summary = "Rechercher par numéro de série",
            description = "Trouve une instance par son numéro de série unique")
    public ResponseEntity<InstanceProduitResponseDto> getInstanceByNumeroSerie(
            @PathVariable String numeroSerie) {
        log.info("Recherche de l'instance avec numéro de série: {}", numeroSerie);
        InstanceProduitResponseDto instance = instanceService.getInstanceByNumeroSerie(numeroSerie);
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/produit/{idProduit}")
    @Operation(summary = "Lister les instances d'un produit",
            description = "Récupère toutes les instances d'un produit")
    public ResponseEntity<List<InstanceProduitResponseDto>> getInstancesByProduit(
            @PathVariable Long idProduit) {
        log.info("Récupération des instances du produit ID: {}", idProduit);
        List<InstanceProduitResponseDto> instances = instanceService.getInstancesByProduit(idProduit);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/produit/{idProduit}/disponibles")
    @Operation(summary = "Lister les instances disponibles",
            description = "Récupère uniquement les instances disponibles pour réservation")
    public ResponseEntity<List<InstanceProduitResponseDto>> getInstancesDisponibles(
            @PathVariable Long idProduit) {
        log.info("Récupération des instances disponibles du produit ID: {}", idProduit);
        List<InstanceProduitResponseDto> instances = instanceService.getInstancesDisponibles(idProduit);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Filtrer par statut", description = "Récupère toutes les instances d'un statut donné")
    public ResponseEntity<List<InstanceProduitResponseDto>> getInstancesByStatut(
            @PathVariable StatutInstance statut) {
        log.info("Récupération des instances avec statut: {}", statut);
        List<InstanceProduitResponseDto> instances = instanceService.getInstancesByStatut(statut);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/ligne-reservation/{idLigneReservation}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Instances d'une ligne de réservation",
            description = "Récupère les instances affectées à une ligne de réservation")
    public ResponseEntity<List<InstanceProduitResponseDto>> getInstancesByLigneReservation(
            @PathVariable Long idLigneReservation) {
        log.info("Récupération des instances de la ligne de réservation ID: {}", idLigneReservation);
        List<InstanceProduitResponseDto> instances =
                instanceService.getInstancesByLigneReservation(idLigneReservation);
        return ResponseEntity.ok(instances);
    }

    // ============ GESTION DES STATUTS ============

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Changer le statut", description = "Modifie le statut d'une instance")
    public ResponseEntity<InstanceProduitResponseDto> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutInstance statut) {
        log.info("Changement du statut de l'instance ID {} vers {}", id, statut);
        String username = authenticationFacade.getAuthentication().getName();
        InstanceProduitResponseDto instance = instanceService.changerStatut(id, statut, username);
        return ResponseEntity.ok(instance);
    }

    // ============ GESTION DE LA MAINTENANCE ============

    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Envoyer en maintenance",
            description = "Marque une instance comme étant en maintenance")
    public ResponseEntity<InstanceProduitResponseDto> envoyerEnMaintenance(
            @PathVariable Long id,
            @RequestParam LocalDate dateRetourPrevue) {
        log.info("Envoi en maintenance de l'instance ID: {}, retour prévu: {}", id, dateRetourPrevue);
        String username = authenticationFacade.getAuthentication().getName();
        InstanceProduitResponseDto instance =
                instanceService.envoyerEnMaintenance(id, dateRetourPrevue, username);
        return ResponseEntity.ok(instance);
    }

    @PostMapping("/{id}/maintenance/retour")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Retour de maintenance",
            description = "Marque une instance comme revenue de maintenance")
    public ResponseEntity<InstanceProduitResponseDto> retournerDeMaintenance(@PathVariable Long id) {
        log.info("Retour de maintenance de l'instance ID: {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        InstanceProduitResponseDto instance = instanceService.retournerDeMaintenance(id, username);
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/maintenance/necessaire")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Instances nécessitant maintenance",
            description = "Liste des instances dont la date de maintenance est dépassée")
    public ResponseEntity<List<InstanceProduitResponseDto>> getInstancesNecessitantMaintenance() {
        log.info("Récupération des instances nécessitant une maintenance");
        List<InstanceProduitResponseDto> instances = instanceService.getInstancesNecessitantMaintenance();
        return ResponseEntity.ok(instances);
    }
}