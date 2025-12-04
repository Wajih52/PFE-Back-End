package tn.weeding.agenceevenementielle.controller.authentification;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.config.JwtUtil;
import tn.weeding.agenceevenementielle.config.TokenBlacklistService;
import tn.weeding.agenceevenementielle.dto.authentification.AuthRequest;
import tn.weeding.agenceevenementielle.dto.authentification.AuthResponse;
import tn.weeding.agenceevenementielle.dto.ErrorResponse;
import tn.weeding.agenceevenementielle.entities.enums.StatutCompte;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;
import tn.weeding.agenceevenementielle.services.UtilisateurServiceInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Slf4j
public class AuthController {
    private final TokenBlacklistService tokenBlacklistService;
    private AuthenticationManager authenticationManager;
    private UtilisateurRepository utilisateurRepository;
    private UtilisateurServiceInterface utilisateurService;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> logIn(@Valid @RequestBody AuthRequest request) {

        try {
            log.info("🔐 Tentative de connexion pour l'utilisateur: {}", request.getIdentifiant());

            // Vérifier si l'utilisateur existe et si son compte est activé
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository
                    .findByPseudoOrEmail(request.getIdentifiant(), request.getIdentifiant());

            if (utilisateurOpt.isPresent()) {
                Utilisateur utilisateur = utilisateurOpt.get();

                // ⚠️ CAS 1 : Compte non activé
                if (!utilisateur.getActivationCompte()) {
                    log.warn("⚠️ Tentative de connexion avec un compte non activé: {}", request.getIdentifiant());

                    Map<String, Object> response = new HashMap<>();
                    response.put("error", "ACCOUNT_NOT_ACTIVATED");
                    response.put("message", "Votre compte n'est pas encore activé");
                    response.put("detail", "Veuillez vérifier votre email pour activer votre compte");
                    response.put("email", utilisateur.getEmail());
                    response.put("canResend", true); // Indiquer qu'on peut renvoyer l'email

                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
                // ✅ CAS 2 : Blocage si Compte Suspendu
                if(utilisateur.getEtatCompte()==StatutCompte.SUSPENDU) {
                    log.warn("⛔ Tentative de connexion avec un compte Suspendu: {}", request.getIdentifiant());
                    ErrorResponse errorResponse = new ErrorResponse(
                            "Compte Suspendu",
                            "Merci de Contacter Notre Support , Votre Compte Est Suspendu",
                            HttpStatus.LOCKED.value()
                    );
                    return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);
                }

                if(utilisateur.getEtatCompte()==StatutCompte.ARCHIVE) {
                    log.warn("⛔ Tentative de connexion avec un compte Supprimé(ARCHIVE): {}", request.getIdentifiant());
                    ErrorResponse errorResponse = new ErrorResponse(
                            "Compte Supprimé",
                            "Identifiant Introuvable",
                            HttpStatus.LOCKED.value()
                    );
                    return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);
                }

                // ✅ CAS 3 : Réactivation automatique si désactivé
                utilisateurService.activerCompteAvecLogin(utilisateur);
            }

            // ✅ CAS 4 : Tentative d'authentification normale
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getIdentifiant(),
                            request.getMotDePasse()
                    )
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails.getUsername());

            log.info("✅ Connexion réussie pour l'utilisateur: {}", userDetails.getUsername());

            // ✅ Vérifier si l'utilisateur doit changer son mot de passe
            Optional<Utilisateur> userOpt = utilisateurRepository.findByPseudo(userDetails.getUsername());
            if (userOpt.isPresent() && Boolean.TRUE.equals(userOpt.get().getDoitChangerMotDePasse())) {
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("requirePasswordChange", true);
                response.put("message", "Vous devez changer votre mot de passe temporaire");

                log.info("🔐 Utilisateur doit changer son mot de passe: {}", userDetails.getUsername());
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.ok(new AuthResponse(token));

        } catch (DisabledException ex) {
            // Compte désactivé par l'admin
            log.warn("⛔ Compte désactivé: {}", request.getIdentifiant());
            ErrorResponse error = new ErrorResponse(
                    "Compte désactivé",
                    "Votre compte a été désactivé. Contactez l'administrateur.",
                    HttpStatus.FORBIDDEN.value()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (BadCredentialsException ex) {
            log.warn("❌ Identifiants invalides pour l'utilisateur: {}", request.getIdentifiant());
            ErrorResponse error = new ErrorResponse(
                    "Authentification échouée",
                    "Identifiant ou mot de passe incorrect",
                    HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception ex) {
            log.error("💥 Erreur lors de l'authentification: {}", ex.getMessage());
            ErrorResponse error = new ErrorResponse(
                    "Erreur d'authentification",
                    "Une erreur interne s'est produite",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logOut(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Ajouter le token à la blacklist
                tokenBlacklistService.addToBlacklist(token);

                // Effacer le contexte de sécurité
                SecurityContextHolder.clearContext();

                log.info("🚪 Utilisateur déconnecté avec succès");

                Map<String, String> response = new HashMap<>();
                response.put("message", "Déconnexion réussie");
                response.put("detail", "Votre session a été fermée");
                return ResponseEntity.ok(response);
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Déconnexion réussie");
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("❌ Erreur lors de la déconnexion: {}", ex.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Erreur lors de la déconnexion");
            error.put("detail", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}