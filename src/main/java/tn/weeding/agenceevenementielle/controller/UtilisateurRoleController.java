package tn.weeding.agenceevenementielle.controller;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.UtilisateurRoleResponseDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRoleWithUserDto;
import tn.weeding.agenceevenementielle.entities.UtilisateurRole;
import tn.weeding.agenceevenementielle.services.UtilisateurRoleServiceInterface;

import java.util.List;


@RestController
@RequestMapping("/api/utilisateur-roles")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurRoleController {

    private UtilisateurRoleServiceInterface utilisateurRoleService;

    @GetMapping("/afficheUtilisateur-Role")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UtilisateurRole>> getAllUtilisateurRoles() {
        List<UtilisateurRole> roles = utilisateurRoleService.getAllUtilisateurRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/Utilisateur-Role/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UtilisateurRole> getUtilisateurRoleById(@PathVariable Long id) {
        return utilisateurRoleService.getUtilisateurRoleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/ajoutUtilisateur-Role")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UtilisateurRole> addUtilisateurRole( @RequestParam Long idUtilisateur,
                                                               @RequestParam Long idRole) {
        UtilisateurRole saved = utilisateurRoleService.addUtilisateurRole(idUtilisateur, idRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/modifierUtilisateur-Role/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UtilisateurRole> updateUtilisateurRole(@PathVariable Long id,
                                                                 @RequestParam(required = false) Long idRole) {
        UtilisateurRole updated = utilisateurRoleService.updateUtilisateurRole(id,idRole);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/supprimerUtilisateur-Roles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteUtilisateurRole(@PathVariable Long id) {
        utilisateurRoleService.deleteUtilisateurRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UtilisateurRole>> getRolesByUtilisateur(@PathVariable Long utilisateurId) {
        List<UtilisateurRole> roles = utilisateurRoleService.getRolesByUtilisateur(utilisateurId);
        return ResponseEntity.ok(roles);
    }

    /**
     * Récupère tous les rôles d'un utilisateur avec les détails d'attribution
     * Utilisé pour afficher dans le modal de gestion des rôles
     */
    @GetMapping("/utilisateur/{utilisateurId}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UtilisateurRoleResponseDto>> getRolesWithDetailsByUtilisateur(
            @PathVariable Long utilisateurId) {
        List<UtilisateurRoleResponseDto> rolesDetails = utilisateurRoleService.getRolesWithDetailsByUtilisateur(utilisateurId);
        return ResponseEntity.ok(rolesDetails);
    }

    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UtilisateurRoleWithUserDto>> getAssociationUtilisateursByRole(@PathVariable Long roleId) {
        List<UtilisateurRoleWithUserDto> associationUtilisateurs =
                utilisateurRoleService.getAssociationUtilisateursByRole(roleId);
        return ResponseEntity.ok(associationUtilisateurs);
    }
    }
