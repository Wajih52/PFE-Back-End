package tn.weeding.agenceevenementielle.config;



import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.services.GoogleOAuth2UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final AppConfig appConfig ;

    @PostConstruct
    public void init() {
        log.info(">>> AppConfig.frontendUrl = {}", appConfig.getFrontendUrl());
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException{

        log.info(">>> onAuthenticationSuccess appelé !");
        log.info(">>> frontendUrl = {}", appConfig.getFrontendUrl());

        try {
            // Récupérer les infos de l'utilisateur Google
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String email = oAuth2User.getAttribute("email");
            String nom = oAuth2User.getAttribute("family_name");
            String prenom = oAuth2User.getAttribute("given_name");
            String photo = oAuth2User.getAttribute("picture");

            log.info(" Authentification Google réussie pour : {}", email);
            log.info(" Nom complet : {} {}", prenom, nom);

            // Créer ou récupérer l'utilisateur en base de données
            String pseudo = googleOAuth2UserService.processOAuth2User(email, prenom, nom, photo);

            //  Générer TON JWT
            String jwtToken = jwtUtil.generateToken(pseudo);
            log.info(" JWT généré pour l'utilisateur : {}", pseudo);

            // Rediriger vers Angular avec le token
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(appConfig.getFrontendUrl() + "/auth/oauth2/redirect")
                    .queryParam("token", jwtToken)
                    .build()
                    .toUriString();

            log.info("Redirection vers Angular : {}", redirectUrl);

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (CustomException ex)
        {
            if (ex.getMessage().equals("CONNEXION_EXIST_IN_CLASSIC_MODE")) {
                log.warn(" Tentative de connexion Google avec un compte classique existant:");

             String redirectUrl = appConfig.getFrontendUrl() + "/auth/login?access=false&message="+ URLEncoder.encode(
                     "Cet email est déjà utilisé avec une connexion Classique. Veuillez utiliser votre mot de passe",
                     StandardCharsets.UTF_8);
             response.sendRedirect(redirectUrl);
                return;
            }
            // Gérer d'autres CustomException
            String redirectUrl = appConfig.getFrontendUrl() + "/auth/login?error=" +
                    URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        }
        catch (Exception e) {
            log.error(" Erreur lors de l'authentification OAuth2 : {}", e.getMessage());

            // En cas d'erreur, rediriger vers une page d'erreur Angular
            String errorUrl = UriComponentsBuilder
                    .fromUriString(appConfig.getFrontendUrl() + "/login")
                    .queryParam("error", e.getMessage())
                    .build()
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }
}