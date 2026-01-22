package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.dto.livraison.*;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.services.LivraisonServiceInterface;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/livraisons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Livraisons", description = "Gestion des livraisons et affectations d'employés")
@CrossOrigin(origins = "*", maxAge = 3600)
public class LivraisonController {

    private final LivraisonServiceInterface livraisonService;
    private final AuthenticationFacade authenticationFacade;

    // ============================================
    // CRUD LIVRAISONS
    // ============================================

    /**
     * Créer une nouvelle livraison
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Créer une livraison",
            description = "Créer une nouvelle livraison et associer des lignes de réservation")
    public ResponseEntity<LivraisonResponseDto> creerLivraison(
            @Valid @RequestBody LivraisonRequestDto livraisonRequest) {

        log.info("Création d'une nouvelle livraison");

        String username = authenticationFacade.getAuthentication().getName();
        LivraisonResponseDto livraison = livraisonService.creerLivraison(livraisonRequest, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(livraison);
    }

    /**
     * Modifier une livraison existante
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Modifier une livraison",
            description = "Modifier les informations d'une livraison existante")
    public ResponseEntity<LivraisonResponseDto> modifierLivraison(
            @PathVariable Long id,
            @Valid @RequestBody LivraisonRequestDto livraisonRequest) {

        log.info("Modification de la livraison ID: {}", id);

        String username = authenticationFacade.getAuthentication().getName();
        LivraisonResponseDto livraison = livraisonService.modifierLivraison(id, livraisonRequest, username);

        return ResponseEntity.ok(livraison);
    }

    /**
     * Récupérer une livraison par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer une livraison",
            description = "Récupérer les détails d'une livraison par son ID")
    public ResponseEntity<LivraisonResponseDto> getLivraisonById(@PathVariable Long id) {
        log.info("Récupération de la livraison ID: {}", id);

        LivraisonResponseDto livraison = livraisonService.getLivraisonById(id);
        return ResponseEntity.ok(livraison);
    }

    /**
     * Récupérer toutes les livraisons
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer toutes les livraisons",
            description = "Récupérer la liste de toutes les livraisons")
    public ResponseEntity<List<LivraisonResponseDto>> getAllLivraisons() {
        log.info("Récupération de toutes les livraisons");

        List<LivraisonResponseDto> livraisons = livraisonService.getAllLivraisons();
        return ResponseEntity.ok(livraisons);
    }

    /**
     * Récupérer les livraisons par statut
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer par statut",
            description = "Récupérer les livraisons avec un statut spécifique")
    public ResponseEntity<List<LivraisonResponseDto>> getLivraisonsByStatut(
            @PathVariable StatutLivraison statut) {

        log.info("Récupération des livraisons avec statut: {}", statut);

        List<LivraisonResponseDto> livraisons = livraisonService.getLivraisonsByStatut(statut);
        return ResponseEntity.ok(livraisons);
    }

    /**
     * Récupérer les livraisons d'une date spécifique
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer par date",
            description = "Récupérer les livraisons d'une date spécifique")
    public ResponseEntity<List<LivraisonResponseDto>> getLivraisonsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Récupération des livraisons du: {}", date);

        List<LivraisonResponseDto> livraisons = livraisonService.getLivraisonsByDate(date);
        return ResponseEntity.ok(livraisons);
    }

    /**
     * Récupérer les livraisons entre deux dates
     */
    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer par période",
            description = "Récupérer les livraisons entre deux dates")
    public ResponseEntity<List<LivraisonResponseDto>> getLivraisonsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Récupération des livraisons entre {} et {}", dateDebut, dateFin);

        List<LivraisonResponseDto> livraisons =
                livraisonService.getLivraisonsBetweenDates(dateDebut, dateFin);

        return ResponseEntity.ok(livraisons);
    }

    /**
     * Récupérer les livraisons d'aujourd'hui
     */
    @GetMapping("/aujourd-hui")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer les livraisons d'aujourd'hui",
            description = "Récupérer toutes les livraisons prévues pour aujourd'hui")
    public ResponseEntity<List<LivraisonResponseDto>> getLivraisonsAujourdhui() {
        log.info("Récupération des livraisons d'aujourd'hui");

        List<LivraisonResponseDto> livraisons = livraisonService.getLivraisonsAujourdhui();
        return ResponseEntity.ok(livraisons);
    }

    /**
     * Récupérer les livraisons d'un employé
     */
    @GetMapping("/employe/{idEmploye}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer par employé",
            description = "Récupérer les livraisons affectées à un employé")
    public ResponseEntity<List<LivraisonResponseDto>> getLivraisonsByEmploye(
            @PathVariable Long idEmploye) {

        log.info("Récupération des livraisons de l'employé ID: {}", idEmploye);

        List<LivraisonResponseDto> livraisons = livraisonService.getLivraisonsByEmploye(idEmploye);
        return ResponseEntity.ok(livraisons);
    }

    /**
     * Récupérer les livraisons d'une réservation
     */
    @GetMapping("/reservation/{idReservation}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(summary = "Récupérer par réservation",
            description = "Récupérer les livraisons liées à une réservation")
    public ResponseEntity<List<LivraisonResponseDto>> getLivraisonsByReservation(
            @PathVariable Long idReservation) {

        log.info("Récupération des livraisons de la réservation ID: {}", idReservation);

        List<LivraisonResponseDto> livraisons =
                livraisonService.getLivraisonsByReservation(idReservation);

        return ResponseEntity.ok(livraisons);
    }

    /**
     * Supprimer une livraison
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Supprimer une livraison",
            description = "Supprimer une livraison (non livrée uniquement)")
    public ResponseEntity<Void> supprimerLivraison(@PathVariable Long id) {
        log.info("Suppression de la livraison ID: {}", id);

        String username = authenticationFacade.getAuthentication().getName();
        livraisonService.supprimerLivraison(id, username);

        return ResponseEntity.noContent().build();
    }

    // ============================================
    // GESTION DES STATUTS
    // ============================================

    /**
     * Changer le statut d'une livraison
     */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Changer le statut",
            description = "Modifier le statut d'une livraison")
    public ResponseEntity<LivraisonResponseDto> changerStatutLivraison(
            @PathVariable Long id,
            @RequestParam StatutLivraison nouveauStatut) {

        log.info("Changement de statut de la livraison ID {} -> {}", id, nouveauStatut);

        String username = authenticationFacade.getAuthentication().getName();
        LivraisonResponseDto livraison =
                livraisonService.changerStatutLivraison(id, nouveauStatut, username);

        return ResponseEntity.ok(livraison);
    }

    /**
     * Marquer une livraison comme "En cours"
     */
    @PatchMapping("/{id}/en-cours")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Marquer en cours",
            description = "Marquer une livraison comme étant en cours")
    public ResponseEntity<LivraisonResponseDto> marquerEnCours(@PathVariable Long id) {
        log.info("Marquage de la livraison ID {} comme EN_COURS", id);

        String username = authenticationFacade.getAuthentication().getName();
        LivraisonResponseDto livraison = livraisonService.marquerLivraisonEnCours(id, username);

        return ResponseEntity.ok(livraison);
    }

    /**
     * Marquer une livraison comme "Livrée"
     */
    @PatchMapping("/{id}/livree")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Marquer livrée",
            description = "Marquer une livraison comme étant livrée")
    public ResponseEntity<LivraisonResponseDto> marquerLivree(@PathVariable Long id) {
        log.info("Marquage de la livraison ID {} comme LIVREE", id);

        String username = authenticationFacade.getAuthentication().getName();
        LivraisonResponseDto livraison = livraisonService.marquerLivraisonLivree(id, username);

        return ResponseEntity.ok(livraison);
    }

    /**
     * Marquer une ligne de réservation spécifique comme "Livrée"
     * Utilisé depuis le détail de livraison pour confirmer la livraison ligne par ligne
     */
    @PatchMapping("/lignes/{idLigne}/livree")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(
            summary = "Marquer une ligne comme livrée",
            description = "Marquer une ligne de réservation spécifique comme livrée. " +
                    "Lorsque toutes les lignes d'une livraison sont livrées, " +
                    "la livraison est automatiquement marquée comme LIVREE."
    )
    public ResponseEntity<LigneReservationResponseDto> marquerLigneLivree(@PathVariable Long idLigne) {
        log.info("Marquage de la ligne de réservation ID {} comme LIVREE", idLigne);

        String username = authenticationFacade.getAuthentication().getName();
        LigneReservationResponseDto ligne = livraisonService.marquerLigneLivree(idLigne, username);

        return ResponseEntity.ok(ligne);
    }

    /**
     * Obtenir toutes les lignes d'une livraison avec leur statut
     * pour les afficher dans le tableau de détail de livraison
     */

    @GetMapping("/{idLivraison}/lignes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE', 'CLIENT')")
    @Operation(
            summary = "Obtenir les lignes d'une livraison",
            description = "Récupérer toutes les lignes de réservation associées à une livraison"
    )
    public ResponseEntity<List<LigneReservationResponseDto>> getLignesLivraison(@PathVariable Long idLivraison) {
        log.info("Récupération des lignes de la livraison ID {}", idLivraison);

        List<LigneReservationResponseDto> lignes = livraisonService.getLignesLivraison(idLivraison);

        return ResponseEntity.ok(lignes);
    }

    // ============================================
    // AFFECTATION D'EMPLOYÉS
    // ============================================

    /**
     * Affecter un employé à une livraison
     */
    @PostMapping("/affectations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Affecter un employé",
            description = "Affecter un employé à une livraison")
    public ResponseEntity<AffectationLivraisonDto> affecterEmploye(
            @Valid @RequestBody AffectationLivraisonRequestDto affectationRequest) {

        log.info("Affectation d'un employé à une livraison");

        String username = authenticationFacade.getAuthentication().getName();
        AffectationLivraisonDto affectation =
                livraisonService.affecterEmploye(affectationRequest, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(affectation);
    }

    /**
     * Retirer un employé d'une livraison
     */
    @DeleteMapping("/affectations/{idAffectation}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Retirer un employé",
            description = "Retirer un employé d'une livraison")
    public ResponseEntity<Void> retirerEmploye(@PathVariable Long idAffectation) {
        log.info("Retrait de l'affectation ID: {}", idAffectation);

        String username = authenticationFacade.getAuthentication().getName();
        livraisonService.retirerEmploye(idAffectation, username);

        return ResponseEntity.noContent().build();
    }

    /**
     * Récupérer les affectations d'une livraison
     */
    @GetMapping("/{idLivraison}/affectations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer les affectations",
            description = "Récupérer les employés affectés à une livraison")
    public ResponseEntity<List<AffectationLivraisonDto>> getAffectationsByLivraison(
            @PathVariable Long idLivraison) {

        log.info("Récupération des affectations de la livraison ID: {}", idLivraison);

        List<AffectationLivraisonDto> affectations =
                livraisonService.getAffectationsByLivraison(idLivraison);

        return ResponseEntity.ok(affectations);
    }

    /**
     * Récupérer les affectations d'un employé
     */
    @GetMapping("/affectations/employe/{idEmploye}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Récupérer par employé",
            description = "Récupérer les affectations d'un employé")
    public ResponseEntity<List<AffectationLivraisonDto>> getAffectationsByEmploye(
            @PathVariable Long idEmploye) {

        log.info("Récupération des affectations de l'employé ID: {}", idEmploye);

        List<AffectationLivraisonDto> affectations =
                livraisonService.getAffectationsByEmploye(idEmploye);

        return ResponseEntity.ok(affectations);
    }

    // ============================================
    // BON DE LIVRAISON
    // ============================================

    /**
     * Générer et télécharger le bon de livraison (PDF)
     */
    @GetMapping("/{id}/bon-livraison")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(summary = "Générer bon de livraison",
            description = "Générer et télécharger le bon de livraison en PDF")
    public ResponseEntity<byte[]> genererBonLivraison(@PathVariable Long id) {
        log.info("Génération du bon de livraison ID: {}", id);

        byte[] pdf = livraisonService.genererBonLivraison(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "bon-livraison-" + id + ".pdf");
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    // ============================================
    // STATISTIQUES
    // ============================================

    /**
     * Compter les livraisons par statut
     */
    @GetMapping("/statistiques/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Compter par statut",
            description = "Compter le nombre de livraisons avec un statut donné")
    public ResponseEntity<Long> countByStatut(@PathVariable StatutLivraison statut) {
        log.info("Comptage des livraisons avec statut: {}", statut);

        Long count = livraisonService.countByStatut(statut);
        return ResponseEntity.ok(count);
    }

    /**
     * Marquer une ligne de réservation comme "En retour"
     * L'équipement est en cours de retour physique
     */
    @PatchMapping("/lignes/{idLigne}/en-retour")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(
            summary = "Marquer une ligne en retour",
            description = "Marquer une ligne de réservation comme étant en cours de retour. " +
                    "Les instances passent en statut EN_RETOUR."
    )
    public ResponseEntity<LigneReservationResponseDto> marquerLigneEnRetour(@PathVariable Long idLigne) {
        log.info(" Marquage de la ligne de réservation ID {} comme EN RETOUR", idLigne);

        String username = authenticationFacade.getAuthentication().getName();
        LigneReservationResponseDto ligne = livraisonService.marquerLigneEnRetour(idLigne, username);

        return ResponseEntity.ok(ligne);
    }

    /**
     * Marquer une ligne de réservation comme "Retournée" (finalisée)
     * Le matériel est physiquement revenu et disponible
     */
    @PatchMapping("/lignes/{idLigne}/retournee")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYE')")
    @Operation(
            summary = "Marquer une ligne comme retournée",
            description = "Marquer une ligne de réservation comme retournée et validée. " +
                    "Les instances redeviennent DISPONIBLES et le stock est réintégré. " +
                    "Lorsque toutes les lignes sont retournées, la livraison passe à RETOURNEE."
    )
    public ResponseEntity<LigneReservationResponseDto> marquerLigneRetournee(@PathVariable Long idLigne) {
        log.info("Marquage de la ligne de réservation ID {} comme RETOURNEE", idLigne);

        String username = authenticationFacade.getAuthentication().getName();
        LigneReservationResponseDto ligne = livraisonService.marquerLigneRetournee(idLigne, username);

        return ResponseEntity.ok(ligne);
    }
}