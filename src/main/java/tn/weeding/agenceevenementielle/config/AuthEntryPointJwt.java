package tn.weeding.agenceevenementielle.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tn.weeding.agenceevenementielle.dto.ErrorResponse;

import java.io.IOException;
@Component
@Slf4j
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {
        log.error("Réponse non autorisée: {}", authException.getMessage());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        final ErrorResponse body = new ErrorResponse(
                "Accès non autorisé",
                "Token JWT manquant ou invalide",
                HttpServletResponse.SC_UNAUTHORIZED
        );
        response.getOutputStream().println(objectMapper.writeValueAsString(body));
    }
}
