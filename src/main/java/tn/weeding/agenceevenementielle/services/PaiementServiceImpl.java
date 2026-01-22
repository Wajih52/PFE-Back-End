package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.dto.paiement.PaiementRequestDto;
import tn.weeding.agenceevenementielle.dto.paiement.PaiementResponseDto;
import tn.weeding.agenceevenementielle.entities.Facture;
import tn.weeding.agenceevenementielle.entities.Paiement;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaiementServiceImpl implements PaiementServiceInterface{

    private final PaiementRepository paiementRepository;
    private final ReservationRepository reservationRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final FactureRepository factureRepository ;
    private final NotificationRepository notificationRepository;
    private final NotificationServiceInterface notificationService;
    private final EmailService emailService;
    private final UtilisateurRepository utilisateurRepository;


    @Override
    public PaiementResponseDto creerPaiement(PaiementRequestDto dto, String username) {
        log.info("Création d'un paiement pour la réservation ID: {} par {}", dto.getIdReservation(), username);

        Reservation reservation = reservationRepository.findById(dto.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable avec l'ID: " + dto.getIdReservation()));

        if (reservation.getStatutReservation() != StatutReservation.CONFIRME &&
                reservation.getStatutReservation() != StatutReservation.TERMINE) {
            throw new CustomException("Impossible d'ajouter un paiement à une réservation non confirmée ou annulée");
        }

        Double montantDejaPayeValide = calculerMontantPaye(dto.getIdReservation());
        Double montantRestant = reservation.getMontantTotal() - montantDejaPayeValide;

        if (dto.getMontantPaiement() > montantRestant + 0.01) {
            throw new CustomException(String.format(
                    "Le montant du paiement (%.2f TND) dépasse le montant restant à payer (%.2f TND)",
                    dto.getMontantPaiement(), montantRestant));
        }

        String codePaiement = codeGeneratorService.generatePaiementCode();

        Paiement paiement = Paiement.builder()
                .codePaiement(codePaiement)
                .datePaiement(LocalDateTime.now())
                .montantPaiement(dto.getMontantPaiement())
                .modePaiement(dto.getModePaiement())
                .statutPaiement(StatutPaiement.EN_ATTENTE)
                .descriptionPaiement(dto.getDescriptionPaiement())
                .referenceExterne(dto.getReferenceExterne())
                .reservation(reservation)
                .build();

        Paiement savedPaiement = paiementRepository.save(paiement);

        log.info("Paiement créé avec succès: {} - Montant: {} TND", codePaiement, dto.getMontantPaiement());

        // ========================================
        //  NOTIFICATIONS + EMAILS
        // ========================================

        Utilisateur client = reservation.getUtilisateur();

        // Déterminer qui a créé le paiement
        Utilisateur createur = utilisateurRepository.findByPseudoOrEmail(username, username)
                .orElse(null);

        boolean creeParClient = createur != null &&
                createur.getIdUtilisateur().equals(client.getIdUtilisateur());

        if (creeParClient) {
            // CLIENT crée un paiement → Notifier les ADMINS/MANAGERS
            log.info("Notification des admins/managers - Nouveau paiement par le client");

            notificationService.creerNotificationPourStaff(
                    TypeNotification.NOUVEAU_PAIEMENT,
                    "Nouveau paiement en attente",
                    String.format("Le client %s %s a effectué un paiement de %.2f TND pour la réservation %s. Mode: %s. En attente de validation.",
                            client.getPrenom(), client.getNom(),
                            dto.getMontantPaiement(),
                            reservation.getReferenceReservation(),
                            dto.getModePaiement().name()),
                    reservation.getIdReservation(),
                    "/admin/paiements" + savedPaiement.getIdPaiement()
            );


        } else {
            // ADMIN/MANAGER crée un paiement → Notifier le CLIENT
            log.info("Notification du client - Paiement enregistré par l'admin");

            NotificationRequestDto notifClient = NotificationRequestDto.builder()
                    .typeNotification(TypeNotification.PAIEMENT_EN_ATTENTE)
                    .titre("Paiement enregistré")
                    .message(String.format("Un paiement de %.2f TND a été enregistré pour votre réservation %s. Mode: %s. Statut: En attente de validation.",
                            dto.getMontantPaiement(),
                            reservation.getReferenceReservation(),
                            dto.getModePaiement().name()))
                    .idUtilisateur(client.getIdUtilisateur())
                    .idReservation(reservation.getIdReservation())
                    .idPaiement(savedPaiement.getIdPaiement())
                    .urlAction("/client/mes-paiements")
                    .build();

            notificationService.creerNotification(notifClient);

            // Email au client
            emailService.envoyerEmailNotification(
                    client.getEmail(),
                    client.getPrenom(),
                    TypeNotification.PAIEMENT_EN_ATTENTE,
                    "Paiement enregistré",
                    String.format("Un paiement de %.2f TND a été enregistré pour votre réservation %s. Vous serez notifié(e) dès sa validation.",
                            dto.getMontantPaiement(),
                            reservation.getReferenceReservation())
            );
        }


        return convertToResponseDto(savedPaiement, montantDejaPayeValide);
    }

    @Override
    public PaiementResponseDto validerPaiement(Long idPaiement, String username) {
        log.info("Validation du paiement ID: {} par {}", idPaiement, username);

        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        if (paiement.getStatutPaiement() != StatutPaiement.EN_ATTENTE) {
            throw new CustomException("Seuls les paiements en attente peuvent être validés");
        }

        paiement.setStatutPaiement(StatutPaiement.VALIDE);
        paiement.setValidePar(username);
        paiement.setDateValidation(LocalDateTime.now());

        Paiement savedPaiement = paiementRepository.save(paiement);

        mettreAJourMontantPayeReservation(paiement.getReservation().getIdReservation());
        verifierEtMettreAJourStatutReservation(paiement.getReservation().getIdReservation());

        log.info("Paiement validé: {} - {} TND", paiement.getCodePaiement(), paiement.getMontantPaiement());


        // ========================================
        //  NOTIFICATION + EMAIL AU CLIENT
        // ========================================

        Utilisateur client = paiement.getReservation().getUtilisateur();
        Reservation reservation = paiement.getReservation();
        Utilisateur validateur = utilisateurRepository.findByPseudoOrEmail(username, username)
                .orElse(null);


            // admin ou manager valide un paiement → Notifier les autres ADMINS/MANAGERS
            log.info("Notification des admins - validation paiement par {}",username);

            // Vérifier si le paiement est maintenant complet
        Boolean paiementComplet = isReservationPayeeCompletement(reservation.getIdReservation());

        String messageNotif;
        String messageEmail;

        if (paiementComplet) {
            messageNotif = String.format(
                    "Votre paiement de %.2f TND a été validé. Votre réservation %s est maintenant entièrement payée !",
                    paiement.getMontantPaiement(),
                    reservation.getReferenceReservation()
            );
            messageEmail = String.format(
                    "Excellente nouvelle ! Votre paiement de %.2f TND a été validé avec succès. " +
                            "Votre réservation %s qui est prévu le (%s) est maintenant entièrement réglée. "
                           ,
                    paiement.getMontantPaiement(),
                    reservation.getReferenceReservation(),
                    reservation.getDateDebut()
            );
        } else {
            Double montantRestant = reservation.getMontantTotal() - calculerMontantPaye(reservation.getIdReservation());
            messageNotif = String.format(
                    " Votre paiement de %.2f TND a été validé. Montant restant: %.2f TND pour la réservation %s.",
                    paiement.getMontantPaiement(),
                    montantRestant,
                    reservation.getReferenceReservation()
            );
            messageEmail = String.format(
                    "Votre paiement de %.2f TND a été validé avec succès pour votre réservation %s. " +
                            "Il reste %.2f TND à régler .",
                    paiement.getMontantPaiement(),
                    reservation.getReferenceReservation(),
                    montantRestant
            );
        }


        // Notification en BD
        notificationService.creerNotificationPourStaff(
                TypeNotification.PAIEMENT_RECU,
                " paiement Validé",
                String.format("le paiement %s de %.2f DT du client %s pour la réservation %s a été validé par %s ",
                        savedPaiement.getCodePaiement(),
                        savedPaiement.getMontantPaiement(),client.getNom(),
                        reservation.getReferenceReservation(),
                      validateur !=null ? validateur.getNom()+" "+validateur.getPrenom():username),
                reservation.getIdReservation(),
                "/admin/paiements" + savedPaiement.getIdPaiement()
        );

        NotificationRequestDto notif = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.PAIEMENT_RECU)
                .titre("Paiement validé")
                .message(messageNotif)
                .idUtilisateur(client.getIdUtilisateur())
                .idReservation(reservation.getIdReservation())
                .idPaiement(paiement.getIdPaiement())
                .urlAction("/client/reservations-details/" + reservation.getIdReservation())
                .build();

        notificationService.creerNotification(notif);

        // Email au client
        emailService.envoyerEmailNotification(
                client.getEmail(),
                client.getPrenom(),
                TypeNotification.PAIEMENT_RECU,
                "Paiement validé ",
                messageEmail
        );

        log.info("Notification + Email envoyés au client {} pour validation paiement", client.getEmail());
        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation()) - paiement.getMontantPaiement();

        return convertToResponseDto(savedPaiement, montantDejaPayeAvant);
    }

    @Override
    public PaiementResponseDto refuserPaiement(Long idPaiement, String motifRefus, String username) {
        log.info("Refus du paiement ID: {} par {}", idPaiement, username);

        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        if (paiement.getStatutPaiement() != StatutPaiement.EN_ATTENTE) {
            throw new CustomException("Seuls les paiements en attente peuvent être refusés");
        }

        paiement.setStatutPaiement(StatutPaiement.REFUSE);
        paiement.setValidePar(username);
        paiement.setDateValidation(LocalDateTime.now());
        paiement.setMotifRefus(motifRefus);

        Paiement savedPaiement = paiementRepository.save(paiement);

        log.info("Paiement refusé: {}", paiement.getCodePaiement());

        // ========================================
        // NOTIFICATION + EMAIL AU CLIENT
        // ========================================

        Utilisateur client = paiement.getReservation().getUtilisateur();
        Reservation reservation = paiement.getReservation();

        // Notification en BD
        NotificationRequestDto notif = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.PAIEMENT_REFUSE)
                .titre("Paiement refusé")
                .message(String.format(
                        "Votre paiement de %.2f TND pour la réservation %s a été refusé. Motif: %s. " +
                                "Veuillez corriger et soumettre un nouveau paiement.",
                        paiement.getMontantPaiement(),
                        reservation.getReferenceReservation(),
                        motifRefus
                ))
                .idUtilisateur(client.getIdUtilisateur())
                .idReservation(reservation.getIdReservation())
                .idPaiement(paiement.getIdPaiement())
                .urlAction("reservations/"+reservation.getIdReservation()+"/ajouter-paiement" )
                .build();

        notificationService.creerNotification(notif);

        // Email au client
        emailService.envoyerEmailNotification(
                client.getEmail(),
                client.getPrenom(),
                TypeNotification.PAIEMENT_RETARD,
                "Paiement refusé",
                String.format(
                        "Votre paiement de %.2f TND pour la réservation %s a été refusé.\n\n" +
                                "Motif du refus: %s\n\n" +
                                "Veuillez soumettre un nouveau paiement dans votre espace client pour régulariser votre réservation.",
                        paiement.getMontantPaiement(),
                        reservation.getReferenceReservation(),
                        motifRefus
                )
        );

        log.info("Notification + Email envoyés au client {} pour refus paiement", client.getEmail());

        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation());

        return convertToResponseDto(savedPaiement, montantDejaPayeAvant);
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementResponseDto getPaiementById(Long idPaiement) {
        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation());

        if (paiement.getStatutPaiement() == StatutPaiement.VALIDE) {
            montantDejaPayeAvant -= paiement.getMontantPaiement();
        }

        return convertToResponseDto(paiement, montantDejaPayeAvant);
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementResponseDto getPaiementByCode(String codePaiement) {
        Paiement paiement = paiementRepository.findByCodePaiement(codePaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec le code: " + codePaiement));

        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation());

        if (paiement.getStatutPaiement() == StatutPaiement.VALIDE) {
            montantDejaPayeAvant -= paiement.getMontantPaiement();
        }

        return convertToResponseDto(paiement, montantDejaPayeAvant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByReservation(Long idReservation) {
        if (!reservationRepository.existsById(idReservation)) {
            throw new CustomException("Réservation introuvable avec l'ID: " + idReservation);
        }

        List<Paiement> paiements = paiementRepository.findByReservationIdReservationOrderByDatePaiementDesc(idReservation);

        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByClient(Long idClient) {
        List<Paiement> paiements = paiementRepository.findByClientIdOrderByDatePaiementDesc(idClient);
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getAllPaiements() {
        List<Paiement> paiements = paiementRepository.findAll();
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByStatut(StatutPaiement statut) {
        List<Paiement> paiements = paiementRepository.findByStatutPaiementOrderByDatePaiementDesc(statut);
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsEnAttente() {
        List<Paiement> paiements = paiementRepository.findPaiementsEnAttente();
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Paiement> paiements = paiementRepository.findPaiementsByPeriode(dateDebut, dateFin);
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculerMontantPaye(Long idReservation) {
        Double montant = paiementRepository.calculerMontantPayeValidePourReservation(idReservation);
        return montant != null ? montant : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isReservationPayeeCompletement(Long idReservation) {
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable avec l'ID: " + idReservation));

        Double montantPaye = calculerMontantPaye(idReservation);
        Double montantTotal = reservation.getMontantTotal();

        return montantPaye >= (montantTotal - 0.01);
    }

    @Override
    public void supprimerPaiement(Long idPaiement, String username) {
        log.info("Suppression du paiement ID: {} par {}", idPaiement, username);

        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        if (paiement.getStatutPaiement() == StatutPaiement.VALIDE) {
            throw new CustomException("Impossible de supprimer un paiement validé. Contactez un administrateur pour un remboursement.");
        }

        paiement.setStatutPaiement(StatutPaiement.REFUSE);
        paiement.setDescriptionPaiement(
                (paiement.getDescriptionPaiement() != null ? paiement.getDescriptionPaiement() + " | " : "") +
                        "Paiement supprimé par: " + username
        );

        paiementRepository.save(paiement);

        log.info("Paiement supprimé (annulé): {}", paiement.getCodePaiement());
    }

    private void mettreAJourMontantPayeReservation(Long idReservation) {
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        Double montantPaye = calculerMontantPaye(idReservation);
        reservation.setMontantPaye(montantPaye);

        reservationRepository.save(reservation);

        log.info("Montant payé mis à jour pour réservation {}: {} TND", reservation.getReferenceReservation(), montantPaye);
    }

    private void verifierEtMettreAJourStatutReservation(Long idReservation) {
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        Boolean paiementComplet = isReservationPayeeCompletement(idReservation);

        if (paiementComplet) {
            log.info("Paiement complet pour la réservation: {}", reservation.getReferenceReservation());

            if (reservation.getStatutPaiement() == StatutPaiementRes.EN_ATTENTE_PAIEMENT||
            reservation.getStatutPaiement()==StatutPaiementRes.PARTIELLEMENT_PAYE) {
                reservation.setStatutPaiement(StatutPaiementRes.TOTALEMENT_PAYE);
                reservation.setDateExpirationDevis(null);
                Optional<Facture> facture =factureRepository.findByReservation_IdReservationAndTypeFacture(reservation.getIdReservation(), TypeFacture.FINALE)
                        .stream().findFirst();
                if (facture.isPresent()){
                    facture.get().setStatutFacture(StatutFacture.PAYEE);
                    factureRepository.save(facture.get());
                }
                reservationRepository.save(reservation);

                log.info("Statut Paiement réservation mis à jour: EN_ATTENTE_PAIEMENT → CONFIRME");
            }
        }else{
            reservation.setStatutPaiement(StatutPaiementRes.PARTIELLEMENT_PAYE);
            reservation.setDateExpirationDevis(null);
            reservationRepository.save(reservation);
        }
    }

    private PaiementResponseDto convertToResponseDto(Paiement paiement, Double montantDejaPayeAvant) {
        Reservation reservation = paiement.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        Double montantTotalReservation = reservation.getMontantTotal();
        Double montantRestantApres = montantTotalReservation -
                (montantDejaPayeAvant +
                        (paiement.getStatutPaiement() == StatutPaiement.VALIDE ?
                                paiement.getMontantPaiement() : 0));

        Boolean paiementComplet = montantRestantApres <= 0.01;

        return PaiementResponseDto.builder()
                .idPaiement(paiement.getIdPaiement())
                .codePaiement(paiement.getCodePaiement())
                .idReservation(reservation.getIdReservation())
                .referenceReservation(reservation.getReferenceReservation())
                .montantPaiement(paiement.getMontantPaiement())
                .modePaiement(paiement.getModePaiement())
                .statutPaiement(paiement.getStatutPaiement())
                .datePaiement(paiement.getDatePaiement())
                .dateValidation(paiement.getDateValidation())
                .descriptionPaiement(paiement.getDescriptionPaiement())
                .motifRefus(paiement.getMotifRefus())
                .referenceExterne(paiement.getReferenceExterne())
                .validePar(paiement.getValidePar())
                .nomClient(client.getNom())
                .prenomClient(client.getPrenom())
                .emailClient(client.getEmail())
                .montantTotalReservation(montantTotalReservation)
                .montantDejaPayeAvant(montantDejaPayeAvant)
                .montantRestantApres(montantRestantApres)
                .paiementComplet(paiementComplet)
                .build();
    }
}
