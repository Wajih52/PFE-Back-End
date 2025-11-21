package tn.weeding.agenceevenementielle.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationRepository reservationRepo;

    /**
     * Tâche planifiée : Annuler automatiquement les devis expirés
     * Exécution : Tous les jours à 2h du matin
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void annulerDevisExpires() {
        log.info("⏰ Démarrage du job d'annulation des devis expirés...");

        LocalDateTime maintenant = LocalDateTime.now();

        // Récupérer les devis EN_ATTENTE expirés
        List<Reservation> devisExpires = reservationRepo
                .findByStatutReservationAndDateExpirationDevisBefore(
                        StatutReservation.EN_ATTENTE,
                        maintenant
                );

        log.info("📋 {} devis expirés trouvés", devisExpires.size());

        for (Reservation devis : devisExpires) {
            try {
                log.warn("❌ Annulation du devis expiré: {}",
                        devis.getReferenceReservation());

                // ✅ SOFT BOOKING: Pas de stock à libérer (jamais réservé)
                devis.setStatutReservation(StatutReservation.ANNULE);
                devis.setCommentaireAdmin(
                        "Devis annulé automatiquement après expiration (" +
                                devis.getDateExpirationDevis().toLocalDate() + ")"
                );
                reservationRepo.save(devis);

                log.info("✅ Devis {} annulé (pas de stock à libérer)",
                        devis.getReferenceReservation());

                // TODO: Envoyer notification email au client
                // notificationService.envoyerNotificationDevisExpire(devis);

            } catch (Exception e) {
                log.error("❌ Erreur lors de l'annulation du devis {}: {}",
                        devis.getReferenceReservation(), e.getMessage());
            }
        }

        log.info("✅ Job terminé : {} devis annulés", devisExpires.size());
    }
}
