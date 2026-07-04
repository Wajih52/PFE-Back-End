package tn.weeding.agenceevenementielle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.recommandation.AiRecommendationRequest;
import tn.weeding.agenceevenementielle.dto.recommandation.AiRecommendationResponse;
import tn.weeding.agenceevenementielle.services.recommandation.AiRecommendationService;

@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/event-pack")
    public AiRecommendationResponse generateEventPackRecommendation(
            @Valid @RequestBody AiRecommendationRequest request
    ) {
        return aiRecommendationService.generateRecommendation(request);
    }
}