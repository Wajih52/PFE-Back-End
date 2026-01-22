package tn.weeding.agenceevenementielle.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.entities.LigneReservation;
import tn.weeding.agenceevenementielle.entities.Livraison;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.repository.LigneReservationRepository;
import tn.weeding.agenceevenementielle.repository.LivraisonRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Job Cron pour la mise à jour automatique des statuts de livraison
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
    private final LivraisonRepository livraisonRepo;


    /**
     * Job Cron qui s'exécute tous les jours à 00:02 (minuit + 2 minutes)
     * pour mettre à jour les statuts des livraisons NOT_TODAY → EN_ATTENTE
     *
     * Format Cron: "0 2 0 * * ?" = seconde minute heure jour mois jour-de-la-semaine
     */
    @Scheduled(cron = "0 2 0 * * ?") // Tous les jours à 00:02
    @Transactional
    public void mettreAJourStatutsLivraisons() {
        log.info("DEBUT - Job Cron de mise à jour des statuts de livraison (NOT_TODAY → EN_ATTENTE)");
        log.info("Date du jour: {}", LocalDate.now());

        try {
            LocalDate aujourdhui = LocalDate.now();
            int livraisonsMisesAJour = 0;

            // ============================================
            // MISE À JOUR DES LIVRAISONS
            // ============================================

            log.info("Recherche des livraisons avec statut NOT_TODAY et date de livraison = aujourd'hui...");

            // Trouver toutes les livraisons NOT_TODAY dont la date de livraison est aujourd'hui
            List<Livraison> livraisons = livraisonRepo.findAll().stream()
                    .filter(l -> l.getStatutLivraison() == StatutLivraison.NOT_TODAY)
                    .filter(l -> l.getDateLivraison().equals(aujourdhui))
                    .toList();

            log.info("{} livraison(s) trouvée(s) avec NOT_TODAY et dateLivraison = aujourd'hui",
                    livraisons.size());

            for (Livraison livraison : livraisons) {
                try {
                    StatutLivraison ancienStatut = livraison.getStatutLivraison();

                    // Changer NOT_TODAY → EN_ATTENTE
                    livraison.setStatutLivraison(StatutLivraison.EN_ATTENTE);
                    livraisonRepo.save(livraison);

                    livraisonsMisesAJour++;

                    log.info("Livraison #{} ({}): {} → EN_ATTENTE",
                            livraison.getIdLivraison(),
                            livraison.getTitreLivraison(),
                            ancienStatut);

                    // Mettre à jour les lignes de réservation de cette livraison
                    List<LigneReservation> lignes = ligneReservationRepo
                            .findByLivraison_IdLivraison(livraison.getIdLivraison());

                    for (LigneReservation ligne : lignes) {
                        if (ligne.getStatutLivraisonLigne() == StatutLivraison.NOT_TODAY) {
                            ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
                            ligneReservationRepo.save(ligne);

                            log.info(" Ligne #{} (Produit: {}) → EN_ATTENTE",
                                    ligne.getIdLigneReservation(),
                                    ligne.getProduit().getNomProduit());
                        }
                    }

                } catch (Exception e) {
                    log.error("Erreur lors de la mise à jour de la livraison #{}: {}",
                            livraison.getIdLivraison(), e.getMessage());
                }
            }

            // ============================================
            // RÉSUMÉ ET LOGS FINAUX
            // ============================================

            log.info("========== RÉSUMÉ DE LA MISE À JOUR ==========");
            log.info(" Livraisons mises à jour: {}", livraisonsMisesAJour);
            log.info("Job Cron terminé avec succès");

        } catch (Exception e) {
            log.error("ERREUR CRITIQUE dans le job Cron de mise à jour des livraisons: {}", e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    /**
     *Job Cron qui s'exécute toutes les heures pour vérifier les retours en retard
     */
    @Scheduled(cron = "0 0 * * * ?") // Toutes les heures à 0 minutes
    @Transactional
    public void verifierRetoursEnRetard() {
        log.info("Vérification des retours en retard...");

        try {
            List<LigneReservation> lignesEnRetard = ligneReservationRepo.findRetoursEnRetard();

            if (!lignesEnRetard.isEmpty()) {
                log.warn("{} ligne(s) de réservation en retard de retour détectée(s)", lignesEnRetard.size());

                for (LigneReservation ligne : lignesEnRetard) {
                    log.warn("Retour en retard - Réservation: {}, Produit: {}, Date fin prévue: {}",
                            ligne.getReservation().getReferenceReservation(),
                            ligne.getProduit().getNomProduit(),
                            ligne.getDateFin());
                    //
                }
            } else {
                log.info("Aucun retour en retard détecté");
            }

        } catch (Exception e) {
            log.error("Erreur lors de la vérification des retours en retard: {}", e.getMessage());
        }
    }

}