package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.reservation.*;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDate;
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
        reservation.setStatutLivraisonRes(StatutLivraison.EN_ATTENTE);
        reservation.setMontantPaye(0.0);

        // Dates globales (du premier au dernier jour)
        Date dateDebutMin = devisRequest.getLignesReservation().stream()
                .map(LigneReservationRequestDto::getDateDebut)
                .min(Date::compareTo)
                .orElseThrow();
        Date dateFinMax = devisRequest.getLignesReservation().stream()
                .map(LigneReservationRequestDto::getDateFin)
                .max(Date::compareTo)
                .orElseThrow();

        reservation.setDateDebut(dateDebutMin);
        reservation.setDateFin(dateFinMax);

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
            ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
            ligne.setObservations(ligneDto.getObservations());


            double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire();
            montantTotal += sousTotal;

            lignes.add(ligne);
            log.info("📝 Ligne ajoutée: {} x {} = {} TND",
                    produit.getNomProduit(), ligne.getQuantite(), sousTotal);
        }

        reservation.setLigneReservations(lignes);
        reservation.setMontantTotal(montantTotal);

        // 6. Sauvegarder
        Reservation devisCree = reservationRepo.save(reservation);
        log.info("✅ Devis créé avec succès: {} - Montant: {} TND",
                devisCree.getReferenceReservation(), montantTotal);

        return convertToResponseDto(devisCree);
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
        if (produit.getTypeProduit() == TypeProduit.enQuantite) {
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

        // 4. Sauvegarder le commentaire de l'admin (via observations si besoin)
        // Note: Vous pouvez ajouter un champ "commentaireAdmin" à l'entité Reservation si nécessaire

        reservationRepo.save(reservation);

        log.info("✅ Devis modifié - Montant original: {} TND, Montant final: {} TND",
                montantOriginal1, montantFinal);

        return convertToResponseDto(reservation);
    }

    // ============ VALIDATION DU DEVIS PAR LE CLIENT ============

    @Override
    public ReservationResponseDto validerDevisParClient(ValidationDevisDto validationDto, String username) {
        log.info("🎯 Validation du devis ID: {} par le client {} - Accepté: {}",
                validationDto.getIdReservation(), username, validationDto.getAccepter());

        Reservation reservation = reservationRepo.findById(validationDto.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        // Vérifier que c'est bien un devis en attente
        if (reservation.getStatutReservation() != StatutReservation.EN_ATTENTE) {
            throw new CustomException("Seuls les devis en attente peuvent être validés");
        }

        if (validationDto.getAccepter()) {
            // ✅ CLIENT ACCEPTE LE DEVIS → CONFIRMER LA RÉSERVATION
            log.info("✅ Client accepte le devis - Confirmation de la réservation");

            // Revérifier la disponibilité avant de confirmer
            for (LigneReservation ligne : reservation.getLigneReservations()) {
                VerificationDisponibiliteDto verif = VerificationDisponibiliteDto.builder()
                        .idProduit(ligne.getProduit().getIdProduit())
                        .quantite(ligne.getQuantite())
                        .dateDebut(ligne.getDateDebut())
                        .dateFin(ligne.getDateFin())
                        .build();

                DisponibiliteResponseDto dispo = verifierDisponibilite(verif);
                if (!dispo.getDisponible()) {
                    throw new CustomException(
                            "Le produit '" + dispo.getNomProduit() + "' n'est plus disponible. " +
                                    "Veuillez créer un nouveau devis."
                    );
                }
            }

            // Passer au statut CONFIRMÉ
            reservation.setStatutReservation(StatutReservation.CONFIRME);
            reservation.setCommentaireClient(validationDto.getCommentaireClient());

            // Pour les produits AVEC RÉFÉRENCE: Réserver les instances spécifiques
            for (LigneReservation ligne : reservation.getLigneReservations()) {
                if (ligne.getProduit().getTypeProduit() == TypeProduit.avecReference) {
                    reserverInstances(ligne);
                }
            }

            // Enregistrer un mouvement de stock (sortie pour réservation)
            enregistrerMouvementStock(reservation, TypeMouvement.SORTIE_RESERVATION);

            log.info("🎉 Réservation confirmée avec succès: {}", reservation.getReferenceReservation());

        } else {
            // ❌ CLIENT REFUSE LE DEVIS → ANNULER
            log.info("❌ Client refuse le devis - Annulation");
            reservation.setStatutReservation(StatutReservation.ANNULE);
        }

        reservationRepo.save(reservation);

        return convertToResponseDto(reservation);
    }

    /**
     * Réserver des instances spécifiques pour une ligne de réservation
     * (pour produits avec référence uniquement)
     */
    private void reserverInstances(LigneReservation ligne) {
        Produit produit = ligne.getProduit();
        int quantiteRequise = ligne.getQuantite();

        log.info("🔒 Réservation de {} instances pour {}", quantiteRequise, produit.getNomProduit());

        // Récupérer les N premières instances disponibles
        List<InstanceProduit> instancesDispos = instanceProduitRepo.
                findInstancesDisponiblesSurPeriode(produit.getIdProduit(),ligne.getDateDebut(),ligne.getDateFin());

        if (instancesDispos.size() < quantiteRequise) {
            throw new CustomException(
                    "Pas assez d'instances disponibles pour " + produit.getNomProduit()
            );
        }

        Set<InstanceProduit> instancesReservees = new HashSet<>();
        for (int i = 0; i < quantiteRequise; i++) {
            InstanceProduit instance = instancesDispos.get(i);
            instance.setStatut(StatutInstance.RESERVE);
            instance.setIdLigneReservation(ligne.getIdLigneReservation());
            instanceProduitRepo.save(instance);
            instancesReservees.add(instance);

            log.debug("🔒 Instance réservée: {}", instance.getNumeroSerie());
        }

        ligne.setInstancesReservees(instancesReservees);
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

        // Libérer les instances si c'était confirmé
        if (reservation.getStatutReservation() == StatutReservation.CONFIRME) {
            libererInstancesReservation(reservation);
            enregistrerMouvementStock(reservation, TypeMouvement.ANNULATION_RESERVATION);
        }

        reservation.setStatutReservation(StatutReservation.ANNULE);
        reservationRepo.save(reservation);

        log.info("✅ Réservation annulée avec succès");
    }

    @Override
    public void annulerDevisParAdmin(Long idReservation, String motif, String username) {
        // Même logique que l'annulation par le client
        annulerReservationParClient(idReservation, motif, username);
    }

    /**
     * Libérer toutes les instances d'une réservation
     */
    private void libererInstancesReservation(Reservation reservation) {
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            if (ligne.getInstancesReservees() != null) {
                for (InstanceProduit instance : ligne.getInstancesReservees()) {
                    instance.setStatut(StatutInstance.DISPONIBLE);
                    instance.setIdLigneReservation(null);
                    instanceProduitRepo.save(instance);
                    log.debug("🔓 Instance libérée: {}", instance.getNumeroSerie());
                }
            }
        }
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

    @Override
    public ReservationResponseDto modifierDatesReservation(
            Long idReservation, Date nouvelleDateDebut, Date nouvelleDateFin, String username) {

        // TODO: Implémenter la modification des dates
        // 1. Vérifier la disponibilité pour les nouvelles dates
        // 2. Libérer les anciennes instances
        // 3. Réserver les nouvelles instances

        throw new UnsupportedOperationException("Fonctionnalité à implémenter");
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
    public ReservationSummaryDto getStatistiquesReservationsClient(Long idUtilisateur) {
        // TODO: Implémenter les stats par client
        throw new UnsupportedOperationException("Fonctionnalité à implémenter");
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
    private void enregistrerMouvementStock(Reservation reservation, TypeMouvement typeMouvement) {
        // TODO: Implémenter l'enregistrement des mouvements de stock
        log.info("📊 Mouvement de stock enregistré: {} pour réservation {}",
                typeMouvement, reservation.getReferenceReservation());
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
            LocalDate debut = convertToLocalDate(reservation.getDateDebut());
            LocalDate fin = convertToLocalDate(reservation.getDateFin());
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