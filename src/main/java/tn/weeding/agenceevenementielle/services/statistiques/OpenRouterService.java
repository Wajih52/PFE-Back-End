package tn.weeding.agenceevenementielle.services.statistiques;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .build();

    public String ask(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "Tu es un assistant IA spécialisé dans l'analyse managériale d'une agence événementielle."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    )
            );

            Map response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "http://localhost:4200")
                    .header("X-OpenRouter-Title", "Elegant Hive PFE")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractText(response);

        } catch (WebClientResponseException e) {

            System.out.println("OpenRouter model = " + model);

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "Erreur OpenRouter : clé API invalide ou absente.";
            }

            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "Quota OpenRouter dépassé. Veuillez réessayer plus tard.";
            }

            return "Erreur OpenRouter : " + e.getStatusCode() + " - " + e.getResponseBodyAsString();

        } catch (Exception e) {
            return "Impossible de générer l'analyse intelligente pour le moment.";
        }
    }

    private String extractText(Map response) {
        try {
            List choices = (List) response.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");
            return message.get("content").toString();
        } catch (Exception e) {
            return "Réponse OpenRouter reçue mais impossible à interpréter.";
        }
    }
}