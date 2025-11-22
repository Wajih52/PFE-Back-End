package tn.weeding.agenceevenementielle.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.entities.LigneReservation;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.repository.LigneReservationRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Job Cron pour la mise à jour automatique des statuts de livraison
 * Sprint 6 - Gestion des livraisons
 *
 * Ce job s'exécute tous les jours à 00:00 (minuit) pour:
 * 1. Mettre les réservations dont la date de début est aujourd'hui en EN_COURS
 * 2. Mettre les lignes de réservation dont la date de début est aujourd'hui en EN_ATTENTE
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LivraisonScheduler {

    private final LigneReservationRepository ligneReservationRepo;

    /**
     * OPTIONNEL: Job Cron qui s'exécute toutes les heures pour vérifier les retours en retard
     *
     * Format Cron: "0 0 * * * ?" = toutes les heures à 0 minutes
     */
    @Scheduled(cron = "0 0 * * * ?") // Toutes les heures
    @Transactional
    public void verifierRetoursEnRetard() {
        log.info("🔍 Vérification des retours en retard...");

        try {
            List<LigneReservation> lignesEnRetard = ligneReservationRepo.findRetoursEnRetard();

            if (!lignesEnRetard.isEmpty()) {
                log.warn("⚠️ {} ligne(s) de réservation en retard de retour détectée(s)", lignesEnRetard.size());

                for (LigneReservation ligne : lignesEnRetard) {
                    log.warn("⚠️ Retour en retard - Réservation: {}, Produit: {}, Date fin prévue: {}",
                            ligne.getReservation().getReferenceReservation(),
                            ligne.getProduit().getNomProduit(),
                            ligne.getDateFin());

                    // TODO Sprint 7: Envoyer une notification à l'admin
                }
            } else {
                log.info("✅ Aucun retour en retard détecté");
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification des retours en retard: {}", e.getMessage());
        }
    }

}