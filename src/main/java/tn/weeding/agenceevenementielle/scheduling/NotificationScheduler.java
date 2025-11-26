package tn.weeding.agenceevenementielle.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.repository.NotificationRepository;

import java.time.LocalDateTime;

/**
 * Job CRON pour le nettoyage automatique des anciennes notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationRepository notificationRepo;

    /**
     * Nettoyer les notifications de plus de 30 jours
     * Exécution : Tous les dimanches à 3h du matin
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void nettoyerAnciennesNotifications() {
        log.info("🧹 Démarrage du nettoyage des anciennes notifications...");

        LocalDateTime dateLimit = LocalDateTime.now().minusDays(30);

        try {
            notificationRepo.deleteByDateCreationBefore(dateLimit);
            log.info("✅ Nettoyage terminé - Notifications avant {} supprimées", dateLimit);
        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage : {}", e.getMessage());
        }
    }
}