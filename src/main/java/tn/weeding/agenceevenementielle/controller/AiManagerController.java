package tn.weeding.agenceevenementielle.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.weeding.agenceevenementielle.dto.statistiques.AiManagerSummaryResponse;
import tn.weeding.agenceevenementielle.dto.statistiques.AnalyseAiHistoriqueResponse;
import tn.weeding.agenceevenementielle.services.statistiques.AiManagerService;

import java.util.List;

@RestController
@RequestMapping("/api/manager/ai")
@RequiredArgsConstructor
public class AiManagerController {

    private final AiManagerService aiManagerService;

    @GetMapping("/summary")
    public ResponseEntity<AiManagerSummaryResponse> getSummary() {
        return ResponseEntity.ok(aiManagerService.generateSummary());
    }

    @GetMapping("/historique")
    public ResponseEntity<List<AnalyseAiHistoriqueResponse>> getHistory() {
        return ResponseEntity.ok(aiManagerService.getHistory());
    }
}
