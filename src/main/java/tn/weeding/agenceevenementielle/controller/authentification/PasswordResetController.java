package tn.weeding.agenceevenementielle.controller.authentification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.authentification.PasswordResetDto;
import tn.weeding.agenceevenementielle.dto.authentification.PasswordResetRequestDto;
import tn.weeding.agenceevenementielle.services.PasswordResetService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> demanderReinitialisation(
            @Valid @RequestBody PasswordResetRequestDto request) {

        passwordResetService.demanderReinitialisationMotDePasse(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Un email de réinitialisation a été envoyé");
        response.put("detail", "Vérifiez votre boîte mail (valide 30 minutes)");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reinitialiserMotDePasse(
            @Valid @RequestBody PasswordResetDto request) {

        passwordResetService.reinitialiserMotDePasse(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mot de passe réinitialisé avec succès");
        response.put("detail", "Vous pouvez maintenant vous connecter");

        return ResponseEntity.ok(response);
    }
}
