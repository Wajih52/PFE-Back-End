package tn.weeding.agenceevenementielle.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.weeding.agenceevenementielle.entities.Role;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.UtilisateurRole;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional
    @Cacheable (value = "users", key = "#identifiant")
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {

        log.info("🔍 Tentative de chargement de l'utilisateur avec identifiant: {}", identifiant);

        // Chercher par pseudo OU email
        Utilisateur utilisateur = utilisateurRepository.findByPseudoOrEmail(identifiant, identifiant)
                .filter(Utilisateur::getActivationCompte)
                .orElseThrow(() -> {
                    log.error("❌ Utilisateur introuvable ou compte non activé ⚠️  : {}", identifiant);
                    return new UsernameNotFoundException("Identifiant ou mot de passe incorrect");
                });


        // Charger les rôles de l'utilisateur
        Collection<? extends GrantedAuthority> authorities = getAuthorities(utilisateur);

        log.info("✅ Utilisateur '{}' chargé avec {} rôle(s)", utilisateur.getPseudo(), authorities.size());

        return User.builder()
                .username(utilisateur.getPseudo())
                .password(utilisateur.getMotDePasse())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!utilisateur.getActivationCompte())
                .build();
    }

        /**
         * Récupère tous les rôles de l'utilisateur
         */
        private Collection<? extends GrantedAuthority> getAuthorities(Utilisateur utilisateur) {
            Set<GrantedAuthority> authorities = new HashSet<>();

            // Récupérer tous les rôles de l'utilisateur via UtilisateurRole
            Set<Role> roles = utilisateur.getUtilisateurRoles().stream()
                    .map(UtilisateurRole::getRole)
                    .collect(Collectors.toSet());

            // Ajouter les rôles avec le préfixe ROLE_
            for (Role role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getNom()));
                log.debug("Ajout du rôle: ROLE_{}", role.getNom());
            }


            return authorities;
        }



}
