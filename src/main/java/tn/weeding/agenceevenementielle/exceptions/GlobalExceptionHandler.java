package tn.weeding.agenceevenementielle.exceptions;


import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tn.weeding.agenceevenementielle.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // ✅ Log lisible avec un champ par ligne
        log.warn("⚠️ Erreur de validation :");
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
            log.warn("  ❌ {}: {}", fieldName, errorMessage);  // ← Un champ par ligne
        });

        // Réponse JSON structurée
        Map<String, Object> response = new HashMap<>();
        response.put("titre", "Erreur de validation");
        response.put("message", "Les Données Saisies Sont Invalides");
        response.put("erreurs", errors);
        response.put("code", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }




    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
         ErrorResponse errorResponse = new ErrorResponse(
                 "Custom Error",
                 ex.getMessage(),
                 HttpStatus.BAD_REQUEST.value());
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<?> handleRoleNotFoundException(RoleNotFoundException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("message", ex.getMessage());
        return new ResponseEntity<>(map,HttpStatus.NOT_FOUND);
    }



    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("⚠ Tentative de connexion échouée: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Identifiants incorrects");
        body.put("detail", "Le nom d'utilisateur ou le mot de passe est incorrect");
        body.put("code", HttpStatus.UNAUTHORIZED.value());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException .class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("⚠ Utilisateur non trouvé: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Utilisateur non trouvé");
        body.put("detail", ex.getMessage());
        body.put("code", HttpStatus.UNAUTHORIZED.value());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<?> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex) {
        log.error("Erreur d'authentification interne: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Erreur d'authentification");
        body.put("detail", "Une erreur interne s'est produite lors de l'authentification");
        body.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Gère les erreurs d'autorisation (@PreAuthorize)
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("⛔ Accès refusé : {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "Accès refusé",
                "Vous n'avez pas les permissions nécessaires pour accéder à cette ressource",
                HttpStatus.FORBIDDEN.value()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gère les erreurs d'accès refusé (ancien type)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        log.warn("⛔ Accès refusé : {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "Accès refusé",
                "Vous n'avez pas les permissions nécessaires pour accéder à cette ressource",
                HttpStatus.FORBIDDEN.value()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gère les erreurs d'authentification
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
        log.warn("🔐 Erreur d'authentification : {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "Authentification requise",
                "Vous devez être connecté pour accéder à cette ressource",
                HttpStatus.UNAUTHORIZED.value()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }


    /**
     * Gestion des exceptions ProduitNotFoundException
     */
    @ExceptionHandler(ProduitException.ProduitNotFoundException.class)
    public ResponseEntity<ErrorResponse2> handleProduitNotFound(
            ProduitException.ProduitNotFoundException ex) {

        log.error("Produit non trouvé : {}", ex.getMessage());

        ErrorResponse2 error = new ErrorResponse2(
                HttpStatus.NOT_FOUND.value(),
                "Produit non trouvé",
                ex.getMessage(),
                "/api/produits"
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gestion des exceptions StockInsuffisantException
     */
    @ExceptionHandler(ProduitException.StockInsuffisantException.class)
    public ResponseEntity<ErrorResponse2> handleStockInsuffisant(
            ProduitException.StockInsuffisantException ex) {

        log.error("Stock insuffisant : {}", ex.getMessage());

        ErrorResponse2 error = new ErrorResponse2(
                HttpStatus.CONFLICT.value(),
                "Stock insuffisant",
                ex.getMessage(),
                "/api/produits"
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gestion des exceptions QuantiteInvalideException
     */
    @ExceptionHandler(ProduitException.QuantiteInvalideException.class)
    public ResponseEntity<ErrorResponse2> handleQuantiteInvalide(
            ProduitException.QuantiteInvalideException ex) {

        log.error("Quantité invalide : {}", ex.getMessage());

        ErrorResponse2 error = new ErrorResponse2(
                HttpStatus.BAD_REQUEST.value(),
                "Quantité invalide",
                ex.getMessage(),
                "/api/produits"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gestion des exceptions CodeProduitExistantException
     */
    @ExceptionHandler(ProduitException.CodeProduitExistantException.class)
    public ResponseEntity<ErrorResponse2> handleCodeProduitExistant(
            ProduitException.CodeProduitExistantException ex) {

        log.error("Code produit existant : {}", ex.getMessage());

        ErrorResponse2 error = new ErrorResponse2(
                HttpStatus.CONFLICT.value(),
                "Code produit déjà existant",
                ex.getMessage(),
                "/api/produits"
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gestion des exceptions ProduitAvecReservationsException
     */
    @ExceptionHandler(ProduitException.ProduitAvecReservationsException.class)
    public ResponseEntity<ErrorResponse2> handleProduitAvecReservations(
            ProduitException.ProduitAvecReservationsException ex) {

        log.error("Produit avec réservations actives : {}", ex.getMessage());

        ErrorResponse2 error = new ErrorResponse2(
                HttpStatus.CONFLICT.value(),
                "Produit avec réservations actives",
                ex.getMessage(),
                "/api/produits"
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gestion générique des ProduitException
     */
    @ExceptionHandler(ProduitException.class)
    public ResponseEntity<ErrorResponse2> handleProduitException(ProduitException ex) {
        log.error("Erreur produit : {}", ex.getMessage());

        ErrorResponse2 error = new ErrorResponse2(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur produit",
                ex.getMessage(),
                "/api/produits"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gestion de toutes les autres exceptions non prévues
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse2> handleGeneralException(Exception ex) {
        log.error(" 🚨 Erreur interne du serveur : ", ex);

        ErrorResponse2 error = new ErrorResponse2(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "🚨Erreur interne du serveur",
                " 🚨 Une erreur inattendue s'est produite. Veuillez réessayer plus tard.",
                "/api"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
