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

    private final ReservationRepository reservationRepo;
    private final LigneReservationRepository ligneReservationRepo;

    /**
     * Job Cron qui s'exécute tous les jours à 00:00 (minuit)
     *
     * Format Cron: "0 0 0 * * ?" = seconde minute heure jour mois jour-de-la-semaine
     * - 0 = 0 secondes
     * - 0 = 0 minutes
     * - 0 = 0 heures (minuit)
     * - * = chaque jour
     * - * = chaque mois
     * - ? = n'importe quel jour de la semaine
     */
    @Scheduled(cron = "0 0 0 * * ?") // Tous les jours à minuit
    @Transactional
    public void mettreAJourStatutsLivraisonQuotidien() {
        log.info("⏰ 🚀 DEBUT - Job Cron de mise à jour des statuts de livraison");
        log.info("📅 Date du jour: {}", LocalDate.now());

        try {
            // Compteurs pour les logs
            int reservationsMisesAJour = 0;
            int lignesMisesAJour = 0;

            // ============================================
            // PARTIE 1: MISE À JOUR DES RÉSERVATIONS
            // ============================================

            // Trouver toutes les réservations confirmées dont au moins une ligne commence aujourd'hui
            List<Reservation> reservations = reservationRepo.findAll().stream()
                    .filter(r -> r.getStatutReservation() == StatutReservation.CONFIRME)
                    .filter(r -> r.getLigneReservations().stream()
                            .anyMatch(ligne -> ligne.getDateDebut().equals(LocalDate.now())))
                    .toList();

            log.info("🔍 {} réservation(s) confirmée(s) avec des lignes débutant aujourd'hui trouvée(s)",
                    reservations.size());

            for (Reservation reservation : reservations) {
                try {
                    // Mettre la réservation en EN_COURS
                    reservation.setStatutLivraisonRes(StatutLivraison.EN_ATTENTE);
                    reservationRepo.save(reservation);

                    reservationsMisesAJour++;
                    log.info("✅ Réservation {} passée en EN_COURS",
                            reservation.getReferenceReservation());

                } catch (Exception e) {
                    log.error("❌ Erreur lors de la mise à jour de la réservation {}: {}",
                            reservation.getReferenceReservation(), e.getMessage());
                }
            }

            // ============================================
            // PARTIE 2: MISE À JOUR DES LIGNES DE RÉSERVATION
            // ============================================

            // Trouver toutes les lignes de réservation confirmées qui commencent aujourd'hui
            // et qui ne sont pas encore en EN_ATTENTE ou plus
            List<LigneReservation> lignes = ligneReservationRepo.findAll().stream()
                    .filter(ligne -> ligne.getReservation().getStatutReservation() == StatutReservation.CONFIRME)
                    .filter(ligne -> ligne.getDateDebut().equals(LocalDate.now()))
                    .filter(ligne -> ligne.getStatutLivraisonLigne() == StatutLivraison.NOT_TODAY
                            || ligne.getStatutLivraisonLigne() == null)
                    .toList();

            log.info("🔍 {} ligne(s) de réservation débutant aujourd'hui et nécessitant une mise à jour trouvée(s)",
                    lignes.size());

            for (LigneReservation ligne : lignes) {
                try {
                    // Mettre la ligne en EN_ATTENTE (en attente de livraison)
                    StatutLivraison ancienStatut = ligne.getStatutLivraisonLigne();
                    ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
                    ligneReservationRepo.save(ligne);

                    lignesMisesAJour++;
                    log.info("✅ Ligne {} (Produit: {}) passée de {} à EN_ATTENTE",
                            ligne.getIdLigneReservation(),
                            ligne.getProduit().getNomProduit(),
                            ancienStatut != null ? ancienStatut : "null");

                } catch (Exception e) {
                    log.error("❌ Erreur lors de la mise à jour de la ligne {}: {}",
                            ligne.getIdLigneReservation(), e.getMessage());
                }
            }

            // ============================================
            // PARTIE 3: RÉSUMÉ ET LOGS FINAUX
            // ============================================

            log.info("📊 ========== RÉSUMÉ DE LA MISE À JOUR ==========");
            log.info("📈 Réservations mises à jour: {}", reservationsMisesAJour);
            log.info("📦 Lignes de réservation mises à jour: {}", lignesMisesAJour);
            log.info("⏰ ✅ FIN - Job Cron terminé avec succès");

        } catch (Exception e) {
            log.error("❌ ⚠️ ERREUR CRITIQUE dans le job Cron de mise à jour des statuts: {}", e.getMessage());
            log.error("Stack trace:", e);
        }
    }

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

    /**
     * OPTIONNEL: Méthode manuelle pour forcer la mise à jour (pour les tests)
     * À appeler via un endpoint REST si nécessaire
     */
    public void forcerMiseAJourManuelle() {
        log.info("🔧 Exécution MANUELLE du job de mise à jour des statuts");
        mettreAJourStatutsLivraisonQuotidien();
    }
}