package tn.weeding.agenceevenementielle.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.StatutPaiementRes;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;
import tn.weeding.agenceevenementielle.repository.NotificationRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;
import tn.weeding.agenceevenementielle.services.EmailService;
import tn.weeding.agenceevenementielle.services.NotificationServiceInterface;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Job CRON pour le nettoyage automatique des anciennes notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationRepository notificationRepo;
    private final ReservationRepository reservationRepo;
    private final NotificationServiceInterface notificationService;
    private final EmailService emailService;


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



    /**
     *  Rappeler aux clients de payer l'acompte avant expiration du délai
     *
     * Exécution: Tous les jours à 10h00 du matin
     *
     * Conditions:
     * - Statut réservation = CONFIRME
     * - Statut paiement = EN_ATTENTE_PAIEMENT
     * - DateExpirationDevis pas encore atteinte
     * - On rappelle 3 jours avant et 1 jour avant l'expiration
     */
    @Scheduled(cron = "0 0 10 * * ?") // Tous les jours à 10h00
    @Transactional
    public void rappelPaiementAcompte() {
        log.info("💰 ⏰ DEBUT - Job de rappel paiement acompte avant expiration");

        LocalDateTime maintenant = LocalDateTime.now();
        LocalDate aujourdhui = maintenant.toLocalDate();

        try {
            // Récupérer toutes les réservations CONFIRMÉES en attente de paiement
            List<Reservation> reservationsEnAttente = reservationRepo.findAll().stream()
                    .filter(r -> r.getStatutReservation() == StatutReservation.CONFIRME)
                    .filter(r -> r.getStatutPaiement() == StatutPaiementRes.EN_ATTENTE_PAIEMENT)
                    .filter(r -> r.getDateExpirationDevis() != null)
                    .filter(r -> r.getDateExpirationDevis().toLocalDate().isAfter(aujourdhui)) // Pas encore expiré
                    .toList();

            log.info("📋 {} réservation(s) confirmée(s) en attente de paiement trouvée(s)",
                    reservationsEnAttente.size());

            int rappelEnvoye = 0;

            for (Reservation reservation : reservationsEnAttente) {
                try {
                    LocalDate dateExpiration = reservation.getDateExpirationDevis().toLocalDate();
                    long joursRestants = ChronoUnit.DAYS.between(aujourdhui, dateExpiration);

                    Utilisateur client = reservation.getUtilisateur();
                    if (client == null) {
                        log.warn("⚠️ Client introuvable pour la réservation {}",
                                reservation.getReferenceReservation());
                        continue;
                    }

                    // Rappel à 3 jours
                    if (joursRestants == 3) {
                        log.info("📅 Rappel à 3 jours pour la réservation {}",
                                reservation.getReferenceReservation());

                        envoyerRappelPaiement(
                                reservation,
                                client,
                                joursRestants,
                                "⏰ Rappel: 3 jours pour payer votre acompte",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "Nous vous rappelons qu'il vous reste 3 jours pour régler l'acompte " +
                                                "de votre réservation %s (Montant total: %.2f TND).\n\n" +
                                                "📅 Date limite de paiement: %s\n" +
                                                "💰 Montant déjà payé: %.2f TND\n" +
                                                "💵 Montant restant: %.2f TND\n\n" +
                                                "Merci de procéder au paiement rapidement pour sécuriser votre réservation.",
                                        client.getPrenom(),
                                        reservation.getReferenceReservation(),
                                        reservation.getMontantTotal(),
                                        dateExpiration.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0,
                                        reservation.getMontantTotal() - (reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0)
                                )
                        );
                        rappelEnvoye++;
                    }
                    // Rappel à 1 jour
                    else if (joursRestants == 1) {
                        log.info("🚨 Rappel URGENT à 1 jour pour la réservation {}",
                                reservation.getReferenceReservation());

                        envoyerRappelPaiement(
                                reservation,
                                client,
                                joursRestants,
                                "🚨 URGENT: Dernier jour pour payer votre acompte",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "🚨 ATTENTION: C'est votre dernier jour pour régler l'acompte " +
                                                "de votre réservation %s !\n\n" +
                                                "📅 Date limite: DEMAIN (%s)\n" +
                                                "💰 Montant total: %.2f TND\n" +
                                                "💵 Montant restant à payer: %.2f TND\n\n" +
                                                "⚠️ Sans paiement avant demain, votre réservation risque d'être annulée.\n\n" +
                                                "Merci de procéder au paiement immédiatement pour conserver votre réservation.",
                                        client.getPrenom(),
                                        reservation.getReferenceReservation(),
                                        dateExpiration.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        reservation.getMontantTotal(),
                                        reservation.getMontantTotal() - (reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0)
                                )
                        );
                        rappelEnvoye++;
                    }

                } catch (Exception e) {
                    log.error("❌ Erreur lors du rappel pour la réservation {}: {}",
                            reservation.getReferenceReservation(), e.getMessage());
                }
            }

            log.info("✅ Job terminé: {} rappel(s) de paiement envoyé(s)", rappelEnvoye);

        } catch (Exception e) {
            log.error("❌ Erreur globale lors du job de rappel paiement: {}", e.getMessage());
        }
    }

    /**
     * Méthode helper pour envoyer un rappel de paiement
     */
    private void envoyerRappelPaiement(Reservation reservation, Utilisateur client,
                                       long joursRestants, String titre, String message) {
        // Créer notification + email pour le client
        NotificationRequestDto notifClient = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.PAIEMENT_EN_ATTENTE)
                .titre(titre)
                .message(message)
                .idUtilisateur(client.getIdUtilisateur())
                .idReservation(reservation.getIdReservation())
                .urlAction("/client/mes-reservations/" + reservation.getIdReservation())
                .build();

        notificationService.creerNotificationAvecEmail(notifClient);

        log.info("📧 Rappel envoyé à {} ({} jours restants)",
                client.getEmail(), joursRestants);
    }


    /**
     * 💳 Rappeler aux clients de régulariser leur situation financière
     * après la fin de leur réservation
     *
     * Exécution: Tous les jours à 11h00
     *
     * Conditions:
     * - Statut réservation = TERMINE
     * - Statut paiement = PARTIELLEMENT_PAYE
     * - DateFin est passée
     * - Rappels: 3 jours, 7 jours, 14 jours après la fin
     */
    @Scheduled(cron = "0 0 11 * * ?") // Tous les jours à 11h00
    @Transactional
    public void rappelRegularisationFinanciere() {
        log.info("💳 ⏰ DEBUT - Job de rappel régularisation financière après fin réservation");

        LocalDate aujourdhui = LocalDate.now();

        try {
            // Récupérer toutes les réservations TERMINÉES partiellement payées
            List<Reservation> reservationsARegulariser = reservationRepo.findAll().stream()
                    .filter(r -> r.getStatutReservation() == StatutReservation.TERMINE)
                    .filter(r -> r.getStatutPaiement() == StatutPaiementRes.PARTIELLEMENT_PAYE)
                    .filter(r -> r.getDateFin() != null)
                    .filter(r -> r.getDateFin().isBefore(aujourdhui)) // Date fin passée
                    .toList();

            log.info("📋 {} réservation(s) terminée(s) avec paiement partiel trouvée(s)",
                    reservationsARegulariser.size());

            int rappelEnvoye = 0;

            for (Reservation reservation : reservationsARegulariser) {
                try {
                    LocalDate dateFin = reservation.getDateFin();
                    long joursDepuisFin = ChronoUnit.DAYS.between(dateFin, aujourdhui);

                    Utilisateur client = reservation.getUtilisateur();
                    if (client == null) {
                        log.warn(" Client introuvable pour la réservation {}",
                                reservation.getReferenceReservation());
                        continue;
                    }

                    Double montantRestant = reservation.getMontantTotal() -
                            (reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0);

                    // Rappel à 3 jours après la fin
                    if (joursDepuisFin == 3) {
                        log.info("📅 Rappel à 3 jours après fin pour la réservation {}",
                                reservation.getReferenceReservation());

                        envoyerRappelRegularisation(
                                reservation,
                                client,
                                joursDepuisFin,
                                montantRestant,
                                "💳 Solde à régler pour votre réservation",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "Nous espérons que votre événement s'est bien déroulé !\n\n" +
                                                "Nous vous rappelons qu'il reste un solde à régler pour votre réservation %s:\n\n" +
                                                "📅 Date de fin de réservation: %s (il y a %d jours)\n" +
                                                "💰 Montant total: %.2f TND\n" +
                                                "✅ Montant payé: %.2f TND\n" +
                                                "💵 Solde restant: %.2f TND\n\n" +
                                                "Merci de procéder au paiement du solde rapidement.\n" +
                                                "Pour toute question, n'hésitez pas à nous contacter.",
                                        client.getPrenom(),
                                        reservation.getReferenceReservation(),
                                        dateFin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        joursDepuisFin,
                                        reservation.getMontantTotal(),
                                        reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0,
                                        montantRestant
                                ),
                                false
                        );
                        rappelEnvoye++;
                    }
                    // Rappel à 7 jours après la fin
                    else if (joursDepuisFin == 7) {
                        log.info("⚠️ Rappel à 7 jours après fin pour la réservation {}",
                                reservation.getReferenceReservation());

                        envoyerRappelRegularisation(
                                reservation,
                                client,
                                joursDepuisFin,
                                montantRestant,
                                "⚠️ Rappel: Solde à régler",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "⚠️ Nous vous rappelons qu'un solde de %.2f TND reste à régler " +
                                                "pour votre réservation %s.\n\n" +
                                                "📅 Fin de réservation: %s (il y a %d jours)\n" +
                                                "💰 Montant total: %.2f TND\n" +
                                                "✅ Montant payé: %.2f TND\n" +
                                                "💵 Solde restant: %.2f TND\n\n" +
                                                "Merci de régulariser votre situation rapidement.\n" +
                                                "Nous restons à votre disposition pour toute question.",
                                        client.getPrenom(),
                                        montantRestant,
                                        reservation.getReferenceReservation(),
                                        dateFin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        joursDepuisFin,
                                        reservation.getMontantTotal(),
                                        reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0,
                                        montantRestant
                                ),
                                true
                        );
                        rappelEnvoye++;
                    }
                    // Rappel à 14 jours après la fin (URGENT + notification staff)
                    else if (joursDepuisFin == 14) {
                        log.warn("🚨 Rappel URGENT à 14 jours après fin pour la réservation {}",
                                reservation.getReferenceReservation());

                        envoyerRappelRegularisation(
                                reservation,
                                client,
                                joursDepuisFin,
                                montantRestant,
                                "🚨 URGENT: Régularisation requise",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "🚨 URGENT: Un solde de %.2f TND reste à régler depuis 14 jours " +
                                                "pour votre réservation %s.\n\n" +
                                                "📅 Fin de réservation: %s (il y a %d jours)\n" +
                                                "💰 Montant total: %.2f TND\n" +
                                                "✅ Montant payé: %.2f TND\n" +
                                                "💵 Solde restant: %.2f TND\n\n" +
                                                "⚠️ Nous vous prions de régulariser votre situation dans les plus brefs délais.\n" +
                                                "Sans retour de votre part, nous serons contraints de prendre des mesures.\n\n" +
                                                "Merci de nous contacter rapidement.",
                                        client.getPrenom(),
                                        montantRestant,
                                        reservation.getReferenceReservation(),
                                        dateFin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        joursDepuisFin,
                                        reservation.getMontantTotal(),
                                        reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0,
                                        montantRestant
                                ),
                                true
                        );

                        // Notifier aussi le STAFF à 14 jours
                        notificationService.creerNotificationPourStaff(
                                TypeNotification.PAIEMENT_RETARD,
                                "⚠️ Paiement en retard - 14 jours",
                                String.format(
                                        "Client: %s %s\n" +
                                                "Réservation: %s\n" +
                                                "Fin: %s (il y a %d jours)\n" +
                                                "Montant restant: %.2f TND\n\n" +
                                                "Action requise: Contacter le client",
                                        client.getPrenom(),
                                        client.getNom(),
                                        reservation.getReferenceReservation(),
                                        dateFin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        joursDepuisFin,
                                        montantRestant
                                ),
                                reservation.getIdReservation(),
                                "/admin/reservations/" + reservation.getIdReservation()
                        );

                        rappelEnvoye++;
                    }

                } catch (Exception e) {
                    log.error("❌ Erreur lors du rappel pour la réservation {}: {}",
                            reservation.getReferenceReservation(), e.getMessage());
                }
            }

            log.info("✅ Job terminé: {} rappel(s) de régularisation envoyé(s)", rappelEnvoye);

        } catch (Exception e) {
            log.error("❌ Erreur globale lors du job de rappel régularisation: {}", e.getMessage());
        }
    }

    /**
     * Méthode helper pour envoyer un rappel de régularisation
     */
    private void envoyerRappelRegularisation(Reservation reservation, Utilisateur client,
                                             long joursDepuisFin, Double montantRestant,
                                             String titre, String message, boolean notifierStaff) {
        // Créer notification + email pour le client
        NotificationRequestDto notifClient = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.PAIEMENT_RETARD)
                .titre(titre)
                .message(message)
                .idUtilisateur(client.getIdUtilisateur())
                .idReservation(reservation.getIdReservation())
                .urlAction("/client/mes-paiements")
                .build();

        notificationService.creerNotificationAvecEmail(notifClient);

        // Si demandé, notifier aussi le staff (pour les cas à 7 jours et +)
        if (notifierStaff) {
            notificationService.creerNotificationPourStaff(
                    TypeNotification.PAIEMENT_RETARD,
                    "⚠️ Paiement en retard",
                    String.format(
                            "Le client %s %s a un solde de %.2f TND à régler depuis %d jours " +
                                    "pour la réservation %s (fin: %s).",
                            client.getPrenom(),
                            client.getNom(),
                            montantRestant,
                            joursDepuisFin,
                            reservation.getReferenceReservation(),
                            reservation.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    ),
                    reservation.getIdReservation(),
                    "/admin/reservations/" + reservation.getIdReservation()
            );
        }

        log.info("📧 Rappel régularisation envoyé à {} ({} jours depuis fin, solde: {} TND)",
                client.getEmail(), joursDepuisFin, montantRestant);
    }


    /**
     * 📋 Rappeler aux clients de valider leur devis avant expiration
     *
     * Exécution: Tous les jours à 09h00 du matin
     *
     * Conditions:
     * - Statut réservation = EN_ATTENTE (devis non validé)
     * - DateExpirationDevis pas encore atteinte
     * - Rappels: 2 jours avant, 1 jour avant, et le jour même
     *
     * Workflow:
     * 1. Client crée un devis → statut EN_ATTENTE
     * 2. Système envoie des rappels avant expiration
     * 3. Si pas validé avant dateExpirationDevis → annulation automatique (job existant)
     */
    @Scheduled(cron = "0 0 9 * * ?") // Tous les jours à 09h00
    @Transactional
    public void rappelerValidationDevisAvantExpiration() {
        log.info("📋 ⏰ DEBUT - Job de rappel validation devis avant expiration");

        LocalDateTime maintenant = LocalDateTime.now();
        LocalDate aujourdhui = maintenant.toLocalDate();

        try {
            // Récupérer tous les devis EN_ATTENTE avec date d'expiration non atteinte
            List<Reservation> devisEnAttente = reservationRepo.findAll().stream()
                    .filter(r -> r.getStatutReservation() == StatutReservation.EN_ATTENTE)
                    .filter(r -> r.getDateExpirationDevis() != null)
                    .filter(r -> r.getDateExpirationDevis().toLocalDate().isAfter(aujourdhui)) // Pas encore expiré
                    .toList();

            log.info("📋 {} devis en attente de validation trouvé(s)", devisEnAttente.size());

            int rappelEnvoye = 0;

            for (Reservation devis : devisEnAttente) {
                try {
                    LocalDate dateExpiration = devis.getDateExpirationDevis().toLocalDate();
                    long joursRestants = ChronoUnit.DAYS.between(aujourdhui, dateExpiration);

                    Utilisateur client = devis.getUtilisateur();
                    if (client == null) {
                        log.warn("⚠️ Client introuvable pour le devis {}",
                                devis.getReferenceReservation());
                        continue;
                    }

                    // Rappel à 2 jours avant expiration
                    if (joursRestants == 2) {
                        log.info("📅 Rappel à 2 jours pour le devis {}",
                                devis.getReferenceReservation());

                        envoyerRappelValidationDevis(
                                devis,
                                client,
                                joursRestants,
                                "📋 Rappel: Validez votre devis sous 2 jours",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "Nous espérons que notre devis %s correspond à vos attentes !\n\n" +
                                                "📋 Votre devis est en attente de validation.\n" +
                                                "📅 Date limite de validation: %s (dans 2 jours)\n" +
                                                "💰 Montant total: %.2f TND\n" +
                                                "📦 Nombre de produits: %d ligne(s)\n" +
                                                "📆 Période: du %s au %s\n\n" +
                                                "⚠️ Important: Si votre devis n'est pas validé avant le %s, " +
                                                "il sera automatiquement annulé et vous devrez créer un nouveau devis.\n\n" +
                                                "Pour valider votre devis:\n" +
                                                "1. Connectez-vous à votre espace client\n" +
                                                "2. Accédez à \"Mes Devis\"\n" +
                                                "3. Cliquez sur \"Valider le devis\"\n\n" +
                                                "N'hésitez pas à nous contacter pour toute question.",
                                        client.getPrenom(),
                                        devis.getReferenceReservation(),
                                        dateExpiration.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        devis.getMontantTotal(),
                                        devis.getLigneReservations().size(),
                                        devis.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        devis.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        dateExpiration.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                )
                        );
                        rappelEnvoye++;
                    }
                    // Rappel à 1 jour avant expiration
                    else if (joursRestants == 1) {
                        log.info("⚠️ Rappel URGENT à 1 jour pour le devis {}",
                                devis.getReferenceReservation());

                        envoyerRappelValidationDevis(
                                devis,
                                client,
                                joursRestants,
                                "⚠️ URGENT: Validez votre devis avant demain",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "⚠️ ATTENTION: Votre devis %s expire DEMAIN !\n\n" +
                                                "📅 Date limite: DEMAIN (%s)\n" +
                                                "💰 Montant: %.2f TND\n" +
                                                "📦 Produits: %d ligne(s)\n" +
                                                "📆 Période: du %s au %s\n\n" +
                                                "🚨 Si vous ne validez pas votre devis avant demain, " +
                                                "il sera automatiquement annulé et vous perdrez votre réservation.\n\n" +
                                                "⏰ Validez dès maintenant:\n" +
                                                "→ Connectez-vous à votre espace client\n" +
                                                "→ Mes Devis → Valider\n\n" +
                                                "Besoin d'aide? Contactez-nous rapidement!",
                                        client.getPrenom(),
                                        devis.getReferenceReservation(),
                                        dateExpiration.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        devis.getMontantTotal(),
                                        devis.getLigneReservations().size(),
                                        devis.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        devis.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                )
                        );
                        rappelEnvoye++;
                    }
                    // Rappel le jour même (dernière chance)
                    else if (joursRestants == 0) {
                        log.warn("🚨 Rappel CRITIQUE - Dernier jour pour le devis {}",
                                devis.getReferenceReservation());

                        envoyerRappelValidationDevis(
                                devis,
                                client,
                                joursRestants,
                                "🚨 DERNIÈRE CHANCE: Validez votre devis AUJOURD'HUI",
                                String.format(
                                        "Bonjour %s,\n\n" +
                                                "🚨 ALERTE: C'est votre DERNIER JOUR pour valider le devis %s !\n\n" +
                                                "📅 Date limite: AUJOURD'HUI (%s)\n" +
                                                "⏰ Le devis sera annulé automatiquement ce soir à minuit.\n\n" +
                                                "💰 Montant: %.2f TND\n" +
                                                "📦 Produits: %d ligne(s)\n" +
                                                "📆 Période: du %s au %s\n\n" +
                                                "⚠️ DERNIÈRE CHANCE: Validez MAINTENANT ou perdez votre réservation!\n\n" +
                                                "Action immédiate requise:\n" +
                                                "1. Connectez-vous MAINTENANT\n" +
                                                "2. Mes Devis → Valider\n" +
                                                "3. Confirmez votre réservation\n\n" +
                                                "❌ Sans validation aujourd'hui, le devis sera annulé définitivement.\n\n" +
                                                "Pour toute urgence, appelez-nous immédiatement!",
                                        client.getPrenom(),
                                        devis.getReferenceReservation(),
                                        dateExpiration.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        devis.getMontantTotal(),
                                        devis.getLigneReservations().size(),
                                        devis.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        devis.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                )
                        );

                        // Notifier aussi le STAFF le jour même pour qu'ils puissent contacter le client
                        notificationService.creerNotificationPourStaff(
                                TypeNotification.DEVIS_PROCHE_EXPIRATION,
                                "⚠️ Devis expire aujourd'hui",
                                String.format(
                                        "Le devis %s du client %s %s expire AUJOURD'HUI.\n" +
                                                "Montant: %.2f TND\n" +
                                                "Période: du %s au %s\n\n" +
                                                "Action: Envisager de contacter le client pour relancer.",
                                        devis.getReferenceReservation(),
                                        client.getPrenom(),
                                        client.getNom(),
                                        devis.getMontantTotal(),
                                        devis.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        devis.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                ),
                                devis.getIdReservation(),
                                "/admin/devis-validation"
                        );

                        rappelEnvoye++;
                    }

                } catch (Exception e) {
                    log.error("❌ Erreur lors du rappel pour le devis {}: {}",
                            devis.getReferenceReservation(), e.getMessage());
                }
            }

            log.info("✅ Job terminé: {} rappel(s) de validation devis envoyé(s)", rappelEnvoye);

        } catch (Exception e) {
            log.error("❌ Erreur globale lors du job de rappel validation devis: {}", e.getMessage());
        }
    }

    /**
     * Méthode helper pour envoyer un rappel de validation de devis
     */
    private void envoyerRappelValidationDevis(Reservation devis, Utilisateur client,
                                              long joursRestants, String titre, String message) {
        // Créer notification + email pour le client
        NotificationRequestDto notifClient = NotificationRequestDto.builder()
                .typeNotification(
                        joursRestants == 0 ? TypeNotification.DEVIS_PROCHE_EXPIRATION : TypeNotification.DEVIS_EN_ATTENTE
                )
                .titre(titre)
                .message(message)
                .idUtilisateur(client.getIdUtilisateur())
                .idReservation(devis.getIdReservation())
                .urlAction("/client/mes-devis")
                .build();

        notificationService.creerNotificationAvecEmail(notifClient);

        log.info("📧 Rappel validation devis envoyé à {} ({} jour(s) restant(s))",
                client.getEmail(), joursRestants);
    }

}