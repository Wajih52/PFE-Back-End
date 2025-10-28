package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.MouvementStockResponseDto;
import tn.weeding.agenceevenementielle.dto.ProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.ProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.Categorie;
import tn.weeding.agenceevenementielle.entities.TypeMouvement;
import tn.weeding.agenceevenementielle.entities.TypeProduit;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.services.ProduitServiceInterface;
import tn.weeding.agenceevenementielle.services.StockStatistiquesDto;

import java.util.Date;
import java.util.List;

/**
 * Contrôleur REST pour la gestion des produits et du stock
 * Sprint 3 : Gestion des produits et du stock
 */
@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Produits", description = "APIs pour la gestion des produits et du stock")
@CrossOrigin(origins = "*")
public class ProduitController {

    private final ProduitServiceInterface produitService;
    private final AuthenticationFacade authenticationFacade;

    // ============ GESTION DES PRODUITS ============

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Créer un nouveau produit", description = "Permet à l'admin ou employé d'ajouter un nouveau produit au catalogue")
    public ResponseEntity<ProduitResponseDto> creerProduit(@Valid @RequestBody ProduitRequestDto produitDto) {
        log.info("Requête de création de produit reçue");
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.creerProduit(produitDto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(produit);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Modifier un produit", description = "Permet de modifier les informations d'un produit existant")
    public ResponseEntity<ProduitResponseDto> modifierProduit(
            @PathVariable Long id,
            @Valid @RequestBody ProduitRequestDto produitDto) {
        log.info("Requête de modification du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.modifierProduit(id, produitDto, username);
        return ResponseEntity.ok(produit);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un produit", description = "Suppression logique d'un produit (réservé aux admins)")
    public ResponseEntity<Void> supprimerProduit(@PathVariable Long id) {
        log.info("Requête de suppression du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        produitService.supprimerProduit(id, username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/base")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un produit", description = "Suppression  d'un produit De la base de données(réservé aux admins)")
    public ResponseEntity<Void> supprimerProduitDeBase(@PathVariable Long id) {
        log.info("Requête de suppression de la base du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        produitService.supprimerProduitDeBase(id, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Désactiver un produit", description = "Met la quantité disponible à 0 sans supprimer le produit")
    public ResponseEntity<Void> desactiverProduit(@PathVariable Long id) {
        log.info("Requête de désactivation du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        produitService.desactiverProduit(id, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Réactiver un produit", description = "Réactive un produit désactivé avec une nouvelle quantité")
    public ResponseEntity<ProduitResponseDto> reactiverProduit(
            @PathVariable Long id,
            @RequestParam Integer quantite) {
        log.info("Requête de réactivation du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.reactiverProduit(id, quantite, username);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un produit par ID", description = "Récupère les détails d'un produit spécifique")
    public ResponseEntity<ProduitResponseDto> getProduitById(@PathVariable Long id) {
        log.info("Requête de récupération du produit ID : {}", id);
        ProduitResponseDto produit = produitService.getProduitById(id);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Obtenir un produit par code", description = "Récupère les détails d'un produit via son code unique")
    public ResponseEntity<ProduitResponseDto> getProduitByCode(@PathVariable String code) {
        log.info("Requête de récupération du produit avec code : {}", code);
        ProduitResponseDto produit = produitService.getProduitByCode(code);
        return ResponseEntity.ok(produit);
    }

    @GetMapping
    @Operation(summary = "Obtenir tous les produits", description = "Récupère la liste complète des produits")
    public ResponseEntity<List<ProduitResponseDto>> getAllProduits() {
        log.info("Requête de récupération de tous les produits");
        List<ProduitResponseDto> produits = produitService.getAllProduits();
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Obtenir les produits disponibles", description = "Récupère uniquement les produits en stock (quantité > 0)")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsDisponibles() {
        log.info("Requête de récupération des produits disponibles");
        List<ProduitResponseDto> produits = produitService.getProduitsDisponibles();
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/rupture")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir les produits en rupture de stock", description = "Liste des produits avec quantité = 0")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsEnRupture() {
        log.info("Requête de récupération des produits en rupture");
        List<ProduitResponseDto> produits = produitService.getProduitsEnRupture();
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/stock-critique")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir les produits avec stock critique", description = "Liste des produits dont le stock est en dessous du seuil critique")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsStockCritique(
            @RequestParam(required = false) @Parameter(description = "Seuil critique personnalisé (défaut: 5)") Integer seuil) {
        log.info("Requête de récupération des produits avec stock critique");
        List<ProduitResponseDto> produits = produitService.getProduitsStockCritique(seuil);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des produits par nom", description = "Recherche partielle sur le nom du produit")
    public ResponseEntity<List<ProduitResponseDto>> searchProduitsByNom(@RequestParam String nom) {
        log.info("Recherche de produits avec nom : {}", nom);
        List<ProduitResponseDto> produits = produitService.searchProduitsByNom(nom);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/categorie/{categorie}")
    @Operation(summary = "Obtenir les produits par catégorie", description = "Filtre les produits par catégorie")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsByCategorie(@PathVariable Categorie categorie) {
        log.info("Requête de récupération des produits de catégorie : {}", categorie);
        List<ProduitResponseDto> produits = produitService.getProduitsByCategorie(categorie);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Obtenir les produits par type", description = "Filtre les produits par type (avecReference/enQuantite)")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsByType(@PathVariable TypeProduit type) {
        log.info("Requête de récupération des produits de type : {}", type);
        List<ProduitResponseDto> produits = produitService.getProduitsByType(type);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/search-advanced")
    @Operation(summary = "Recherche multicritères", description = "Recherche avancée avec plusieurs filtres")
    public ResponseEntity<List<ProduitResponseDto>> searchProduits(
            @RequestParam(required = false) Categorie categorie,
            @RequestParam(required = false) TypeProduit typeProduit,
            @RequestParam(required = false) Double minPrix,
            @RequestParam(required = false) Double maxPrix,
            @RequestParam(required = false) Boolean disponible) {
        log.info("Recherche multicritères de produits");
        List<ProduitResponseDto> produits = produitService.searchProduits(
                categorie, typeProduit, minPrix, maxPrix, disponible);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/plus-loues")
    @Operation(summary = "Obtenir les produits les plus loués", description = "Top des produits par nombre de réservations")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsLesPlusLoues() {
        log.info("Requête des produits les plus loués");
        List<ProduitResponseDto> produits = produitService.getProduitsLesPlusLoues();
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/mieux-notes")
    @Operation(summary = "Obtenir les produits les mieux notés", description = "Produits ayant les meilleures notes")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsMieuxNotes(
            @RequestParam(required = false, defaultValue = "4.0") Double minNote) {
        log.info("Requête des produits les mieux notés");
        List<ProduitResponseDto> produits = produitService.getProduitsMieuxNotes(minNote);
        return ResponseEntity.ok(produits);
    }

    // ============ GESTION DU STOCK ============

    @PatchMapping("/{id}/ajuster-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Ajuster manuellement le stock", description = "Permet d'augmenter ou diminuer le stock d'un produit")
    public ResponseEntity<ProduitResponseDto> ajusterStock(
            @PathVariable Long id,
            @RequestParam Integer quantite,
            @RequestParam String motif) {
        log.info("Ajustement du stock pour le produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.ajusterStock(id, quantite, motif, username);
        return ResponseEntity.ok(produit);
    }

    @PatchMapping("/{id}/marquer-endommage")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Marquer un produit comme endommagé", description = "Retire du stock les unités endommagées")
    public ResponseEntity<Void> marquerProduitEndommage(
            @PathVariable Long id,
            @RequestParam Integer quantite,
            @RequestParam String motif) {
        log.info("Marquage de produit endommagé ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        produitService.marquerProduitEndommage(id, quantite, motif, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Mettre un produit en maintenance", description = "Retire temporairement des unités du stock pour maintenance")
    public ResponseEntity<Void> mettreEnMaintenance(
            @PathVariable Long id,
            @RequestParam Integer quantite,
            @RequestParam String motif) {
        log.info("Mise en maintenance du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        produitService.mettreEnMaintenance(id, quantite, motif, username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/retour-maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Retourner un produit de la maintenance", description = "Réintègre les unités au stock après maintenance")
    public ResponseEntity<Void> retournerDeMaintenance(
            @PathVariable Long id,
            @RequestParam Integer quantite,
            @RequestParam String motif) {
        log.info("Retour de maintenance du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        produitService.retournerDeMaintenance(id, quantite, motif, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/disponibilite")
    @Operation(summary = "Vérifier la disponibilité d'un produit", description = "Vérifie si le produit est disponible en quantité suffisante")
    public ResponseEntity<Boolean> verifierDisponibilite(
            @PathVariable Long id,
            @RequestParam Integer quantiteRequise) {
        log.info("Vérification de disponibilité pour le produit ID : {}", id);
        boolean disponible = produitService.verifierDisponibilite(id, quantiteRequise);
        return ResponseEntity.ok(disponible);
    }

    @GetMapping("/{id}/stock-critique")
    @Operation(summary = "Vérifier si le stock est critique", description = "Indique si le produit a atteint le seuil critique")
    public ResponseEntity<Boolean> verifierStockCritique(@PathVariable Long id) {
        log.info("Vérification du stock critique pour le produit ID : {}", id);
        boolean critique = produitService.verifierStockCritique(id);
        return ResponseEntity.ok(critique);
    }

    // ============ HISTORIQUE DES MOUVEMENTS ============

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir l'historique des mouvements d'un produit", description = "Liste complète des mouvements de stock")
    public ResponseEntity<List<MouvementStockResponseDto>> getHistoriqueMouvements(@PathVariable Long id) {
        log.info("Requête d'historique des mouvements pour le produit ID : {}", id);
        List<MouvementStockResponseDto> mouvements = produitService.getHistoriqueMouvements(id);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/mouvements/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir les mouvements par type", description = "Filtre les mouvements par type")
    public ResponseEntity<List<MouvementStockResponseDto>> getMouvementsByType(@PathVariable TypeMouvement type) {
        log.info("Requête des mouvements de type : {}", type);
        List<MouvementStockResponseDto> mouvements = produitService.getMouvementsByType(type);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/mouvements/utilisateur/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir les mouvements par utilisateur", description = "Historique des mouvements effectués par un utilisateur")
    public ResponseEntity<List<MouvementStockResponseDto>> getMouvementsByUser(@PathVariable String username) {
        log.info("Requête des mouvements de l'utilisateur : {}", username);
        List<MouvementStockResponseDto> mouvements = produitService.getMouvementsByUser(username);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/mouvements/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir les mouvements par période", description = "Filtre les mouvements entre deux dates")
    public ResponseEntity<List<MouvementStockResponseDto>> getMouvementsByPeriode(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFin) {
        log.info("Requête des mouvements entre {} et {}", dateDebut, dateFin);
        List<MouvementStockResponseDto> mouvements = produitService.getMouvementsByPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/mouvements/recents")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir les mouvements récents", description = "Derniers mouvements de stock pour le dashboard")
    public ResponseEntity<List<MouvementStockResponseDto>> getRecentMouvements(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("Requête des {} derniers mouvements", limit);
        List<MouvementStockResponseDto> mouvements = produitService.getRecentMouvements(limit);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/{id}/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Obtenir les statistiques d'un produit", description = "Statistiques détaillées des mouvements de stock")
    public ResponseEntity<StockStatistiquesDto> getStatistiquesProduit(@PathVariable Long id) {
        log.info("Requête des statistiques pour le produit ID : {}", id);
        StockStatistiquesDto stats = produitService.getStatistiquesProduit(id);
        return ResponseEntity.ok(stats);
    }
}