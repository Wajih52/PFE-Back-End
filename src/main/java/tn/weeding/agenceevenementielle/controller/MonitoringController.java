package tn.weeding.agenceevenementielle.controller;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.weeding.agenceevenementielle.config.TokenBlacklistService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/monitoring")
@AllArgsConstructor
public class MonitoringController {
    private TokenBlacklistService tokenBlacklistService;

    @GetMapping("/blacklist-status")
    public ResponseEntity<?> getBlacklistStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("mode", tokenBlacklistService.isUsingRedis() ? "REDIS" : "MÉMOIRE");
        status.put("tokensBlacklistes", tokenBlacklistService.countBlacklistedTokens());
        status.put("redis_actif", tokenBlacklistService.isUsingRedis());

        return ResponseEntity.ok(status);
    }
}
