package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

@Service("utilisateurSecurityService")
@RequiredArgsConstructor
public class UtilisateurSecurityService {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * Vérifie si l'utilisateur connecté est le propriétaire du profil
     */
    public boolean isOwner(Long utilisateurId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String pseudo = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByPseudo(pseudo).orElse(null);

        if (utilisateur == null) {
            return false;
        }

        return utilisateur.getIdUtilisateur().equals(utilisateurId);
    }

    /**
     * Vérifie si l'utilisateur connecté peut modifier un utilisateur
     * (soit il est ADMIN, soit c'est son propre profil)
     */
    public boolean canModify(Long utilisateurId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Si c'est un ADMIN, il peut tout modifier
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        // Sinon, seulement son propre profil
        return isOwner(utilisateurId);
    }
}