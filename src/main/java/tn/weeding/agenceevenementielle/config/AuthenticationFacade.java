package tn.weeding.agenceevenementielle.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

@Component
@RequiredArgsConstructor
public class AuthenticationFacade {

    private final UtilisateurRepository utilisateurRepository;

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
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByPseudo(username)
                .orElseThrow(() -> new CustomException("Utilisateur non trouvé"));

        return utilisateur.getIdUtilisateur();
    }
}