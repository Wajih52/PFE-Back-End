package tn.weeding.agenceevenementielle.controller.authentification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2/test")
@RequiredArgsConstructor
@Slf4j
public class OAuth2TestController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @GetMapping("/status")
    public ResponseEntity<?> checkOAuth2Status() {
        Map<String, Object> status = new HashMap<>();

        try {
            ClientRegistration googleClient = clientRegistrationRepository.findByRegistrationId("google");

            if (googleClient != null) {
                status.put("oauth2Enabled", true);
                status.put("clientName", googleClient.getClientName());
                status.put("clientId", googleClient.getClientId().substring(0, 10) + "...");
                status.put("scopes", googleClient.getScopes());
                status.put("redirectUri", googleClient.getRedirectUri());
            } else {
                status.put("oauth2Enabled", false);
                status.put("error", "Google client registration not found");
            }

        } catch (Exception e) {
            status.put("oauth2Enabled", false);
            status.put("error", e.getMessage());
        }

        return ResponseEntity.ok(status);
    }
}