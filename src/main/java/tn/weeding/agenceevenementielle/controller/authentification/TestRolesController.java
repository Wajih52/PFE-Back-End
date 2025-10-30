package tn.weeding.agenceevenementielle.controller.authentification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestRolesController {

    /**
     * Accessible à tous les utilisateurs authentifiés
     */
    @GetMapping("/user")
    public ResponseEntity<?> userAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Accès utilisateur autorisé");
        response.put("username", auth.getName());
        response.put("roles", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    /**
     * Accessible uniquement aux ADMIN
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Accès ADMIN autorisé");
        response.put("username", auth.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Accessible aux ADMIN ou MANAGER
     */
    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> managerAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Accès MANAGER autorisé");
        response.put("username", auth.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Accessible aux EMPLOYE, MANAGER et ADMIN
     */
    @GetMapping("/employe")
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> employeAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Accès EMPLOYE autorisé");
        response.put("username", auth.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Accessible uniquement aux CLIENT
     */
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> clientAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Accès CLIENT autorisé");
        response.put("username", auth.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Afficher tous les rôles de l'utilisateur connecté
     */
    @GetMapping("/my-roles")
    public ResponseEntity<?> myRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("username", auth.getName());
        response.put("authenticated", auth.isAuthenticated());
        response.put("roles", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }
}