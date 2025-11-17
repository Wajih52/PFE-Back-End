package tn.weeding.agenceevenementielle.services.Reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.produit.InstanceProduitResponseDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.ReservationException;
import tn.weeding.agenceevenementielle.repository.*;
import tn.weeding.agenceevenementielle.services.ProduitServiceInterface;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ==========================================
 * SERVICE POUR LA GESTION DES LIGNES DE RÉSERVATION
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 * Responsabilités :
 * - CRUD des lignes de réservation (produits dans le panier)
 * - Vérification de disponibilité AVANT création
 * - Affectation automatique des instances (produits avec référence)
 * - Gestion du stock (décrémentation/incrémentation)
 * - Calcul des montants (sous-totaux)
 * - Modification des quantités et dates
 * - Suppression avec libération du stock
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LigneReservationServiceImpl implements LigneReservationServiceInterface {

    private final LigneReservationRepository ligneReservationRepo;
    private final ReservationRepository reservationRepo;
    private final ProduitRepository produitRepo;
    private final InstanceProduitRepository instanceProduitRepo;
    private final InstanceProduitServiceInterface instanceProduitService;
    private final InstanceProduitServiceImpl instanceProduitServiceImpl;
    private final MontantReservationCalculService montantCalculService ;
    private final ProduitServiceInterface produitService;
    private final MouvementStockRepository mouvementStockRepo;

    // ============================================
    // CRÉATION ET AJOUT DE LIGNES
    // ============================================

    /**
     * 📝 Créer une nouvelle ligne de réservation
     * Vérifie la disponibilité et affecte automatiquement les instances si nécessaire
     */
    @Override
    public LigneReservationResponseDto creerLigneReservation(LigneReservationRequestDto dto, Long idReservation, String username) {
        log.info("🛒 Ajout d'une ligne de réservation: Produit ID {} (Qté: {})", dto.getIdProduit(), dto.getQuantite());

        // Vérifier que la réservation existe
        Reservation reservation = reservationRepo.findById(idReservation)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Réservation avec ID " + idReservation + " introuvable"));

        // Vérifier que le produit existe
        Produit produit = produitRepo.findById(dto.getIdProduit())
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + dto.getIdProduit() + " introuvable"));

        if(reservation.getStatutReservation().equals(StatutReservation.EN_COURS)){
            throw new CustomException("Reservation Déjà en cours de Préparation , veuillez contacter l'administration");
        }

        // Vérifier la disponibilité
        verifierDisponibilite(produit, dto.getQuantite(), dto.getDateDebut(), dto.getDateFin());

        // Créer la ligne de réservation
        LigneReservation ligne = new LigneReservation();
        ligne.setReservation(reservation);
        ligne.setProduit(produit);
        ligne.setQuantite(dto.getQuantite());
        ligne.setPrixUnitaire(produit.getPrixUnitaire());
        ligne.setDateDebut(dto.getDateDebut());
        ligne.setDateFin(dto.getDateFin());
        ligne.setObservations(dto.getObservations());

        if(dto.getDateDebut().isEqual(LocalDate.now())) {
            ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
        }else{
            ligne.setStatutLivraisonLigne(StatutLivraison.NOT_TODAY);
        }

        // Sauvegarder la ligne
        ligne = ligneReservationRepo.save(ligne);
        log.info("✅ Ligne créée avec ID: {}", ligne.getIdLigneReservation());
        //  Recalculer le montant total
        double ancienMontant = reservation.getMontantTotal() != null ? reservation.getMontantTotal() : 0.0;
        double nouveauMontant = montantCalculService.recalculerEtMettreAJourMontantTotal(reservation);
        reservationRepo.save(reservation);

        log.info("💰 Montant recalculé aprés Ajout nouvelle ligne: {}DT → {}DT (différence: {}DT)",
                ancienMontant, nouveauMontant, nouveauMontant - ancienMontant);


        // Gérer le stock selon le type de produit
        if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
            // Affecter automatiquement les instances disponibles
            affecterInstancesAutomatiquement(ligne, produit, dto.getQuantite(), username);

            // decrement le stock que lorsque la réservation est aujourd'hui
        } else if(reservation.getStatutReservation().equals(StatutReservation.EN_ATTENTE)){
            // Décrémenter le stock pour les produits quantitatifs
            produit.setQuantiteDisponible(produit.getQuantiteDisponible() - dto.getQuantite());
            produitRepo.save(produit);
            log.info("📉 Stock décrémenté: {} -> {} (Produit: {})",
                    produit.getQuantiteDisponible() + dto.getQuantite(),
                    produit.getQuantiteDisponible(),
                    produit.getNomProduit());
        }

        // ✅  Enregistrer mouvement stock
        if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
            // Pour produits avec référence: un mouvement par instance
            for (InstanceProduit instance : ligne.getInstancesReservees()) {
                enregistrerMouvementInstance(
                        instance,
                        TypeMouvement.RESERVATION,
                        "Ajout ligne à réservation " + reservation.getReferenceReservation() +
                                " (produit: " + produit.getNomProduit() + ")",
                        username,
                        reservation
                );
            }
        } else if(reservation.getStatutReservation().equals(StatutReservation.EN_ATTENTE)){
            // Pour produits quantitatifs: un seul mouvement
            enregistrerMouvementStock(
                    produit,
                    dto.getQuantite(),
                    TypeMouvement.RESERVATION,
                    reservation,
                    "Ajout ligne à réservation " + reservation.getReferenceReservation() +
                            " (" + dto.getQuantite() + "x " + produit.getNomProduit() + ")",
                    username
            );
        }

        return toDto(ligne);
    }

    /**
     * 🔄 Affecter automatiquement les N premières instances disponibles
     */
    private void affecterInstancesAutomatiquement(LigneReservation ligne, Produit produit, Integer quantite, String username) {
        log.info("🔗 Affectation automatique de {} instances du produit ID: {}", quantite, produit.getIdProduit());

        try {
            // Utiliser le service InstanceProduit pour réserver les instances
            List<InstanceProduitResponseDto> instancesReservees = instanceProduitService.reserverInstances(
                    produit.getIdProduit(),
                    quantite,
                    ligne.getIdLigneReservation(),
                    ligne.getDateDebut(),
                    ligne.getDateFin(),
                    username
            );

            // Récupérer les entités pour les associer à la ligne
            List<Long> instanceIds = instancesReservees.stream()
                    .map(InstanceProduitResponseDto::getIdInstance)
                    .toList();
            List<InstanceProduit> instancesTrouvees = instanceProduitRepo.findAllById(instanceIds);

            if (instancesTrouvees.size() != instanceIds.size()) {
                throw new CustomException("Certaines instances réservées sont introuvables en base");
            }

            // Ajouter aux instances existantes (ne pas écraser)
            Set<InstanceProduit> instancesExistantes = ligne.getInstancesReservees();
            if (instancesExistantes == null) {
                instancesExistantes = new HashSet<>();
            }
            instancesExistantes.addAll(instancesTrouvees);
            ligne.setInstancesReservees(instancesExistantes);

            ligneReservationRepo.save(ligne);

            log.info("✅ {} instances affectées avec succès à la ligne {} (total: {})",
                    instancesTrouvees.size(),
                    ligne.getIdLigneReservation(),
                    instancesExistantes.size());

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'affectation des instances: {}", e.getMessage());
            throw new CustomException("Impossible d'affecter les instances: " + e.getMessage());
        }
    }

    // ============================================
    // CONSULTATION DES LIGNES
    // ============================================

    /**
     * 📋 Récupérer une ligne par son ID
     */
    @Override
    public LigneReservationResponseDto getLigneReservationById(Long id) {
        log.info("🔍 Recherche de la ligne de réservation ID: {}", id);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));

        return toDto(ligne);
    }

    /**
     * 📋 Récupérer toutes les lignes d'une réservation
     */
    @Override
    public List<LigneReservationResponseDto> getLignesReservationByReservation(Long idReservation) {
        log.info("📋 Récupération des lignes de la réservation ID: {}", idReservation);

        // Vérifier que la réservation existe
        if (!reservationRepo.existsById(idReservation)) {
            throw new ReservationException.ReservationNotFoundException(
                    "Réservation avec ID " + idReservation + " introuvable");
        }

        List<LigneReservation> lignes = ligneReservationRepo.findByReservation_IdReservation(idReservation);

        return lignes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 📋 Récupérer les lignes contenant un produit spécifique
     */
    @Override
    public List<LigneReservationResponseDto> getLignesReservationByProduit(Long idProduit) {
        log.info("📋 Recherche des lignes contenant le produit ID: {}", idProduit);

        List<LigneReservation> lignes = ligneReservationRepo.findByProduit_IdProduit(idProduit);

        return lignes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 📋 Récupérer les lignes par statut de livraison
     */
    @Override
    public List<LigneReservationResponseDto> getLignesReservationByStatut(StatutLivraison statut) {
        log.info("📋 Recherche des lignes avec statut: {}", statut);

        List<LigneReservation> lignes = ligneReservationRepo.findByStatutLivraisonLigne(statut);

        return lignes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // MODIFICATION DES LIGNES
    // ============================================

    /**
     * ✏️ Modifier une ligne de réservation
     * Permet de changer la quantité, les dates ou les observations
     */
    @Override
    public LigneReservationResponseDto modifierLigneReservation(Long id, LigneReservationRequestDto dto, String username) {
        log.info("✏️ Modification de la ligne de réservation ID: {}", id);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));


        Reservation reservation = ligne.getReservation();
        Produit produit = ligne.getProduit();


        Integer ancienneQuantite = ligne.getQuantite();
        Integer nouvelleQuantite = dto.getQuantite();



        // 🎯 La réservation a-t-elle déjà commencé ?
        boolean reservationCommencee = ligne.getStatutLivraisonLigne().equals(StatutLivraison.EN_ATTENTE)
                || ligne.getDateDebut().isEqual(LocalDate.now());

        if (reservation.getStatutReservation() == StatutReservation.EN_COURS) {
            throw new CustomException(
                    "Impossible de modifier une ligne d'une réservation en cours. " +
                            "Veuillez contacter l'administration."
            );
        }
        // Si la quantité change, gérer le stock
        if (!ancienneQuantite.equals(nouvelleQuantite)) {
            log.info("🔄 Changement de quantité: {} -> {}", ancienneQuantite, nouvelleQuantite);

            if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
                // Gérer les instances
                gererChangementQuantiteAvecInstances(ligne, ancienneQuantite, nouvelleQuantite, username);
            } else {
                // Gérer le stock quantitatif
                int difference = nouvelleQuantite - ancienneQuantite;

                if (difference > 0) {
                    // Augmentation: vérifier la disponibilité
                    verifierDisponibilite(produit, difference, dto.getDateDebut(), dto.getDateFin());
                    if(reservationCommencee) {
                        produit.setQuantiteDisponible(produit.getQuantiteDisponible() - difference);
                    }
                } else {

                    if(reservationCommencee) {
                        // Diminution: libérer le stock
                        produit.setQuantiteDisponible(produit.getQuantiteDisponible() + Math.abs(difference));
                    }
                }
                produitRepo.save(produit);
            }
        }

        // Mettre à jour les autres champs
        ligne.setQuantite(nouvelleQuantite);
        ligne.setDateDebut(dto.getDateDebut());
        ligne.setDateFin(dto.getDateFin());
        ligne.setObservations(dto.getObservations());

        ligne = ligneReservationRepo.save(ligne);
        log.info("✅ Ligne modifiée avec succès");
        //  Recalculer le montant total
        double ancienMontant = reservation.getMontantTotal() != null ? reservation.getMontantTotal() : 0.0;
        double nouveauMontant = montantCalculService.recalculerEtMettreAJourMontantTotal(reservation);
        reservationRepo.save(reservation);

        log.info("💰 Montant recalculé aprés modification: {}DT → {}DT (différence: {}DT)",
                ancienMontant, nouveauMontant, nouveauMontant - ancienMontant);
        return toDto(ligne);
    }

    /**
     * 🔄 Gérer le changement de quantité pour les produits avec référence
     */
    private void gererChangementQuantiteAvecInstances(LigneReservation ligne, Integer ancienneQte, Integer nouvelleQte, String username) {
        int difference = nouvelleQte - ancienneQte;

        if (difference > 0) {
            // Augmentation: affecter plus d'instances
            log.info("➕ Affectation de {} instances supplémentaires", difference);
            affecterInstancesAutomatiquement(ligne, ligne.getProduit(), difference, username);

        } else if (difference < 0) {
            // Diminution: libérer des instances
            int nombreALiberer = Math.abs(difference);
            log.info("➖ Libération de {} instances", nombreALiberer);

            List<InstanceProduit> instancesActuelles = new ArrayList<>(ligne.getInstancesReservees());

            // Libérer en priorité les instances nécessitant une maintenance prochaine
            List<InstanceProduit> instancesALiberer = instancesActuelles.stream()
                    .sorted(Comparator.comparing(
                            InstanceProduit::getDateProchaineMaintenance,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ))
                    .limit(nombreALiberer)
                    .toList();

            // Retirer les instances libérées
            instancesActuelles.removeAll(instancesALiberer);
            ligne.setInstancesReservees(new HashSet<>(instancesActuelles));
        }
    }

    /**
     * 🔄 Mettre à jour le statut de livraison d'une ligne
     */
    @Override
    public LigneReservationResponseDto updateStatutLivraison(Long id, StatutLivraison nouveauStatut,String username) {
        log.info("🔄 Changement de statut de livraison pour la ligne ID: {} -> {}", id, nouveauStatut);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));



        // Si passage à EN_LIVRAISON, mettre à jour le statut des instances
        if (nouveauStatut == StatutLivraison.EN_COURS && ligne.getInstancesReservees() != null) {
            for (InstanceProduit instance : ligne.getInstancesReservees()) {
                instance.setStatut(StatutInstance.EN_LIVRAISON);
                instanceProduitRepo.save(instance);
            }
            log.info("📦 {} instances passées en EN_LIVRAISON", ligne.getInstancesReservees().size());
        }

        // Si passage à LIVRE, mettre à jour le statut des instances
        if (nouveauStatut == StatutLivraison.LIVREE && ligne.getInstancesReservees() != null) {
            for (InstanceProduit instance : ligne.getInstancesReservees()) {
                instance.setStatut(StatutInstance.EN_UTILISATION);
                instanceProduitRepo.save(instance);
            }
            log.info("✅ {} instances passées en EN_UTILISATION", ligne.getInstancesReservees().size());
        }

// ✅ Tracer TOUS les changements de statut
        switch (nouveauStatut) {
            case EN_ATTENTE:
                // Pas de mouvement stock nécessaire (juste statut logistique)
                log.info("📦 Ligne en préparation");
                break;

            case EN_COURS:
                log.info("🚚 Livraison en cours vers le client");

                if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
                    // Instances passent en EN_LIVRAISON
                    if (ligne.getInstancesReservees() != null) {
                        for (InstanceProduit instance : ligne.getInstancesReservees()) {
                            instance.setStatut(StatutInstance.EN_LIVRAISON);
                            instanceProduitRepo.save(instance);

                            // ✅ AJOUT: Enregistrer mouvement LIVRAISON
                            enregistrerMouvementInstance(
                                    instance,
                                    TypeMouvement.LIVRAISON,
                                    "Livraison physique vers client - Réservation " +
                                            ligne.getReservation().getReferenceReservation() +
                                            (ligne.getLivraison() != null ?
                                                    " - Livraison " + ligne.getLivraison().getIdLivraison() : ""),
                                    username,
                                    ligne.getReservation()
                            );
                        }
                        log.info("✅ {} instances passées EN_LIVRAISON",
                                ligne.getInstancesReservees().size());
                    }
                } else {
                    // Pour produits quantitatifs: mouvement informatif
                    enregistrerMouvementStock(
                            ligne.getProduit(),
                            ligne.getQuantite(),
                            TypeMouvement.LIVRAISON,
                            ligne.getReservation(),
                            "Livraison physique de " + ligne.getQuantite() + "x " +
                                    ligne.getProduit().getNomProduit() + " vers client - Réservation " +
                                    ligne.getReservation().getReferenceReservation(),
                            username
                    );
                }
                break;

            case LIVREE:
                log.info("✅ Ligne livrée et en utilisation chez le client");

                if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
                    // Instances passent en EN_UTILISATION
                    if (ligne.getInstancesReservees() != null) {
                        for (InstanceProduit instance : ligne.getInstancesReservees()) {
                            instance.setStatut(StatutInstance.EN_UTILISATION);
                            instanceProduitRepo.save(instance);
                        }
                        log.info("✅ {} instances EN_UTILISATION chez client",
                                ligne.getInstancesReservees().size());
                    }
                }
                break;

            case RETOUR:
                log.info("🔄 Début du retour depuis le client");

                if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
                    // Instances passent en EN_RETOUR
                    if (ligne.getInstancesReservees() != null) {
                        for (InstanceProduit instance : ligne.getInstancesReservees()) {
                            instance.setStatut(StatutInstance.EN_RETOUR);
                            instanceProduitRepo.save(instance);

                            // ✅ AJOUT: Enregistrer début du retour
                            enregistrerMouvementInstance(
                                    instance,
                                    TypeMouvement.RETOUR,
                                    "Retour en cours depuis client - Réservation " +
                                            ligne.getReservation().getReferenceReservation(),
                                    username,
                                    ligne.getReservation()
                            );
                        }
                        log.info("✅ {} instances EN_RETOUR",
                                ligne.getInstancesReservees().size());
                    }
                } else {
                    // Pour produits quantitatifs
                    enregistrerMouvementStock(
                            ligne.getProduit(),
                            ligne.getQuantite(),
                            TypeMouvement.RETOUR,
                            ligne.getReservation(),
                            "Retour en cours de " + ligne.getQuantite() + "x " +
                                    ligne.getProduit().getNomProduit() + " depuis client - Réservation " +
                                    ligne.getReservation().getReferenceReservation(),
                            username
                    );
                }
                break;

            case RETOURNEE:
                log.info("✅ Retour validé, stock réintégré");

                if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
                    // Instances redeviennent DISPONIBLES
                    if (ligne.getInstancesReservees() != null) {
                        for (InstanceProduit instance : ligne.getInstancesReservees()) {
                            instance.setStatut(StatutInstance.DISPONIBLE);
                            instanceProduitRepo.save(instance);

                            // ✅ AJOUT: Enregistrer fin du retour
                            enregistrerMouvementInstance(
                                    instance,
                                    TypeMouvement.RETOUR_RESERVATION,
                                    "Retour validé, instance disponible - Réservation " +
                                            ligne.getReservation().getReferenceReservation(),
                                    username,
                                    ligne.getReservation()
                            );
                        }

                        // Libérer les instances de la ligne
                        ligne.getInstancesReservees().clear();

                        log.info("✅ {} instances libérées et DISPONIBLES",
                                ligne.getQuantite());
                    }
                } else {
                    // Réintégrer le stock
                    Integer quantiteAvant = ligne.getProduit().getQuantiteDisponible();
                    Integer quantiteApres = quantiteAvant + ligne.getQuantite();

                    ligne.getProduit().setQuantiteDisponible(quantiteApres);
                    produitRepo.save(ligne.getProduit());

                    // ✅ AJOUT: Enregistrer réintégration stock
                    enregistrerMouvementStock(
                            ligne.getProduit(),
                            ligne.getQuantite(),
                            TypeMouvement.RETOUR_RESERVATION,
                            ligne.getReservation(),
                            "Retour validé, stock réintégré (" + quantiteAvant + "→" +
                                    quantiteApres + ") - Réservation " +
                                    ligne.getReservation().getReferenceReservation(),
                            username
                    );

                    log.info("✅ Stock réintégré: {} → {} (+{})",
                            quantiteAvant, quantiteApres, ligne.getQuantite());
                }
                break;

            default:
                log.warn("⚠️ Statut non géré: {}", nouveauStatut);
        }

        ligne.setStatutLivraisonLigne(nouveauStatut);
        ligne = ligneReservationRepo.save(ligne);

        return toDto(ligne);
    }

    // ============================================
    // SUPPRESSION DES LIGNES
    // ============================================

    /**
     * ❌ Supprimer une ligne de réservation
     * Libère automatiquement le stock et les instances
     */
    @Override
    public void supprimerLigneReservation(Long id, String username) {
        log.info("🗑️ Suppression de la ligne de réservation ID: {}", id);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));
        Reservation reservation = ligne.getReservation();
        Produit produit = ligne.getProduit();
        LocalDate dateActuelle = LocalDate.now();

        // 🎯 VÉRIFICATION CRITIQUE : La réservation a-t-elle déjà commencé ?
        boolean reservationCommencee = ligne.getDateDebut().isBefore(dateActuelle)
                || ligne.getDateDebut().isEqual(dateActuelle);

        if (reservation.getStatutReservation() == StatutReservation.EN_COURS) {
            throw new CustomException(
                    "Impossible de supprimer une ligne d'une réservation en cours. "
            );
        }

        if (reservation.getStatutReservation() == StatutReservation.EN_ATTENTE) {
            log.warn("⚠️ Suppression d'une ligne ACTIVE (dateDebut: {}, aujourd'hui: {})",
                    ligne.getDateDebut(), dateActuelle);

            // 1️⃣ Libérer le stock/instances CAR ils sont déjà décrémentés
            if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
                // Libérer le stock
                produit.setQuantiteDisponible(produit.getQuantiteDisponible() + ligne.getQuantite());
                produitRepo.save(produit);
                log.info("📦 Stock libéré: +{} pour {} (nouveau stock: {})",
                        ligne.getQuantite(),
                        produit.getNomProduit(),
                        produit.getQuantiteDisponible());
            } else {
                // Libérer les instances
                if (ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {
                    for (InstanceProduit instance : ligne.getInstancesReservees()) {
                        instance.setStatut(StatutInstance.DISPONIBLE);
                        instanceProduitRepo.save(instance);
                        produit.setQuantiteDisponible(produit.getQuantiteDisponible()-1);
                    }
                    log.info("🔓 {} instances libérées", ligne.getInstancesReservees().size());
                    produitRepo.save(produit);
                }
            }
        } else {
            log.info("ℹ️ Suppression d'une ligne FUTURE (dateDebut: {}, aujourd'hui: {})",
                    ligne.getDateDebut(), dateActuelle);
            log.info("✅ Stock/instances PAS touchés car la réservation n'a pas encore commencé");
            // Pas de libération, car le stock n'a jamais été décrémenté
        }



        // Supprimer la ligne
        ligneReservationRepo.delete(ligne);
        log.info("✅ Ligne supprimée avec succès");

        // 3️⃣ Recalculer les dates de la réservation
        List<LigneReservation> lignesRestantes = ligneReservationRepo
                .findByReservation_IdReservation(reservation.getIdReservation());

        if (!lignesRestantes.isEmpty()) {
            LocalDate minDebut = lignesRestantes.stream()
                    .map(LigneReservation::getDateDebut)
                    .min(Comparator.naturalOrder())
                    .orElse(reservation.getDateDebut());

            LocalDate maxFin = lignesRestantes.stream()
                    .map(LigneReservation::getDateFin)
                    .max(Comparator.naturalOrder())
                    .orElse(reservation.getDateFin());

            reservation.setDateDebut(minDebut);
            reservation.setDateFin(maxFin);
            log.info("📅 Dates réservation recalculées: {} → {}", minDebut, maxFin);
        } else {
            log.warn("⚠️ Plus aucune ligne dans la réservation {}",
                    reservation.getReferenceReservation());
        }

        // 4️⃣ Recalculer le montant total
        double ancienMontant = reservation.getMontantTotal() != null ? reservation.getMontantTotal() : 0.0;
        double nouveauMontant = montantCalculService.recalculerEtMettreAJourMontantTotal(reservation);
        reservationRepo.save(reservation);

        log.info("💰 Montant recalculé: {}DT → {}DT (différence: {}DT)",
                ancienMontant, nouveauMontant, nouveauMontant - ancienMontant);

        log.info("✅ Ligne supprimée avec succès (Stock libéré: {})", reservationCommencee);

        // ✅ AJOUT: Enregistrer mouvement avant suppression
        if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
            enregistrerMouvementStock(
                    produit,
                    ligne.getQuantite(),
                    TypeMouvement.ANNULATION_RESERVATION,
                    reservation,
                    "Suppression ligne de réservation " + reservation.getReferenceReservation() +
                            " (" + ligne.getQuantite() + "x " + produit.getNomProduit() + ")",
                    username
            );
        } else {
            // Pour instances
            if (ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {
                for (InstanceProduit instance : ligne.getInstancesReservees()) {
                    enregistrerMouvementInstance(
                            instance,
                            TypeMouvement.ANNULATION_RESERVATION,
                            "Suppression ligne de réservation " + reservation.getReferenceReservation() +
                                    " (instance " + instance.getNumeroSerie() + ")",
                            username,
                            reservation
                    );
                }
            }
        }
    }

    // ============================================
    // PARTIE 5: STATISTIQUES ET CALCULS
    // ============================================

    /**
     * 💰 Calculer le montant total d'une réservation
     */
    @Override
    public Double calculerMontantTotalReservation(Long idReservation) {
        log.info("💰 Calcul du montant total pour la réservation ID: {}", idReservation);

        List<LigneReservation> lignes = ligneReservationRepo.findByReservation_IdReservation(idReservation);

        double montantTotal = lignes.stream()
                .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
                .sum();

        log.info("💵 Montant total calculé: {} TND", montantTotal);
        return montantTotal;
    }

    /**
     * 📊 Obtenir les statistiques d'une réservation
     */
    @Override
    public Map<String, Object> getStatistiquesReservation(Long idReservation) {
        log.info("📊 Récupération des statistiques de la réservation ID: {}", idReservation);

        List<LigneReservation> lignes = ligneReservationRepo.findByReservation_IdReservation(idReservation);

        Map<String, Object> stats = new HashMap<>();
        stats.put("nombreLignes", lignes.size());
        stats.put("nombreProduitsTotal", lignes.stream().mapToInt(LigneReservation::getQuantite).sum());
        stats.put("montantTotal", calculerMontantTotalReservation(idReservation));
        stats.put("produitsParCategorie", grouperParCategorie(lignes));

        return stats;
    }

    @Override
    public Set<InstanceProduitResponseDto> getInstancesLigneReservation(Long idLigneReservation) {
        LigneReservation ligne = ligneReservationRepo.findById(idLigneReservation).
                orElseThrow(() -> new CustomException("Ligne " + idLigneReservation + " n'existe pas "));

         Set<InstanceProduitResponseDto> instances = ligne.getInstancesReservees().stream()
                .map(instanceProduitServiceImpl::toDto)
                .collect(Collectors.toSet());
        return instances ;
    }

    /**
     * 📊 Grouper les produits par catégorie
     */
    private Map<String, Integer> grouperParCategorie(List<LigneReservation> lignes) {
        return lignes.stream()
                .collect(Collectors.groupingBy(
                        ligne -> ligne.getProduit().getCategorieProduit().toString(),
                        Collectors.summingInt(LigneReservation::getQuantite)
                ));
    }

    // ============================================
    // VÉRIFICATIONS ET VALIDATIONS
    // ============================================

    /**
     *  Vérifier la disponibilité d'un produit
     */
    private void verifierDisponibilite(Produit produit, Integer quantiteDemandee, LocalDate dateDebut, LocalDate dateFin) {
        log.info("🔍 Vérification de disponibilité pour {} unités de {}", quantiteDemandee, produit.getNomProduit());

        if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
            // Pour les produits avec référence, vérifier le nombre d'instances disponibles
            int instancesDisponibles =
                    instanceProduitRepo.countInstancesDisponiblesSurPeriode(produit.getIdProduit(),dateDebut,dateFin);

            if (instancesDisponibles < quantiteDemandee) {
                throw new CustomException(String.format(
                        " Instances insuffisantes pour %s. Demandé: %d de plus , Disponible: %d instances",
                        produit.getNomProduit(), quantiteDemandee, instancesDisponibles));
            }
        } else {
            // Pour les produits quantitatifs, vérifier le stock via le service
            Integer quantiteDisponible = produitService.calculerQuantiteDisponibleSurPeriode(
                    produit.getIdProduit(),
                    dateDebut,
                    dateFin
            );

            if (quantiteDisponible < quantiteDemandee) {
                throw new CustomException(String.format(
                        " Stock insuffisant pour %s. Demandé: %d de plus , Disponible: %d",
                        produit.getNomProduit(), quantiteDemandee, quantiteDisponible));
            }
        }

        log.info("✅ Disponibilité confirmée");
    }

    // ============================================
    //  CONVERSION DTO 🔄
    // ============================================

    /**
     *  Convertir une entité en DTO
     */
    private LigneReservationResponseDto toDto(LigneReservation ligne) {
        Produit produit = ligne.getProduit();

        LigneReservationResponseDto dto = LigneReservationResponseDto.builder()
                .idLigneReservation(ligne.getIdLigneReservation())
                .idProduit(produit.getIdProduit())
                .nomProduit(produit.getNomProduit())
                .codeProduit(produit.getCodeProduit())
                .imageProduit(produit.getImageProduit())
                .quantite(ligne.getQuantite())
                .prixUnitaire(ligne.getPrixUnitaire())
                .sousTotal(ligne.getQuantite() * ligne.getPrixUnitaire())
                .dateDebut(ligne.getDateDebut())
                .dateFin(ligne.getDateFin())
                .statutLivraisonLigne(ligne.getStatutLivraisonLigne())
                .observations(ligne.getObservations())
                .build();

        // Ajouter les instances si produit avec référence
        if (ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {
            List<String> numerosSeries = ligne.getInstancesReservees().stream()
                    .map(InstanceProduit::getNumeroSerie)
                    .collect(Collectors.toList());
            dto.setNumerosSeries(numerosSeries);
        }

        // Ajouter les infos de livraison si présente
        if (ligne.getLivraison() != null) {
            dto.setIdLivraison(ligne.getLivraison().getIdLivraison());
            dto.setTitreLivraison(ligne.getLivraison().getTitreLivraison());
        }

        return dto;
    }

    // ============================================
    // MÉTHODES PRIVÉES - MOUVEMENTS STOCK
    // ============================================

    /**
     * Enregistrer un mouvement de stock pour produits EN_QUANTITE
     */
    private void enregistrerMouvementStock(
            Produit produit,
            Integer quantite,
            TypeMouvement type,
            Reservation reservation,
            String motif,
            String username) {

        Integer quantiteAvant = null;
        Integer quantiteApres = null;

        if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
            quantiteAvant = produit.getQuantiteDisponible();

            if (type.isEntree()) {
                quantiteApres = quantiteAvant + quantite;
            } else if (type.isSortie()) {
                quantiteApres = quantiteAvant - quantite;
            } else {
                quantiteApres = quantiteAvant;
            }
        }

        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setTypeMouvement(type);
        mouvement.setQuantite(quantite);
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setMotif(motif);
        mouvement.setEffectuePar(username);
        mouvement.setDateMouvement(LocalDateTime.now());

        if (reservation != null) {
            mouvement.setReferenceReservation(reservation.getReferenceReservation());
            mouvement.setIdReservation(reservation.getIdReservation());
            mouvement.setDateDebut(reservation.getDateDebut());
            mouvement.setDateFin(reservation.getDateFin());
        }

        mouvementStockRepo.save(mouvement);

        log.debug("📦 Mouvement stock: {} - {}", type, motif);
    }

    /**
     * Enregistrer un mouvement pour une instance (produits AVEC_REFERENCE)
     */
    private void enregistrerMouvementInstance(
            InstanceProduit instance,
            TypeMouvement type,
            String motif,
            String username,
            Reservation reservation) {

        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(instance.getProduit());
        mouvement.setTypeMouvement(type);
        mouvement.setQuantite(1);
        mouvement.setCodeInstance(instance.getNumeroSerie());
        mouvement.setIdInstance(instance.getIdInstance());
        mouvement.setMotif(motif);
        mouvement.setEffectuePar(username);
        mouvement.setDateMouvement(LocalDateTime.now());

        if (reservation != null) {
            mouvement.setReferenceReservation(reservation.getReferenceReservation());
            mouvement.setIdReservation(reservation.getIdReservation());
        }

        mouvementStockRepo.save(mouvement);

        log.debug("📦 Mouvement instance: {} - {}",
                instance.getNumeroSerie(), type);
    }
}