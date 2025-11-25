package tn.weeding.agenceevenementielle.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tn.weeding.agenceevenementielle.services.CustomUserDetailsService;
import tn.weeding.agenceevenementielle.services.GoogleOAuth2UserService;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private GoogleOAuth2UserService googleOAuth2UserService;

    //pour autoriser l'utilisation des End Points
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(e->
                        e.authenticationEntryPoint(unauthorizedHandler)
                )
                .sessionManagement(s->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(a->
                        a.requestMatchers("/auth/**",
                                          "/inscriptions/**",
                                          "/oauth2/**",
                                          "/login/oauth2/**",
                                          "/swagger-ui/**",
                                          "/v3/api-docs/**",
                                          "/swagger-resources/**",
                                          "/swagger-ui.html",
                                          "/webjars/**",
                                          "/uploads/**",
                                          "/error",
                                          "/api/reclamations/create"
                        ).permitAll().anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2LoginSuccessHandler())
                );
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    //  Fournit l’AuthenticationManager pour déclencher la connexion
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }
    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedDoubleSlash(true);
        return firewall;
    }

    @Bean
    public OAuth2LoginSuccessHandler oauth2LoginSuccessHandler() {
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(jwtUtil, googleOAuth2UserService);
        log.info("✅ OAuth2LoginSuccessHandler bean créé avec succès");
        return handler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Autoriser Angular (localhost:4200)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));

        // Autoriser toutes les méthodes HTTP
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Autoriser tous les headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Autoriser l'envoi de credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // Durée de cache de la config CORS (en secondes)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("✅ Configuration CORS activée pour http://localhost:4200");
        return source;
    }
}
