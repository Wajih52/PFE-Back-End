package tn.weeding.agenceevenementielle.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.UtilisateurInscriptionDto;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.services.InscriptionServiceInterface;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/inscriptions")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class InscriptionController {

    private InscriptionServiceInterface inscriptionService;

    @PostMapping("/inscrire")
    public ResponseEntity<?> inscription(@Valid @RequestBody UtilisateurInscriptionDto dtoInscription) {
        try {
            log.info("📝 Nouvelle demande d'inscription pour : {}", dtoInscription.getEmail());

            // Appeler le service (toute la logique est là)
            Utilisateur utilisateurCree = inscriptionService.inscription(dtoInscription);

            log.info("✅ Inscription réussie pour : {}", utilisateurCree.getEmail());

            // Retourner 201 Created avec l'URI de la ressource créée
            return ResponseEntity
                    .created(URI.create("/utilisateurs/" + utilisateurCree.getIdUtilisateur()))
                    .body(Map.of(
                            "message", "Inscription réussie",
                            "detail", "Un email d'activation vous a été envoyé",
                            "email", utilisateurCree.getEmail(),
                            "codeUtilisateur", utilisateurCree.getCodeUtilisateur()
                    ));

        } catch (CustomException e) {
            log.warn("⚠️ Erreur d'inscription : {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", "Inscription échouée",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("💥 Erreur inattendue lors de l'inscription : {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Erreur serveur",
                            "message", "Une erreur interne s'est produite"
                    ));
        }
    }

    @GetMapping("/activation")
    public void activerCompte(
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {

        try {
            log.info("🔓 Tentative d'activation avec le token : {}...", token.substring(0, 8));

            // Récupérer l'email AVANT l'activation (car le token sera supprimé)
            String email = inscriptionService.getEmailByToken(token);

            // Appeler le service pour activer le compte
            inscriptionService.activerCompte(token);

            log.info("✅ Compte activé avec succès");

            // Encodage de l'URL pour éviter "Malformed URI"
            String redirectUrl = "http://localhost:4200/auth/login?activated=true";
            if (email != null && !email.isEmpty()) {
                redirectUrl += "&email=" + java.net.URLEncoder.encode(email, "UTF-8");
            }

            response.sendRedirect(redirectUrl);

        } catch (CustomException e) {
            log.error("❌ Erreur d'activation : {}", e.getMessage());

            // Encodage du message d'erreur
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), "UTF-8");
            response.sendRedirect(
                    "http://localhost:4200/auth/login?activated=false&error=" + errorMessage
            );

        } catch (Exception e) {
            log.error("💥 Erreur inattendue lors de l'activation : {}", e.getMessage(), e);

            response.sendRedirect(
                    "http://localhost:4200/auth/login?activated=false&error=Erreur+serveur"
            );
        }
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<?> renvoyerEmailActivation(@RequestParam("email") String email) {
        try {
            log.info("📧 Demande de renvoi d'email pour : {}", email);

            // Appeler le service (toute la logique est là)
            inscriptionService.resendActivationEmail(email);

            log.info("✅ Email d'activation renvoyé pour : {}", email);

            return ResponseEntity.ok(Map.of(
                    "message", "Email renvoyé avec succès",
                    "detail", "Vérifiez votre boîte email (valide 5 minutes)"
            ));

        } catch (CustomException e) {
            log.warn("⚠️ Erreur lors du renvoi : {}", e.getMessage());

            // Déterminer le code HTTP selon le type d'erreur
            HttpStatus status = e.getMessage().contains("introuvable")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity
                    .status(status)
                    .body(Map.of(
                            "error", "Renvoi impossible",
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("💥 Erreur inattendue lors du renvoi : {}", e.getMessage(), e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Erreur serveur",
                            "message", "Impossible de renvoyer l'email"
                    ));
        }
    }

    // ==================== HEALTH CHECK (optionnel) ====================

    /**
     * Vérifier que le service d'inscription fonctionne
     * GET /inscriptions/health
     * Utile pour les tests et le monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "InscriptionService",
                "timestamp", java.time.LocalDateTime.now()
        ));
    }
}