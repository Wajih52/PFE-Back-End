package tn.weeding.agenceevenementielle.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.weeding.agenceevenementielle.services.CustomUserDetailsService;

import java.io.IOException;

@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String BEARER_ = "Bearer ";
    @Autowired
    private JwtUtil jwtUtil ;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {


            try {
                String jwt = parseJwt(request);
                if (jwt != null) {
                    // 1. Vérifier si le token est blacklisté
                    if (tokenBlacklistService.isBlacklisted(jwt)) {
                        log.warn("❌ Tentative d'utilisation d'un token blacklisté");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 2. Valider le token JWT
                    if (jwtUtil.validateJwtToken(jwt)) {
                        final String identifiant = jwtUtil.getUserFromToken(jwt);

                        // 3. Charger l'utilisateur avec ses rôles
                        final UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifiant);

                        // 4. Créer l'authentification avec les autorités (rôles)
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities());

                        authenticationToken.setDetails(new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                        // 5. Définir l'authentification dans le contexte de sécurité
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                        log.debug("✅ Utilisateur '{}' authentifié avec {} rôles",
                                identifiant, userDetails.getAuthorities().size());
                    }
                }
            }catch (Exception e) {
                log.error("❌ Erreur lors de l'authentification de l'utilisateur: {}", e.getMessage());
            }
            filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith(BEARER_)) {
            return headerAuth.substring(BEARER_.length());
        }
        return null ;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars")
                || path.startsWith("/api/monitoring")
                || path.startsWith("/api/oauth2")
                || path.startsWith("/api/login/oauth2")
                || path.startsWith("api//utilisateur/ajouter")
                || path.startsWith("/uploads");
    }
}
