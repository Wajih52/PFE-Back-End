package tn.weeding.agenceevenementielle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.dto.notification.NotificationResponseDto;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;
import tn.weeding.agenceevenementielle.services.NotificationServiceInterface;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Gestion des notifications utilisateur")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationServiceInterface notificationService;
    private final AuthenticationFacade authenticationFacade;
    private final UtilisateurRepository utilisateurRepo;


    /**
     * Récupérer toutes les notifications de l'utilisateur connecté
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Récupérer mes notifications",
            description = "Liste de toutes les notifications de l'utilisateur connecté")
    public ResponseEntity<List<NotificationResponseDto>> getMesNotifications() {
        Long idUtilisateur = getAuthenticatedUserId();
        List<NotificationResponseDto> notifications = notificationService.getNotificationsByUtilisateur(idUtilisateur);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Récupérer les notifications non lues
     */
    @GetMapping("/non-lues")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Récupérer les notifications non lues")
    public ResponseEntity<List<NotificationResponseDto>> getNotificationsNonLues() {
        Long idUtilisateur = getAuthenticatedUserId();
        List<NotificationResponseDto> notifications = notificationService.getNotificationsNonLues(idUtilisateur);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Compter les notifications non lues (pour le badge)
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Compter les notifications non lues",
            description = "Retourne le nombre de notifications non lues pour le badge")
    public ResponseEntity<Map<String, Long>> compterNotificationsNonLues() {
        Long idUtilisateur = getAuthenticatedUserId();
        Long count = notificationService.compterNotificationsNonLues(idUtilisateur);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Marquer une notification comme lue
     */
    @PutMapping("/{id}/lire")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<NotificationResponseDto> marquerCommeLue(@PathVariable Long id) {
        Long idUtilisateur = getAuthenticatedUserId();
        NotificationResponseDto notification = notificationService.marquerCommeLue(id, idUtilisateur);
        return ResponseEntity.ok(notification);
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    @PutMapping("/lire-toutes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<Map<String, String>> marquerToutesCommeLues() {
        Long idUtilisateur = getAuthenticatedUserId();
        notificationService.marquerToutesCommeLues(idUtilisateur);
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications ont été marquées comme lues"));
    }

    /**
     * Supprimer une notification
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Supprimer une notification")
    public ResponseEntity<Void> supprimerNotification(@PathVariable Long id) {
        Long idUtilisateur = getAuthenticatedUserId();
        notificationService.supprimerNotification(id, idUtilisateur);
        return ResponseEntity.noContent().build();
    }

    /**
     * Créer une notification (admin seulement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une notification (admin)",
            description = "Permet à l'admin de créer une notification manuelle")
    public ResponseEntity<NotificationResponseDto> creerNotification(
            @Valid @RequestBody NotificationRequestDto dto) {
        NotificationResponseDto notification = notificationService.creerNotification(dto);
        return ResponseEntity.ok(notification);
    }



    /**
     * Récupérer l'utilisateur connecté
     */
    private Long getAuthenticatedUserId() {
        String username = authenticationFacade.getAuthentication().getName();
        Utilisateur utilisateur = utilisateurRepo.findByPseudoOrEmail(username, username)
                .orElseThrow(() -> new CustomException("Utilisateur non trouvé"));
        return utilisateur.getIdUtilisateur();
    }

}