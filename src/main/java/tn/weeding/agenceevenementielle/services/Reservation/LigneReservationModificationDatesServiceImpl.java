package tn.weeding.agenceevenementielle.services.Reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.DecalerToutesLignesRequestDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModificationDatesResponseDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModifierPlusieurLignesRequestDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModifierUneLigneRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.ReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.ReservationException;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ==========================================
 * IMPLÉMENTATION DU SERVICE DE MODIFICATION DE DATES
 * Sprint 4 - Gestion des réservations
 * ==========================================
 *
 * 🎯 RESPONSABILITÉS :
 * 1. Modifier les dates d'une ou plusieurs lignes
 * 2. Vérifier la disponibilité AVANT modification
 * 3. Recalculer automatiquement les dates de la réservation
 * 4. Gérer les instances réservées (produits avec référence)
 * 5. Tracer les modifications dans les commentaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LigneReservationModificationDatesServiceImpl implements LigneReservationModificationDatesService {

    private final LigneReservationRepository ligneReservationRepo;
    private final ReservationRepository reservationRepo;
    private final DateReservationValidator dateValidator;
    private final ReservationServiceInterface reservationService;
    private final ProduitRepository produitRepo;
    private final MontantReservationCalculService montantCalculService;
    private final UtilisateurRepository utilisateurRepo;
    private final UtilisateurRoleRepository utilisateurRoleRepo;

    // ============================================
    // FONCTIONNALITÉ 1 : MODIFIER UNE SEULE LIGNE
    // ============================================

    @Override
    public ModificationDatesResponseDto modifierUneLigne(
            Long idReservation,
            Long idLigne,
            ModifierUneLigneRequestDto request,
            String username) {

        log.info("🔧 Modification d'une ligne - Réservation: {}, Ligne: {}", idReservation, idLigne);


        // 1️⃣ Récupérer la réservation et la ligne
        Reservation reservation = getReservationOrThrow(idReservation);
        LigneReservation ligne = getLigneReservationOrThrow(idLigne);


        if (username != null) {
            // Récupérer l'utilisateur connecté
            Utilisateur currentUser = utilisateurRepo.findByPseudo(username)
                    .orElseThrow(() -> new CustomException("Utilisateur non trouvé"));

            // Récupérer les rôles via le repository (évite lazy loading)
            List<UtilisateurRole> utilisateurRoles =
                    utilisateurRoleRepo.findByUtilisateurIdUtilisateur(currentUser.getIdUtilisateur());

            // Vérifier si l'utilisateur a le rôle ADMIN ou MANAGER
            boolean isAdmin = utilisateurRoles.stream()
                    .anyMatch(ur -> {
                        String roleName = ur.getRole().getNom();
                        return "ADMIN".equals(roleName) || "MANAGER".equals(roleName);
                    });

            // Si ce n'est pas un admin, vérifier que c'est bien sa réservation
            if (!isAdmin && !username.equals(reservation.getUtilisateur().getPseudo())) {
                throw new CustomException("Vous ne pouvez modifier que vos propres réservations");
            }
        }

        // Vérifier que la ligne appartient bien à cette réservation
        if (!ligne.getReservation().getIdReservation().equals(idReservation)) {
            throw new CustomException("La ligne " + idLigne + " n'appartient pas à la réservation " + idReservation);
        }

        // Sauvegarder les anciennes dates
        LocalDate ancienneDateDebut = ligne.getDateDebut();
        LocalDate ancienneDateFin = ligne.getDateFin();

        // 2️⃣ Valider les nouvelles dates
        dateValidator.validerPeriodeReservation(
                request.getNouvelleDateDebut(),
                request.getNouvelleDateFin(),
                "Modification ligne #" + idLigne
        );

        // 3️⃣ Vérifier la disponibilité sur la nouvelle période
        verifierDisponibilitePourLigne(
                ligne,
                request.getNouvelleDateDebut(),
                request.getNouvelleDateFin(),
                idReservation
        );

        // 4️⃣ Mettre à jour les dates de la ligne
        ligne.setDateDebut(request.getNouvelleDateDebut());
        ligne.setDateFin(request.getNouvelleDateFin());
        ligneReservationRepo.save(ligne);

        log.info("✅ Dates de la ligne {} mises à jour: {} -> {}",
                idLigne, ancienneDateDebut, request.getNouvelleDateDebut());

        // Sauvegarder les anciennes valeurs
        LocalDate ancienneDateDebutRes = reservation.getDateDebut();
        LocalDate ancienneDateFinRes = reservation.getDateFin();

        double ancienMontantTotal = reservation.getMontantTotal() != null ? reservation.getMontantTotal() : 0.0;

        // 5️⃣ Recalculer les dates de la réservation
        recalculerDatesReservation(reservation);

        // 6️⃣ 💰 RECALCULER LE MONTANT TOTAL 💰
        double nouveauMontantTotal = montantCalculService.recalculerEtMettreAJourMontantTotal(reservation);
        reservationRepo.save(reservation);

        // 7️⃣ Ajouter un commentaire d'historique
        String commentaire = String.format(
                "[%s] Modification ligne #%d (%s):%n Dates changées de %s→%s vers %s→%s.%n Motif: %s.%n Responsable: %s%n",
                LocalDateTime.now(),
                idLigne,
                ligne.getProduit().getNomProduit(),
                ancienneDateDebut,
                ancienneDateFin,
                request.getNouvelleDateDebut(),
                request.getNouvelleDateFin(),
                request.getMotif() != null ? request.getMotif() : "Non spécifié",
                username
        );
        ajouterCommentaireHistorique(reservation, commentaire);

        // 8️⃣ Construire la réponse
        return construireReponse(
                reservation,
                ancienneDateDebutRes,
                ancienneDateFinRes,
                List.of(construireDetailLigneModifiee(
                        ligne,
                        ancienneDateDebut,
                        ancienneDateFin,
                        request.getNouvelleDateDebut(),
                        request.getNouvelleDateFin()
                )),
                String.format("Ligne #%d modifiée avec succès.%n Montant : %.2fDT → %.2fDT%n",
                        idLigne, ancienMontantTotal, nouveauMontantTotal),
                ancienMontantTotal,
                nouveauMontantTotal
        );
    }

    // ============================================
    // FONCTIONNALITÉ 2 : DÉCALER TOUTES LES LIGNES
    // ============================================

    @Override
    public ModificationDatesResponseDto decalerToutesLesLignes(
            Long idReservation,
            DecalerToutesLignesRequestDto request,
            String username) {

        log.info("🔧 Décalage de toutes les lignes - Réservation: {}, Décalage: {} jours",
                idReservation, request.getNombreJours());

        // 1️⃣ Récupérer la réservation
        Reservation reservation = getReservationOrThrow(idReservation);
        List<LigneReservation> lignes = new ArrayList<>(reservation.getLigneReservations());

        if (lignes.isEmpty()) {
            throw new CustomException("Aucune ligne trouvée pour la réservation " + idReservation);
        }

        // Sauvegarder les anciennes dates
        LocalDate ancienneDateDebutRes = reservation.getDateDebut();
        LocalDate ancienneDateFinRes = reservation.getDateFin();
        double montantTotal = reservation.getMontantTotal() != null ? reservation.getMontantTotal() : 0.0;

        List<ModificationDatesResponseDto.DetailLigneModifiee> detailsLignes = new ArrayList<>();

        // 2️⃣ Décaler chaque ligne
        for (LigneReservation ligne : lignes) {
            LocalDate ancienneDateDebut = ligne.getDateDebut();
            LocalDate ancienneDateFin = ligne.getDateFin();
            LocalDate nouvelleDateDebut;
            LocalDate nouvelleDateFin;
            if(request.getNombreJours()<0){
                 nouvelleDateDebut = ancienneDateDebut.minusDays(Math.abs(request.getNombreJours()));
                 nouvelleDateFin = ancienneDateFin.minusDays(Math.abs(request.getNombreJours()));
            }else {
                 nouvelleDateDebut = ancienneDateDebut.plusDays(request.getNombreJours());
                 nouvelleDateFin = ancienneDateFin.plusDays(request.getNombreJours());
            }
            // Valider les nouvelles dates
            dateValidator.validerPeriodeReservation(
                    nouvelleDateDebut,
                    nouvelleDateFin,
                    "Décalage ligne #" + ligne.getIdLigneReservation()
            );

            // Vérifier la disponibilité
            verifierDisponibilitePourLigne(ligne, nouvelleDateDebut, nouvelleDateFin, idReservation);

            // Mettre à jour
            ligne.setDateDebut(nouvelleDateDebut);
            ligne.setDateFin(nouvelleDateFin);
            ligneReservationRepo.save(ligne);

            log.info("✅ Ligne {} décalée: {} -> {}",
                    ligne.getIdLigneReservation(), ancienneDateDebut, nouvelleDateDebut);

            detailsLignes.add(construireDetailLigneModifiee(
                    ligne, ancienneDateDebut, ancienneDateFin, nouvelleDateDebut, nouvelleDateFin
            ));
        }

        // 3️⃣ Recalculer les dates de la réservation
        recalculerDatesReservation(reservation);

        // 💰 PAS DE RECALCUL DES MONTANTS (durée identique)
        log.info("💰 Décalage uniquement : montant total inchangé = {}DT", montantTotal);

        // 4️⃣ Ajouter un commentaire d'historique
        String commentaire = String.format(
                "[%s] Décalage global de %+d jours pour toutes les lignes (%d produits).%n " +
                        "Montant inchangé: %.2fDT.%n Motif: %s.%n responsable : %s%n",
                LocalDateTime.now(),
                request.getNombreJours(),
                lignes.size(),
                montantTotal,
                request.getMotif(),
                username
        );
        ajouterCommentaireHistorique(reservation, commentaire);

        // 5️⃣ Construire la réponse
        return construireReponse(
                reservation,
                ancienneDateDebutRes,
                ancienneDateFinRes,
                detailsLignes,
                String.format("Toutes les lignes (%d) décalées de %+d jours.%n Montant inchangé: %.2fDT.%n",
                        lignes.size(), request.getNombreJours(), montantTotal),
                montantTotal,
                montantTotal  // Montant identique
        );
    }

    // ============================================
    // FONCTIONNALITÉ 3 : MODIFIER PLUSIEURS LIGNES SPÉCIFIQUES
    // ============================================

    @Override
    public ModificationDatesResponseDto modifierPlusieurLignes(
            Long idReservation,
            ModifierPlusieurLignesRequestDto request,
            String username) {

        log.info("🔧 Modification de plusieurs lignes - Réservation: {}, Nombre: {}",
                idReservation, request.getModifications().size());

        // 1️⃣ Récupérer la réservation
        Reservation reservation = getReservationOrThrow(idReservation);

        // Sauvegarder les anciennes dates
        LocalDate ancienneDateDebutRes = reservation.getDateDebut();
        LocalDate ancienneDateFinRes = reservation.getDateFin();
        double ancienMontantTotal = reservation.getMontantTotal() != null ? reservation.getMontantTotal() : 0.0;


        List<ModificationDatesResponseDto.DetailLigneModifiee> detailsLignes = new ArrayList<>();

        // 2️⃣ Traiter chaque modification
        for (ModifierPlusieurLignesRequestDto.ModificationLigneDto modif : request.getModifications()) {
            LigneReservation ligne = getLigneReservationOrThrow(modif.getIdLigne());

            // Vérifier que la ligne appartient à cette réservation
            if (!ligne.getReservation().getIdReservation().equals(idReservation)) {
                throw new CustomException("La ligne " + modif.getIdLigne() +
                        " n'appartient pas à la réservation " + idReservation);
            }

            LocalDate ancienneDateDebut = ligne.getDateDebut();
            LocalDate ancienneDateFin = ligne.getDateFin();

            // Valider les nouvelles dates
            dateValidator.validerPeriodeReservation(
                    modif.getNouvelleDateDebut(),
                    modif.getNouvelleDateFin(),
                    "Modification ligne #" + modif.getIdLigne()
            );

            // Vérifier la disponibilité
            verifierDisponibilitePourLigne(
                    ligne,
                    modif.getNouvelleDateDebut(),
                    modif.getNouvelleDateFin(),
                    idReservation
            );

            // Mettre à jour
            ligne.setDateDebut(modif.getNouvelleDateDebut());
            ligne.setDateFin(modif.getNouvelleDateFin());
            ligneReservationRepo.save(ligne);

            log.info("✅ Ligne {} modifiée: {} -> {}",
                    modif.getIdLigne(), ancienneDateDebut, modif.getNouvelleDateDebut());

            detailsLignes.add(construireDetailLigneModifiee(
                    ligne, ancienneDateDebut, ancienneDateFin,
                    modif.getNouvelleDateDebut(), modif.getNouvelleDateFin()
            ));
        }

        // 3️⃣ Recalculer les dates de la réservation
        recalculerDatesReservation(reservation);

        // 4️⃣ 💰 RECALCULER LE MONTANT TOTAL 💰
        double nouveauMontantTotal = montantCalculService.recalculerEtMettreAJourMontantTotal(reservation);
        reservationRepo.save(reservation);

        // 5️⃣ Ajouter un commentaire d'historique AVEC changement de montant
        String commentaire = String.format(
                "[%s] Modification de %d lignes spécifiques.%n Montant : %.2fDT → %.2fDT (différence: %+.2fDT).%n Motif: %s.%n",
                LocalDateTime.now(),
                request.getModifications().size(),
                ancienMontantTotal,
                nouveauMontantTotal,
                nouveauMontantTotal - ancienMontantTotal,
                request.getMotif() != null ? request.getMotif() : "Non spécifié"
        );
        ajouterCommentaireHistorique(reservation, commentaire);

        // 6️⃣ Construire la réponse
        return construireReponse(
                reservation,
                ancienneDateDebutRes,
                ancienneDateFinRes,
                detailsLignes,
                String.format("%d lignes modifiées.%n Montant : %.2fDT → %.2fDT%n",
                        request.getModifications().size(), ancienMontantTotal, nouveauMontantTotal),
                ancienMontantTotal,
                nouveauMontantTotal
        );
    }

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * 🔄 Recalculer les dates de la réservation en fonction des lignes
     * dateDebut = MIN(toutes les dates de début des lignes)
     * dateFin = MAX(toutes les dates de fin des lignes)
     */
    private void recalculerDatesReservation(Reservation reservation) {
        List<LigneReservation> lignes = new ArrayList<>(reservation.getLigneReservations());

        if (lignes.isEmpty()) {
            log.warn("⚠️ Aucune ligne trouvée pour recalculer les dates");
            return;
        }

        LocalDate minDebut = lignes.stream()
                .map(LigneReservation::getDateDebut)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new CustomException("Impossible de calculer la date de début"));

        LocalDate maxFin = lignes.stream()
                .map(LigneReservation::getDateFin)
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new CustomException("Impossible de calculer la date de fin"));

        log.info("🔄 Recalcul des dates de réservation: {} -> {}", minDebut, maxFin);

        reservation.setDateDebut(minDebut);
        reservation.setDateFin(maxFin);
        reservationRepo.save(reservation);

        log.info("✅ Dates de réservation recalculées: {} au {}", minDebut, maxFin);
    }

    /**
     * ✅ Vérifier la disponibilité d'une ligne sur une nouvelle période
     */
    private void verifierDisponibilitePourLigne(
            LigneReservation ligne,
            LocalDate nouvelleDateDebut,
            LocalDate nouvelleDateFin,
            Long reservationExclue) {

        Produit produit = ligne.getProduit();

        log.debug("🔍 Vérification disponibilité - Produit: {} ({}), Période: {} -> {}",
                produit.getNomProduit(), produit.getTypeProduit(), nouvelleDateDebut, nouvelleDateFin);

        if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
            // Produit quantitatif
            verifierDisponibiliteQuantitative(
                    produit.getIdProduit(),
                    ligne.getQuantite(),
                    nouvelleDateDebut,
                    nouvelleDateFin,
                    reservationExclue
            );
        } else {
            // Produit avec référence (instances)
            verifierDisponibiliteInstances(
                    ligne,
                    nouvelleDateDebut,
                    nouvelleDateFin,
                    reservationExclue
            );
        }
    }

    /**
     * Vérifier disponibilité pour produit quantitatif
     */
    private void verifierDisponibiliteQuantitative(
            Long idProduit,
            Integer quantiteDemandee,
            LocalDate dateDebut,
            LocalDate dateFin,
            Long reservationExclue) {

        // Calculer quantité disponible (en excluant la réservation actuelle)
        int quantiteReservee = ligneReservationRepo
                .findQuantiteReserveeForProduitInPeriodExcludingReservation(
                        idProduit, dateDebut, dateFin, reservationExclue
                );

        // Récupérer le produit
        Produit produit = produitRepo.findById(idProduit)
                .orElseThrow(() -> new CustomException("Produit introuvable"));


        int quantiteTotale = produit.getQuantiteInitial();
        int quantiteDisponible = quantiteTotale - quantiteReservee;

        log.debug("📊 Disponibilité quantitative - Total: {}, Réservée: {}, Disponible: {}, Demandée: {}",
                quantiteTotale, quantiteReservee, quantiteDisponible, quantiteDemandee);

        if (quantiteDisponible < quantiteDemandee) {
            throw new CustomException(String.format(
                    "Stock insuffisant pour %s sur la période %s -> %s. " +
                            "Disponible: %d, Demandé: %d",
                    produit.getNomProduit(), dateDebut, dateFin,
                    quantiteDisponible, quantiteDemandee
            ));
        }

        log.info("✅ Disponibilité quantitative OK - {} unités disponibles", quantiteDisponible);
    }

    /**
     * Vérifier disponibilité pour produit avec référence (instances)
     */
    private void verifierDisponibiliteInstances(
            LigneReservation ligne,
            LocalDate dateDebut,
            LocalDate dateFin,
            Long reservationExclue) {

        // Vérifier que chaque instance réservée est disponible sur la nouvelle période
        ligne.getInstancesReservees().forEach(instance -> {
            long count = ligneReservationRepo
                    .countReservationsForInstanceInPeriodExcludingReservation(
                            instance.getIdInstance(),
                            dateDebut,
                            dateFin,
                            reservationExclue
                    );

            if (count > 0) {
                throw new CustomException(String.format(
                        "Instance %s du produit %s n'est pas disponible sur la période %s -> %s",
                        instance.getNumeroSerie(),
                        ligne.getProduit().getNomProduit(),
                        dateDebut,
                        dateFin
                ));
            }
        });

        log.info("✅ Toutes les instances sont disponibles");
    }

    /**
     * Construire un commentaire avec changement de montant
     */
    private String construireCommentaireModificationAvecMontant(
            Long idLigne,
            String nomProduit,
            LocalDate ancienneDateDebut,
            LocalDate ancienneDateFin,
            LocalDate nouvelleDateDebut,
            LocalDate nouvelleDateFin,
            double ancienMontant,
            double nouveauMontant,
            String motif) {

        double difference = nouveauMontant - ancienMontant;
        String symbole = difference >= 0 ? "+" : "";

        return String.format(
                "[%s] Modification ligne #%d (%s): Dates %s→%s vers %s→%s. " +
                        "💰 Montant: %.2fDT → %.2fDT (%s%.2fDT). Motif: %s",
                LocalDate.now(),
                idLigne,
                nomProduit,
                ancienneDateDebut,
                ancienneDateFin,
                nouvelleDateDebut,
                nouvelleDateFin,
                ancienMontant,
                nouveauMontant,
                symbole,
                difference,
                motif != null ? motif : "Non spécifié"
        );
    }
    /**
     * Ajouter un commentaire d'historique
     */
    private void ajouterCommentaireHistorique(Reservation reservation, String commentaire) {
        String commentaireActuel = reservation.getCommentaireAdmin();
        reservation.setCommentaireAdmin(
                (commentaireActuel != null ? commentaireActuel + "\n" : "") + commentaire
        );
        reservationRepo.save(reservation);
    }

    /**
     * Construire le détail d'une ligne modifiée
     */
    private ModificationDatesResponseDto.DetailLigneModifiee construireDetailLigneModifiee(
            LigneReservation ligne,
            LocalDate ancienneDateDebut,
            LocalDate ancienneDateFin,
            LocalDate nouvelleDateDebut,
            LocalDate nouvelleDateFin) {

        return ModificationDatesResponseDto.DetailLigneModifiee.builder()
                .idLigne(ligne.getIdLigneReservation())
                .nomProduit(ligne.getProduit().getNomProduit())
                .ancienneDateDebut(ancienneDateDebut)
                .ancienneDateFin(ancienneDateFin)
                .nouvelleDateDebut(nouvelleDateDebut)
                .nouvelleDateFin(nouvelleDateFin)
                .joursDifferenceDebut((int) ChronoUnit.DAYS.between(ancienneDateDebut, nouvelleDateDebut))
                .joursDifferenceF((int) ChronoUnit.DAYS.between(ancienneDateFin, nouvelleDateFin))
                .build();
    }

    /**
     * Construire la réponse complète AVEC montants
     */
    private ModificationDatesResponseDto construireReponse(
            Reservation reservation,
            LocalDate ancienneDateDebutRes,
            LocalDate ancienneDateFinRes,
            List<ModificationDatesResponseDto.DetailLigneModifiee> detailsLignes,
            String message,
            double ancienMontant,
            double nouveauMontant) {

        ReservationResponseDto reservationDto = reservationService
                .getReservationById(reservation.getIdReservation());

        return ModificationDatesResponseDto.builder()
                .succes(true)
                .message(message)
                .reservationMiseAJour(reservationDto)
                .ancienneDateDebutReservation(ancienneDateDebutRes)
                .ancienneDateFinReservation(ancienneDateFinRes)
                .nouvelleDateDebutReservation(reservation.getDateDebut())
                .nouvelleDateFinReservation(reservation.getDateFin())
                .lignesModifiees(detailsLignes)
                .ancienMontantTotal(ancienMontant)  // 💰 NOUVEAU
                .nouveauMontantTotal(nouveauMontant)  // 💰 NOUVEAU
                .differenceMontant(nouveauMontant - ancienMontant)  // 💰 NOUVEAU
                .build();
    }

    /**
     * Récupérer une réservation ou lever une exception
     */
    private Reservation getReservationOrThrow(Long idReservation) {
        return reservationRepo.findById(idReservation)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Réservation avec ID " + idReservation + " introuvable"
                ));
    }

    /**
     * Récupérer une ligne de réservation ou lever une exception
     */
    private LigneReservation getLigneReservationOrThrow(Long idLigne) {
        return ligneReservationRepo.findById(idLigne)
                .orElseThrow(() -> new CustomException(
                        "Ligne de réservation avec ID " + idLigne + " introuvable"
                ));
    }
}