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
import tn.weeding.agenceevenementielle.dto.modifDateReservation.*;
import tn.weeding.agenceevenementielle.dto.reservation.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.DateValidationException;
import tn.weeding.agenceevenementielle.services.Reservation.DateReservationValidator;
import tn.weeding.agenceevenementielle.services.Reservation.LigneReservationModificationDatesService;
import tn.weeding.agenceevenementielle.services.Reservation.ReservationServiceInterface;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ==========================================
 * CONTRÔLEUR REST POUR LA GESTION DES RÉSERVATIONS
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 *
 * Workflow:
 * 1. CLIENT: Crée un devis (POST /api/reservations/devis)
 * 2. ADMIN: Modifie et valide le devis (PUT /api/reservations/devis/{id}/modifier)
 * 3. CLIENT: Accepte ou refuse le devis (POST /api/reservations/devis/{id}/valider)
 * 4. → Réservation confirmée ou annulée
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Réservations", description = "APIs pour la gestion du cycle complet de réservation (devis + confirmation)")
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationServiceInterface reservationService;
    private final AuthenticationFacade authenticationFacade;
    private final DateReservationValidator dateReservationValidator;
    private final LigneReservationModificationDatesService modificationDatesService;

    // ============================================
    // PARTIE 1: CRÉATION DE DEVIS (CLIENT)
    // ============================================

    /**
     * 🛒 ÉTAPE 1: Le client crée un DEVIS (panier)
     * - Sélectionne les produits avec dates
     * - Vérification automatique de disponibilité
     * - Génération du devis avec statut "En Attente"
     */
    @PostMapping("/devis")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Créer un devis (CLIENT)",
            description = "Le client sélectionne des produits et génère un devis. Le système vérifie automatiquement la disponibilité.")
    public ResponseEntity<ReservationResponseDto> creerDevis(
            @Valid @RequestBody DevisRequestDto devisRequest) {

        log.info("📝 Nouvelle demande de devis reçue");

        String username = authenticationFacade.getAuthentication().getName();
        Long idUtilisateur = authenticationFacade.getCurrentUserId();

        ReservationResponseDto devis = reservationService.creerDevis(devisRequest, idUtilisateur, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(devis);
    }

    /**
     * 🔍 Vérifier la disponibilité d'un produit AVANT de créer le devis
     * Permet d'afficher en temps réel si un produit est disponible
     */
    @PostMapping("/disponibilite/verifier")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'EMPLOYE')")
    @Operation(summary = "Vérifier la disponibilité d'un produit",
            description = "Vérifie si un produit est disponible pour une période et une quantité données")
    public ResponseEntity<DisponibiliteResponseDto> verifierDisponibilite(
            @Valid @RequestBody VerificationDisponibiliteDto verificationDto) {

        DisponibiliteResponseDto disponibilite = reservationService.verifierDisponibilite(verificationDto);
        return ResponseEntity.ok(disponibilite);
    }

    /**
     * 🔍 Vérifier la disponibilité de plusieurs produits en une fois
     */
    @PostMapping("/disponibilite/verifier-plusieurs")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'EMPLOYE')")
    @Operation(summary = "Vérifier la disponibilité de plusieurs produits",
            description = "Vérifie la disponibilité d'une liste de produits en une seule requête")
    public ResponseEntity<List<DisponibiliteResponseDto>> verifierDisponibilites(
            @Valid @RequestBody List<VerificationDisponibiliteDto> verifications) {

        List<DisponibiliteResponseDto> disponibilites = reservationService.verifierDisponibilites(verifications);
        return ResponseEntity.ok(disponibilites);
    }

    // ============================================
    // PARTIE 2: MODIFICATION DU DEVIS (ADMIN)
    // ============================================

    /**
     * 🔧 ÉTAPE 2: L'ADMIN modifie et valide un DEVIS
     * - Peut modifier les prix unitaires
     * - Peut appliquer des remises (% ou montant fixe)
     * - Le devis reste en statut "En Attente" jusqu'à la validation client
     */
    @PutMapping("/devis/{id}/modifier")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Modifier un devis (ADMIN)",
            description = "L'admin modifie les prix et applique des remises au devis")
    public ResponseEntity<ReservationResponseDto> modifierDevisParAdmin(
            @PathVariable Long id,
            @Valid @RequestBody DevisModificationDto modificationDto) {

        log.info("🔧 Modification du devis ID: {} par l'admin", id);

        String username = authenticationFacade.getAuthentication().getName();
        modificationDto.setIdReservation(id);

        ReservationResponseDto devisModifie = reservationService.modifierDevisParAdmin(modificationDto, username);

        return ResponseEntity.ok(devisModifie);
    }

    /**
     * ❌ L'admin peut annuler un devis
     */
    @DeleteMapping("/devis/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Annuler un devis (ADMIN)",
            description = "L'admin annule un devis (ex: client ne répond pas)")
    public ResponseEntity<Map<String, String>> annulerDevisParAdmin(
            @PathVariable Long id,
            @RequestParam(required = false) String motif) {

        log.info("❌ Annulation du devis ID: {} par l'admin", id);

        String username = authenticationFacade.getAuthentication().getName();
        reservationService.annulerDevisParAdmin(id, motif, username);

        return ResponseEntity.ok(Map.of("message", "Devis annulé avec succès"));
    }

    // ============================================
    // PARTIE 3: VALIDATION DU DEVIS (CLIENT)
    // ============================================

    /**
     * ✅ ÉTAPE 3: Le CLIENT valide ou refuse le devis
     * - Si accepté → Réservation CONFIRMÉE + Instances réservées
     * - Si refusé → Réservation ANNULÉE
     */
    @PostMapping("/devis/{id}/valider")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Valider ou refuser un devis (CLIENT)",
            description = "Le client accepte (confirme la réservation) ou refuse le devis modifié par l'admin")
    public ResponseEntity<ReservationResponseDto> validerDevisParClient(
            @PathVariable Long id,
            @Valid @RequestBody ValidationDevisDto validationDto) {

        log.info("🎯 Validation du devis ID: {} par le client", id);

        String username = authenticationFacade.getAuthentication().getName();
        validationDto.setIdReservation(id);

        ReservationResponseDto reservation = reservationService.validerDevisParClient(validationDto, username);

        String message = validationDto.getAccepter()
                ? "🎉 Réservation confirmée avec succès!"
                : "❌ Devis refusé";
        log.info(message);

        return ResponseEntity.ok(reservation);
    }

    // ============================================
    // PARTIE 4: CONSULTATION DES RÉSERVATIONS
    // ============================================

    /**
     * 📋 Récupérer une réservation par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'EMPLOYE','MANAGER')")
    @Operation(summary = "Récupérer une réservation par ID",
            description = "Obtenir les détails complets d'une réservation")
    public ResponseEntity<ReservationResponseDto> getReservationById(@PathVariable Long id) {
        ReservationResponseDto reservation = reservationService.getReservationById(id);
        return ResponseEntity.ok(reservation);
    }

    /**
     * 📋 Récupérer une réservation par sa référence
     */
    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'EMPLOYE','MANAGER')")
    @Operation(summary = "Récupérer une réservation par référence",
            description = "Rechercher une réservation par son numéro de référence (ex: RES-2025-0001)")
    public ResponseEntity<ReservationResponseDto> getReservationByReference(@PathVariable String reference) {
        ReservationResponseDto reservation = reservationService.getReservationByReference(reference);
        return ResponseEntity.ok(reservation);
    }

    /**
     * 📋 Récupérer TOUTES les réservations d'un client (ses propres réservations)
     */
    @GetMapping("/mes-reservations")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYE','MANAGER')")
    @Operation(summary = "Mes réservations (CLIENT)",
            description = "Le client consulte toutes ses réservations et devis")
    public ResponseEntity<List<ReservationResponseDto>> getMesReservations() {
        Long idUtilisateur = authenticationFacade.getCurrentUserId();
        List<ReservationResponseDto> reservations = reservationService.getReservationsByClient(idUtilisateur);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 📋 Récupérer les devis en attente d'un client
     */
    @GetMapping("/mes-devis-en-attente")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Mes devis en attente (CLIENT)",
            description = "Le client consulte ses devis en attente de validation admin")
    public ResponseEntity<List<ReservationResponseDto>> getMesDevisEnAttente() {
        Long idUtilisateur = authenticationFacade.getCurrentUserId();
        List<ReservationResponseDto> devis = reservationService.getDevisEnAttenteByClient(idUtilisateur);
        return ResponseEntity.ok(devis);
    }

    /**
     * 📋 Récupérer TOUTES les réservations (ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Toutes les réservations (ADMIN)",
            description = "L'admin consulte toutes les réservations de tous les clients")
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations() {
        List<ReservationResponseDto> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    /**
     * 📋 Récupérer tous les devis en attente (ADMIN)
     */
    @GetMapping("/devis-en-attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tous les devis en attente (ADMIN)",
            description = "L'admin consulte tous les devis en attente de validation")
    public ResponseEntity<List<ReservationResponseDto>> getAllDevisEnAttente() {
        List<ReservationResponseDto> devis = reservationService.getAllDevisEnAttente();
        return ResponseEntity.ok(devis);
    }

    /**
     * 📋 Récupérer les réservations par statut
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Filtrer par statut (ADMIN)",
            description = "Récupérer toutes les réservations d'un statut donné (EN_ATTENTE, CONFIRME, ANNULE)")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByStatut(
            @PathVariable StatutReservation statut) {

        List<ReservationResponseDto> reservations = reservationService.getReservationsByStatut(statut);
        return ResponseEntity.ok(reservations);
    }


    // ============================================
    // PARTIE 5: RECHERCHE AVANCÉE
    // ============================================

    /**
     * 🔍 Recherche multicritères de réservations
     */
    @PostMapping("/recherche")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Recherche multicritères (ADMIN)",
            description = "Rechercher des réservations selon plusieurs critères")
    public ResponseEntity<List<ReservationResponseDto>> searchReservations(
            @Valid @RequestBody ReservationSearchDto searchDto) {

        List<ReservationResponseDto> reservations = reservationService.searchReservations(searchDto);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 📅 Récupérer les réservations dans une période
     */
    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Réservations sur une période (ADMIN)",
            description = "Récupérer les réservations entre deux dates")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByPeriode(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFin) {

        List<ReservationResponseDto> reservations =
                reservationService.getReservationsByPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 📅 Réservations confirmées à venir
     */
    @GetMapping("/a-venir")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Réservations à venir (ADMIN)",
            description = "Récupérer les réservations confirmées futures")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsAVenir() {
        List<ReservationResponseDto> reservations = reservationService.getReservationsAVenir();
        return ResponseEntity.ok(reservations);
    }

    /**
     * 📅 Réservations en cours
     */
    @GetMapping("/en-cours")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Réservations en cours (ADMIN)",
            description = "Récupérer les réservations actuellement actives")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsEnCours() {
        List<ReservationResponseDto> reservations = reservationService.getReservationsEnCours();
        return ResponseEntity.ok(reservations);
    }

    /**
     * 📅 Réservations passées
     */
    @GetMapping("/passees")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Réservations passées (ADMIN)",
            description = "Récupérer l'historique des réservations terminées")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsPassees() {
        List<ReservationResponseDto> reservations = reservationService.getReservationsPassees();
        return ResponseEntity.ok(reservations);
    }

    // ============================================
    // PARTIE 6: ANNULATION ET MODIFICATION
    // ============================================

    /**
     * ❌ Le client annule sa réservation ou l'admin
     */
    @DeleteMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN','MANAGER')")
    @Operation(summary = "Annuler une réservation (CLIENT)",
            description = "Le client annule sa réservation (si pas encore livrée)")
    public ResponseEntity<Map<String, String>> annulerReservationParClient(
            @PathVariable Long id,
            @RequestParam(required = false) String motif) {

        String username = authenticationFacade.getAuthentication().getName();
        reservationService.annulerReservationParClient(id, motif, username);

        return ResponseEntity.ok(Map.of("message", "Réservation annulée avec succès"));
    }

    // ============================================
    // PARTIE 7: STATISTIQUES ET RAPPORTS
    // ============================================

    /**
     * 📊 Statistiques globales des réservations
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Statistiques des réservations (ADMIN)",
            description = "Obtenir les statistiques globales (nombre, CA, etc.)")
    public ResponseEntity<ReservationSummaryDto> getStatistiques() {
        ReservationSummaryDto stats = reservationService.getStatistiquesReservations();
        return ResponseEntity.ok(stats);
    }

    /**
     * 📊 Statistiques d'un client
     */
    @GetMapping("/statistiques/client/{idClient}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Statistiques d'un client (ADMIN)",
            description = "Obtenir les statistiques de réservation d'un client spécifique")
    public ResponseEntity<ReservationSummaryDto> getStatistiquesClient(@PathVariable Long idClient) {
        ReservationSummaryDto stats = reservationService.getStatistiquesReservationsClient(idClient);
        return ResponseEntity.ok(stats);
    }

    /**
     * 💰 Chiffre d'affaires sur une période
     */
    @GetMapping("/chiffre-affaires")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Chiffre d'affaires sur période (ADMIN)",
            description = "Calculer le CA généré sur une période donnée")
    public ResponseEntity<Map<String, Double>> getChiffreAffairesPeriode(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateFin) {

        Double ca = reservationService.calculateChiffreAffairesPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(Map.of("chiffreAffaires", ca));
    }

    // ============================================
    // PARTIE 8: ALERTES ET NOTIFICATIONS
    // ============================================

    /**
     * 🔔 Réservations qui commencent bientôt
     */
    @GetMapping("/alertes/commencant-dans/{nbreJours}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Alertes de livraisons prochaines (ADMIN)",
            description = "Réservations qui commencent dans N jours (pour préparer les livraisons)")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsCommencantDans(
            @PathVariable int nbreJours) {

        List<ReservationResponseDto> reservations =
                reservationService.getReservationsCommencantDansNJours(nbreJours);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 🔔 Réservations qui se terminent bientôt
     */
    @GetMapping("/alertes/finissant-dans/{nbreJours}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Alertes de retours prochains (ADMIN)",
            description = "Réservations qui se terminent dans N jours (pour organiser les retours)")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsFinissantDans(
            @PathVariable int nbreJours) {

        List<ReservationResponseDto> reservations =
                reservationService.getReservationsFinissantDansNJours(nbreJours);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 🔔 Devis expirés (pour relance client)
     */
    @GetMapping("/alertes/devis-expires/{nbreJours}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Devis expirés (ADMIN)",
            description = "Devis en attente depuis plus de N jours (nécessitent une relance)")
    public ResponseEntity<List<ReservationResponseDto>> getDevisExpires(
            @PathVariable int nbreJours) {

        List<ReservationResponseDto> devis = reservationService.getDevisExpires(nbreJours);
        return ResponseEntity.ok(devis);
    }

    /**
     * 🔔 Devis expirés (pour relance client)
     */
    @GetMapping("/alertes/devis-expires-ajourdhui")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Devis expirés aujourd'hui (ADMIN)",
            description = "Devis déjà expiré")
    public ResponseEntity<List<ReservationResponseDto>> getDevisExpiresToday() {

        List<ReservationResponseDto> devis = reservationService.getDevisExpiresToday();
        return ResponseEntity.ok(devis);
    }

    /**
     * 💰 Réservations avec paiement incomplet
     */
    @GetMapping("/alertes/paiements-incomplets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Alertes paiements incomplets (ADMIN)",
            description = "Réservations confirmées avec paiement incomplet")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsAvecPaiementIncomplet() {
        List<ReservationResponseDto> reservations =
                reservationService.getReservationsAvecPaiementIncomplet();
        return ResponseEntity.ok(reservations);
    }

    /**
     * 📅 Obtenir les contraintes de dates pour les réservations
     *
     * Endpoint utilisé par le frontend pour:
     * - Configurer les datepickers
     * - Afficher les règles au client
     * - Validation côté client
     */
    @GetMapping("/contraintes-dates")
    @Operation(summary = "Obtenir les contraintes de dates",
            description = "Retourne les règles de dates pour les réservations " +
                    "(dates min/max, durée min/max, etc.)")
    public ResponseEntity<DateConstraintesDto> getContraintesDates() {
        log.debug("📅 Récupération des contraintes de dates");

        DateConstraintesDto contraintes = reservationService.getContraintesDates();

        return ResponseEntity.ok(contraintes);
    }

    /**
     * 📅 Valider une période de dates
     *
     * Endpoint pour valider une période AVANT de créer le devis
     * Permet au frontend d'afficher des erreurs en temps réel
     */
    @PostMapping("/valider-periode")
    @Operation(summary = "Valider une période de dates",
            description = "Vérifie si une période est valide selon les règles métier")
    public ResponseEntity<DateValidationResponseDto> validerPeriode(
            @RequestBody @Valid DatePeriodeDto periodeDto) {

        log.debug("📅 Validation période: {} -> {}",
                periodeDto.getDateDebut(), periodeDto.getDateFin());

        try {
            dateReservationValidator.validerPeriodeReservation(
                    periodeDto.getDateDebut(),
                    periodeDto.getDateFin()
            );

            long nbJours = dateReservationValidator.calculerNombreJours(
                    periodeDto.getDateDebut(),
                    periodeDto.getDateFin()
            );

            return ResponseEntity.ok(DateValidationResponseDto.builder()
                    .valide(true)
                    .message("Période valide - " + nbJours + " jour(s)")
                    .nombreJours(nbJours)
                    .build());

        } catch (DateValidationException e) {
            return ResponseEntity.ok(DateValidationResponseDto.builder()
                    .valide(false)
                    .message(e.getMessage())
                    .nombreJours(0L)
                    .build());
        }
    }

    /**
     * 📅 Vérifier si des nouvelles dates sont disponibles pour une réservation
     *
     * Permet au client de vérifier AVANT de modifier
     * Ne modifie rien, juste vérifie
     */
    @PostMapping("/{idReservation}/verifier-nouvelles-dates")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'EMPLOYE')")
    @Operation(summary = "Vérifier disponibilité pour nouvelles dates",
            description = "Vérifie si une réservation peut être déplacée à de nouvelles dates")
    public ResponseEntity<VerificationModificationDatesDto> verifierNouvellesDates(
            @PathVariable Long idReservation,
            @RequestBody @Valid DatePeriodeDto nouvellesDates) {

        log.debug("🔍 Vérification nouvelles dates pour réservation {}", idReservation);

        // Valider les nouvelles dates
        try {
            dateReservationValidator.validerPeriodeReservation(
                    nouvellesDates.getDateDebut(),
                    nouvellesDates.getDateFin(),
                    "vérification modification"
            );
        } catch (DateValidationException e) {
            return ResponseEntity.ok(VerificationModificationDatesDto.builder()
                    .possible(false)
                    .message("Dates invalides: " + e.getMessage())
                    .build());
        }
        return ResponseEntity.ok(reservationService.verifAvantModifDateReservation(idReservation, nouvellesDates));


    }

    // ============================================
    // FONCTIONNALITÉ 1 : MODIFIER UNE SEULE LIGNE
    // ============================================

    /**
     * 🎯 Modifier les dates d'une seule ligne de réservation
     *
     * IMPORTANT : Seul l'admin, le manager ou le client propriétaire peuvent modifier
     *
     * @param idRes ID de la réservation
     * @param idLigne ID de la ligne à modifier
     * @param request Nouvelles dates
     * @return Réservation mise à jour avec détails des modifications
     */
    @PutMapping("/{idRes}/lignes/{idLigne}/dates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLIENT')")
    @Operation(
            summary = "Modifier UNE ligne de réservation",
            description = """
                    Permet de modifier les dates d'une seule ligne (un produit) sans toucher aux autres.
                    
                    **Cas d'usage :**
                    - Client veut garder les chaises 2 jours de plus
                    - Ajuster juste l'éclairage car montage plus tôt
                    
                    **Comportement :**
                    - ✅ Vérification automatique de disponibilité
                    - ✅ Recalcul des dates de la réservation (min/max des lignes)
                    - ✅ Validation des instances pour produits avec référence
                    - ✅ Ajout dans l'historique (commentaireAdmin)
                    """
    )
    public ResponseEntity<ModificationDatesResponseDto> modifierUneLigne(
            @PathVariable Long idRes,
            @PathVariable Long idLigne,
            @Valid @RequestBody ModifierUneLigneRequestDto request) {

        log.info("🔧 API - Modification d'une ligne - Réservation: {}, Ligne: {}", idRes, idLigne);

        String username = authenticationFacade.getAuthentication().getName();

        ModificationDatesResponseDto response = modificationDatesService.modifierUneLigne(
                idRes, idLigne, request, username
        );

        log.info("✅ Ligne {} modifiée avec succès", idLigne);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // FONCTIONNALITÉ 2 : DÉCALER TOUTES LES LIGNES
    // ============================================

    /**
     * 🎯 Décaler toutes les lignes d'une réservation
     *
     * IMPORTANT : Seul l'admin ou le manager peuvent effectuer un décalage global
     *
     * @param idRes ID de la réservation
     * @param request Nombre de jours de décalage (+/-) et motif
     * @return Réservation mise à jour avec détails des modifications
     */
    @PutMapping("/{idRes}/decaler")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Décaler TOUTES les lignes d'une réservation",
            description = """
                    Permet de décaler toutes les lignes d'un même nombre de jours.
                    
                    **Cas d'usage :**
                    - Événement reporté d'une semaine (COVID, météo, etc.)
                    - Client veut avancer/reculer tout l'événement
                    
                    **Paramètres :**
                    - `nombreJours` : +7 pour avancer de 7 jours, -7 pour reculer
                    - `motif` : Raison du décalage (obligatoire)
                    
                    **Comportement :**
                    - ✅ Décalage de TOUTES les lignes
                    - ✅ Vérification automatique de disponibilité pour chaque ligne
                    - ✅ Recalcul des dates de la réservation
                    - ✅ Ajout dans l'historique avec le motif
                    """
    )
    public ResponseEntity<ModificationDatesResponseDto> decalerToutesLesLignes(
            @PathVariable Long idRes,
            @Valid @RequestBody DecalerToutesLignesRequestDto request) {

        log.info("🔧 API - Décalage de toutes les lignes - Réservation: {}, Décalage: {} jours",
                idRes, request.getNombreJours());

        String username = authenticationFacade.getAuthentication().getName();

        ModificationDatesResponseDto response = modificationDatesService.decalerToutesLesLignes(
                idRes, request, username
        );

        log.info("✅ Toutes les lignes décalées de {} jours avec succès", request.getNombreJours());

        return ResponseEntity.ok(response);
    }

    // ============================================
    // FONCTIONNALITÉ 3 : MODIFIER PLUSIEURS LIGNES SPÉCIFIQUES
    // ============================================

    /**
     * 🎯 Modifier plusieurs lignes spécifiques en une seule requête
     *
     * IMPORTANT : Seul l'admin ou le manager peuvent modifier plusieurs lignes à la fois
     *
     * @param idRes ID de la réservation
     * @param request Liste des modifications à effectuer
     * @return Réservation mise à jour avec détails des modifications
     */
    @PutMapping("/{idRes}/lignes-multiples")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Modifier plusieurs lignes spécifiques",
            description = """
                    Permet de modifier les dates de plusieurs lignes différentes en une seule requête.
                    
                    **Cas d'usage :**
                    - Réorganisation complète de la logistique
                    - Ajustement fin de plusieurs produits
                    - Modifier certaines lignes sans toucher aux autres
                    
                    **Format de la requête :**
                    ```json
                    {
                      "modifications": [
                        {
                          "idLigne": 1,
                          "nouvelleDateDebut": "2025-11-10",
                          "nouvelleDateFin": "2025-11-11"
                        },
                        {
                          "idLigne": 2,
                          "nouvelleDateDebut": "2025-11-12",
                          "nouvelleDateFin": "2025-11-13"
                        }
                      ],
                      "motif": "Réorganisation logistique"
                    }
                    ```
                    
                    **Comportement :**
                    - ✅ Mise à jour batch (toutes les lignes en une transaction)
                    - ✅ Vérification de disponibilité pour chaque ligne
                    - ✅ Recalcul des dates de la réservation
                    - ✅ Ajout dans l'historique
                    """
    )
    public ResponseEntity<ModificationDatesResponseDto> modifierPlusieurLignes(
            @PathVariable Long idRes,
            @Valid @RequestBody ModifierPlusieurLignesRequestDto request) {

        log.info("🔧 API - Modification de plusieurs lignes - Réservation: {}, Nombre: {}",
                idRes, request.getModifications().size());

        String username = authenticationFacade.getAuthentication().getName();

        ModificationDatesResponseDto response = modificationDatesService.modifierPlusieurLignes(
                idRes, request, username
        );

        log.info("✅ {} lignes modifiées avec succès", request.getModifications().size());

        return ResponseEntity.ok(response);
    }

}