package tn.weeding.agenceevenementielle.controller;


import jakarta.validation.Valid;
import lombok.AllArgsConstructor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.authentification.ActiverCompteDto;
import tn.weeding.agenceevenementielle.dto.authentification.ChangePasswordDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRequestDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRequestPatchDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurResponseDto;
import tn.weeding.agenceevenementielle.services.UtilisateurServiceInterface;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/utilisateurs")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class UtilisateurController {

    UtilisateurServiceInterface utilisateurServiceInterface;


    /**
     *ajouter un utilisateur
     */
    @PostMapping("/ajouter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurResponseDto> ajouter(@Valid @RequestBody UtilisateurRequestDto utilisateurRequestDto) {
        UtilisateurResponseDto created = utilisateurServiceInterface.ajouterUtilisateur(utilisateurRequestDto);
        return ResponseEntity.created(URI.create("/utilisateurs/"+created.getIdUtilisateur())).body(created);
    }

    /**
     * modifier toutes les informations de l'utilisateur
     */
    @PutMapping("/modifier/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utilisateurSecurityService.isOwner(#id)")
    public ResponseEntity<UtilisateurResponseDto> modifier(@PathVariable Long id,@Valid @RequestBody UtilisateurRequestDto utilisateurRequestDto) {
        return ResponseEntity.ok(utilisateurServiceInterface.modifierUtilisateurPut(id,utilisateurRequestDto));
    }

    /**
     * modifier un de informations utilisateur
     */
    @PatchMapping("/modifierPartiel/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utilisateurSecurityService.isOwner(#id)")
    public ResponseEntity<UtilisateurResponseDto> modifierpatch(@PathVariable Long id,@Valid @RequestBody UtilisateurRequestPatchDto utilisateurRequestPatchDto) {
        return ResponseEntity.ok(utilisateurServiceInterface.modifierUtilisateurPatch(id,utilisateurRequestPatchDto));
    }

    /**
     * suprimer un compte (suppression physique )
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> supprimer(@PathVariable Long id) {

        utilisateurServiceInterface.supprimerUtilisateur(id);
        return ResponseEntity.ok( "Utilisateur supprimé avec succès");
    }

    /**
     * désactiver le compte ( peut etre activer auto en se connectant )
     */
    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN') or @utilisateurSecurityService.isOwner(#id)")
    public ResponseEntity<Map<String, String>> desactiverCompte(@PathVariable Long id) {
        log.info("Désactivation compte utilisateur ID: {}", id);
        utilisateurServiceInterface.desactiverCompte(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Compte désactivé avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * suspendre le compte ( il ne s'ouvre plus )
     */
    @PatchMapping("/{id}/suspendre")
    @PreAuthorize("hasRole('ADMIN') and @utilisateurSecurityService.canModify(#id)")
    public ResponseEntity<Map<String, String>> supendreCompte(@PathVariable Long id) {
        log.info("Suspendre compte utilisateur ID: {}", id);
        utilisateurServiceInterface.suspenduCompte(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Compte Suspendu avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * activer le compte
     */
    @PostMapping("/{id}/activer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activerCompte(@PathVariable Long id) {
        log.info("activer compte utilisateur ID: {}", id);
        utilisateurServiceInterface.activerCompte(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Compte Activé avec succès");
        return ResponseEntity.ok(response);
    }
    /**
     * archiver le compte (suppression logique )
     */
    @PostMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN')or @utilisateurSecurityService.isOwner(#id)")
    public ResponseEntity<Map<String, String>> archiverCompte(@PathVariable Long id) {
        log.info("archiver compte utilisateur ID: {}", id);
        utilisateurServiceInterface.archiverCompte(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Compte archivé avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * les informations de l'utilisateur avec l'id #id
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @utilisateurSecurityService.isOwner(#id)")
    public ResponseEntity<UtilisateurResponseDto> afficher(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurServiceInterface.afficherUtilisateur(id));
    }
    /**
     * la liste des utilisateurs
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<List<UtilisateurResponseDto>> afficherTous() {
        return ResponseEntity.status(200).body(utilisateurServiceInterface.afficherUtilisateurs());
    }
    /**
     * récupuréer les infos de l'utilisateur actuel
     */
    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponseDto> monProfil() {
        // Récupérer l'utilisateur authentifié
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Tentative d'accès à /me sans authentification");
            return ResponseEntity.status(401).build();
        }

        String pseudo = authentication.getName();
        log.info("Récupération des infos pour l'utilisateur : {}", pseudo);


        // Récupérer les infos de l'utilisateur
        UtilisateurResponseDto user = utilisateurServiceInterface.afficherParPseudo(pseudo);

        return ResponseEntity.ok(user);
    }

    /**
     * Modifier l'image de profil
     */
    @PatchMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN') or @utilisateurSecurityService.isOwner(#id)")
    public ResponseEntity<UtilisateurResponseDto> modifierImage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        log.info("Modification image utilisateur ID: {}", id);

        String base64Image = body.get("image");
        UtilisateurResponseDto response = utilisateurServiceInterface.modifierImage(id, base64Image);

        return ResponseEntity.ok(response);
    }

    /**
     * Changer le mot de passe
     */
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ADMIN') or @utilisateurSecurityService.isOwner(#id)")
    public ResponseEntity<Map<String, String>> changerMotDePasse(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordDto dto) {

        utilisateurServiceInterface.changerMotDePasse(id, dto);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mot de passe modifié avec succès");

        return ResponseEntity.ok(response);
    }

    /**
     * vérifier le token de premiere connexion et activer le compte
     */
    @PostMapping("/activer-compte")
    public ResponseEntity<?> activerCompte(@RequestBody ActiverCompteDto dto) {
        utilisateurServiceInterface.activerCompteViaToken(dto.getToken(), dto.getNouveauMotDePasse());
        return ResponseEntity.ok(Map.of("message", "Compte activé avec succès !"));
    }

    /**
     * vérifier le token de premiere connexion
     */
    @GetMapping("/verifier-token")
    public ResponseEntity<?> verifierToken(@RequestParam String token) {
        utilisateurServiceInterface.verifierTokenActivation(token);
        return ResponseEntity.ok(Map.of("message", "Token valide"));
    }

}
