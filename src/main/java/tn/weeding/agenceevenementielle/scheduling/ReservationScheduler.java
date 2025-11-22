package tn.weeding.agenceevenementielle.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.entities.InstanceProduit;
import tn.weeding.agenceevenementielle.entities.LigneReservation;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.enums.StatutInstance;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;
import tn.weeding.agenceevenementielle.repository.InstanceProduitRepository;
import tn.weeding.agenceevenementielle.repository.LigneReservationRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationRepository reservationRepo;
    private final LigneReservationRepository ligneReservationRepo;
    private final InstanceProduitRepository instanceProduitRepo;

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

    /**
     * ⏰ Job Cron qui s'exécute tous les jours à 00:01 (minuit + 1 minute)
     *
     * Format Cron: "0 1 0 * * ?" = seconde minute heure jour mois jour-de-la-semaine
     * - 0 = 0 secondes
     * - 1 = 1 minute
     * - 0 = 0 heures (minuit)
     * - * = chaque jour
     * - * = chaque mois
     * - ? = n'importe quel jour de la semaine
     */
    @Scheduled(cron = "0 1 0 * * ?") // Tous les jours à 00:01
    @Transactional
    public void mettreAJourStatutsQuotidien() {
        log.info("⏰ 🚀 DEBUT - Job Cron de mise à jour des statuts (NOT_TODAY → EN_ATTENTE)");
        log.info("📅 Date du jour: {}", LocalDate.now());

        try {
            LocalDate aujourdhui = LocalDate.now();

            // Compteurs pour les logs
            int reservationsMisesAJour = 0;
            int lignesMisesAJour = 0;
            int instancesMisesAJour = 0;

            // ============================================
            // PARTIE 1: MISE À JOUR DES RÉSERVATIONS
            // ============================================

            log.info("📋 ÉTAPE 1: Recherche des réservations avec statutLivraisonRes = NOT_TODAY...");

            // Trouver toutes les réservations CONFIRMÉES avec NOT_TODAY
            // dont au moins une ligne commence aujourd'hui
            List<Reservation> reservations = reservationRepo.findAll().stream()
                    .filter(r -> r.getStatutReservation() == StatutReservation.CONFIRME)
                    .filter(r -> r.getStatutLivraisonRes() == StatutLivraison.NOT_TODAY)
                    .filter(r -> r.getLigneReservations().stream()
                            .anyMatch(ligne -> ligne.getDateDebut().equals(aujourdhui)))
                    .toList();

            log.info("🔍 {} réservation(s) trouvée(s) avec NOT_TODAY et dateDebut = aujourd'hui",
                    reservations.size());

            for (Reservation reservation : reservations) {
                try {
                    StatutLivraison ancienStatut = reservation.getStatutLivraisonRes();

                    // ✅ Changer NOT_TODAY → EN_ATTENTE
                    reservation.setStatutLivraisonRes(StatutLivraison.EN_ATTENTE);
                    reservationRepo.save(reservation);

                    reservationsMisesAJour++;

                    log.info("✅ Réservation {} : {} → EN_ATTENTE",
                            reservation.getReferenceReservation(),
                            ancienStatut);

                } catch (Exception e) {
                    log.error("❌ Erreur lors de la mise à jour de la réservation {}: {}",
                            reservation.getReferenceReservation(), e.getMessage());
                }
            }

            // ============================================
            // PARTIE 2: MISE À JOUR DES LIGNES DE RÉSERVATION
            // ============================================

            log.info("📦 ÉTAPE 2: Recherche des lignes de réservation avec statutLivraisonLigne = NOT_TODAY...");

            // Trouver toutes les lignes avec NOT_TODAY dont la date de début est aujourd'hui
            List<LigneReservation> lignes = ligneReservationRepo.findAll().stream()
                    .filter(ligne -> ligne.getReservation().getStatutReservation() == StatutReservation.CONFIRME)
                    .filter(ligne -> ligne.getStatutLivraisonLigne() == StatutLivraison.NOT_TODAY)
                    .filter(ligne -> ligne.getDateDebut().equals(aujourdhui))
                    .toList();

            log.info("🔍 {} ligne(s) de réservation trouvée(s) avec NOT_TODAY et dateDebut = aujourd'hui",
                    lignes.size());

            for (LigneReservation ligne : lignes) {
                try {
                    StatutLivraison ancienStatut = ligne.getStatutLivraisonLigne();

                    // ✅ Changer NOT_TODAY → EN_ATTENTE
                    ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
                    ligneReservationRepo.save(ligne);

                    lignesMisesAJour++;

                    log.info("✅ Ligne {} (Réservation {}, Produit {}) : {} → EN_ATTENTE",
                            ligne.getIdLigneReservation(),
                            ligne.getReservation().getReferenceReservation(),
                            ligne.getProduit().getNomProduit(),
                            ancienStatut);

                    // ============================================
                    // PARTIE 3: MISE À JOUR DES INSTANCES
                    // ============================================

                    // Si produit avec référence, mettre les instances EN_ATTENTE
                    if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE
                            && ligne.getInstancesReservees() != null
                            && !ligne.getInstancesReservees().isEmpty()) {

                        for (InstanceProduit instance : ligne.getInstancesReservees()) {
                            // Vérifier que l'instance est bien disponible
                            if (instance.getStatut() == StatutInstance.DISPONIBLE) {
                                instance.setStatut(StatutInstance.EN_ATTENTE);
                                instanceProduitRepo.save(instance);

                                instancesMisesAJour++;

                                log.info("📦 Instance {} : RESERVE → EN_ATTENTE",
                                        instance.getNumeroSerie());
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("❌ Erreur lors de la mise à jour de la ligne {}: {}",
                            ligne.getIdLigneReservation(), e.getMessage());
                }
            }

            // ============================================
            // PARTIE 4: RÉSUMÉ ET LOGS FINAUX
            // ============================================

            log.info("📊 ========== RÉSUMÉ DE LA MISE À JOUR ==========");
            log.info("📈 Réservations mises à jour: {}", reservationsMisesAJour);
            log.info("📦 Lignes de réservation mises à jour: {}", lignesMisesAJour);
            log.info("🔧 Instances mises à jour: {}", instancesMisesAJour);
            log.info("⏰ ✅ FIN - Job Cron terminé avec succès");

        } catch (Exception e) {
            log.error("❌ ⚠️ ERREUR CRITIQUE dans le job Cron de mise à jour des statuts: {}", e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    /**
     * OPTIONNEL: Méthode manuelle pour forcer la mise à jour (pour les tests)
     * À appeler via un endpoint REST si nécessaire
     */
    public void forcerMiseAJourManuelle() {
        log.info("🔧 Exécution MANUELLE du job de mise à jour des statuts");
        mettreAJourStatutsQuotidien();
    }

}
