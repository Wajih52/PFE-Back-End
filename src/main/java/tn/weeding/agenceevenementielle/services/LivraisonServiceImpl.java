package tn.weeding.agenceevenementielle.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.livraison.*;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LivraisonServiceImpl implements LivraisonServiceInterface {

    private final LivraisonRepository livraisonRepo;
    private final AffectationLivraisonRepository affectationRepo;
    private final LigneReservationRepository ligneReservationRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final UtilisateurRoleRepository utilisateurRoleRepo;
    private final ReservationRepository reservationRepo;
    private final InstanceProduitRepository instanceProduitRepo;
    private final MouvementStockRepository mouvementStockRepo;
    private final ProduitRepository produitRepo;
    private final NotificationServiceInterface notificationService;

    // ============================================
    // CRUD LIVRAISONS
    // ============================================

    @Override
    public LivraisonResponseDto creerLivraison(LivraisonRequestDto dto, String username) {
        log.info("🚚 Création d'une nouvelle livraison: {}", dto.getTitreLivraison());

        // Vérifier que les lignes de réservation existent
        List<LigneReservation> lignes = ligneReservationRepo.findAllById(dto.getIdLignesReservation());

        if (lignes.isEmpty()) {
            throw new CustomException("Aucune ligne de réservation trouvée");
        }

        if (lignes.size() != dto.getIdLignesReservation().size()) {
            throw new CustomException("Certaines lignes de réservation sont introuvables");
        }

        //  CONTRAINTE: Vérifier que toutes les lignes appartiennent à LA MÊME réservation
        Reservation reservation = lignes.get(0).getReservation();
        boolean toutesMemReservation = lignes.stream()
                .allMatch(ligne -> ligne.getReservation().getIdReservation().equals(reservation.getIdReservation()));

        if (!toutesMemReservation) {
            throw new CustomException(
                    "Toutes les lignes doivent appartenir à la même réservation. " +
                            "Une livraison ne peut concerner qu'une seule réservation."
            );
        }

        log.info("✅ Validation: Toutes les lignes appartiennent à la réservation {}",
                reservation.getReferenceReservation());

        // Vérifier que la réservation est confirmée
        if (reservation.getStatutReservation() != StatutReservation.CONFIRME) {
            throw new CustomException(
                    "La réservation " + reservation.getReferenceReservation() +
                            " n'est pas confirmée (statut: " + reservation.getStatutReservation() + ")"
            );
        }

        // Vérifier que les lignes ne sont pas déjà affectées à une autre livraison
        for (LigneReservation ligne : lignes) {
            if (ligne.getLivraison() != null) {
                throw new CustomException(
                        "La ligne ID " + ligne.getIdLigneReservation() +
                                " est déjà affectée à la livraison " + ligne.getLivraison().getIdLivraison()
                );
            }
        }

        // Créer la livraison
        Livraison livraison = new Livraison();
        livraison.setTitreLivraison(dto.getTitreLivraison());
        livraison.setAdresserLivraison(dto.getAdresseLivraison());
        livraison.setDateLivraison(dto.getDateLivraison());
        livraison.setHeureLivraison(dto.getHeureLivraison());
        livraison.setObservations(dto.getObservations());

        // Statut initial selon la date
        if (dto.getDateLivraison().equals(LocalDate.now())) {
            livraison.setStatutLivraison(StatutLivraison.EN_ATTENTE);
            log.info("📅 Date de livraison = aujourd'hui → Statut = EN_ATTENTE");
        } else {
            livraison.setStatutLivraison(StatutLivraison.NOT_TODAY);
            log.info("📅 Date de livraison = {} → Statut = NOT_TODAY", dto.getDateLivraison());
        }

        livraison.setAffectationLivraisons(new HashSet<>());
        livraison = livraisonRepo.save(livraison);

        // Associer les lignes de réservation à la livraison
        for (LigneReservation ligne : lignes) {
            ligne.setLivraison(livraison);

            // Mettre à jour le statut de la ligne selon la date
            if (dto.getDateLivraison().equals(LocalDate.now())) {
                ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);

                // Si produit avec référence, mettre les instances en EN_ATTENTE
                if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE
                        && ligne.getInstancesReservees() != null) {
                    for (InstanceProduit instance : ligne.getInstancesReservees()) {
                        instance.setStatut(StatutInstance.EN_ATTENTE);
                        instanceProduitRepo.save(instance);
                        log.info("📦 Instance {} → EN_ATTENTE", instance.getNumeroSerie());
                    }
                }
            } else {
                ligne.setStatutLivraisonLigne(StatutLivraison.NOT_TODAY);
            }

            ligneReservationRepo.save(ligne);
        }

        // Mettre à jour le statut de la réservation
        if (dto.getDateLivraison().equals(LocalDate.now())) {
            reservation.setStatutLivraisonRes(StatutLivraison.EN_ATTENTE);
        } else {
            reservation.setStatutLivraisonRes(StatutLivraison.NOT_TODAY);
        }
        reservationRepo.save(reservation);

        log.info("✅ Livraison créée avec succès - ID: {}, Réservation: {}, {} ligne(s)",
                livraison.getIdLivraison(),
                reservation.getReferenceReservation(),
                lignes.size());

        // Notifier tout le staff (ADMIN et MANAGER)
        notificationService.creerNotificationPourStaff(
                TypeNotification.LIVRAISON_A_EFFECTUER,
                "Nouvelle livraison créée",
                String.format("Une nouvelle livraison '%s' a été créée pour le %s à %s. Réservation: %s",
                        livraison.getTitreLivraison(),
                        livraison.getDateLivraison(),
                        livraison.getHeureLivraison(),
                        reservation.getReferenceReservation()),
                reservation.getIdReservation(),
                "/admin/livraisons/" + livraison.getIdLivraison()
        );

        log.info("✅ Livraison créée avec succès - ID: {}, Réservation: {}, {} ligne(s)",
                livraison.getIdLivraison(),
                reservation.getReferenceReservation(),
                lignes.size());

        return toDto(livraison);
    }

    @Override
    public LivraisonResponseDto modifierLivraison(Long idLivraison, LivraisonRequestDto dto, String username) {
        log.info("✏️ Modification de la livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        // Vérifier que la livraison peut être modifiée
        if (livraison.getStatutLivraison() == StatutLivraison.LIVREE
                || livraison.getStatutLivraison() == StatutLivraison.RETOURNEE) {
            throw new CustomException("Impossible de modifier une livraison avec statut " + livraison.getStatutLivraison());
        }


        // Mettre à jour les informations
        livraison.setTitreLivraison(dto.getTitreLivraison());
        livraison.setAdresserLivraison(dto.getAdresseLivraison());
        livraison.setDateLivraison(dto.getDateLivraison());
        livraison.setHeureLivraison(dto.getHeureLivraison());
        livraison.setObservations(dto.getObservations());

        // Si les lignes de réservation ont changé
        if (dto.getIdLignesReservation() != null && !dto.getIdLignesReservation().isEmpty()) {
            // Récupérer les anciennes lignes et les dissocier
            List<LigneReservation> anciennesLignes =
                    ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

            for (LigneReservation ligne : anciennesLignes) {
                ligne.setLivraison(null);
                ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
                ligneReservationRepo.save(ligne);
            }

            // Associer les nouvelles lignes
            List<LigneReservation> nouvellesLignes =
                    ligneReservationRepo.findAllById(dto.getIdLignesReservation());

            for (LigneReservation ligne : nouvellesLignes) {
                // Vérifier que la ligne n'est pas déjà affectée ailleurs
                if (ligne.getLivraison() != null && !ligne.getLivraison().getIdLivraison().equals(idLivraison)) {
                    throw new CustomException(
                            "La ligne ID " + ligne.getIdLigneReservation() +
                                    " est déjà affectée à une autre livraison"
                    );
                }

                ligne.setLivraison(livraison);
                ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
                ligneReservationRepo.save(ligne);
            }

            log.info("🔄 Lignes de réservation mises à jour pour la livraison");
        }

        livraison = livraisonRepo.save(livraison);
        log.info("✅ Livraison modifiée avec succès");

        return toDto(livraison);
    }

    @Override
    @Transactional(readOnly = true)
    public LivraisonResponseDto getLivraisonById(Long idLivraison) {
        log.info("📋 Récupération de la livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        return toDto(livraison);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getAllLivraisons() {
        log.info("📋 Récupération de toutes les livraisons");

        return livraisonRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByStatut(StatutLivraison statut) {
        log.info("📋 Récupération des livraisons avec statut: {}", statut);

        return livraisonRepo.findByStatutLivraison(statut).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByDate(LocalDate date) {
        log.info("📋 Récupération des livraisons du: {}", date);
        return livraisonRepo.findByDateLivraison(date).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsBetweenDates(LocalDate dateDebut, LocalDate dateFin) {
        log.info("📋 Récupération des livraisons entre {} et {}", dateDebut, dateFin);

        return livraisonRepo.findLivraisonsBetweenDates(dateDebut, dateFin).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsAujourdhui() {
        log.info("📋 Récupération des livraisons d'aujourd'hui");

        return livraisonRepo.findLivraisonsAujourdhui().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByEmploye(Long idEmploye) {
        log.info("📋 Récupération des livraisons de l'employé ID: {}", idEmploye);

        return livraisonRepo.findLivraisonsByEmploye(idEmploye).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByReservation(Long idReservation) {
        log.info("📋 Récupération des livraisons de la réservation ID: {}", idReservation);

        return livraisonRepo.findLivraisonsByReservation(idReservation).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LigneReservationResponseDto> getLignesLivraison(Long idLivraison) {
        log.info("📋 Récupération des lignes de la livraison ID {}", idLivraison);

        // Vérifier que la livraison existe
        livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        // Récupérer les lignes de réservation
        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

        log.info("✅ {} ligne(s) trouvée(s) pour la livraison #{}", lignes.size(), idLivraison);

        // Convertir en DTO
        return lignes.stream()
                .map(this::toLigneReservationResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void supprimerLivraison(Long idLivraison, String username) {
        log.info("🗑️ Suppression de la livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        // Vérifier que la livraison n'est pas déjà livrée
        if (livraison.getStatutLivraison() == StatutLivraison.LIVREE ||
                livraison.getStatutLivraison() == StatutLivraison.EN_COURS) {
            throw new CustomException("Impossible de supprimer une livraison déjà livrée");
        }

        // Dissocier les lignes de réservation
        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);
        Reservation reservation = !lignes.isEmpty() ? lignes.get(0).getReservation() : null;
        String titreLivraison = livraison.getTitreLivraison();


        for (LigneReservation ligne : lignes) {
            ligne.setLivraison(null);
            ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
            ligneReservationRepo.save(ligne);
        }

        // Supprimer les affectations
        affectationRepo.deleteByLivraison_IdLivraison(idLivraison);

        // Supprimer la livraison
        livraisonRepo.delete(livraison);

        // ✅ AJOUT : Notification pour le staff après suppression
        if (reservation != null) {
            notificationService.creerNotificationPourStaff(
                    TypeNotification.SYSTEME_INFO,
                    "Livraison supprimée",
                    String.format("La livraison '%s' (Réservation: %s) a été supprimée par %s.",
                            titreLivraison,
                            reservation.getReferenceReservation(),
                            username),
                    reservation.getIdReservation(),
                    "/admin/livraisons"
            );
        }

        log.info("✅ Livraison supprimée avec succès");
    }

    // ============================================
    // GESTION DES STATUTS
    // ============================================

    @Override
    public LivraisonResponseDto changerStatutLivraison(Long idLivraison, StatutLivraison nouveauStatut, String username) {
        log.info("🔄 Changement de statut de la livraison ID: {} → {}", idLivraison, nouveauStatut);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        StatutLivraison ancienStatut = livraison.getStatutLivraison();


        // Récupérer les lignes de cette livraison
        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

        // Récupérer la réservation (toutes les lignes ont la même réservation)
        Reservation reservation = !lignes.isEmpty() ? lignes.get(0).getReservation() : null;

        //  Décrémentation lors du passage EN_COURS
        switch (nouveauStatut) {
            case EN_COURS:
                log.info("🚚 Passage EN_COURS: Décrémentation du stock et mise à jour des statuts");

                // Mettre à jour les lignes
                for (LigneReservation ligne : lignes) {
                    ligne.setStatutLivraisonLigne(StatutLivraison.EN_COURS);

                    Produit produit = ligne.getProduit();

                    // ✅ DÉCRÉMENTATION DU STOCK (selon le type de produit)
                    if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {

                        // Enregistrer le mouvement de stock
                        enregistrerMouvementStock(
                                produit,
                                ligne.getQuantite(),
                                TypeMouvement.LIVRAISON,
                                reservation,
                                "Décrémentation stock lors de la livraison EN_COURS - Client -  " +
                                        (reservation != null ?
                                                reservation.getUtilisateur().getNom()+" "+reservation.getUtilisateur().getPrenom()+
                                                        " ("+reservation.getUtilisateur().getPseudo()+")"
                                                : null),
                                username
                        );
                        // Produit quantitatif: décrémenter le stock
                        int quantiteAvant = produit.getQuantiteDisponible();
                        produit.setQuantiteDisponible(quantiteAvant - ligne.getQuantite());
                        produitRepo.save(produit);

                        log.info("📉 Stock décrémenté pour {}: {} → {} (- {})",
                                produit.getNomProduit(),
                                quantiteAvant,
                                produit.getQuantiteDisponible(),
                                ligne.getQuantite());

                    } else if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE
                            && ligne.getInstancesReservees() != null) {
                        // Produit avec référence: passer les instances en EN_LIVRAISON
                        for (InstanceProduit instance : ligne.getInstancesReservees()) {
                            instance.setStatut(StatutInstance.EN_LIVRAISON);
                            instanceProduitRepo.save(instance);

                            // Enregistrer le mouvement d'instance
                            enregistrerMouvementInstance(
                                    instance,
                                    TypeMouvement.LIVRAISON,
                                    "Livraison en cours vers client - " +
                                            (reservation != null ?
                                                    reservation.getUtilisateur().getNom()+" "+reservation.getUtilisateur().getPrenom()+
                                                            " ("+reservation.getUtilisateur().getPseudo()+")"
                                                    : null),
                                    username,
                                    reservation
                            );
                            // Décrémenter le stock du produit (1 instance = -1 stock)
                            int quantiteAvant = produit.getQuantiteDisponible();
                            produit.setQuantiteDisponible(quantiteAvant - 1);
                            produitRepo.save(produit);

                            log.info("📦 Instance {} → EN_LIVRAISON (Stock: {} → {})",
                                    instance.getNumeroSerie(),
                                    quantiteAvant,
                                    produit.getQuantiteDisponible());


                        }

                        log.info("📦 {} instances passées en EN_LIVRAISON pour ligne {}",
                                ligne.getInstancesReservees().size(),
                                ligne.getIdLigneReservation());
                    }

                    ligneReservationRepo.save(ligne);
                }
                Objects.requireNonNull(reservation).setStatutLivraisonRes(StatutLivraison.EN_COURS);
                reservationRepo.save(reservation);
                livraison.setStatutLivraison(nouveauStatut);
                break;

            case LIVREE:
                log.info("✅ Passage LIVREE: Produits livrés chez le client");
                if (!lignes.isEmpty()) {
                    reservation = lignes.get(0).getReservation();
                    // Vérifier si toutes les lignes de la réservation sont livrées
                    List<LigneReservation> toutesLignes =
                            ligneReservationRepo.findByReservation_IdReservation(reservation.getIdReservation());
                    boolean toutesLivrees = toutesLignes.stream()
                            .allMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.LIVREE);
                    if (toutesLivrees && reservation.getStatutReservation() == StatutReservation.CONFIRME) {
                        // Changer le statut de la livraison et des lignes
                        for (LigneReservation ligne : lignes) {
                            ligne.setStatutLivraisonLigne(StatutLivraison.LIVREE);

                            // Si produit avec référence, passer les instances en EN_UTILISATION
                            if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE
                                    && ligne.getInstancesReservees() != null) {
                                for (InstanceProduit instance : ligne.getInstancesReservees()) {
                                    instance.setStatut(StatutInstance.EN_UTILISATION);
                                    instanceProduitRepo.save(instance);
                                }
                                log.info("📦 {} instances passées en EN_UTILISATION",
                                        ligne.getInstancesReservees().size());
                            }

                            ligneReservationRepo.save(ligne);
                        }
                        // Mettre la réservation en EN_COURS
                        reservation.setStatutLivraisonRes(StatutLivraison.LIVREE);
                        // Le save sera fait automatiquement par JPA grâce à la cascade
                        log.info("📋 Réservation {} passée EN_COURS (toutes les lignes sont livrées)",
                                reservation.getReferenceReservation());
                        livraison.setStatutLivraison(nouveauStatut);
                    }
                }
                break;

            default:
                // Pour les autres statuts, juste mettre à jour
                for (LigneReservation ligne : lignes) {
                    ligne.setStatutLivraisonLigne(nouveauStatut);
                    ligneReservationRepo.save(ligne);
                }
        }

        livraison = livraisonRepo.save(livraison);

        log.info("✅ Statut changé de {} à {} pour {} lignes", ancienStatut, nouveauStatut, lignes.size());

        // Notifications selon le nouveau statut
        if (reservation != null && reservation.getUtilisateur() != null) {

            // CAS 1 : Livraison EN_COURS → Notifier le client + employés affectés
            if (nouveauStatut == StatutLivraison.EN_COURS) {
                // Notification au client
                NotificationRequestDto notifClient = NotificationRequestDto.builder()
                        .typeNotification(TypeNotification.LIVRAISON_EN_COURS)
                        .titre("Livraison en cours")
                        .message(String.format("La livraison de votre Reservation '%s' est en cours. " +
                                        "Préparez-vous à recevoir votre matériel le %s à %s.",
                                reservation.getReferenceReservation(),
                                livraison.getDateLivraison(),
                                livraison.getHeureLivraison()))
                        .idUtilisateur(reservation.getUtilisateur().getIdUtilisateur())
                        .idLivraison(idLivraison)
                        .idReservation(reservation.getIdReservation())
                        .urlAction("/client/reservation-details/" + reservation.getIdReservation())
                        .build();

                notificationService.creerNotificationAvecEmail(notifClient);

                // Notification aux employés affectés (s'ils existent)
                List<AffectationLivraison> affectations =
                        affectationRepo.findByLivraison_IdLivraison(idLivraison);

                for (AffectationLivraison affectation : affectations) {
                    NotificationRequestDto notifEmploye = NotificationRequestDto.builder()
                            .typeNotification(TypeNotification.LIVRAISON_A_EFFECTUER)
                            .titre("Livraison à effectuer")
                            .message(String.format("La livraison '%s' est maintenant EN COURS. " +
                                            "Client: %s %s, Adresse: %s",
                                    livraison.getTitreLivraison(),
                                    reservation.getUtilisateur().getNom(),
                                    reservation.getUtilisateur().getPrenom(),
                                    livraison.getAdresserLivraison()))
                            .idUtilisateur(affectation.getUtilisateur().getIdUtilisateur())
                            .idLivraison(idLivraison)
                            .urlAction("/admin/livraisons/" + idLivraison)
                            .build();

                    notificationService.creerNotificationAvecEmail(notifEmploye);
                }
            }

            // CAS 2 : Livraison LIVREE → Notifier le staff + employés affectés
            else if (nouveauStatut == StatutLivraison.LIVREE) {
                // Notification au staff
                notificationService.creerNotificationPourStaff(
                        TypeNotification.LIVRAISON_EFFECTUEE,
                        "Livraison effectuée",
                        String.format("La livraison '%s' a été marquée comme livrée. Réservation: %s",
                                livraison.getTitreLivraison(),
                                reservation.getReferenceReservation()),
                        reservation.getIdReservation(),
                        "/admin/livraisons/" + idLivraison
                );

                // Notification aux employés affectés
                List<AffectationLivraison> affectations =
                        affectationRepo.findByLivraison_IdLivraison(idLivraison);

                for (AffectationLivraison affectation : affectations) {
                    NotificationRequestDto notifEmploye = NotificationRequestDto.builder()
                            .typeNotification(TypeNotification.LIVRAISON_EFFECTUEE)
                            .titre("Livraison terminée")
                            .message(String.format("La livraison '%s' que vous aviez en charge a été marquée comme livrée.",
                                    livraison.getTitreLivraison()))
                            .idUtilisateur(affectation.getUtilisateur().getIdUtilisateur())
                            .idLivraison(idLivraison)
                            .urlAction("/admin/livraisons/" + idLivraison)
                            .build();

                    notificationService.creerNotification(notifEmploye);
                }
            }
        }

        return toDto(livraison);
    }

    @Override
    public LivraisonResponseDto marquerLivraisonEnCours(Long idLivraison, String username) {
        log.info("🚚 Marquage de la livraison ID {} comme EN_COURS", idLivraison);
        return changerStatutLivraison(idLivraison, StatutLivraison.EN_COURS, username);
    }

    @Override
    public LivraisonResponseDto marquerLivraisonLivree(Long idLivraison, String username) {
        log.info("✅ Marquage de la livraison ID {} comme LIVREE", idLivraison);

        return changerStatutLivraison(idLivraison, StatutLivraison.LIVREE, username);
    }

    /**
     * ✅ Marquer une ligne de réservation spécifique comme LIVREE
     * Cette méthode est appelée depuis le détail de la livraison
     * lorsque l'employé confirme la livraison d'une ligne
     *
     * @param idLigne ID de la ligne de réservation
     * @param username Nom d'utilisateur de l'employé qui effectue l'action
     * @return LigneReservationResponseDto mise à jour
     */
    @Override
    @Transactional
    public LigneReservationResponseDto marquerLigneLivree(Long idLigne, String username) {
        log.info("📦 Marquage de la ligne de réservation ID {} comme LIVREE par {}", idLigne, username);

        // Récupérer la ligne de réservation
        LigneReservation ligne = ligneReservationRepo.findById(idLigne)
                .orElseThrow(() -> new CustomException("Ligne de réservation introuvable avec ID: " + idLigne));

        // Mettre à jour le statut de livraison de la réservation
        Reservation reservation = ligne.getReservation();


        // Vérifications
        if (ligne.getLivraison() == null) {
            throw new CustomException("Cette ligne n'est pas associée à une livraison");
        }

        Livraison livraison = ligne.getLivraison();

        // Vérifier que la livraison est EN_COURS
        if (livraison.getStatutLivraison() != StatutLivraison.EN_COURS) {
            throw new CustomException(
                    "La livraison doit être EN_COURS pour marquer une ligne comme livrée. " +
                            "Statut actuel: " + livraison.getStatutLivraison()
            );
        }

        // Vérifier que la ligne n'est pas déjà livrée
        if (ligne.getStatutLivraisonLigne() == StatutLivraison.LIVREE) {
            log.warn("⚠️ La ligne {} est déjà marquée comme LIVREE", idLigne);
            throw new CustomException("Cette ligne est déjà marquée comme livrée");
        }

        // ============================================
        // METTRE À JOUR LE STATUT DE LA LIGNE
        // ============================================

        StatutLivraison ancienStatut = ligne.getStatutLivraisonLigne();
        ligne.setStatutLivraisonLigne(StatutLivraison.LIVREE);
        ligne = ligneReservationRepo.save(ligne);

        log.info("✅ Ligne #{} : {} → LIVREE (Produit: {})",
                ligne.getIdLigneReservation(),
                ancienStatut,
                ligne.getProduit().getNomProduit());


        // Si produit avec référence, mettre les instances EN_ATTENTE
        if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE
                && ligne.getInstancesReservees() != null
                && !ligne.getInstancesReservees().isEmpty()) {

            for (InstanceProduit instance : ligne.getInstancesReservees()) {
                // Vérifier que l'instance est bien disponible
                if (instance.getStatut() == StatutInstance.EN_LIVRAISON) {
                    instance.setStatut(StatutInstance.EN_UTILISATION);
                    instanceProduitRepo.save(instance);

                    log.info("📦 Instance {} :  EN_LIVRAISON → EN_UTILISATION ",
                            instance.getNumeroSerie());
                }
            }
        }

        // ============================================
        // VÉRIFIER SI TOUTES LES LIGNES SONT LIVRÉES
        // ============================================

        List<LigneReservation> toutesLignesDeLivraison = ligneReservationRepo
                .findByLivraison_IdLivraison(livraison.getIdLivraison());

        boolean toutesLignesLivrees = toutesLignesDeLivraison.stream()
                .allMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.LIVREE);

        log.info("📊 État de la livraison #{}: {}/{} lignes livrées",
                livraison.getIdLivraison(),
                toutesLignesDeLivraison.stream()
                        .filter(l -> l.getStatutLivraisonLigne() == StatutLivraison.LIVREE)
                        .count(),
                toutesLignesDeLivraison.size());

        // Si toutes les lignes sont livrées, marquer la livraison comme LIVREE
        if (toutesLignesLivrees) {
            livraison.setStatutLivraison(StatutLivraison.LIVREE);
            livraisonRepo.save(livraison);

            log.info("🎉 TOUTES les lignes de la livraison #{} sont livrées → Livraison marquée LIVREE",
                    livraison.getIdLivraison());


            // Vérifier si toutes les lignes de la réservation sont livrées
            List<LigneReservation> toutesLignesReservation = ligneReservationRepo
                    .findByReservation_IdReservation(reservation.getIdReservation());

            boolean toutesLignesReservationLivrees = toutesLignesReservation.stream()
                    .allMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.LIVREE);

            if (toutesLignesReservationLivrees) {
                reservation.setStatutLivraisonRes(StatutLivraison.LIVREE);
                reservationRepo.save(reservation);

                log.info("🎉 TOUTES les lignes de la réservation {} sont livrées",
                        reservation.getReferenceReservation());
            }
        }

        // Notification au staff et au client (notification seulement, pas d'email)
        if (reservation != null) {
            // Notification au staff
            notificationService.creerNotificationPourStaff(
                    TypeNotification.SYSTEME_INFO,
                    "Ligne de livraison marquée",
                    String.format("Une ligne de la livraison '%s' (Produit: %s, Qté: %d) a été marquée comme livrée.",
                            livraison.getTitreLivraison(),
                            ligne.getProduit().getNomProduit(),
                            ligne.getQuantite()),
                    reservation.getIdReservation(),
                    "/admin/livraisons/" + livraison.getIdLivraison()
            );

            // Notification au client (si le client existe)
            if (reservation.getUtilisateur() != null) {
                NotificationRequestDto notifClient = NotificationRequestDto.builder()
                        .typeNotification(TypeNotification.LIVRAISON_EN_COURS)
                        .titre("Progression de votre livraison")
                        .message(String.format("Le produit '%s' (Quantité: %d) de votre réservation %s a été livré.",
                                ligne.getProduit().getNomProduit(),
                                ligne.getQuantite(),
                                reservation.getReferenceReservation()))
                        .idUtilisateur(reservation.getUtilisateur().getIdUtilisateur())
                        .idLivraison(livraison.getIdLivraison())
                        .idReservation(reservation.getIdReservation())
                        .urlAction("/client/reservation-details/" + reservation.getIdReservation())
                        .build();

                notificationService.creerNotification(notifClient);
            }
        }

        // Retourner le DTO de la ligne mise à jour
        return toLigneReservationResponseDto(ligne);
    }


    // ============================================
    // AFFECTATION D'EMPLOYÉS
    // ============================================

    @Override
    public AffectationLivraisonDto affecterEmploye(AffectationLivraisonRequestDto dto, String username) {
        log.info("👤 Affectation de l'employé ID {} à la livraison ID {}",
                dto.getIdEmploye(), dto.getIdLivraison());

        // Vérifier que la livraison existe
        Livraison livraison = livraisonRepo.findById(dto.getIdLivraison())
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + dto.getIdLivraison()));

        // Vérifier que l'employé existe et a le rôle approprié
        Utilisateur employe = utilisateurRepo.findById(dto.getIdEmploye())
                .orElseThrow(() -> new CustomException("Employé introuvable avec ID: " + dto.getIdEmploye()));

        List<UtilisateurRole> utilisateurRoles = utilisateurRoleRepo.findByUtilisateurIdUtilisateur(dto.getIdEmploye());
        boolean estEmploye = utilisateurRoles.stream()
                .anyMatch(utilisateurRole -> utilisateurRole.getRole().getNom().equals("EMPLOYE") ||
                        utilisateurRole.getRole().getNom().equals("ADMIN") ||
                        utilisateurRole.getRole().getNom().equals("MANAGER"));

        if (!estEmploye) {
            throw new CustomException("L'utilisateur doit avoir le rôle EMPLOYE, ADMIN ou MANAGER");
        }

        // Vérifier que l'employé n'est pas déjà affecté à cette livraison
        if (affectationRepo.existsByLivraisonAndEmploye(dto.getIdLivraison(), dto.getIdEmploye())) {
            throw new CustomException("L'employé est déjà affecté à cette livraison");
        }

        // Créer l'affectation
        AffectationLivraison affectation = new AffectationLivraison();
        affectation.setLivraison(livraison);
        affectation.setUtilisateur(employe);
        affectation.setDateAffectationLivraison(LocalDate.now());
        affectation.setHeureAffectation(LocalTime.now());
        affectation.setNotes(dto.getNotes());

        affectation = affectationRepo.save(affectation);


        //  Notification à l'employé affecté + staff

        // Récupérer la réservation pour les détails
        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(dto.getIdLivraison());
        Reservation reservation = !lignes.isEmpty() ? lignes.get(0).getReservation() : null;

        // Notification à l'employé affecté
        NotificationRequestDto notifEmploye = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.LIVRAISON_A_EFFECTUER)
                .titre("Nouvelle affectation de livraison")
                .message(String.format("Vous avez été affecté à la livraison '%s' prévue le %s à %s. " +
                                "Adresse: %s",
                        livraison.getTitreLivraison(),
                        livraison.getDateLivraison(),
                        livraison.getHeureLivraison(),
                        livraison.getAdresserLivraison()))
                .idUtilisateur(employe.getIdUtilisateur())
                .idLivraison(livraison.getIdLivraison())
                .idReservation(reservation != null ? reservation.getIdReservation() : null)
                .urlAction("/admin/livraisons/" + livraison.getIdLivraison())
                .build();

        notificationService.creerNotificationAvecEmail(notifEmploye);

        // Notification au staff
        if (reservation != null) {
            notificationService.creerNotificationPourStaff(
                    TypeNotification.SYSTEME_INFO,
                    "Employé affecté à une livraison",
                    String.format("L'employé %s %s a été affecté à la livraison '%s' (Réservation: %s).",
                            employe.getPrenom(),
                            employe.getNom(),
                            livraison.getTitreLivraison(),
                            reservation.getReferenceReservation()),
                    reservation.getIdReservation(),
                    "/admin/livraisons/" + livraison.getIdLivraison()
            );
        }

        log.info("✅ Employé {} affecté à la livraison {}", employe.getEmail(), livraison.getTitreLivraison());

        return toAffectationDto(affectation);
    }

    @Override
    public void retirerEmploye(Long idAffectation, String username) {
        log.info("🗑️ Retrait de l'affectation ID: {}", idAffectation);

        AffectationLivraison affectation = affectationRepo.findById(idAffectation)
                .orElseThrow(() -> new CustomException("Affectation introuvable avec ID: " + idAffectation));

        // Récupérer les informations avant suppression pour les notifications
        Utilisateur employe = affectation.getUtilisateur();
        Livraison livraison = affectation.getLivraison();

        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(livraison.getIdLivraison());
        Reservation reservation = !lignes.isEmpty() ? lignes.get(0).getReservation() : null;


        affectationRepo.delete(affectation);

        // Notification à l'employé retiré + staff

        // Notification à l'employé retiré
        NotificationRequestDto notifEmploye = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.SYSTEME_INFO)
                .titre("Retrait d'affectation de livraison")
                .message(String.format("Vous avez été retiré de la livraison '%s' prévue le %s.",
                        livraison.getTitreLivraison(),
                        livraison.getDateLivraison()))
                .idUtilisateur(employe.getIdUtilisateur())
                .idLivraison(livraison.getIdLivraison())
                .urlAction("/admin/livraisons")
                .build();

        notificationService.creerNotification(notifEmploye);

        // Notification au staff
        if (reservation != null) {
            notificationService.creerNotificationPourStaff(
                    TypeNotification.SYSTEME_INFO,
                    "Employé retiré d'une livraison",
                    String.format("L'employé %s %s a été retiré de la livraison '%s' (Réservation: %s).",
                            employe.getPrenom(),
                            employe.getNom(),
                            livraison.getTitreLivraison(),
                            reservation.getReferenceReservation()),
                    reservation.getIdReservation(),
                    "/admin/livraisons/" + livraison.getIdLivraison()
            );
        }

        log.info("✅ Affectation supprimée avec succès");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffectationLivraisonDto> getAffectationsByLivraison(Long idLivraison) {
        log.info("📋 Récupération des affectations de la livraison ID: {}", idLivraison);

        return affectationRepo.findByLivraison_IdLivraison(idLivraison).stream()
                .map(this::toAffectationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffectationLivraisonDto> getAffectationsByEmploye(Long idEmploye) {
        log.info("📋 Récupération des affectations de l'employé ID: {}", idEmploye);

        return affectationRepo.findByUtilisateur_IdUtilisateur(idEmploye).stream()
                .map(this::toAffectationDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // BON DE LIVRAISON (PDF)
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public byte[] genererBonLivraison(Long idLivraison) {
        log.info("📄 Génération du bon de livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

        if (lignes.isEmpty()) {
            throw new CustomException("Aucune ligne de réservation associée à cette livraison");
        }

        Reservation reservation = lignes.get(0).getReservation();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Titre
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("BON DE LIVRAISON", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Informations de livraison
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

            Paragraph infoLivraison = new Paragraph();
            infoLivraison.add(new Chunk("Numéro de livraison: ", headerFont));
            infoLivraison.add(new Chunk("LIV-" + livraison.getIdLivraison() + "\n", normalFont));
            infoLivraison.add(new Chunk("Date: ", headerFont));
            infoLivraison.add(new Chunk(livraison.getDateLivraison().toString() + "\n", normalFont));
            infoLivraison.add(new Chunk("Heure: ", headerFont));
            infoLivraison.add(new Chunk(livraison.getHeureLivraison().toString() + "\n", normalFont));
            infoLivraison.add(new Chunk("Adresse: ", headerFont));
            infoLivraison.add(new Chunk(livraison.getAdresserLivraison() + "\n", normalFont));
            infoLivraison.setSpacingAfter(20);
            document.add(infoLivraison);

            // Informations client
            Paragraph infoClient = new Paragraph();
            infoClient.add(new Chunk("Client: \n", headerFont));
            infoClient.add(new Chunk(reservation.getUtilisateur().getNom() + " " +
                    reservation.getUtilisateur().getPrenom() + "\n" + reservation.getUtilisateur().getEmail() +
                    "\n" + reservation.getUtilisateur().getTelephone().toString() + " \n", normalFont));
            infoClient.add(new Chunk("Réservation: ", headerFont));
            infoClient.add(new Chunk(reservation.getReferenceReservation() + "\n", normalFont));
            infoClient.setSpacingAfter(20);
            document.add(infoClient);

            // Table des produits
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 3, 2, 2, 2});

            // En-têtes
            addTableHeader(table, "Qté");
            addTableHeader(table, "Produit");
            addTableHeader(table, "Date début");
            addTableHeader(table, "Date fin");
            addTableHeader(table, "Références");

            // Lignes de produits
            for (LigneReservation ligne : lignes) {
                table.addCell(String.valueOf(ligne.getQuantite()));
                table.addCell(ligne.getProduit().getNomProduit());
                table.addCell(ligne.getDateDebut().toString());
                table.addCell(ligne.getDateFin().toString());

                // Ajouter les références si produit avec référence
                if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE &&
                        ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {
                    String refs = ligne.getInstancesReservees().stream()
                            .map(InstanceProduit::getNumeroSerie)
                            .collect(Collectors.joining(", "));
                    table.addCell(refs);
                } else {
                    table.addCell("-");
                }
            }

            document.add(table);

            // Employés affectés
            List<AffectationLivraison> affectations =
                    affectationRepo.findByLivraison_IdLivraison(idLivraison);

            if (!affectations.isEmpty()) {
                Paragraph employesTitle = new Paragraph("\nEmployés affectés:", headerFont);
                employesTitle.setSpacingBefore(20);
                document.add(employesTitle);

                for (AffectationLivraison aff : affectations) {
                    Paragraph emp = new Paragraph("- " + aff.getUtilisateur().getNom() + " " +
                            aff.getUtilisateur().getPrenom() + " (" +
                            aff.getDateAffectationLivraison() + " - " + aff.getHeureAffectation() + ")", normalFont);
                    document.add(emp);
                }
            }

            // Signature
            Paragraph signature = new Paragraph();
            signature.setSpacingBefore(50);
            signature.add(new Chunk("Signature du client: ______________________\n\n", normalFont));
            signature.add(new Chunk("Date et heure de réception: ______________________", normalFont));
            document.add(signature);

            document.close();

            log.info("✅ Bon de livraison généré avec succès");
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("❌ Erreur lors de la génération du PDF: {}", e.getMessage());
            throw new CustomException("Erreur lors de la génération du bon de livraison: " + e.getMessage());
        }
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerTitle, headerFont));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.setPadding(5);
        table.addCell(header);
    }

    // ============================================
    // GESTION DES RETOURS
    // ============================================

    /**
     * 🔙 Marquer une ligne comme "En retour"
     */
    @Override
    @Transactional
    public LigneReservationResponseDto marquerLigneEnRetour(Long idLigne, String username) {
        log.info("🔙 Début marquage ligne EN RETOUR - ID: {}", idLigne);

        // ============================================
        // RÉCUPÉRATION ET VALIDATIONS
        // ============================================

        LigneReservation ligne = ligneReservationRepo.findById(idLigne)
                .orElseThrow(() -> new CustomException(
                        "Ligne de réservation avec ID " + idLigne + " introuvable"));

        Livraison livraison = ligne.getLivraison();
        if (livraison == null) {
            throw new CustomException("Cette ligne n'est pas associée à une livraison");
        }

        // Vérifier que la livraison est dans un état permettant le retour
        if (livraison.getStatutLivraison() != StatutLivraison.LIVREE &&
                livraison.getStatutLivraison() != StatutLivraison.RETOUR &&
                livraison.getStatutLivraison() != StatutLivraison.RETOUR_PARTIEL) {
            throw new CustomException(
                    "Impossible de marquer en retour. " +
                            "La livraison doit être livrée. " +
                            "Statut actuel: " + livraison.getStatutLivraison()
            );
        }

        // Vérifier que la ligne est livrée
        if (ligne.getStatutLivraisonLigne() != StatutLivraison.LIVREE) {
            throw new CustomException(
                    "Cette ligne doit être livrée avant de pouvoir être marquée en retour. " +
                            "Statut actuel: " + ligne.getStatutLivraisonLigne()
            );
        }

        // ============================================
        // METTRE À JOUR LE STATUT DE LA LIGNE
        // ============================================

        StatutLivraison ancienStatut = ligne.getStatutLivraisonLigne();
        ligne.setStatutLivraisonLigne(StatutLivraison.RETOUR);
        ligne = ligneReservationRepo.save(ligne);
        ligne.getReservation().setStatutLivraisonRes(StatutLivraison.RETOUR);

        log.info("✅ Ligne #{} : {} → RETOUR (Produit: {})",
                ligne.getIdLigneReservation(),
                ancienStatut,
                ligne.getProduit().getNomProduit());

        // ============================================
        // METTRE À JOUR LES INSTANCES (AVEC_REFERENCE)
        // ============================================

        if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE
                && ligne.getInstancesReservees() != null
                && !ligne.getInstancesReservees().isEmpty()) {

            for (InstanceProduit instance : ligne.getInstancesReservees()) {
                if (instance.getStatut() == StatutInstance.EN_UTILISATION) {
                    instance.setStatut(StatutInstance.EN_RETOUR);
                    instanceProduitRepo.save(instance);

                    // Enregistrer mouvement
                    enregistrerMouvementInstance(
                            instance,
                            TypeMouvement.RETOUR,
                            "Début du retour physique - Réservation " +
                                    ligne.getReservation().getReferenceReservation(),
                            username,
                            ligne.getReservation()
                    );

                    log.info("📦 Instance {} : EN_UTILISATION → EN_RETOUR",
                            instance.getNumeroSerie());
                }
            }
        }

        // ============================================
        // METTRE À JOUR LE STATUT DE LA LIVRAISON
        // ============================================

        List<LigneReservation> toutesLignesDeLivraison = ligneReservationRepo
                .findByLivraison_IdLivraison(livraison.getIdLivraison());

        // Vérifier si au moins une ligne est en retour
        boolean auMoinsUneEnRetour = toutesLignesDeLivraison.stream()
                .anyMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOUR);

        // Vérifier si toutes les lignes sont retournées ou en retour
        boolean toutesRetourneesOuEnRetour = toutesLignesDeLivraison.stream()
                .allMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOURNEE ||
                        l.getStatutLivraisonLigne() == StatutLivraison.RETOUR);

        if (auMoinsUneEnRetour && toutesRetourneesOuEnRetour) {
            // Vérifier si certaines sont complètement retournées
            boolean certainesRetournees = toutesLignesDeLivraison.stream()
                    .anyMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOURNEE);

            if (certainesRetournees) {
                livraison.setStatutLivraison(StatutLivraison.RETOUR_PARTIEL);
                log.info("📊 Livraison #{}: Passage à RETOUR_PARTIEL", livraison.getIdLivraison());
            } else {
                livraison.setStatutLivraison(StatutLivraison.RETOUR);
                log.info("📊 Livraison #{}: Passage a RETOUR", livraison.getIdLivraison());
            }

            livraisonRepo.save(livraison);
        }

        log.info("📊 État de la livraison #{}: {}/{} lignes en retour ou retournées",
                livraison.getIdLivraison(),
                toutesLignesDeLivraison.stream()
                        .filter(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOUR ||
                                l.getStatutLivraisonLigne() == StatutLivraison.RETOURNEE)
                        .count(),
                toutesLignesDeLivraison.size());

        return toLigneReservationResponseDto(ligne);
    }

    /**
     * ✅ Marquer une ligne comme "Retournée" (finalisée)
     */
    @Override
    @Transactional
    public LigneReservationResponseDto marquerLigneRetournee(Long idLigne, String username) {
        log.info("✅ Début marquage ligne RETOURNEE - ID: {}", idLigne);

        // ============================================
        // RÉCUPÉRATION ET VALIDATIONS
        // ============================================

        LigneReservation ligne = ligneReservationRepo.findById(idLigne)
                .orElseThrow(() -> new CustomException(
                        "Ligne de réservation avec ID " + idLigne + " introuvable"));

        Livraison livraison = ligne.getLivraison();
        Reservation reservation = ligne.getReservation();
        if (livraison == null) {
            throw new CustomException("Cette ligne n'est pas associée à une livraison");
        }

        // Vérifier que la ligne n'est pas déjà retournée
        if (ligne.getStatutLivraisonLigne() == StatutLivraison.RETOURNEE) {
            log.warn("⚠️ La ligne {} est déjà marquée comme RETOURNEE", idLigne);
            throw new CustomException("Cette ligne est déjà marquée comme retournée");
        }

        // Vérifier que la ligne est en retour ou livrée
        if (ligne.getStatutLivraisonLigne() != StatutLivraison.RETOUR &&
                ligne.getStatutLivraisonLigne() != StatutLivraison.LIVREE) {
            throw new CustomException(
                    "Cette ligne doit être en retour ou livrée. " +
                            "Statut actuel: " + ligne.getStatutLivraisonLigne()
            );
        }


        // ============================================
        // METTRE À JOUR LE STATUT DE LA LIGNE
        // ============================================

        StatutLivraison ancienStatut = ligne.getStatutLivraisonLigne();
        ligne.setStatutLivraisonLigne(StatutLivraison.RETOURNEE);
        ligne = ligneReservationRepo.save(ligne);

        log.info("✅ Ligne #{} : {} → RETOURNEE (Produit: {})",
                ligne.getIdLigneReservation(),
                ancienStatut,
                ligne.getProduit().getNomProduit());

        // ============================================
        // GÉRER LES INSTANCES ET LE STOCK
        // ============================================

        if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
            // Produit avec référence : libérer les instances
            if (ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {

                for (InstanceProduit instance : ligne.getInstancesReservees()) {
                    // Remettre l'instance disponible
                    instance.setStatut(StatutInstance.DISPONIBLE);
                    instanceProduitRepo.save(instance);

                    // Enregistrer mouvement
                    enregistrerMouvementInstance(
                            instance,
                            TypeMouvement.RETOUR,
                            "Retour validé, instance disponible - Réservation " +
                                    ligne.getReservation().getReferenceReservation(),
                            username,
                            ligne.getReservation()
                    );

                    // Produit en quantité : réintégrer le stock
                    Integer quantiteAvant = ligne.getProduit().getQuantiteDisponible();
                    ligne.getProduit().setQuantiteDisponible(quantiteAvant+1);
                    produitRepo.save(ligne.getProduit());


                    log.info("📦 Instance {} : {} → DISPONIBLE",
                            instance.getNumeroSerie(),
                            instance.getStatut());
                }

                // Libérer les instances de la ligne
                ligne.getInstancesReservees().clear();
                ligneReservationRepo.save(ligne);

                log.info("✅ {} instances libérées et DISPONIBLES", ligne.getQuantite());


            }

        } else {

            // Produit en quantité : réintégrer le stock
            Integer quantiteAvant = ligne.getProduit().getQuantiteDisponible();
            Integer quantiteApres = quantiteAvant + ligne.getQuantite();

            // Enregistrer mouvement stock
            enregistrerMouvementStock(
                    ligne.getProduit(),
                    ligne.getQuantite(),
                    TypeMouvement.RETOUR,
                    ligne.getReservation(),
                    "Retour validé: +" + ligne.getQuantite() + "x " +
                            ligne.getProduit().getNomProduit() +
                            " (Quantité disponible: " + quantiteAvant + " → " + quantiteApres + ") - " +
                            "Réservation " + ligne.getReservation().getReferenceReservation(),
                    username
            );

            ligne.getProduit().setQuantiteDisponible(quantiteApres);
            produitRepo.save(ligne.getProduit());



            log.info("📦 Stock réintégré: {} → {} (+{})",
                    quantiteAvant,
                    quantiteApres,
                    ligne.getQuantite());
        }

        // ============================================
        // VÉRIFIER SI TOUTES LES LIGNES SONT RETOURNÉES
        // ============================================

        List<LigneReservation> toutesLignesDeLivraison = ligneReservationRepo
                .findByLivraison_IdLivraison(livraison.getIdLivraison());

        boolean toutesLignesRetournees = toutesLignesDeLivraison.stream()
                .allMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOURNEE);

        log.info("📊 État de la livraison #{}: {}/{} lignes retournées",
                livraison.getIdLivraison(),
                toutesLignesDeLivraison.stream()
                        .filter(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOURNEE)
                        .count(),
                toutesLignesDeLivraison.size());

        if (toutesLignesRetournees) {
            livraison.setStatutLivraison(StatutLivraison.RETOURNEE);
            livraisonRepo.save(livraison);

            log.info("🎉 Livraison #{} : TOUTES les lignes sont retournées → Statut RETOURNEE",
                    livraison.getIdLivraison());

            // ============================================
            // VÉRIFIER SI LA RÉSERVATION EST TERMINÉE
            // ============================================


            List<LigneReservation> toutesLignesReservation =
                    ligneReservationRepo.findByReservation_IdReservation(reservation.getIdReservation());

            boolean toutesLignesReservationRetournees = toutesLignesReservation.stream()
                    .allMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOURNEE);

            if (toutesLignesReservationRetournees &&
                    reservation.getStatutReservation() == StatutReservation.CONFIRME) {

                reservation.setStatutReservation(StatutReservation.TERMINE);
                reservation.setStatutLivraisonRes(StatutLivraison.RETOURNEE);
                reservationRepo.save(reservation);

                log.info("🎉 Réservation {} : TOUTES les lignes retournées → Statut TERMINE",
                        reservation.getReferenceReservation());
            }
        } else {
            // Certaines lignes sont retournées, d'autres non
            boolean auMoinsUneEnRetour = toutesLignesDeLivraison.stream()
                    .anyMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.RETOUR);

            if (auMoinsUneEnRetour) {
                livraison.setStatutLivraison(StatutLivraison.RETOUR_PARTIEL);
                log.info("📊 Livraison #{}: Passage à RETOUR_PARTIEL (retour en cours)",
                        livraison.getIdLivraison());
            } else {
                livraison.setStatutLivraison(StatutLivraison.RETOUR);
                log.info("📊 Livraison #{}: Passage à RETOUR", livraison.getIdLivraison());
            }

            livraisonRepo.save(livraison);
        }

        //Notification aux employés concernés + staff

        // Notification au staff
        notificationService.creerNotificationPourStaff(
                TypeNotification.SYSTEME_INFO,
                "Ligne de retour confirmée",
                String.format("Le produit '%s' (Qté: %d) de la livraison '%s' a été retourné et le stock a été réintégré. Réservation: %s",
                        ligne.getProduit().getNomProduit(),
                        ligne.getQuantite(),
                        livraison.getTitreLivraison(),
                        reservation.getReferenceReservation()),
                reservation.getIdReservation(),
                "/admin/livraisons/" + livraison.getIdLivraison()
        );

        // Notification aux employés affectés à cette livraison
        List<AffectationLivraison> affectations =
                affectationRepo.findByLivraison_IdLivraison(livraison.getIdLivraison());

        for (AffectationLivraison affectation : affectations) {
            NotificationRequestDto notifEmploye = NotificationRequestDto.builder()
                    .typeNotification(TypeNotification.SYSTEME_INFO)
                    .titre("Ligne retournée")
                    .message(String.format("Le produit '%s' (Qté: %d) de la livraison '%s' a été marqué comme retourné.",
                            ligne.getProduit().getNomProduit(),
                            ligne.getQuantite(),
                            livraison.getTitreLivraison()))
                    .idUtilisateur(affectation.getUtilisateur().getIdUtilisateur())
                    .idLivraison(livraison.getIdLivraison())
                    .urlAction("/admin/livraisons/" + livraison.getIdLivraison())
                    .build();

            notificationService.creerNotification(notifEmploye);
        }
        // ============================================
        // RETOURNER LE DTO
        // ============================================

        return toLigneReservationResponseDto(ligne);
    }

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Enregistrer un mouvement de stock
     */
    private void enregistrerMouvementStock(
            Produit produit,
            int quantite,
            TypeMouvement typeMouvement,
            Reservation reservation,
            String motif,
            String username) {

        Integer quantiteAvant = produit.getQuantiteDisponible();
        Integer quantiteApres = 0 ;
        if(typeMouvement==TypeMouvement.LIVRAISON) {
            quantiteApres = quantiteAvant - quantite;
        }else{
            quantiteApres = quantiteAvant + quantite;
        }

        MouvementStock mouvement = MouvementStock.builder()
                .produit(produit)
                .quantite(quantite)
                .quantiteAvant(quantiteAvant)
                .quantiteApres(quantiteApres)
                .typeMouvement(typeMouvement)
                .motif(motif)
                .effectuePar(username)
                .dateMouvement(LocalDateTime.now())
                .build();

        if (reservation != null) {
            mouvement.setIdReservation(reservation.getIdReservation());
            mouvement.setReferenceReservation(reservation.getReferenceReservation());
        }

        mouvementStockRepo.save(mouvement);
    }

    /**
     * Enregistrer un mouvement d'instance pour traçabilité
     */
    private void enregistrerMouvementInstance(
            InstanceProduit instance,
            TypeMouvement typeMouvement,
            String motif,
            String username,
            Reservation reservation) {

        Integer quantiteAvant = instance.getProduit().getQuantiteDisponible();
        Integer quantiteApres = 0 ;
        if(typeMouvement==TypeMouvement.LIVRAISON) {
             quantiteApres = quantiteAvant - 1;
        }else if (instance.getStatut().equals(StatutInstance.EN_RETOUR)){
             quantiteApres = quantiteAvant;
        }else{
            quantiteApres = quantiteAvant +1 ;
        }

        MouvementStock mouvement = MouvementStock.builder()
                .produit(instance.getProduit())
                .quantite(1)
                .quantiteAvant(quantiteAvant)
                .quantiteApres(quantiteApres)
                .typeMouvement(typeMouvement)
                .motif(motif)
                .codeInstance(instance.getNumeroSerie())
                .effectuePar(username)
                .dateMouvement(LocalDateTime.now())
                .build();

        if (reservation != null) {
            mouvement.setIdReservation(reservation.getIdReservation());
            mouvement.setReferenceReservation(reservation.getReferenceReservation());
        }

        mouvementStockRepo.save(mouvement);
    }

    // ============================================
    // STATISTIQUES
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public Long countByStatut(StatutLivraison statut) {
        return livraisonRepo.countByStatut(statut);
    }

    // ============================================
    // MÉTHODES DE CONVERSION (MAPPERS)
    // ============================================

    private LivraisonResponseDto toDto(Livraison livraison) {
        LivraisonResponseDto dto = new LivraisonResponseDto();
        dto.setIdLivraison(livraison.getIdLivraison());
        dto.setTitreLivraison(livraison.getTitreLivraison());
        dto.setAdresseLivraison(livraison.getAdresserLivraison());
        dto.setDateLivraison(livraison.getDateLivraison());
        dto.setHeureLivraison(livraison.getHeureLivraison());
        dto.setStatutLivraison(livraison.getStatutLivraison());
        dto.setObservations(livraison.getObservations());


        dto.setDateCreation(livraison.getDateCreation());
        dto.setDateModification(livraison.getDateModification());

        // Récupérer les lignes associées
        List<LigneReservation> lignes =
                ligneReservationRepo.findByLivraison_IdLivraison(livraison.getIdLivraison());

        dto.setLignesReservation(lignes.stream()
                .map(this::toLigneLivraisonDto)
                .collect(Collectors.toList()));

        // Calculer le nombre total d'articles
        dto.setNombreTotalArticles(lignes.stream()
                .mapToInt(LigneReservation::getQuantite)
                .sum());


        if (!lignes.isEmpty()) {

            Reservation reservation = lignes.get(0).getReservation();
            dto.setNomClient(reservation.getUtilisateur().getNom());
            dto.setPrenomClient(reservation.getUtilisateur().getPrenom());
            dto.setEmailClient(reservation.getUtilisateur().getEmail());
            dto.setTelephoneClient(reservation.getUtilisateur().getTelephone());
            dto.setReferenceReservation(reservation.getReferenceReservation());


        }

        // Récupérer les affectations
        dto.setAffectations(affectationRepo.findByLivraison_IdLivraison(livraison.getIdLivraison())
                .stream()
                .map(this::toAffectationDto)
                .collect(Collectors.toList()));

        return dto;
    }

    private LivraisonResponseDto.LigneLivraisonDto toLigneLivraisonDto(LigneReservation ligne) {
        LivraisonResponseDto.LigneLivraisonDto dto = new LivraisonResponseDto.LigneLivraisonDto();
        dto.setIdLigne(ligne.getIdLigneReservation());
        dto.setNomProduit(ligne.getProduit().getNomProduit());
        dto.setQuantite(ligne.getQuantite());
        dto.setDateDebut(ligne.getDateDebut());
        dto.setDateFin(ligne.getDateFin());
        dto.setStatutLivraisonLigne(ligne.getStatutLivraisonLigne());
        dto.setTypeProduit(ligne.getProduit().getTypeProduit().toString());

        // Si produit avec référence, ajouter les instances
        if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE &&
                ligne.getInstancesReservees() != null) {
            dto.setInstancesReservees(ligne.getInstancesReservees().stream()
                    .map(InstanceProduit::getNumeroSerie)
                    .collect(Collectors.toList()));
        }


        return dto;
    }

    private AffectationLivraisonDto toAffectationDto(AffectationLivraison affectation) {
        AffectationLivraisonDto dto = new AffectationLivraisonDto();
        dto.setIdAffectation(affectation.getIdAffectationLivraison());

        dto.setDateAffectation(affectation.getDateAffectationLivraison());
        dto.setHeureAffectation(affectation.getHeureAffectation());


        // Infos employé
        dto.setIdEmploye(affectation.getUtilisateur().getIdUtilisateur());
        dto.setNomEmploye(affectation.getUtilisateur().getNom());
        dto.setPrenomEmploye(affectation.getUtilisateur().getPrenom());
        dto.setEmailEmploye(affectation.getUtilisateur().getEmail());
        dto.setTelephoneEmploye(affectation.getUtilisateur().getTelephone().toString());

        // Infos livraison
        dto.setIdLivraison(affectation.getLivraison().getIdLivraison());
        dto.setTitreLivraison(affectation.getLivraison().getTitreLivraison());

        return dto;
    }

    /**
     * Méthode helper pour convertir LigneReservation en DTO
     */
    private LigneReservationResponseDto toLigneReservationResponseDto(LigneReservation ligne) {
        LigneReservationResponseDto dto = new LigneReservationResponseDto();
        dto.setIdLigneReservation(ligne.getIdLigneReservation());
        dto.setIdProduit(ligne.getProduit().getIdProduit());
        dto.setNomProduit(ligne.getProduit().getNomProduit());
        dto.setQuantite(ligne.getQuantite());
        dto.setDateDebut(ligne.getDateDebut());
        dto.setDateFin(ligne.getDateFin());
        dto.setStatutLivraisonLigne(ligne.getStatutLivraisonLigne());
        dto.setPrixUnitaire(ligne.getPrixUnitaire());
        dto.setSousTotal(ligne.getPrixTotal());
        dto.setNomProduit(ligne.getProduit().getNomProduit());
        dto.setCodeProduit(ligne.getProduit().getCodeProduit());
        dto.setQuantite(ligne.getQuantite());

        // Ajouter les instances si produit avec référence
        if (ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {
            dto.setNumerosSeries(
                    ligne.getInstancesReservees().stream()
                            .map(InstanceProduit::getNumeroSerie)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}