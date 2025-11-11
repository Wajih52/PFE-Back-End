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
import tn.weeding.agenceevenementielle.dto.produit.MouvementStockResponseDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.Categorie;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.services.ProduitServiceInterface;
import tn.weeding.agenceevenementielle.services.StockStatistiquesDto;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des produits et du stock
 *
 * Nouveaux endpoints ajoutés pour gérer la disponibilité avec périodes
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

    // ============================================
    // GESTION DES PRODUITS (CRUD)
    // ============================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Créer un nouveau produit",
            description = "Permet à l'admin ou employé d'ajouter un nouveau produit au catalogue")
    public ResponseEntity<ProduitResponseDto> creerProduit(@Valid @RequestBody ProduitRequestDto produitDto) {
        log.info("📦 Requête de création de produit reçue");
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.creerProduit(produitDto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(produit);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Modifier un produit",
            description = "Permet de modifier les informations d'un produit existant")
    public ResponseEntity<ProduitResponseDto> modifierProduit(
            @PathVariable Long id,
            @Valid @RequestBody ProduitRequestDto produitDto) {
        log.info("🔧 Requête de modification du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.modifierProduit(id, produitDto, username);
        return ResponseEntity.ok(produit);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Supprimer/Désactiver un produit",
            description = "Désactive un produit (soft delete)")
    public ResponseEntity<Void> supprimerProduit(@PathVariable Long id) {
        log.info("🗑️ Requête de suppression du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        produitService.supprimerProduit(id, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactiver")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Réactiver un produit",
            description = "Réactive un produit désactivé avec une nouvelle quantité")
    public ResponseEntity<ProduitResponseDto> reactiverProduit(
            @PathVariable Long id,
            @RequestParam Integer quantite) {
        log.info("♻️ Requête de réactivation du produit ID : {}", id);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.reactiverProduit(id, quantite, username);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un produit par ID",
            description = "Récupère les détails d'un produit spécifique")
    public ResponseEntity<ProduitResponseDto> getProduitById(@PathVariable Long id) {
        log.info("🔍 Requête de récupération du produit ID : {}", id);
        ProduitResponseDto produit = produitService.getProduitById(id);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Obtenir un produit par code",
            description = "Récupère les détails d'un produit via son code unique")
    public ResponseEntity<ProduitResponseDto> getProduitByCode(@PathVariable String code) {
        log.info("🔍 Requête de récupération du produit avec code : {}", code);
        ProduitResponseDto produit = produitService.getProduitByCode(code);
        return ResponseEntity.ok(produit);
    }

    @GetMapping
    @Operation(summary = "Obtenir tous les produits",
            description = "Récupère la liste complète des produits")
    public ResponseEntity<List<ProduitResponseDto>> getAllProduits() {
        log.info("📋 Requête de récupération de tous les produits");
        List<ProduitResponseDto> produits = produitService.getAllProduits();
        return ResponseEntity.ok(produits);
    }

    // ============================================
    // RECHERCHE ET FILTRAGE (SANS PÉRIODE)
    // ============================================

    @GetMapping("/disponibles")
    @Operation(summary = "Obtenir les produits disponibles (stock global)",
            description = "⚠️ NE PREND PAS EN COMPTE LES PÉRIODES - Utilisé pour vue admin uniquement")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsDisponibles() {
        log.info("📋 Requête de récupération des produits disponibles (stock global)");
        List<ProduitResponseDto> produits = produitService.getProduitsDisponibles();
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/rupture")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir les produits en rupture de stock",
            description = "Liste des produits avec quantité = 0")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsEnRupture() {
        log.info("📋 Requête de récupération des produits en rupture");
        List<ProduitResponseDto> produits = produitService.getProduitsEnRupture();
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/stock-critique")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Obtenir les produits avec stock critique (stock global)",
            description = "⚠️ NE PREND PAS EN COMPTE LES PÉRIODES")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsStockCritique(
            @RequestParam(required = false) @Parameter(description = "Seuil critique (défaut: 5)") Integer seuil) {
        log.info("⚠️ Requête de récupération des produits en stock critique (seuil: {})", seuil);
        List<ProduitResponseDto> produits = produitService.getProduitsStockCritique(seuil);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des produits par nom",
            description = "Recherche par correspondance partielle insensible à la casse")
    public ResponseEntity<List<ProduitResponseDto>> searchProduitsByNom(@RequestParam String nom) {
        log.info("🔍 Recherche de produits par nom : {}", nom);
        List<ProduitResponseDto> produits = produitService.searchProduitsByNom(nom);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/categorie/{categorie}")
    @Operation(summary = "Filtrer par catégorie",
            description = "Récupère tous les produits d'une catégorie")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsByCategorie(@PathVariable Categorie categorie) {
        log.info("📋 Requête de récupération des produits de catégorie : {}", categorie);
        List<ProduitResponseDto> produits = produitService.getProduitsByCategorie(categorie);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Filtrer par type de produit",
            description = "Récupère tous les produits d'un type (EN_QUANTITE ou avecReference)")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsByType(@PathVariable TypeProduit type) {
        log.info("📋 Requête de récupération des produits de type : {}", type);
        List<ProduitResponseDto> produits = produitService.getProduitsByType(type);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/recherche")
    @Operation(summary = "Recherche multicritères (sans période)",
            description = "Filtre les produits selon plusieurs critères")
    public ResponseEntity<List<ProduitResponseDto>> searchProduits(
            @RequestParam(required = false) Categorie categorie,
            @RequestParam(required = false) TypeProduit type,
            @RequestParam(required = false) Double minPrix,
            @RequestParam(required = false) Double maxPrix,
            @RequestParam(required = false) Boolean disponible) {

        log.info("🔍 Recherche multicritères : cat={}, type={}, prix={}-{}, dispo={}",
                categorie, type, minPrix, maxPrix, disponible);

        List<ProduitResponseDto> produits = produitService.searchProduits(
                categorie, type, minPrix, maxPrix, disponible);
        return ResponseEntity.ok(produits);
    }

    // ============================================
    // DISPONIBILITÉ AVEC PÉRIODE
    // ============================================

    @GetMapping("/{id}/quantite-disponible")
    @Operation(summary = " Calculer quantité disponible sur période",
            description = "Retourne la quantité réellement disponible en tenant compte des réservations")
    public ResponseEntity<Integer> calculerQuantiteDisponibleSurPeriode(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de début (format: yyyy-MM-dd)") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de fin (format: yyyy-MM-dd)") LocalDate dateFin) {

        log.info("🔍 Calcul quantité disponible produit ID: {} du {} au {}", id, dateDebut, dateFin);

        Integer quantite = produitService.calculerQuantiteDisponibleSurPeriode(id, dateDebut, dateFin);
        return ResponseEntity.ok(quantite);
    }

    @GetMapping("/{id}/disponibilite-periode")
    @Operation(summary = "Vérifier disponibilité sur période",
            description = "Vérifie si une quantité est disponible pour une période donnée")
    public ResponseEntity<Map<String, Object>> verifierDisponibiliteSurPeriode(
            @PathVariable Long id,
            @RequestParam Integer quantite,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de début (format: yyyy-MM-dd)") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de fin (format: yyyy-MM-dd)") LocalDate dateFin) {

        log.info("🔍 Vérification disponibilité produit ID: {}, quantité: {}, période: {} -> {}",
                id, quantite, dateDebut, dateFin);

        Boolean disponible = produitService.verifierDisponibiliteSurPeriode(
                id, quantite, dateDebut, dateFin);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("idProduit", id);
        response.put("quantiteDemandee", quantite);
        response.put("dateDebut", dateDebut);
        response.put("dateFin", dateFin);
        response.put("disponible", disponible);
        Integer quantiteDispo = produitService.calculerQuantiteDisponibleSurPeriode(
                id, dateDebut, dateFin);
        response.put("quantiteDisponible", quantiteDispo);

        if (!disponible) {

            response.put("message", String.format(
                    "Stock insuffisant : %d unités disponibles",
                    quantiteDispo));
        } else {
            response.put("message", "Produit disponible pour la période demandée");
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/catalogue-disponible")
    @Operation(summary = "Catalogue disponible sur période",
            description = "Retourne les produits réellement disponibles pendant une période")
    public ResponseEntity<List<ProduitResponseDto>> getCatalogueDisponibleSurPeriode(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de début (format: yyyy-MM-dd)") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de fin (format: yyyy-MM-dd)") LocalDate dateFin) {

        log.info("📋 Catalogue disponible du {} au {}", dateDebut, dateFin);

        List<ProduitResponseDto> produits = produitService.getCatalogueDisponibleSurPeriode(
                dateDebut, dateFin);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/recherche-avec-periode")
    @Operation(summary = "Recherche multicritères avec période",
            description = "Filtre les produits avec vérification de disponibilité sur période")
    public ResponseEntity<List<ProduitResponseDto>> searchProduitsAvecPeriode(
            @RequestParam(required = false) Categorie categorie,
            @RequestParam(required = false) TypeProduit type,
            @RequestParam(required = false) Double minPrix,
            @RequestParam(required = false) Double maxPrix,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de début (format: yyyy-MM-dd)") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de fin (format: yyyy-MM-dd)") LocalDate dateFin) {

        log.info("🔍 Recherche avec période : cat={}, type={}, prix={}-{}, période={}-{}",
                categorie, type, minPrix, maxPrix, dateDebut, dateFin);

        List<ProduitResponseDto> produits = produitService.searchProduitsAvecPeriode(
                categorie, type, minPrix, maxPrix, dateDebut, dateFin);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/quantite-minimum")
    @Operation(summary = "Produits avec quantité minimum sur période",
            description = "Filtre les produits ayant au moins la quantité demandée disponible")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsAvecQuantiteMinSurPeriode(
            @RequestParam Integer quantiteMin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de début (format: yyyy-MM-dd)") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de fin (format: yyyy-MM-dd)") LocalDate dateFin) {

        log.info("🔍 Produits avec quantité >= {} du {} au {}", quantiteMin, dateDebut, dateFin);

        List<ProduitResponseDto> produits = produitService.getProduitsAvecQuantiteMinSurPeriode(
                quantiteMin, dateDebut, dateFin);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/stock-critique-periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Stock critique sur période",
            description = "Identifie les produits en stock critique pendant une période")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsStockCritiqueSurPeriode(
            @RequestParam(required = false) Integer seuil,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de début (format: yyyy-MM-dd)") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de fin (format: yyyy-MM-dd)") LocalDate dateFin) {

        log.warn("⚠️ Vérification stock critique sur période: seuil={}, période={}-{}",
                seuil, dateDebut, dateFin);

        List<ProduitResponseDto> produits = produitService.getProduitsStockCritiqueSurPeriode(
                seuil, dateDebut, dateFin);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/taux-occupation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Taux d'occupation sur période",
            description = "Statistiques d'utilisation des produits")
    public ResponseEntity<List<Map<String, Object>>> getTauxOccupationSurPeriode(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de début (format: yyyy-MM-dd)") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "Date de fin (format: yyyy-MM-dd)") LocalDate dateFin) {

        log.info("📊 Calcul taux d'occupation du {} au {}", dateDebut, dateFin);

        List<Map<String, Object>> stats = produitService.getTauxOccupationSurPeriode(
                dateDebut, dateFin);
        return ResponseEntity.ok(stats);
    }

    // ============================================
    // GESTION DU STOCK (PRODUITS EN_QUANTITE)
    // ============================================

    @PostMapping("/{id}/ajout-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Ajouter du stock",
            description = "Augmente la quantité d'un produit EN_QUANTITE")
    public ResponseEntity<ProduitResponseDto> ajouterStock(
            @PathVariable Long id,
            @RequestParam Integer quantite,
            @RequestParam(required = false) String motif) {
        log.info("➕ Ajout de stock: produit ID={}, quantité={}", id, quantite);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.ajouterStock(id, quantite, motif, username);
        return ResponseEntity.ok(produit);
    }

    @PostMapping("/{id}/retrait-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    @Operation(summary = "Retirer du stock",
            description = "Diminue la quantité d'un produit EN_QUANTITE")
    public ResponseEntity<ProduitResponseDto> retirerStock(
            @PathVariable Long id,
            @RequestParam Integer quantite,
            @RequestParam(required = false) String motif) {
        log.info("➖ Retrait de stock: produit ID={}, quantité={}", id, quantite);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.retirerStock(id, quantite, motif, username);
        return ResponseEntity.ok(produit);
    }

    @PutMapping("/{id}/ajuster-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Ajuster le stock",
            description = "Définit une nouvelle quantité pour un produit EN_QUANTITE")
    public ResponseEntity<ProduitResponseDto> ajusterStock(
            @PathVariable Long id,
            @RequestParam Integer nouvelleQuantite,
            @RequestParam(required = false) String motif) {
        log.info("🔧 Ajustement de stock: produit ID={}, nouvelle quantité={}", id, nouvelleQuantite);
        String username = authenticationFacade.getAuthentication().getName();
        ProduitResponseDto produit = produitService.ajusterStock(id, nouvelleQuantite, motif, username);
        return ResponseEntity.ok(produit);
    }

    @GetMapping("/{id}/stock-critique")
    @Operation(summary = "Vérifier si le stock est critique",
            description = "Indique si le produit a atteint le seuil critique")
    public ResponseEntity<Boolean> verifierStockCritique(@PathVariable Long id) {
        log.info("⚠️ Vérification du stock critique pour le produit ID : {}", id);
        boolean critique = produitService.verifierStockCritique(id);
        return ResponseEntity.ok(critique);
    }

    // ============================================
    // STATISTIQUES ET RAPPORTS
    // ============================================

    @GetMapping("/{id}/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir les statistiques d'un produit",
            description = "Retourne les stats complètes (entrées, sorties, mouvements)")
    public ResponseEntity<StockStatistiquesDto> getStatistiquesProduit(@PathVariable Long id) {
        log.info("📊 Requête des statistiques pour le produit ID : {}", id);
        StockStatistiquesDto stats = produitService.getStatistiquesProduit(id);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/plus-loues")
    @Operation(summary = "Obtenir les produits les plus loués",
            description = "Top des produits par nombre de réservations")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsLesPlusLoues() {
        log.info("📊 Requête des produits les plus loués");
        List<ProduitResponseDto> produits = produitService.getProduitsLesPlusLoues();
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/mieux-notes")
    @Operation(summary = "Obtenir les produits les mieux notés",
            description = "Produits avec note moyenne supérieure au seuil")
    public ResponseEntity<List<ProduitResponseDto>> getProduitsMieuxNotes(
            @RequestParam(required = false) Double minNote) {
        log.info("📊 Requête des produits avec note >= {}", minNote);
        List<ProduitResponseDto> produits = produitService.getProduitsMieuxNotes(minNote);
        return ResponseEntity.ok(produits);
    }

    // ============================================
    // HISTORIQUE DES MOUVEMENTS
    // ============================================

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir l'historique des mouvements d'un produit",
            description = "Liste complète des mouvements de stock")
    public ResponseEntity<List<MouvementStockResponseDto>> getHistoriqueMouvements(@PathVariable Long id) {
        log.info("📜 Requête d'historique des mouvements pour le produit ID : {}", id);
        List<MouvementStockResponseDto> mouvements = produitService.getHistoriqueMouvements(id);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/mouvements/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir les mouvements par type",
            description = "Filtre les mouvements par type")
    public ResponseEntity<List<MouvementStockResponseDto>> getMouvementsByType(@PathVariable TypeMouvement type) {
        log.info("📜 Requête des mouvements de type : {}", type);
        List<MouvementStockResponseDto> mouvements = produitService.getMouvementsByType(type);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/mouvements/utilisateur/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir les mouvements par utilisateur",
            description = "Liste tous les mouvements effectués par un utilisateur")
    public ResponseEntity<List<MouvementStockResponseDto>> getMouvementsByUser(@PathVariable String username) {
        log.info("📜 Requête des mouvements de l'utilisateur : {}", username);
        List<MouvementStockResponseDto> mouvements = produitService.getMouvementsByUser(username);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/mouvements/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir les mouvements sur une période",
            description = "Filtre tous les mouvements entre deux dates")
    public ResponseEntity<List<MouvementStockResponseDto>> getMouvementsByPeriode(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date debut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fin) {
        log.info("📜 Requête des mouvements du {} au {}", debut, fin);
        List<MouvementStockResponseDto> mouvements = produitService.getMouvementsByPeriode(debut, fin);
        return ResponseEntity.ok(mouvements);
    }

    @GetMapping("/{id}/mouvements/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir les mouvements d'un produit sur une période",
            description = "Historique filtré par produit et période")
    public ResponseEntity<List<MouvementStockResponseDto>> getMouvementsProduitParPeriode(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date debut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fin) {
        log.info("📜 Requête des mouvements du produit ID: {} du {} au {}", id, debut, fin);
        List<MouvementStockResponseDto> mouvements =
                produitService.getMouvementsProduitParPeriode(id, debut, fin);
        return ResponseEntity.ok(mouvements);
    }
}