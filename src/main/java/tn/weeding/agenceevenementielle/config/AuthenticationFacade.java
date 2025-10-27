package tn.weeding.agenceevenementielle.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacade {

    /**
     * Récupère l'authentification actuelle
     */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Récupère le pseudo de l'utilisateur connecté
     */
    public String getCurrentUserPseudo() {
        Authentication authentication = getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "SYSTEM";  // Par défaut si pas connecté
        }

        return authentication.getName();
    }

    /**
     * Vérifie si un utilisateur est connecté
     */
    public boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }
}