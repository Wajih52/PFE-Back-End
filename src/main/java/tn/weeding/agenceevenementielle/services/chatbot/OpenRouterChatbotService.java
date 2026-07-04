package tn.weeding.agenceevenementielle.services.chatbot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterChatbotService {

    @Value("${openrouter.chatbot.api.key}")
    private String apiKey;

    @Getter
    @Value("${openrouter.chatbot.model}")
    private String model;

    @Value("${openrouter.site.url}")
    private String url;

    @Value("${openrouter.chatbot.app.name}")
    private String title;

    @Value("${openrouter.temperature}")
    private double temperature;

    @Value("${openrouter.max-tokens}")
    private int maxTokens;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .build();

    public String ask(List<Map<String, String>> messages) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", temperature,
                    "max_tokens", maxTokens
            );

            Map response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", url)
                    .header("X-OpenRouter-Title", title)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractText(response);

        } catch (WebClientResponseException e) {

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "Erreur OpenRouter : clé API du chatbot invalide ou absente.";
            }

            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "Quota OpenRouter du chatbot dépassé. Veuillez réessayer plus tard.";
            }

            return "Erreur OpenRouter Chatbot : " + e.getStatusCode() + " - " + e.getResponseBodyAsString();

        } catch (Exception e) {
            return "Impossible de générer une réponse du chatbot pour le moment.";
        }
    }

        public String buildSystemPrompt() {
        return """
                Tu es le chatbot officiel de la plateforme Elegant Hive.
                             
                           Elegant Hive est une plateforme web de gestion pour une agence événementielle
                           spécialisée dans la location de matériel et la gestion des réservations.
                             
                           Tu dois guider le client uniquement selon le fonctionnement réel de la plateforme.
                             
                           Fonctionnement de la plateforme côté client :
                           1. Le client consulte le catalogue des produits.
                           2. Il peut filtrer les produits selon la date de réservation souhaitée.
                           3. Il consulte les détails d’un produit et ses évaluations.
                           4. Il ajoute les produits souhaités au panier.
                           5. Dans le panier, il choisit les quantités, les dates et peut ajouter des remarques.
                           6. Il peut demander un devis.
                           7. Le manager ou l’administrateur traite le devis et peut appliquer une remise.
                           8. Le client reçoit le devis validé.
                           9. Le client accepte le devis pour transformer la demande en réservation confirmée.
                           10. Une facture est générée.
                           11. Le client peut consulter et télécharger ses factures.
                           12. Le client peut effectuer un paiement total ou partiel selon les modes disponibles(D17,Especes1 ou virement avec indication de réference de transaction).
                           13. Le client peut suivre l’état de sa commande et de sa livraison.
                           14. Après l’événement, il peut suivre le retour du matériel.
                           15. Il peut soumettre une réclamation en cas de problème.
                           16. Il peut laisser une évaluation sur les produits.
                             
                           Règles obligatoires :
                           - Réponds toujours en français.
                           - Réponds avec 4 à 7 lignes maximum.
                           - Ne donne jamais une réponse trop longue.
                           - Ne propose pas des services non confirmés par la plateforme comme traiteur, photographie, salle ou animation, sauf si l’utilisateur parle simplement d’un besoin général.
                           - Ne confirme jamais la disponibilité réelle d’un produit.
                           - Si le client demande la disponibilité, explique qu’il doit utiliser le filtre par date dans le catalogue.
                           - Si le client demande un prix exact, explique qu’il doit ajouter les produits au panier puis demander un devis.
                           - Si le client demande le suivi d’une commande, dis-lui de consulter son espace client.
                           - Pose au maximum deux questions si des informations sont nécessaires.
                           - Ne parle jamais de l’API, du modèle IA, des tokens ou des détails techniques internes.
                """;
    }

    private String extractText(Map response) {
        try {
            System.out.println("Réponse brute OpenRouter Chatbot = " + response);

            if (response == null) {
                return "Réponse OpenRouter vide.";
            }

            if (response.containsKey("error")) {
                Map error = (Map) response.get("error");
                return "Erreur OpenRouter : " + error.get("message");
            }

            List choices = (List) response.get("choices");

            if (choices == null || choices.isEmpty()) {
                return "OpenRouter n'a retourné aucun choix de réponse.";
            }

            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");

            if (message == null) {
                return "OpenRouter n'a pas retourné de message.";
            }

            Object content = message.get("content");

            if (content == null) {
                return "OpenRouter a retourné une réponse sans contenu.";
            }

            return content.toString();

        } catch (Exception e) {
            System.out.println("Erreur extractText chatbot = " + e.getMessage());
            System.out.println("Réponse problématique = " + response);
            return "Réponse OpenRouter reçue mais impossible à interpréter.";
        }
    }
}