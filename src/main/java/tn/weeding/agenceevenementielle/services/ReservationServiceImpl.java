package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.reservation.*;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.ProduitException;
import tn.weeding.agenceevenementielle.exceptions.ReservationException;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ==========================================
 * IMPLÉMENTATION DU SERVICE DE RÉSERVATION
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 */
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
    private final InstanceProduitServiceInterface instanceProduitService;

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

        // 3. Vérifier la disponibilité de TOUS les produits AVANT de créer le devis
        log.info("📦 Vérification de la disponibilité de {} produits", devisRequest.getLignesReservation().size());
        List<DisponibiliteResponseDto> disponibilites = new ArrayList<>();

        for (LigneReservationRequestDto ligneDto : devisRequest.getLignesReservation()) {
            VerificationDisponibiliteDto verif = VerificationDisponibiliteDto.builder()
                    .idProduit(ligneDto.getIdProduit())
                    .quantite(ligneDto.getQuantite())
                    .dateDebut(ligneDto.getDateDebut())
                    .dateFin(ligneDto.getDateFin())
                    .build();

            DisponibiliteResponseDto dispo = verifierDisponibilite(verif);
            disponibilites.add(dispo);

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


            double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire();
            montantTotal += sousTotal;

            lignes.add(ligne);
            log.info("📝 Ligne ajoutée: {} x {} = {} TND",
                    produit.getNomProduit(), ligne.getQuantite(), sousTotal);
        }

        reservation.setLigneReservations(lignes);
        reservation.setMontantTotal(montantTotal);
        reservation.setStatutReservation(StatutReservation.EN_ATTENTE);


        // 6. Sauvegarder
        Reservation devisCree = reservationRepo.save(reservation);
        log.info("✅ Devis créé avec succès: {} - Montant: {} TND",
                devisCree.getReferenceReservation(), montantTotal);

        //  VALIDATION AUTOMATIQUE si client Valide directement sans Review Admin
        if(devisRequest.isValidationAutomatique()){
           Reservation resValide = reserverStockPourReservation(devisCree );
            log.info("✅ Devis validé automatiquement {} - montant {} TND - Réservation confirmée",
                    devisCree.getReferenceReservation(),montantTotal);
           return convertToResponseDto(resValide);
        }


        // 📋 MODE CLASSIQUE : Attente review admin
        reservation.setValidationAutomatique(false);
        // Définir date d'expiration
        reservation.setDateExpirationDevis(LocalDateTime.now().plusDays(2));

        Reservation devis = reserverTemporaireStockPourReservation(reservation);

        return convertToResponseDto(devis);
    }

    // ============ VÉRIFICATION DE DISPONIBILITÉ ============

    @Override
    public DisponibiliteResponseDto verifierDisponibilite(VerificationDisponibiliteDto verificationDto) {
        log.debug("🔍 Vérification disponibilité - Produit: {}, Quantité: {}, Période: {} -> {}",
                verificationDto.getIdProduit(), verificationDto.getQuantite(),
                verificationDto.getDateDebut(), verificationDto.getDateFin());

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
        double montantOriginal1 = reservation.getMontantTotal();
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
                .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
                .sum();

        // 3. Appliquer les remises
        double montantFinal = montantOriginal2;

        if (modificationDto.getRemisePourcentage() != null && modificationDto.getRemisePourcentage() > 0) {
            double remise = montantOriginal2 * (modificationDto.getRemisePourcentage() / 100.0);
            montantFinal -= remise;
            log.info("💸 Remise de {}%: -{} TND", modificationDto.getRemisePourcentage(), remise);
        }

        if (modificationDto.getRemiseMontant() != null && modificationDto.getRemiseMontant() > 0) {
            montantFinal -= modificationDto.getRemiseMontant();
            log.info("💸 Remise fixe: -{} TND", modificationDto.getRemiseMontant());
        }

        // S'assurer que le montant ne soit pas négatif
        if (montantFinal < 0) {
            montantFinal = 0.0;
        }

        reservation.setMontantTotal(montantFinal);
        reservation.setCommentaireAdmin(modificationDto.getCommentaireAdmin());
        reservation.setDateExpirationDevis(LocalDateTime.now().plusDays(3));
        reservation.setValidationAutomatique(true);


        reservationRepo.save(reservation);

        log.info("✅ Devis modifié - Montant original: {} TND, Montant final: {} TND",
                montantOriginal1, montantFinal);


        return buildToResponseDto(reservation,montantOriginal1,modificationDto.getRemiseMontant(),
                modificationDto.getRemisePourcentage());


    }

    // ============ VALIDATION DU DEVIS PAR LE CLIENT ============

    @Override
    public ReservationResponseDto validerDevisParClient(ValidationDevisDto validationDto, String username) {
        log.info("🎯 Validation du devis ID: {} par le client {} - Accepté: {}",
                validationDto.getIdReservation(), username, validationDto.getAccepter());

        Reservation reservation = reservationRepo.findById(validationDto.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        if(!reservation.isValidationAutomatique()){
            throw new CustomException("Veuillez patienter la validation Administration");
        }

        if (!validationDto.getAccepter()) {
            // Client refuse le devis
            reservation.setStatutReservation(StatutReservation.ANNULE);
            reservationRepo.save(reservation);
            return convertToResponseDto(reservation);
        }


        // Vérifier que c'est bien un devis en attente
        if (reservation.getStatutReservation() != StatutReservation.EN_ATTENTE) {
            throw new CustomException("Seuls les devis en attente peuvent être validés");
        }


        reservation.setDateExpirationDevis(null);
        Reservation resValideCLient = reserverStockPourReservation(reservation);



        return convertToResponseDto(resValideCLient);
    }

    // ============ ANNULATION ============

    @Override
    public void annulerReservationParClient(Long idReservation, String motif, String username) {
        log.info("❌ Annulation de la réservation ID: {} par le client {} - Motif: {}",
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

        Reservation reservationlibere = new Reservation(); ;
        // Libérer les instances si c'était confirmé
        if (reservation.getStatutReservation() == StatutReservation.CONFIRME) {

            reservationlibere = libererStockReservation(reservation);


        }

        reservationlibere.setStatutReservation(StatutReservation.ANNULE);
        reservationlibere.setCommentaireClient(motif);
        reservationRepo.save(reservationlibere);

        log.info("✅ Réservation annulée avec succès");
    }

    @Override
    public void annulerDevisParAdmin(Long idReservation, String motif, String username) {
        // Même logique que l'annulation par le client
        annulerReservationParClient(idReservation, motif, username);
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
    public List<ReservationResponseDto> getReservationsByPeriode(Date dateDebut, Date dateFin) {
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

    // ============ MODIFICATION ============
    /**
     * Modifier les dates d'une réservation existante
     * ⚠️ RÈGLES MÉTIER :
     * - Vérifier que la réservation n'est pas déjà livrée
     * - Vérifier la disponibilité des produits pour les nouvelles dates
     * - Mettre à jour toutes les lignes de réservation
     * - Enregistrer l'historique
     */
    @Override
    public ReservationResponseDto modifierDatesReservation(
            Long idReservation, LocalDate nouvelleDateDebut, LocalDate nouvelleDateFin, String username) {

        log.info("📅 Modification des dates pour la réservation ID: {} par {}", idReservation, username);

        // 1️⃣ VALIDATION - Récupérer la réservation
        Reservation reservation = reservationRepo.findById(idReservation)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Réservation avec ID " + idReservation + " introuvable"));

        // 2️⃣ VÉRIFICATIONS DES RÈGLES MÉTIER

        // Vérifier que les nouvelles dates sont cohérentes
        if (nouvelleDateDebut.isAfter(nouvelleDateFin)) {
            throw new ReservationException("La date de début ne peut pas être après la date de fin");
        }

        // Vérifier que la date de début n'est pas dans le passé
        if (nouvelleDateDebut.isBefore(LocalDate.now())) {
            throw new ReservationException("La date de début ne peut pas être dans le passé");
        }

        // Vérifier que la réservation peut encore être modifiée
        if (reservation.getStatutReservation() == StatutReservation.ANNULE) {
            throw new ReservationException("Impossible de modifier une réservation annulée");
        }

        if (reservation.getStatutLivraisonRes() == StatutLivraison.LIVREE) {
            throw new ReservationException("Impossible de modifier une réservation déjà livrée");
        }

        // 3️⃣ VÉRIFIER LA DISPONIBILITÉ POUR LES NOUVELLES DATES
        log.info("🔍 Vérification de la disponibilité pour les nouvelles dates...");

        for (LigneReservation ligne : reservation.getLigneReservations()) {
            Produit produit = ligne.getProduit();

            if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
                // Vérifier disponibilité pour produits quantitatifs
                int quantiteDisponible = verifierDisponibiliteQuantitative(
                        produit.getIdProduit(),
                        nouvelleDateDebut,
                        nouvelleDateFin,
                        idReservation  // Exclure cette réservation du calcul
                );

                if (quantiteDisponible < ligne.getQuantite()) {
                    throw new ReservationException(
                            String.format("Le produit '%s' n'est pas disponible en quantité suffisante " +
                                            "pour les nouvelles dates. Disponible: %d, Demandé: %d",
                                    produit.getNomProduit(), quantiteDisponible, ligne.getQuantite()));
                }

            } else if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
                // Vérifier disponibilité pour produits avec référence
                for (InstanceProduit instance : ligne.getInstancesReservees()) {
                    boolean estDisponible = verifierDisponibiliteInstance(
                            instance.getIdInstance(),
                            nouvelleDateDebut,
                            nouvelleDateFin,
                            idReservation
                    );

                    if (!estDisponible) {
                        throw new ReservationException(
                                String.format("L'instance '%s' du produit '%s' n'est pas disponible " +
                                                "pour les nouvelles dates",
                                        instance.getNumeroSerie(), produit.getNomProduit()));
                    }
                }
            }
        }

        // 4️⃣ SAUVEGARDER LES ANCIENNES DATES (pour historique)
        LocalDate ancienneDateDebut = reservation.getDateDebut();
        LocalDate ancienneDateFin = reservation.getDateFin();

        // 5️⃣ METTRE À JOUR LES DATES DE LA RÉSERVATION
        reservation.setDateDebut(nouvelleDateDebut);
        reservation.setDateFin(nouvelleDateFin);

        // Mettre à jour toutes les lignes de réservation
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            ligne.setDateDebut(nouvelleDateDebut);
            ligne.setDateFin(nouvelleDateFin);
            ligneReservationRepo.save(ligne);
        }

        // 6️⃣ ENREGISTRER LA MODIFICATION
        reservationRepo.save(reservation);

        // 7️⃣ AJOUTER UN COMMENTAIRE D'HISTORIQUE
        String commentaire = String.format(
                "Dates modifiées par %s - Anciennes dates: %s au %s - Nouvelles dates: %s au %s",
                username,
                ancienneDateDebut,
                ancienneDateFin,
                nouvelleDateDebut,
                nouvelleDateFin
        );

        reservation.setCommentaireAdmin(
                (reservation.getCommentaireAdmin() != null ? reservation.getCommentaireAdmin() + "\n" : "")
                        + commentaire
        );

        reservationRepo.save(reservation);

        log.info("✅ Dates modifiées avec succès pour la réservation {}", reservation.getReferenceReservation());
        log.info("   Anciennes dates: {} au {}", ancienneDateDebut, ancienneDateFin);
        log.info("   Nouvelles dates: {} au {}", nouvelleDateDebut, nouvelleDateFin);

        // 8️⃣ RETOURNER LA RÉSERVATION MISE À JOUR
        return convertToResponseDto(reservation);
    }

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
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .statutReservation(reservation.getStatutReservation())
                .statutLivraisonRes(reservation.getStatutLivraisonRes())
                .montantTotal(reservation.getMontantTotal())
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
     * Convertir une entité Reservation en DTO de réponse
     */
    private ReservationResponseDto buildToResponseDto(Reservation reservation,Double montantOriginal2,
                                                      Double remiseMontant,Double remisePourcentage) {
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
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .statutReservation(reservation.getStatutReservation())
                .statutLivraisonRes(reservation.getStatutLivraisonRes())
                .montantTotal(reservation.getMontantTotal())
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
                .montantOriginal(montantOriginal2)
                .remiseMontant(remiseMontant)
                .remisePourcentage(remisePourcentage)
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

                log.info("{} instances affectées à la ligne {} pour la période {}-{}",
                        ligne.getQuantite(),
                        ligne.getIdLigneReservation(),
                        ligne.getDateDebut(),
                        ligne.getDateFin());
            }else{
                int quantiteDisponible = ligneReservationRepo.calculateQuantiteReserveeSurPeriode(
                        ligne.getProduit().getIdProduit(),
                        ligne.getDateDebut(),
                        ligne.getDateFin()
                );
                if(quantiteDisponible < ligne.getQuantite()){
                    throw new ProduitException(
                            "Stock insuffisant pour " + ligne.getProduit().getNomProduit() +
                                    " du " + ligne.getDateDebut() + " au " + ligne.getDateFin()
                    );
                }
                ligneReservationRepo.save(ligne);
            }
        }
        // Confirmer la réservation
        reservation.setStatutReservation(StatutReservation.CONFIRME);

        reservation.setStockReserve(Boolean.TRUE);

        log.info("🎉 Réservation confirmée avec succès: {}", reservation.getReferenceReservation());

        reservationRepo.save(reservation);
        return reservation ;
    }

    /**
     * Méthode utilitaire pour reserver le stock une fois le client valide le devis
     */
    public Reservation reserverTemporaireStockPourReservation(Reservation reservation){
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

                log.info("{} instances affectées Temporairement à la ligne {} pour la période {}-{}",
                        ligne.getQuantite(),
                        ligne.getIdLigneReservation(),
                        ligne.getDateDebut(),
                        ligne.getDateFin());
            }else{
                int quantiteDisponible = produitRepo.calculerQuantiteDisponibleSurPeriode(
                        ligne.getProduit().getIdProduit(),
                        ligne.getDateDebut(),
                        ligne.getDateFin()
                );
                if(quantiteDisponible < ligne.getQuantite()){
                    throw new ProduitException(
                            "Stock insuffisant pour " + ligne.getProduit().getNomProduit() +
                                    " du " + ligne.getDateDebut() + " au " + ligne.getDateFin()
                    );
                }
                ligneReservationRepo.save(ligne);
            }
        }
        // Confirmer la réservation
        reservation.setStatutReservation(StatutReservation.EN_ATTENTE);

        reservation.setStockReserve(Boolean.TRUE);

        log.info("🎉 Réservation Temporaire Crée avec succès: {}", reservation.getReferenceReservation());

        reservationRepo.save(reservation);
        return reservation ;
    }

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

                // Libérer le stock
                libererStockReservation(devis);

                // Changer le statut
                devis.setStatutReservation(StatutReservation.ANNULE);
                devis.setCommentaireAdmin(
                        "Devis annulé automatiquement après expiration (" +
                                devis.getDateExpirationDevis().toLocalDate() + ")"
                );
                reservationRepo.save(devis);

                // TODO: Envoyer notification email au client
                // notificationService.envoyerNotificationDevisExpire(devis);

            } catch (Exception e) {
                log.error("❌ Erreur lors de l'annulation du devis {}: {}",
                        devis.getReferenceReservation(), e.getMessage());
            }
        }

        log.info("✅ Job terminé : {} devis annulés", devisExpires.size());
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


            }

            if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE
                        && ligne.getStatutLivraisonLigne() != StatutLivraison.NOT_TODAY) {
                    // Remettre la quantité disponible
                    int nouvelleQte = produit.getQuantiteDisponible() + ligne.getQuantite();
                    produit.setQuantiteDisponible(nouvelleQte);
                    produitRepo.save(produit);

                enregistrerMouvementStock(
                        produit,
                        ligne.getQuantite(),
                        TypeMouvement.ANNULATION_RESERVATION,
                        reservation,
                        "Libération du stock - Annulation/Expiration",
                        "SYSTEM"
                );

            }
                reservation.setStockReserve(Boolean.FALSE);
                reservationRepo.save(reservation);

                log.info("✅ Stock libéré avec succès");
        }
        return reservation;
    }

    /**
     * Méthode utilitaire pour convertir java.sql.Date en LocalDate de manière sécurisée
     */
    private LocalDate convertToLocalDate(Date date) {
        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Date) {
            // Conversion directe pour java.sql.Date
            return ((java.sql.Date) date).toLocalDate();
        } else {
            // Conversion pour java.util.Date
            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
    }
}