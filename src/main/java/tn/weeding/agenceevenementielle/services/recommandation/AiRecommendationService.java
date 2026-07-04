package tn.weeding.agenceevenementielle.services.recommandation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.dto.recommandation.AiRecommendationRequest;
import tn.weeding.agenceevenementielle.dto.recommandation.AiRecommendationResponse;
import tn.weeding.agenceevenementielle.dto.recommandation.ProductForAiRecommendation;
import tn.weeding.agenceevenementielle.entities.Produit;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;
import tn.weeding.agenceevenementielle.repository.ProduitRepository;
import tn.weeding.agenceevenementielle.services.chatbot.OpenRouterChatbotService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final OpenRouterChatbotService openRouterChatbotService;
    private final ProduitRepository produitRepository;

    public AiRecommendationResponse generateRecommendation(AiRecommendationRequest request) {

        validateDates(request.getStartDate(), request.getEndDate());

        List<ProductForAiRecommendation> availableProducts =
                getAvailableProductsForAi(request.getStartDate(), request.getEndDate());

        if (availableProducts.isEmpty()) {
            return new AiRecommendationResponse(
                    "Aucun produit disponible n’a été trouvé pour cette période. " +
                            "Veuillez essayer une autre date ou consulter le catalogue."
            );
        }

        List<Map<String, String>> messages = List.of(
                Map.of(
                        "role", "system",
                        "content", buildSystemPrompt()
                ),
                Map.of(
                        "role", "user",
                        "content", buildUserPrompt(request, availableProducts)
                )
        );

        String recommendation = openRouterChatbotService.ask(messages);

        return new AiRecommendationResponse(recommendation);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("La date de fin doit être supérieure ou égale à la date de début.");
        }
    }

    private List<ProductForAiRecommendation> getAvailableProductsForAi(LocalDate startDate, LocalDate endDate) {

        return produitRepository.findProduitsDisponiblesSurPeriode(startDate, endDate)
                .stream()
                .map(produit -> toProductForAi(produit, startDate, endDate))
                .filter(product -> product.getQuantiteDisponible() != null && product.getQuantiteDisponible() > 0)
                .sorted(Comparator.comparing(ProductForAiRecommendation::getCategorie))
                .limit(35)
                .toList();
    }

    private ProductForAiRecommendation toProductForAi(
            Produit produit,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Integer quantiteDisponible;

        if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
            quantiteDisponible = produitRepository.calculerQuantiteDisponibleSurPeriode(
                    produit.getIdProduit(),
                    startDate,
                    endDate
            );
        } else {
            quantiteDisponible = produit.getQuantiteDisponible();
        }

        if (quantiteDisponible == null) {
            quantiteDisponible = 0;
        }

        return new ProductForAiRecommendation(
                produit.getIdProduit(),
                produit.getNomProduit(),
                produit.getCategorieProduit() != null ? produit.getCategorieProduit().name() : "Non classé",
                produit.getTypeProduit() != null ? produit.getTypeProduit().name() : "Non précisé",
                produit.getPrixUnitaire(),
                quantiteDisponible
        );
    }

    private String buildSystemPrompt() {
        return """
                Tu es un assistant IA de recommandation pour la plateforme Elegant Hive.

                Elegant Hive est une plateforme de gestion pour une agence événementielle
                spécialisée dans la location de matériel et la gestion des réservations.

                Ton rôle est de recommander des produits réels disponibles dans la plateforme.

                Règles obligatoires :
                - Réponds toujours en français.
                - Réponds avec 6 à 10 lignes maximum.
                - Recommande uniquement les produits présents dans la liste fournie.
                - Ne jamais inventer un produit.
                - Ne jamais proposer traiteur, photographe, salle ou animation.
                - Ne donne jamais un prix exact total.
                - Ne confirme pas une disponibilité autre que celle fournie dans la liste.
                - Mentionne les produits par leur nom exact.
                - Si la liste ne couvre pas totalement le besoin, dis-le clairement.
                - Termine toujours par : "Ajoutez les produits au panier puis demandez un devis pour obtenir le prix final."
                """;
    }

    private String buildUserPrompt(
            AiRecommendationRequest request,
            List<ProductForAiRecommendation> availableProducts
    ) {
        return """
                Besoin du client :
                Type d'événement : %s
                Nombre d'invités : %d
                Budget : %s
                Type de lieu : %s
                Date début : %s
                Date fin : %s
                Remarques : %s

                Liste réelle des produits disponibles sur cette période :
                %s

                Tâche :
                Recommande les produits les plus adaptés depuis cette liste uniquement.
                le nom de produit en Gras et la quantité disponible aussi 
                Pour chaque produit recommandé, explique trés brièvement son utilité.
                ecriture sous forme des tirets 
                Ne recommande pas plus de 8 produits.
                """.formatted(
                request.getEventType(),
                request.getGuestCount(),
                request.getBudget(),
                request.getLocationType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes() == null || request.getNotes().isBlank()
                        ? "Aucune remarque"
                        : request.getNotes(),
                formatProductsForPrompt(availableProducts)
        );
    }

    private String formatProductsForPrompt(List<ProductForAiRecommendation> products) {

        StringBuilder builder = new StringBuilder();

        for (ProductForAiRecommendation product : products) {
            builder.append("- ID ")
                    .append(product.getIdProduit())
                    .append(" | Nom : ")
                    .append(product.getNomProduit())
                    .append(" | Catégorie : ")
                    .append(product.getCategorie())
                    .append(" | Type : ")
                    .append(product.getTypeProduit())
                    .append(" | Prix unitaire : ")
                    .append(product.getPrixUnitaire())
                    .append(" DT")
                    .append(" | Quantité disponible : ")
                    .append(product.getQuantiteDisponible())
                    .append("\n");
        }

        return builder.toString();
    }
}