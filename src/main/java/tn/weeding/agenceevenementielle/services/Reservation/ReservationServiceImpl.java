package tn.weeding.agenceevenementielle.services.Reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.weeding.agenceevenementielle.dto.modifDateReservation.DateConstraintesDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.DatePeriodeDto;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.*;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.DateValidationException;
import tn.weeding.agenceevenementielle.exceptions.ProduitException;
import tn.weeding.agenceevenementielle.exceptions.ReservationException;
import tn.weeding.agenceevenementielle.repository.*;
import  tn.weeding.agenceevenementielle.exceptions.ReservationException.StockIndisponibleException;
import tn.weeding.agenceevenementielle.services.EmailService;
import tn.weeding.agenceevenementielle.services.FactureServiceInterface;
import tn.weeding.agenceevenementielle.services.NotificationServiceInterface;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReservationServiceImpl implements ReservationServiceInterface {

    private final ReservationRepository reservationRepo;
    private final LigneReservationRepository ligneReservationRepo;
    private final ProduitRepository produitRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final InstanceProduitRepository instanceProduitRepo;
    private final MouvementStockRepository mouvementStockRepo;
    private final DateReservationValidator dateValidator;
    private final FactureServiceInterface factureService;
    private final AffectationLivraisonRepository affectationRepo ;

    private final NotificationServiceInterface notificationService;
    private final EmailService emailService;

    // ============ CRÉATION DE DEVIS PAR LE CLIENT ============

    @Override
    public ReservationResponseDto creerDevis(DevisRequestDto devisRequest, Long idUtilisateur, String username) {
        log.info("🎯 Création d'un devis par le client ID: {} ({})", idUtilisateur, username);

        // 1. Vérifier que l'utilisateur existe
        Utilisateur client = utilisateurRepo.findById(idUtilisateur)
                .orElseThrow(() -> new CustomException("Client introuvable avec ID: " + idUtilisateur));

        // 2. Vérifier que toutes les lignes ont des produits valides
        if (devisRequest.getLignesReservation() == null || devisRequest.getLignesReservation().isEmpty()) {
            throw new CustomException("Le devis doit contenir au moins un produit");
        }

        // VALIDER LES DATES DE CHAQUE LIGNE
        log.info("📅 Validation des dates pour {} lignes", devisRequest.getLignesReservation().size());

        for (LigneReservationRequestDto ligneDto : devisRequest.getLignesReservation()) {
            try {
                dateValidator.validerPeriodeReservation(
                        ligneDto.getDateDebut(),
                        ligneDto.getDateFin(),
                        "devis - produit ID " + ligneDto.getIdProduit()
                );

                long nbJours = dateValidator.calculerNombreJours(
                        ligneDto.getDateDebut(),
                        ligneDto.getDateFin()
                );

                log.debug("✅ Dates valides pour produit {} - Durée: {} jours",
                        ligneDto.getIdProduit(), nbJours);

            } catch (DateValidationException e) {
                log.error("❌ Dates invalides pour produit {}: {}",
                        ligneDto.getIdProduit(), e.getMessage());
               // throw e; // Propager l'exception au controller
            }
        }

        log.info("✅ Toutes les dates sont valides");


        // 3. Vérifier la disponibilité de TOUS les produits AVANT de créer le devis
        log.info("📦 Vérification de la disponibilité de {} produits", devisRequest.getLignesReservation().size());


        for (LigneReservationRequestDto ligneDto : devisRequest.getLignesReservation()) {


            VerificationDisponibiliteDto verif = VerificationDisponibiliteDto.builder()
                    .idProduit(ligneDto.getIdProduit())
                    .quantite(ligneDto.getQuantite())
                    .dateDebut(ligneDto.getDateDebut())
                    .dateFin(ligneDto.getDateFin())
                    .build();

            DisponibiliteResponseDto dispo = verifierDisponibilite(verif);


            if (!dispo.getDisponible()) {
                log.warn("❌ Produit {} non disponible", dispo.getNomProduit());
                throw new CustomException(
                        "Le produit '" + dispo.getNomProduit() + "' n'est pas disponible. " +
                                dispo.getMessage()
                );
            }
        }
        log.info("✅ Tous les produits sont disponibles");

        // 4. Créer la réservation (devis) avec statut "EnAttente"
        Reservation reservation = new Reservation();
        reservation.setReferenceReservation(genererReferenceReservation());
        reservation.setStatutReservation(StatutReservation.EN_ATTENTE);
        reservation.setUtilisateur(client);

        reservation.setMontantPaye(0.0);

        // Dates globales (du premier au dernier jour)
        LocalDate dateDebutMin = devisRequest.getLignesReservation().stream()
                .map(LigneReservationRequestDto::getDateDebut)
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate dateFinMax = devisRequest.getLignesReservation().stream()
                .map(LigneReservationRequestDto::getDateFin)
                .max(LocalDate::compareTo)
                .orElseThrow();

        reservation.setDateDebut(dateDebutMin);
        reservation.setDateFin(dateFinMax);

        if(dateDebutMin.isEqual(LocalDate.now())) {
            reservation.setStatutLivraisonRes(StatutLivraison.EN_ATTENTE);
        }else {
            reservation.setStatutLivraisonRes(StatutLivraison.NOT_TODAY);
        }


        // 5. Créer les lignes de réservation
        Set<LigneReservation> lignes = new HashSet<>();
        double montantTotal = 0.0;

        for (LigneReservationRequestDto ligneDto : devisRequest.getLignesReservation()) {
            Produit produit = produitRepo.findById(ligneDto.getIdProduit())
                    .orElseThrow(() -> new CustomException("Produit introuvable"));

            LigneReservation ligne = new LigneReservation();
            ligne.setReservation(reservation);
            ligne.setProduit(produit);
            ligne.setQuantite(ligneDto.getQuantite());
            ligne.setPrixUnitaire(produit.getPrixUnitaire());  // Prix du produit au moment de la réservation
            ligne.setDateDebut(ligneDto.getDateDebut());
            ligne.setDateFin(ligneDto.getDateFin());
            if(ligneDto.getDateDebut().isEqual(LocalDate.now())) {
                ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
            }else {
                ligne.setStatutLivraisonLigne(StatutLivraison.NOT_TODAY);
            }

            ligne.setObservations(ligneDto.getObservations());

            long nbJours = ChronoUnit.DAYS.between(ligneDto.getDateDebut(), ligneDto.getDateFin()) + 1;
            double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire() * nbJours;
            montantTotal += sousTotal;

            lignes.add(ligne);
            log.info("📝 Ligne ajoutée: {} x {} = {} TND pour {} jours",
                    produit.getNomProduit(), ligne.getQuantite(), sousTotal,nbJours);
        }

        reservation.setLigneReservations(lignes);
        reservation.setMontantTotal(montantTotal);
        reservation.setMontantOriginal(montantTotal);
        reservation.setRemisePourcentage(0.0);
        reservation.setRemiseMontant(0.0);
        reservation.setStatutReservation(StatutReservation.EN_ATTENTE);
        reservation.setCommentaireClient(devisRequest.getObservationsClient());



        log.info("✅ Devis créé avec succès: {} - Montant: {} TND",
                reservation.getReferenceReservation(), montantTotal);

        //  VALIDATION AUTOMATIQUE si client Valide directement sans Review Admin
        if(devisRequest.isValidationAutomatique()){
            log.info("🚀 Mode validation automatique → Réservation immédiate du stock");
            reservation.setValidationAutomatique(true);
            Reservation devisSaved = reservationRepo.save(reservation);
            // Réserver le stock immédiatement
           Reservation resValidee = reserverStockPourReservation(devisSaved );

            log.info("✅ Devis validé automatiquement {} - montant {} TND - Réservation confirmée",
                    resValidee.getReferenceReservation(),montantTotal);

           return convertToResponseDto(resValidee);
        }else{
            // 📋 MODE CLASSIQUE : Attente review admin
            log.info("⏳ Mode classique → Stock NON réservé, en attente de validation");
            reservation.setValidationAutomatique(false);
            reservation.setStockReserve(false);
            // Définir date d'expiration
            reservation.setDateExpirationDevis(LocalDateTime.now().plusDays(2));

            Reservation devisSaved = reservationRepo.save(reservation);
            log.info("✅ Devis créé {} - Montant: {} TND (stock NON réservé)",
                    devisSaved.getReferenceReservation(), montantTotal);

            // ========================================
            // 🔔 NOTIFICATIONS + EMAIL ADMINS/MANAGERS
            // ========================================

            // Créer message détaillé
            StringBuilder messageNotif = new StringBuilder();
            messageNotif.append(String.format(
                    "Le client %s %s a créé un nouveau devis (%s).\n\n",
                    client.getPrenom(), client.getNom(),
                    devisSaved.getReferenceReservation()
            ));
            messageNotif.append(String.format("📅 Période: %s au %s\n",
                    devisSaved.getDateDebut(),
                    devisSaved.getDateFin()
            ));
            messageNotif.append(String.format("💰 Montant: %.2f TND\n", devisSaved.getMontantTotal()));
            messageNotif.append(String.format("📦 Produits: %d lignes\n", devisSaved.getLigneReservations().size()));
            messageNotif.append("\n⏰ En attente de validation et modification.");

            // Notifier tous les admins et managers
            notificationService.creerNotificationPourStaff(
                    TypeNotification.NOUVEAU_DEVIS,
                    "Nouveau devis en attente",
                    messageNotif.toString(),
                    devisSaved.getIdReservation(),
                    "/admin/devis-validation"
            );

            log.info("📧 Notifications envoyées aux admins/managers pour le devis {}",
                    devisSaved.getReferenceReservation());

            return convertToResponseDto(devisSaved);
        }
    }

    // ============ VÉRIFICATION DE DISPONIBILITÉ ============

    @Override
    public DisponibiliteResponseDto verifierDisponibilite(VerificationDisponibiliteDto verificationDto) {
        log.debug("🔍 Vérification disponibilité - Produit: {}, Quantité: {}, Période: {} -> {}",
                verificationDto.getIdProduit(), verificationDto.getQuantite(),
                verificationDto.getDateDebut(), verificationDto.getDateFin());

        // 1. VALIDATION DES DATES EN PREMIER
        try {
            dateValidator.validerPeriodeReservation(
                    verificationDto.getDateDebut(),
                    verificationDto.getDateFin(),
                    "vérification disponibilité"
            );
        } catch (DateValidationException e) {
            log.error("❌ Dates invalides: {}", e.getMessage());

            // Retourner une réponse avec les informations d'erreur
            return DisponibiliteResponseDto.builder()
                    .idProduit(verificationDto.getIdProduit())
                    .quantiteDemandee(verificationDto.getQuantite())
                    .disponible(false)
                    .message("Dates invalides: " + e.getMessage())
                    .build();
        }

        Produit produit = produitRepo.findById(verificationDto.getIdProduit())
                .orElseThrow(() -> new CustomException("Produit introuvable"));


        DisponibiliteResponseDto response = DisponibiliteResponseDto.builder()
                .idProduit(produit.getIdProduit())
                .nomProduit(produit.getNomProduit())
                .quantiteDemandee(verificationDto.getQuantite())
                .build();

        // Vérifier selon le type de produit
        if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
            return verifierDisponibiliteQuantite(produit, verificationDto, response);
        } else {
            return verifierDisponibiliteAvecReference(produit, verificationDto, response);
        }
    }

    /**
     * Vérifier la disponibilité pour un produit EN QUANTITÉ (chaises, assiettes, etc.)
     */
    private DisponibiliteResponseDto verifierDisponibiliteQuantite(
            Produit produit,
            VerificationDisponibiliteDto verificationDto,
            DisponibiliteResponseDto response) {
        // Calculer la quantité déjà réservée sur cette période
        Integer quantiteReservee = ligneReservationRepo.calculateQuantiteReserveeSurPeriode(
                produit.getIdProduit(),
                verificationDto.getDateDebut(),
                verificationDto.getDateFin()
        );

        if (quantiteReservee == null) {
            quantiteReservee = 0;
        }

        int quantiteDisponible = produit.getQuantiteDisponible() - quantiteReservee;

        response.setQuantiteDisponible(quantiteDisponible);
        response.setDisponible(quantiteDisponible >= verificationDto.getQuantite());

        if (response.getDisponible()) {
            response.setMessage("Produit disponible. " + quantiteDisponible + " unités disponibles.");
        } else {
            response.setMessage(
                    "Stock insuffisant. Demandé: " + verificationDto.getQuantite() +
                            ", Disponible: " + quantiteDisponible
            );
        }

        log.debug("📊 Quantité - Demandée: {}, Réservée: {}, Disponible: {}",
                verificationDto.getQuantite(), quantiteReservee, quantiteDisponible);

        return response;
    }

    /**
     * Vérifier la disponibilité pour un produit AVEC RÉFÉRENCE (projecteurs, caméras, etc.)
     */
    private DisponibiliteResponseDto verifierDisponibiliteAvecReference(
            Produit produit,
            VerificationDisponibiliteDto verificationDto,
            DisponibiliteResponseDto response) {


        // Compter les instances disponibles sur la période
        int instancesDisponiblesPourPeriode = instanceProduitRepo.countInstancesDisponiblesSurPeriode(
                produit.getIdProduit(),
                verificationDto.getDateDebut(),
                verificationDto.getDateFin()
        );



        log.debug("📊 Instances disponibles sur période [{} - {}]: {}",
                verificationDto.getDateDebut(),
                verificationDto.getDateFin(),
                instancesDisponiblesPourPeriode);

        // Compter les instances réservées sur cette période
        Long instancesReservees = ligneReservationRepo.countInstancesReserveesSurPeriode(
                produit.getIdProduit(),
                verificationDto.getDateDebut(),
                verificationDto.getDateFin()
        );

        if (instancesReservees == null) {
            instancesReservees = 0L;
        }

        response.setQuantiteDisponible(instancesDisponiblesPourPeriode);
        response.setDisponible(instancesDisponiblesPourPeriode >= verificationDto.getQuantite());

        // Récupérer les instances disponibles
        List<InstanceProduit> instancesDispos = instanceProduitRepo.findInstancesDisponiblesSurPeriode(
                produit.getIdProduit(),
                verificationDto.getDateDebut(),
                verificationDto.getDateFin()
        );
        List<String> numerosSeries = instancesDispos.stream()
                .limit(verificationDto.getQuantite())
                .map(InstanceProduit::getNumeroSerie)
                .collect(Collectors.toList());

        response.setInstancesDisponibles(numerosSeries);

        if (response.getDisponible()) {
            response.setMessage("Produit disponible. " + instancesDisponiblesPourPeriode + " instances disponibles.");
        } else {
            response.setMessage(
                    "Instances insuffisantes. Demandé: " + verificationDto.getQuantite() +
                            ", Disponible: " + instancesDisponiblesPourPeriode
            );
        }

        log.debug("📊 Instances - Demandées: {}, Réservées: {}, Disponibles: {}",
                verificationDto.getQuantite(), instancesReservees, instancesDisponiblesPourPeriode);

        return response;
    }

    @Override
    public List<DisponibiliteResponseDto> verifierDisponibilites(List<VerificationDisponibiliteDto> verifications) {
        return verifications.stream()
                .map(this::verifierDisponibilite)
                .collect(Collectors.toList());
    }

    // ============ MODIFICATION DU DEVIS PAR L'ADMIN ============

    @Override
    public ReservationResponseDto modifierDevisParAdmin(DevisModificationDto modificationDto, String username) {
        log.info("🔧 Modification du devis ID: {} par l'admin {}",
                modificationDto.getIdReservation(), username);

        Reservation reservation = reservationRepo.findById(modificationDto.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        // Vérifier que c'est bien un devis (en attente)
        if (reservation.getStatutReservation() != StatutReservation.EN_ATTENTE) {
            throw new CustomException("Seuls les devis en attente peuvent être modifiés");
        }

        // 1. Modifier les lignes individuelles (prix unitaire, quantité)
        if (modificationDto.getLignesModifiees() != null) {
            for (LigneModificationDto ligneModif : modificationDto.getLignesModifiees()) {
                LigneReservation ligne = ligneReservationRepo.findById(ligneModif.getIdLigneReservation())
                        .orElseThrow(() -> new CustomException("Ligne introuvable"));

                if (ligneModif.getNouveauPrixUnitaire() != null) {
                    log.info("💰 Modification prix: {} -> {} TND",
                            ligne.getPrixUnitaire(), ligneModif.getNouveauPrixUnitaire());
                    ligne.setPrixUnitaire(ligneModif.getNouveauPrixUnitaire());
                }

                if (ligneModif.getNouvelleQuantite() != null) {

                    log.info("🔢 Modification quantité: {} -> {}",
                            ligne.getQuantite(), ligneModif.getNouvelleQuantite());
                    ligne.setQuantite(ligneModif.getNouvelleQuantite());
                }

                ligneReservationRepo.save(ligne);
            }
        }

        // 2. Recalculer le montant original
        double montantOriginal2 = reservation.getLigneReservations().stream()
                .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire()*
                        (ChronoUnit.DAYS.between(ligne.getDateDebut(), ligne.getDateFin()) + 1))
                .sum();

        // 3. Appliquer les remises
        double montantFinal = montantOriginal2;
        reservation.setRemisePourcentage(0.0);
        reservation.setRemiseMontant(0.0);

        if (modificationDto.getRemisePourcentage() != null && modificationDto.getRemisePourcentage() > 0) {
            double remise = montantOriginal2 * (modificationDto.getRemisePourcentage() / 100.0);
            montantFinal -= remise;
            log.info("💸 Remise de {}%: -{} TND", modificationDto.getRemisePourcentage(), remise);
            reservation.setRemisePourcentage(modificationDto.getRemisePourcentage());
            reservation.setRemiseMontant(0.0);
        }

        if (modificationDto.getRemiseMontant() != null && modificationDto.getRemiseMontant() > 0) {
            montantFinal -= modificationDto.getRemiseMontant();
            log.info("💸 Remise fixe: -{} TND", modificationDto.getRemiseMontant());
            reservation.setRemiseMontant(modificationDto.getRemiseMontant());
            reservation.setRemisePourcentage(0.0);
        }

        // S'assurer que le montant ne soit pas négatif
        if (montantFinal < 0) {
            montantFinal = 0.0;
        }

        reservation.setMontantTotal(montantFinal);
        reservation.setCommentaireAdmin(modificationDto.getCommentaireAdmin());
        reservation.setDateExpirationDevis(LocalDateTime.now().plusDays(2));
        reservation.setValidationAutomatique(false);


        reservationRepo.save(reservation);

        log.info("✅ Devis modifié - Montant original: {} TND, Montant final: {} TND",
                reservation.getMontantOriginal(), montantFinal);

        // Mettre à jour la facture DEVIS si elle existe
        mettreAJourFactureDevis(reservation);

        // ========================================
        // 🔔 NOTIFICATION + EMAIL CLIENT
        // ========================================

        Utilisateur client = reservation.getUtilisateur();

        // Construire le message de notification
        StringBuilder messageNotif = new StringBuilder();
        messageNotif.append(String.format(
                "Votre devis %s a été modifié par notre équipe.\n\n",
                reservation.getReferenceReservation()
        ));

        // Détails des modifications
        if (reservation.getMontantOriginal() != null &&
                !reservation.getMontantOriginal().equals(reservation.getMontantTotal())) {
            messageNotif.append(String.format(
                    " Montant mis à jour: %.2f TND → %.2f TND\n",
                    reservation.getMontantOriginal(),
                    reservation.getMontantTotal()
            ));
        }

        if (reservation.getRemisePourcentage() != null && reservation.getRemisePourcentage() > 0) {
            messageNotif.append(String.format(
                    " Remise appliquée: %.1f%%\n",
                    reservation.getRemisePourcentage()
            ));
        } else if (reservation.getRemiseMontant() != null && reservation.getRemiseMontant() > 0) {
            messageNotif.append(String.format(
                    " Remise appliquée: %.2f TND\n",
                    reservation.getRemiseMontant()
            ));
        }

        if (modificationDto.getCommentaireAdmin() != null &&
                !modificationDto.getCommentaireAdmin().isBlank()) {
            messageNotif.append(String.format(
                    "\n💬 Commentaire: %s\n",
                    modificationDto.getCommentaireAdmin()
            ));
        }

        messageNotif.append(String.format(
                "\n⏰ Vous avez jusqu'au %s pour accepter ou refuser ce devis.",
                reservation.getDateExpirationDevis().toLocalDate()
        ));

        // Créer la notification en BD
        NotificationRequestDto notif = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.DEVIS_VALIDE)
                .titre("Votre devis a été modifié")
                .message(messageNotif.toString())
                .idUtilisateur(client.getIdUtilisateur())
                .idReservation(reservation.getIdReservation())
                .urlAction("/client/mes-devis")
                .build();

        notificationService.creerNotification(notif);

        // Envoyer email au client
        StringBuilder emailBody = new StringBuilder();
        emailBody.append(String.format(
                "Votre devis %s a été examiné et modifié par notre équipe.\n\n",
                reservation.getReferenceReservation()
        ));
        emailBody.append(String.format(
                "Montant final: %.2f TND\n",
                reservation.getMontantTotal()
        ));

        if (reservation.getRemisePourcentage() != null && reservation.getRemisePourcentage() > 0) {
            emailBody.append(String.format(
                    "Une remise de %.1f%% a été appliquée.\n\n",
                    reservation.getRemisePourcentage()
            ));
        }

        if (modificationDto.getCommentaireAdmin() != null) {
            emailBody.append(String.format("Commentaire de notre équipe:\n%s\n\n",
                    modificationDto.getCommentaireAdmin()));
        }

        emailBody.append(String.format(
                "Vous avez jusqu'au %s pour accepter ou refuser ce devis dans votre espace client.",
                reservation.getDateExpirationDevis().toLocalDate()
        ));

        emailService.envoyerEmailNotification(
                client.getEmail(),
                client.getPrenom(),
                TypeNotification.DEVIS_VALIDE,
                "Votre devis a été modifié",
                emailBody.toString()
        );

        log.info("📧 Notification + Email envoyés au client {} pour modification devis",
                client.getEmail());

        return convertToResponseDto(reservation);
    }

    // ============ VALIDATION DU DEVIS PAR LE CLIENT ============

    @Override
    public ReservationResponseDto validerDevisParClient(ValidationDevisDto validationDto, String username) {
        log.info("🎯 ✅ Client {} {} le devis ID: {}",
                username,
                validationDto.getAccepter() ? "ACCEPTE" : "REFUSE",
                validationDto.getIdReservation());

        Utilisateur client = utilisateurRepo.findByPseudoOrEmail(username,username).orElse(null);

        Reservation reservation = reservationRepo.findById(validationDto.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

//        if(!reservation.isValidationAutomatique()){
//            throw new CustomException("Veuillez patienter la validation Administration");
//        }

        // Client refuse le devis
        if (!validationDto.getAccepter()) {
            log.warn("❌ Client refuse le devis {}", reservation.getReferenceReservation());
            reservation.setStatutReservation(StatutReservation.ANNULE);
            reservation.setCommentaireClient(validationDto.getCommentaireClient());
            reservationRepo.save(reservation);

            // 🔔 NOTIFICATION ADMINS/MANAGERS - DEVIS REFUSÉ
            notificationService.creerNotificationPourStaff(
                    TypeNotification.SYSTEME_ALERTE,
                    "Devis refusé par le client",
                    String.format(
                            "Le client %s  a refusé le devis %s.\n\n" +
                                    "Motif: %s\n\n" +
                                    "Montant du devis: %.2f TND",
                            client!=null ? client.getPrenom()+" "+client.getNom() : "N/A",
                            reservation.getReferenceReservation(),
                            validationDto.getCommentaireClient() != null ? validationDto.getCommentaireClient() : "Non spécifié",
                            reservation.getMontantTotal()
                    ),
                    reservation.getIdReservation(),
                    "/admin/reservations-details/" + reservation.getIdReservation()
            );

            log.info("📧 Admins notifiés du refus du devis {}", reservation.getReferenceReservation());


            return convertToResponseDto(reservation);
        }


        // Vérifier que c'est bien un devis en attente
        if (reservation.getStatutReservation() != StatutReservation.EN_ATTENTE) {
            throw new CustomException("Seuls les devis en attente peuvent être validés");
        }

        // VÉRIFIER LA DISPONIBILITÉ AVANT DE CONFIRMER
        log.info("🔍 Vérification de la disponibilité AVANT validation...");
        try {
            verifierDisponibiliteAvantValidation(reservation);
            log.info("✅ Disponibilité confirmée, réservation du stock...");
        } catch (ReservationException.StockIndisponibleException e) {
            // ❌ Le stock n'est plus disponible
            log.error("❌ Stock devenu indisponible: {}", e.getMessage());

            // Informer le client et lui proposer des alternatives
            reservation.setStatutReservation(StatutReservation.ANNULE);
            reservation.setCommentaireAdmin(
                    "Désolé, certains produits ne sont plus disponibles. " + e.getMessage() +
                            " Veuillez créer un nouveau devis."
            );
            reservationRepo.save(reservation);

            throw new CustomException(
                    "Impossible de valider le devis car certains produits ne sont plus disponibles. " +
                            e.getMessage()
            );
        }

        //  Le stock est disponible → On peut réserver
        //Date Expiration Reservation si Le client ne fais pas un acompte (une semaine )
        reservation.setDateExpirationDevis(LocalDateTime.now().plusDays(7));
        Reservation resValidee = reserverStockPourReservation(reservation);

        log.info("🎉 Réservation {} confirmée par le client et stock réservé avec succès",
                resValidee.getReferenceReservation());

        // 🆕 GÉNÉRATION AUTOMATIQUE DE LA FACTURE PRO_FORMA
        try {
            log.info("📄 Génération automatique de la facture PRO_FORMA...");
            factureService.genererOuMettreAJourFacture(
                    resValidee.getIdReservation(),
                    TypeFacture.PRO_FORMA,
                    username
            );
            log.info("✅ Facture PRO_FORMA générée/mise à jour avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur génération facture PRO_FORMA : {}", e.getMessage());

        }

        // ========================================
        // 🔔 NOTIFICATION + EMAIL ADMINS/MANAGERS
        // ========================================

        StringBuilder messageNotif = new StringBuilder();
        messageNotif.append(String.format(
                "🎉 Le client %s a accepté le devis et confirmé sa réservation!\n\n",
                client!= null ? client.getPrenom()+" "+client.getNom(): "N/A"
        ));
        messageNotif.append(String.format("📋 Réservation: %s\n", resValidee.getReferenceReservation()));
        messageNotif.append(String.format("📅 Période: %s au %s\n",
                resValidee.getDateDebut(),
                resValidee.getDateFin()
        ));
        messageNotif.append(String.format("💰 Montant total: %.2f TND\n", resValidee.getMontantTotal()));
        messageNotif.append(String.format("💵 Montant payé: %.2f TND\n",
                resValidee.getMontantPaye() != null ? resValidee.getMontantPaye() : 0.0));
        messageNotif.append(String.format("📦 Produits: %d lignes\n", resValidee.getLigneReservations().size()));
        messageNotif.append("\n✅ Le stock a été réservé automatiquement.");
        messageNotif.append("\n📋 Une facture PRO_FORMA a été générée.");

        // Notifier les admins/managers
        notificationService.creerNotificationPourStaff(
                TypeNotification.NOUVELLE_RESERVATION,
                "Nouvelle réservation confirmée",
                messageNotif.toString(),
                resValidee.getIdReservation(),
                "/admin/reservation-details/" + resValidee.getIdReservation()
        );

        log.info("📧 Notifications envoyées aux admins/managers pour la réservation confirmée {}",
                resValidee.getReferenceReservation());

        StringBuilder messageNotifClient = new StringBuilder();
        messageNotifClient.append(String.format(
                "Nous vous remercions vivement d'avoir choisi ELEGANT HIVE pour votre prochaine réservation du %s.\n\n",
                resValidee.getDateDebut()
        ));
        messageNotifClient.append(" Nous sommes ravis de vous servir très prochainement.\n");
        messageNotifClient.append("  Afin de finaliser la validation de votre dossier et de bloquer définitivement cette reservation pour vous,\n");
        messageNotifClient.append(String.format("pourriez-vous procéder au règlement de l'acompte 💵  d'ici le 📅 %s ?\n",   resValidee.getDateExpirationDevis().toLocalDate()));

        // ========================================
        // 🔔 NOTIFICATION + EMAIL Client
        // ========================================

        // Créer la notification en BD pour client
        NotificationRequestDto notif = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.RESERVATION_CONFIRMEE)
                .titre("🎉 Réservation Confirmé")
                .message(messageNotifClient.toString())
                .idUtilisateur(Objects.requireNonNull(client).getIdUtilisateur())
                .idReservation(resValidee.getIdReservation())
                .urlAction("/client/reservation-details/"+resValidee.getIdReservation())
                .build();
        notificationService.creerNotification(notif);

        emailService.envoyerEmailNotification(
                client.getEmail(),
                client.getPrenom(),
                TypeNotification.RESERVATION_CONFIRMEE,
                "Votre Reservation est Confirmé",
                messageNotifClient.toString()
        );

        return convertToResponseDto(resValidee);
    }

    // ============ ANNULATION ============

    @Override
    public void annulerReservationParClient(Long idReservation, String motif, String username) {
        log.info("❌ Annulation de la réservation ID: {} par le client {} - Motif: {}",
                idReservation, username, motif);

        Utilisateur client = utilisateurRepo.findByPseudoOrEmail(username,username).orElse(null);

        Reservation reservation = reservationRepo.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        // Vérifier que la réservation peut être annulée
        if (reservation.getStatutReservation() == StatutReservation.ANNULE) {
            throw new CustomException("Cette réservation est déjà annulée");
        }

        if (reservation.getStatutLivraisonRes() == StatutLivraison.LIVREE) {
            throw new CustomException("Impossible d'annuler une réservation déjà livrée");
        }


        // Libérer les instances si c'était confirmé
        if (reservation.getStatutReservation() == StatutReservation.CONFIRME) {
            log.info("🔓 Libération du stock pour réservation CONFIRMÉE");
           Reservation reservationlibere = libererStockReservation(reservation);
            reservationlibere.setStatutReservation(StatutReservation.ANNULE);
            reservationlibere.setCommentaireClient(motif);
            reservationlibere.setStockReserve(false);
            reservationRepo.save(reservationlibere);
            log.info("✅ Réservation annulée avec libération du stock");
        }else if (reservation.getStatutReservation() == StatutReservation.EN_ATTENTE) {
            // Simple annulation, pas de stock à libérer
            log.info("✅ Annulation devis EN_ATTENTE (pas de stock réservé)");
            reservation.setStatutReservation(StatutReservation.ANNULE);
            reservation.setCommentaireClient(motif);
            reservationRepo.save(reservation);
            log.info("✅ Devis annulé (aucune libération de stock nécessaire)");
        }

        // ========================================
        // 🔔 NOTIFICATION + EMAIL ADMINS/MANAGERS
        // ========================================

        boolean etaitConfirme = reservation.getStatutReservation() == StatutReservation.ANNULE &&
                !reservation.isStockReserve();

        StringBuilder messageNotif = new StringBuilder();
        messageNotif.append(String.format(
                " Le client %s  a annulé %s %s.\n\n",
                client!=null ? client.getPrenom()+" "+client.getNom():"N/A",
                etaitConfirme ? "sa réservation" : "son devis",
                reservation.getReferenceReservation()
        ));
        messageNotif.append(String.format("💰 Montant: %.2f TND\n", reservation.getMontantTotal()));

        if (etaitConfirme && reservation.getMontantPaye() != null && reservation.getMontantPaye() > 0) {
            messageNotif.append(String.format("💵 Montant déjà payé: %.2f TND (remboursement à prévoir)\n",
                    reservation.getMontantPaye()));
        }

        if (motif != null && !motif.isBlank()) {
            messageNotif.append(String.format("\n💬 Motif: %s\n", motif));
        }

        if (etaitConfirme) {
            messageNotif.append("\nLe stock a été libéré automatiquement.");
        }

        // Notifier les admins/managers
        notificationService.creerNotificationPourStaff(
                TypeNotification.SYSTEME_ALERTE,
                etaitConfirme ? "Réservation annulée par le client" : "Devis annulé par le client",
                messageNotif.toString(),
                reservation.getIdReservation(),
                "/admin/reservation-details/" + reservation.getIdReservation()
        );

        log.info("📧 Notifications envoyées aux admins/managers pour annulation de {}",
                reservation.getReferenceReservation());



    }

    @Override
    public void annulerDevisParAdmin(Long idReservation, String motif, String username) {
        // Même logique que l'annulation par le client (différence commentaire)
        log.info("❌ Annulation de la réservation ID: {} par l'admin {} - Motif: {}",
                idReservation, username, motif);

        Reservation reservation = reservationRepo.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        // Vérifier que la réservation peut être annulée
        if (reservation.getStatutReservation() == StatutReservation.ANNULE) {
            throw new CustomException("Cette réservation est déjà annulée");
        }

        if (reservation.getStatutLivraisonRes() == StatutLivraison.LIVREE) {
            throw new CustomException("Impossible d'annuler une réservation déjà livrée");
        }


        // Libérer les instances si c'était confirmé
        if (reservation.getStatutReservation() == StatutReservation.CONFIRME) {
            log.info("🔓 Libération du stock pour réservation CONFIRMÉE : Par {}",username);
            Reservation reservationlibere = libererStockReservation(reservation);
            reservationlibere.setStatutReservation(StatutReservation.ANNULE);
            reservationlibere.setCommentaireAdmin(motif);
            reservationlibere.setStockReserve(false);
            reservationRepo.save(reservationlibere);
            log.info("✅ Réservation annulée avec libération du stock:  Par {}",username);
        }else if (reservation.getStatutReservation() == StatutReservation.EN_ATTENTE) {
            // Simple annulation, pas de stock à libérer
            log.info("✅ Annulation devis EN_ATTENTE (pas de stock réservé) :  Par {}",username);
            reservation.setStatutReservation(StatutReservation.ANNULE);
            reservation.setCommentaireAdmin(motif);
            reservationRepo.save(reservation);
            log.info("✅ Devis annulé (aucune libération de stock nécessaire) :  Par {}",username);
        }

        // ========================================
        // 🔔 NOTIFICATION + EMAIL CLIENT
        // ========================================

        StringBuilder messageNotif = new StringBuilder();
        messageNotif.append(String.format(
                "⚠️ Votre devis %s a été annulé par notre équipe.\n\n",
                reservation.getReferenceReservation()
        ));

        if (motif != null && !motif.isBlank()) {
            messageNotif.append(String.format("💬 Motif: %s\n\n", motif));
        }

        messageNotif.append("Vous pouvez créer un nouveau devis à tout moment dans votre espace client.");

        // Créer la notification en BD
        NotificationRequestDto notif = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.SYSTEME_ALERTE)
                .titre("Votre devis a été annulé")
                .message(messageNotif.toString())
                .idUtilisateur(reservation.getUtilisateur().getIdUtilisateur())
                .idReservation(reservation.getIdReservation())
                .urlAction("/client/mes-commandes")
                .build();

        notificationService.creerNotification(notif);

        // Envoyer email au client
        StringBuilder emailBody = new StringBuilder();
        emailBody.append(String.format(
                "Nous vous informons que votre devis %s a été annulé.\n\n",
                reservation.getReferenceReservation()
        ));

        if (motif != null && !motif.isBlank()) {
            emailBody.append(String.format("Raison: %s\n\n", motif));
        }

        emailBody.append("N'hésitez pas à créer un nouveau devis ou à nous contacter pour plus d'informations.\n\n");
        emailBody.append("L'équipe Elegant Hive reste à votre disposition.");

        emailService.envoyerEmailNotification(
                reservation.getUtilisateur().getEmail(),
                reservation.getUtilisateur().getPrenom(),
                TypeNotification.SYSTEME_ALERTE,
                "Votre devis a été annulé",
                emailBody.toString()
        );

        log.info("📧 Notification + Email envoyés au client {} pour annulation devis",
                reservation.getUtilisateur().getEmail());

    }

    // ============ CONSULTATION ============

    @Override
    public ReservationResponseDto getReservationById(Long idReservation) {
        Reservation reservation = reservationRepo.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));
        return convertToResponseDto(reservation);
    }

    @Override
    public ReservationResponseDto getReservationByReference(String referenceReservation) {
        Reservation reservation = reservationRepo.findByReferenceReservation(referenceReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));
        return convertToResponseDto(reservation);
    }

    @Override
    public List<ReservationResponseDto> getReservationsByClient(Long idUtilisateur) {
        return reservationRepo.findByUtilisateur_IdUtilisateurOrderByDateDebutDesc(idUtilisateur)
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getDevisEnAttenteByClient(Long idUtilisateur) {
        return reservationRepo.findDevisEnAttenteByClient(idUtilisateur)
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getAllReservations() {
        return reservationRepo.findAll()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getReservationsByStatut(StatutReservation statut) {
        return reservationRepo.findByStatutReservation(statut)
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getAllDevisEnAttente() {
        return reservationRepo.findAllDevisEnAttente()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // ============ RECHERCHE AVANCÉE ============

    @Override
    public List<ReservationResponseDto> searchReservations(ReservationSearchDto searchDto) {
        return reservationRepo.searchReservations(
                        searchDto.getIdUtilisateur(),
                        searchDto.getStatut(),
                        searchDto.getDateDebutMin(),
                        searchDto.getDateDebutMax(),
                        searchDto.getReferenceReservation()
                ).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getReservationsByPeriode(LocalDate dateDebut, LocalDate dateFin) {
        return reservationRepo.findReservationsBetweenDates(dateDebut, dateFin)
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getReservationsAVenir() {
        return reservationRepo.findReservationsConfirmeesAVenir()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getReservationsEnCours() {
        return reservationRepo.findReservationsEnCours()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getReservationsPassees() {
        return reservationRepo.findReservationsPassees()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getReservationsEmployeAffecte(String username) {
        log.info("📋 Recherche des réservations pour l'employé: {}", username);

        // 1. Récupérer l'utilisateur connecté
        Utilisateur employe = utilisateurRepo.findByPseudoOrEmail(username,username)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        // 2. Récupérer toutes les affectations de cet employé
        List<AffectationLivraison> affectations =
                affectationRepo.findByUtilisateur_IdUtilisateur(employe.getIdUtilisateur());

        log.info("✅ Trouvé {} affectations pour l'employé", affectations.size());

        // 3. Extraire les IDs de livraisons
        Set<Long> idsLivraisons = affectations.stream()
                .map(a -> a.getLivraison().getIdLivraison())
                .collect(Collectors.toSet());

        if (idsLivraisons.isEmpty()) {
            log.info("ℹ️ Aucune livraison affectée à cet employé");
            return Collections.emptyList();
        }

        // 4. Récupérer les lignes de réservation associées à ces livraisons
        List<LigneReservation> lignes = ligneReservationRepo
                .findByLivraison_IdLivraisonIn(new ArrayList<>(idsLivraisons));

        log.info("✅ Trouvé {} lignes de réservation", lignes.size());

        // 5. Extraire les réservations uniques
        Set<Long> idsReservations = lignes.stream()
                .map(ligne -> ligne.getReservation().getIdReservation())
                .collect(Collectors.toSet());

        // 6. Récupérer les réservations complètes
        List<Reservation> reservations = reservationRepo
                .findAllById(idsReservations);

        log.info("✅ Trouvé {} réservations affectées à l'employé", reservations.size());

        // 7. Convertir en DTO et retourner
        return reservations.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // ============ MODIFICATION ============


    /**
     * Méthode auxiliaire : Vérifier disponibilité quantitative en excluant une réservation
     */
    private int verifierDisponibiliteQuantitative(
            Long idProduit,
            LocalDate dateDebut,
            LocalDate dateFin,
            Long reservationExclue) {

        Produit produit = produitRepo.findById(idProduit)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Produit avec ID " + idProduit + " introuvable"));

        int quantiteTotale = produit.getQuantiteInitial();

        // Calculer quantité déjà réservée (en excluant la réservation actuelle)
        int quantiteReservee = ligneReservationRepo
                .findQuantiteReserveeForProduitInPeriodExcludingReservation(
                        idProduit,
                        dateDebut,
                        dateFin,
                        reservationExclue
                );

        return quantiteTotale - quantiteReservee;
    }

    /**
     * Méthode auxiliaire : Vérifier disponibilité d'une instance en excluant une réservation
     */
    private boolean verifierDisponibiliteInstance(
            Long idInstance,
            LocalDate dateDebut,
            LocalDate dateFin,
            Long reservationExclue) {

        // Compter combien de fois cette instance est réservée sur la période
        // (en excluant la réservation actuelle)
        long count = ligneReservationRepo
                .countReservationsForInstanceInPeriodExcludingReservation(
                        idInstance,
                        dateDebut,
                        dateFin,
                        reservationExclue
                );

        return count == 0;  // Disponible si aucune autre réservation
    }

    /**
     *
     * Vérifier la disponibilité RÉELLE avant de valider un devis
     *
     * Cette méthode est appelée juste avant de confirmer la réservation
     * pour s'assurer que le stock n'a pas été réservé entre-temps
     *
     * @throws StockIndisponibleException si un produit n'est plus disponible
     */
    private void verifierDisponibiliteAvantValidation(Reservation reservation)
            throws StockIndisponibleException {

        log.debug("🔍 Vérification disponibilité pour réservation {}",
                reservation.getReferenceReservation());

        for (LigneReservation ligne : reservation.getLigneReservations()) {
            Produit produit = ligne.getProduit();

            DisponibiliteResponseDto dispo = verifierDisponibilite(
                    VerificationDisponibiliteDto.builder()
                            .idProduit(produit.getIdProduit())
                            .quantite(ligne.getQuantite())
                            .dateDebut(ligne.getDateDebut())
                            .dateFin(ligne.getDateFin())
                            .build()
            );

            if (!dispo.getDisponible()) {
                String message = String.format(
                        "Le produit '%s' n'est plus disponible pour la période demandée. " +
                                "Quantité demandée: %d, Quantité disponible: %d",
                        produit.getNomProduit(),
                        ligne.getQuantite(),
                        dispo.getQuantiteDisponible()
                );

                log.error("❌ {}", message);
                throw new StockIndisponibleException(message);
            }
        }

        log.debug("✅ Tous les produits sont disponibles");
    }

    public VerificationModificationDatesDto verifAvantModifDateReservation (Long idReservation, DatePeriodeDto nouvellesDates){
        Reservation reservation = reservationRepo.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));
        // Vérifier la disponibilité pour chaque ligne
        boolean toutDisponible = true;
        StringBuilder message = new StringBuilder();

        for (LigneReservation ligne : reservation.getLigneReservations()) {
            Produit produit = ligne.getProduit();

            if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
                // Vérifier disponibilité pour produits quantitatifs
                int quantiteDisponible = verifierDisponibiliteQuantitative(
                        produit.getIdProduit(),
                        nouvellesDates.getDateDebut(),
                        nouvellesDates.getDateFin(),
                        idReservation  // Exclure cette réservation du calcul
                );

                if (quantiteDisponible < ligne.getQuantite()) {
                    toutDisponible = false;
                            message.append(String.format("Le produit '%s' n'est pas disponible en quantité suffisante " +
                                            "pour les nouvelles dates. Disponible: %d, Demandé: %d\n",
                                    produit.getNomProduit(), quantiteDisponible, ligne.getQuantite()));
                }

            } else if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
                // Vérifier disponibilité pour produits avec référence
                for (InstanceProduit instance : ligne.getInstancesReservees()) {
                    boolean estDisponible = verifierDisponibiliteInstance(
                            instance.getIdInstance(),
                            nouvellesDates.getDateDebut(),
                            nouvellesDates.getDateFin(),
                            idReservation
                    );

                    if (!estDisponible) {
                        toutDisponible = false;
                        message.append(String.format("L'instance '%s' du produit '%s' n'est pas disponible " +
                                        "pour les nouvelles dates\n",
                                instance.getNumeroSerie(), produit.getNomProduit()));
                    }
                }
            }
        }

        if (toutDisponible) {
            long nbJours = dateValidator.calculerNombreJours(
                    nouvellesDates.getDateDebut(),
                    nouvellesDates.getDateFin()
            );

            return VerificationModificationDatesDto.builder()
                    .possible(true)
                    .message("Tous les produits sont disponibles pour les nouvelles dates (" + nbJours + " jours)")
                    .nombreJours(nbJours)
                    .build();
        } else {
            return VerificationModificationDatesDto.builder()
                    .possible(false)
                    .message("Certains produits ne sont pas disponibles:\n" + message.toString())
                    .build();
        }
    }

    // ============ STATISTIQUES ============

    @Override
    public ReservationSummaryDto getStatistiquesReservations() {
        long totalReservations = reservationRepo.count();
        long reservationsEnAttente = reservationRepo.countByStatutReservation(StatutReservation.EN_ATTENTE);
        long reservationsConfirmees = reservationRepo.countByStatutReservation(StatutReservation.CONFIRME);
        long reservationsAnnulees = reservationRepo.countByStatutReservation(StatutReservation.ANNULE);

        Double chiffreAffairesTotal = reservationRepo.calculateChiffreAffairesTotal();
        if (chiffreAffairesTotal == null) chiffreAffairesTotal = 0.0;

        return ReservationSummaryDto.builder()
                .totalReservations(totalReservations)
                .reservationsEnAttente(reservationsEnAttente)
                .reservationsConfirmees(reservationsConfirmees)
                .reservationsAnnulees(reservationsAnnulees)
                .chiffreAffairesTotal(chiffreAffairesTotal)
                .chiffreAffairesConfirmees(chiffreAffairesTotal)
                .build();
    }

    @Override
    public ReservationSummaryDto getStatistiquesReservationsClient(Long idClient) {
        log.info("📊 Récupération des statistiques pour le client ID: {}", idClient);

        // 1️⃣ VÉRIFIER QUE LE CLIENT EXISTE
        Utilisateur client = utilisateurRepo.findById(idClient)
                .orElseThrow(() -> new CustomException("Client avec ID " + idClient + " introuvable"));

        // 2️⃣ RÉCUPÉRER TOUTES LES RÉSERVATIONS DU CLIENT
        List<Reservation> reservations = reservationRepo.findByUtilisateur_IdUtilisateur(idClient);

        // 3️⃣ CALCULER LES STATISTIQUES GÉNÉRALES

        long nombreTotal = reservations.size();

        long nombreEnAttente = reservations.stream()
                .filter(r -> r.getStatutReservation() == StatutReservation.EN_ATTENTE)
                .count();

        long nombreConfirme = reservations.stream()
                .filter(r -> r.getStatutReservation() == StatutReservation.CONFIRME)
                .count();

        long nombreAnnule = reservations.stream()
                .filter(r -> r.getStatutReservation() == StatutReservation.ANNULE)
                .count();

        long nombreTermine = reservations.stream()
                .filter(r -> r.getStatutReservation() == StatutReservation.TERMINE)
                .count();

        // 4️⃣ CALCULER LES MONTANTS

        // Montant total de toutes les réservations confirmées et terminées
        double montantTotal = reservations.stream()
                .filter(r -> r.getStatutReservation() == StatutReservation.CONFIRME ||
                        r.getStatutReservation() == StatutReservation.TERMINE)
                .mapToDouble(r -> r.getMontantTotal() != null ? r.getMontantTotal() : 0.0)
                .sum();

        // Montant total payé
        double montantPaye = reservations.stream()
                .filter(r -> r.getStatutReservation() == StatutReservation.CONFIRME ||
                        r.getStatutReservation() == StatutReservation.TERMINE)
                .mapToDouble(r -> r.getMontantPaye() != null ? r.getMontantPaye() : 0.0)
                .sum();

        // Montant moyen par réservation
        double montantMoyen = nombreConfirme + nombreTermine > 0
                ? montantTotal / (nombreConfirme + nombreTermine)
                : 0.0;

        // 5️⃣ TROUVER LES PRODUITS LES PLUS RÉSERVÉS PAR CE CLIENT

        // Map : ID Produit -> Nombre de fois réservé
        Map<Long, Long> produitsReserves = new HashMap<>();

        for (Reservation reservation : reservations) {
            if (reservation.getStatutReservation() != StatutReservation.ANNULE) {
                for (LigneReservation ligne : reservation.getLigneReservations()) {
                    Long idProduit = ligne.getProduit().getIdProduit();
                    produitsReserves.put(idProduit,
                            produitsReserves.getOrDefault(idProduit, 0L) + 1);
                }
            }
        }

        // Trier et prendre le top 3
        List<Map.Entry<Long, Long>> topProduits = produitsReserves.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(3)
                .toList();

        // 6️⃣ CONSTRUIRE LE RÉSULTAT

        ReservationSummaryDto summary = ReservationSummaryDto.builder()
                .totalReservations(nombreTotal)
                .reservationsEnAttente( nombreEnAttente)
                .reservationsConfirmees( nombreConfirme)
                .reservationsAnnulees(nombreAnnule)
                .reservationsTermine(nombreTermine)
                .montantTotal(montantTotal)
                .montantPaye(montantPaye)
                .montantMoyen(montantMoyen)
                .build();

        // Ajouter des informations supplémentaires sur le client
        summary.setNomClient(client.getNom() + " " + client.getPrenom());
        summary.setEmailClient(client.getEmail());

        // Ajouter les produits préférés
        List<String> produitsPreferences = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : topProduits) {
            produitRepo.findById(entry.getKey()).ifPresent(produit -> produitsPreferences.add(
                    String.format("%s (%d fois)", produit.getNomProduit(), entry.getValue())
            ));
        }
        summary.setProduitsPreferences(produitsPreferences);

        log.info("✅ Statistiques client calculées: {} réservations totales, {} € de CA",
                nombreTotal, montantTotal);

        return summary;
    }

    @Override
    public Double calculateChiffreAffairesPeriode(Date dateDebut, Date dateFin) {
        Double ca = reservationRepo.calculateChiffreAffairesPeriode(dateDebut, dateFin);
        return ca != null ? ca : 0.0;
    }

    // ============ ALERTES ============

    @Override
    public List<ReservationResponseDto> getReservationsCommencantDansNJours(int nbreJours) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, nbreJours);
        Date dateLimit = calendar.getTime();

        return reservationRepo.findReservationsCommencantDansNJours(dateLimit)
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getReservationsFinissantDansNJours(int nbreJours) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, nbreJours);
        Date dateLimit = calendar.getTime();

        return reservationRepo.findReservationsFinissantDansNJours(dateLimit)
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getDevisExpires(int nbreJours) {
        return reservationRepo.findDevisExpires(nbreJours)
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponseDto> getDevisExpiresToday() {
        log.info("La liste de devis expiré (Reservation expiré)");
        return   reservationRepo.findByDateExpirationDevis(LocalDateTime.now())
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

    }

    @Override
    public List<ReservationResponseDto> getReservationsAvecPaiementIncomplet() {
        return reservationRepo.findReservationsAvecPaiementIncomplet()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // ============ UTILITAIRES ============

    /**
     * Générer une référence unique pour une réservation
     * Format: RES-YYYY-NNNN (ex: RES-2025-0001)
     */
    private String genererReferenceReservation() {
        int annee = Calendar.getInstance().get(Calendar.YEAR);
        String prefix = "RES-" + annee + "-";

        // Chercher la dernière référence de l'année
        long count = reservationRepo.count();
        String numero = String.format("%04d", count + 1);

        String reference = prefix + numero;

        // Vérifier l'unicité
        while (reservationRepo.existsByReferenceReservation(reference)) {
            count++;
            numero = String.format("%04d", count + 1);
            reference = prefix + numero;
        }

        return reference;
    }

    /**
     * Enregistrer un mouvement de stock
     */
    /**
     * Enregistrer un mouvement de stock lié à une réservation
     *
     * @param produit Produit concerné
     * @param quantite Quantité (positive pour entrée, négative pour sortie)
     * @param typeMouvement Type de mouvement
     * @param reservation Réservation associée
     * @param motif Motif du mouvement
     * @param username Utilisateur ayant effectué l'action
     *
     * 📝 TYPES DE MOUVEMENTS GÉRÉS :
     * - RESERVATION : Allocation du stock pour une réservation
     * - ANNULATION_RESERVATION : Libération du stock
     * - LIVRAISON : Sortie physique du stock
     * - RETOUR : Retour du stock après événement
     */
    @Transactional
    public void enregistrerMouvementStock(
            Produit produit,
            Integer quantite,
            TypeMouvement typeMouvement,
            Reservation reservation,
            String motif,
            String username) {

        log.info("📦 Enregistrement mouvement stock: {} {} pour produit '{}'",
                quantite, typeMouvement, produit.getNomProduit());

        // 1️⃣ CALCULER LA QUANTITÉ AVANT/APRÈS LE MOUVEMENT

        Integer quantiteAvant = null;
        Integer quantiteApres = null;

        if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
            quantiteAvant = produit.getQuantiteDisponible();

            // Calculer la nouvelle quantité selon le type de mouvement
            switch (typeMouvement) {
                case RESERVATION:
                case LIVRAISON:
                    // Sortie : diminuer le stock
                    quantiteApres = quantiteAvant - quantite;
                    break;

                case ANNULATION_RESERVATION:
                case RETOUR:
                    // Entrée : augmenter le stock
                    quantiteApres = quantiteAvant + quantite;
                    break;

                case AJOUT_STOCK:
                    // Entrée directe
                    quantiteApres = quantiteAvant + quantite;
                    break;

                case RETRAIT_STOCK:
                case PRODUIT_ENDOMMAGE:
                    // Sortie directe
                    quantiteApres = quantiteAvant - quantite;
                    break;

                default:
                    quantiteApres = quantiteAvant;
            }
        }

        // 2️⃣ CRÉER LE MOUVEMENT DE STOCK

        MouvementStock mouvement = new MouvementStock(produit,typeMouvement,Math.abs(quantite),quantiteAvant,quantiteApres,
                motif != null ? motif : typeMouvement.toString(),username);

        // 3️⃣ ASSOCIER LA RÉSERVATION SI FOURNIE
        if (reservation != null) {
            mouvement.setReferenceReservation(reservation.getReferenceReservation());
            mouvement.setIdReservation(reservation.getIdReservation());
        }

        // 4️⃣ SAUVEGARDER LE MOUVEMENT
        mouvementStockRepo.save(mouvement);

        // 5️⃣ LOG DÉTAILLÉ
        log.info("✅ Mouvement stock enregistré:");
        log.info("   - Produit: {} (ID: {})", produit.getNomProduit(), produit.getIdProduit());
        log.info("   - Type: {}", typeMouvement);
        log.info("   - Quantité: {}", quantite);
        if (quantiteAvant != null && quantiteApres != null) {
            log.info("   - Stock: {} → {}", quantiteAvant, quantiteApres);
        }
        if (reservation != null) {
            log.info("   - Réservation: {}", reservation.getReferenceReservation());
        }
        log.info("   - Par: {}", username);
        log.info("   - Motif: {}", motif);
    }

    /**
     * VARIANTE : Enregistrer un mouvement pour un produit AVEC_REFERENCE
     *
     * @param instance Instance de produit concernée
     * @param typeMouvement Type de mouvement
     * @param reservation Réservation associée
     * @param motif Motif du mouvement
     * @param username Utilisateur ayant effectué l'action
     */
    @Transactional
    public void enregistrerMouvementStockInstance(
            InstanceProduit instance,
            TypeMouvement typeMouvement,
            Reservation reservation,
            String motif,
            String username) {

        log.info("📦 Enregistrement mouvement pour instance: {} - {}",
                instance.getNumeroSerie(), typeMouvement);

        MouvementStock mouvement = new MouvementStock(instance.getProduit(),typeMouvement,1,
                motif != null ? motif : typeMouvement.toString(),username);



        // Associer l'instance
        mouvement.setCodeInstance(instance.getNumeroSerie());
        mouvement.setIdInstance(instance.getIdInstance());

        // Associer la réservation si fournie
        if (reservation != null) {
            mouvement.setReferenceReservation(reservation.getReferenceReservation());
            mouvement.setIdReservation(reservation.getIdReservation());
            mouvement.setDateDebut(reservation.getDateDebut());
            mouvement.setDateFin(reservation.getDateFin());
        }


        // Sauvegarder
        mouvementStockRepo.save(mouvement);

        log.info("✅ Mouvement instance enregistré: {} - Instance: {}",
                typeMouvement, instance.getNumeroSerie());
    }

    /**
     * Convertir une entité Reservation en DTO de réponse
     */
    private ReservationResponseDto convertToResponseDto(Reservation reservation) {
        Utilisateur client = reservation.getUtilisateur();

        List<LigneReservationResponseDto> lignesDto = new ArrayList<>();
        if (reservation.getLigneReservations() != null) {
            lignesDto = reservation.getLigneReservations().stream()
                    .map(this::convertLigneToDto)
                    .collect(Collectors.toList());
        }

        // Calculer le montant restant
        double montantRestant = reservation.getMontantTotal() -
                (reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0);

        // Calculer la durée
        long joursLocation = 0;
        if (reservation.getDateDebut() != null && reservation.getDateFin() != null) {
            LocalDate debut = reservation.getDateDebut();
            LocalDate fin = reservation.getDateFin();
            joursLocation = ChronoUnit.DAYS.between(debut, fin) + 1;  // +1 pour inclure le dernier jour
        }

        return ReservationResponseDto.builder()
                .idReservation(reservation.getIdReservation())
                .referenceReservation(reservation.getReferenceReservation())
                .idUtilisateur(client.getIdUtilisateur())
                .nomClient(client.getNom())
                .prenomClient(client.getPrenom())
                .emailClient(client.getEmail())
                .telephoneClient(client.getTelephone())
                .dateCreation(reservation.getDateCreation())
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .statutReservation(reservation.getStatutReservation())
                .statutLivraisonRes(reservation.getStatutLivraisonRes())
                .montantOriginal(reservation.getMontantOriginal())
                .montantTotal(reservation.getMontantTotal())
                .remiseMontant(reservation.getRemiseMontant())
                .remisePourcentage(reservation.getRemisePourcentage())
                .montantPaye(reservation.getMontantPaye())
                .montantRestant(montantRestant)
                .modePaiementRes(reservation.getModePaiementRes())
                .lignesReservation(lignesDto)
                .estDevis(reservation.getStatutReservation() == StatutReservation.EN_ATTENTE)
                .paiementComplet(montantRestant <= 0)
                .nombreProduits(lignesDto.size())
                .joursLocation((int) joursLocation)
                .commentaireAdmin(reservation.getCommentaireAdmin())
                .observationsClient(reservation.getCommentaireClient())
                .build();
    }

    /**
     * Convertir une ligne de réservation en DTO
     */
    private LigneReservationResponseDto convertLigneToDto(LigneReservation ligne) {
        Produit produit = ligne.getProduit();

        List<String> numerosSeries = new ArrayList<>();
        if (ligne.getInstancesReservees() != null) {
            numerosSeries = ligne.getInstancesReservees().stream()
                    .map(InstanceProduit::getNumeroSerie)
                    .collect(Collectors.toList());
        }

        double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire();

        return LigneReservationResponseDto.builder()
                .idLigneReservation(ligne.getIdLigneReservation())
                .idProduit(produit.getIdProduit())
                .nomProduit(produit.getNomProduit())
                .codeProduit(produit.getCodeProduit())
                .imageProduit(produit.getImageProduit())
                .quantite(ligne.getQuantite())
                .prixUnitaire(ligne.getPrixUnitaire())
                .sousTotal(sousTotal)
                .dateDebut(ligne.getDateDebut())
                .dateFin(ligne.getDateFin())
                .statutLivraisonLigne(ligne.getStatutLivraisonLigne())
                .observations(ligne.getObservations())
                .numerosSeries(numerosSeries)
                .idLivraison(ligne.getLivraison() != null ? ligne.getLivraison().getIdLivraison() : null)
                .titreLivraison(ligne.getLivraison() != null ? ligne.getLivraison().getTitreLivraison() : null)
                .build();
    }

    /**
     * Méthode utilitaire pour reserver le stock une fois le client valide le devis
     */
    public Reservation reserverStockPourReservation(Reservation reservation){
        // Client accepte → Affecter les instances
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            if (ligne.isProduitAvecReference()) {

                // ✅ Vérifier la disponibilité sur la période
                List<InstanceProduit> instancesDisponibles = instanceProduitRepo.findInstancesDisponiblesSurPeriode(
                        ligne.getProduit().getIdProduit(),
                        ligne.getDateDebut(),
                        ligne.getDateFin()
                );

                if (instancesDisponibles.size() < ligne.getQuantite()) {
                    throw new ProduitException(
                            "Stock insuffisant pour " + ligne.getProduit().getNomProduit() +
                                    " du " + ligne.getDateDebut() + " au " + ligne.getDateFin()
                    );
                }

                // ✅ Affecter les instances à la ligne (ManyToMany)
                Set<InstanceProduit> instancesAAffecter = instancesDisponibles.stream()
                        .limit(ligne.getQuantite())
                        .collect(Collectors.toSet());

                ligne.setInstancesReservees(instancesAAffecter);
                ligneReservationRepo.save(ligne);

                for(InstanceProduit instanceProduit : instancesAAffecter){
                    enregistrerMouvementStockInstance(instanceProduit,TypeMouvement.RESERVATION,
                            reservation,"Reservation Instance",reservation.getUtilisateur().getPseudo());
                }


                log.info("{} instances affectées à la ligne {} pour la période {}-{}",
                        ligne.getQuantite(),
                        ligne.getIdLigneReservation(),
                        ligne.getDateDebut(),
                        ligne.getDateFin());
            }else{
                Integer quantiteReservee = ligneReservationRepo.calculateQuantiteReserveeSurPeriode(
                        ligne.getProduit().getIdProduit(),
                        ligne.getDateDebut(),
                        ligne.getDateFin()
                );

                if (quantiteReservee == null) {
                    quantiteReservee = 0;
                }

                // Calculer la quantité réellement disponible
                int quantiteDisponible = ligne.getProduit().getQuantiteDisponible() - quantiteReservee;

                if(quantiteDisponible < ligne.getQuantite()){
                    throw new ProduitException(
                            "Stock insuffisant pour " + ligne.getProduit().getNomProduit() +
                                    " du " + ligne.getDateDebut() + " au " + ligne.getDateFin()
                    );
                }
                ligneReservationRepo.save(ligne);
                //enregistrer mouvemenet
                enregistrerMouvementStock(ligne.getProduit(), ligne.getQuantite(), TypeMouvement.RESERVATION,
                        reservation,"Reservation",reservation.getUtilisateur().getPseudo());
            }
        }
        // Confirmer la réservation
        reservation.setStatutReservation(StatutReservation.CONFIRME);
        reservation.setStatutPaiement(StatutPaiementRes.EN_ATTENTE_PAIEMENT);

        reservation.setStockReserve(Boolean.TRUE);

        log.info("🎉 Réservation confirmée avec succès: {}", reservation.getReferenceReservation());

        reservationRepo.save(reservation);
        return reservation ;
    }


    @Transactional
    public Reservation libererStockReservation(Reservation reservation) {
        log.info("🔓 Libération du stock pour {}",
                reservation.getReferenceReservation());

        if (!Boolean.TRUE.equals(reservation.getStockReserve())) {
            log.warn("Stock pas réservé - Aucune action");
            throw new CustomException("Stock pas réservé");
        }
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            Produit produit = ligne.getProduit();

            if (ligne.isProduitAvecReference()) {
                // Simplement vider la collection
                for (InstanceProduit instanceProduit : ligne.getInstancesReservees()){
                    enregistrerMouvementStockInstance(
                            instanceProduit,
                            TypeMouvement.ANNULATION_RESERVATION,
                            reservation,
                            "Libération - Annulation/Expiration",
                            "SYSTEM"
                    );
                }
                ligne.getInstancesReservees().clear();
                ligneReservationRepo.save(ligne);

                log.info("Instances de la ligne {} libérées automatiquement",
                        ligne.getIdLigneReservation());


            }else {
                // Enregistrer mouvement pour produit en quantité
                enregistrerMouvementStock(
                        produit,
                        ligne.getQuantite(),
                        TypeMouvement.ANNULATION_RESERVATION,
                        reservation,
                        "Libération suite annulation",
                        reservation.getUtilisateur().getPseudo()
                );
            }


        }
        reservation.setStockReserve(Boolean.FALSE);
        reservationRepo.save(reservation);
        log.info("✅ Stock libéré pour {}", reservation.getReferenceReservation());
        return reservation;
    }

    // ============ MÉTHODE UTILITAIRE POUR OBTENIR LES CONTRAINTES ============

    /**
     * Obtenir les contraintes de dates pour l'affichage au client
     * (Utile pour le frontend)
     */
    public DateConstraintesDto getContraintesDates() {
        return DateConstraintesDto.builder()
                .dateMinimale(dateValidator.getDateMinimaleReservation())
                .dateMaximale(dateValidator.getDateMaximaleReservation())
                .dureeMinJours(dateValidator.getDureeMinLocation())
                .dureeMaxJours(dateValidator.getDureeMaxLocation())
                .reservationAujourdhuiAutorisee(dateValidator.getDateMinimaleReservation().equals(LocalDate.now()))
                .build();
    }

    protected void mettreAJourFactureDevis(Reservation reservation) {
        factureService.mettreAJourFactureDevisSafe(reservation.getIdReservation());
    }
}