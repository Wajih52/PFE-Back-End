package tn.weeding.agenceevenementielle.services.Reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.entities.LigneReservation;
import tn.weeding.agenceevenementielle.entities.Reservation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * ==========================================
 * SERVICE DE CALCUL DES MONTANTS DE RÉSERVATION
 * ==========================================

 *  RESPONSABILITÉS :
 * - Calculer le nombre de jours de location
 * - Calculer le sous-total d'une ligne
 * - Recalculer le montant total d'une réservation
 * - Gérer les remises

 *  FORMULES :
 * - Nombre de jours = (dateFin - dateDebut) + 1
 * - Sous-total ligne = quantité × prixUnitaire × nombreDeJours
 * - Montant total = SUM(tous les sous-totaux)
 * - Montant avec remise = montantTotal - remise
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MontantReservationCalculService {

    /**
     * Calculer le nombre de jours entre deux dates
     *
     * Exemple :
     * - 08/11 → 10/11 = 3 jours (08, 09, 10)
     * - 08/11 → 08/11 = 1 jour
     */
    public int calculerNombreDeJours(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            log.warn("Dates nulles pour le calcul de jours");
            return 1;
        }

        long jours = ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;

        if (jours < 1) {
            log.warn("Nombre de jours calculé < 1 : {} (dateDebut: {}, dateFin: {})",
                    jours, dateDebut, dateFin);
            return 1;
        }

        log.debug(" Nombre de jours calculé : {} ({} → {})", jours, dateDebut, dateFin);
        return (int) jours;
    }

    /**
     * Calculer le sous-total d'une ligne de réservation
     *
     * notes :
     * - prixUnitaire = prix PAR JOUR PAR UNITÉ
     * - Formule : quantité × prixParJour × nombreDeJours

     * Exemple :
     * - 50 chaises × 1DT/jour × 3 jours = 150DT
     */
    public double calculerSousTotalLigne(LigneReservation ligne) {
        if (ligne == null) {
            log.warn("Ligne null pour le calcul du sous-total");
            return 0.0;
        }

        int nombreDeJours = calculerNombreDeJours(ligne.getDateDebut(), ligne.getDateFin());
        double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire() * nombreDeJours;

        log.debug("Sous-total ligne {} ({}): {} unités × {}DT × {} jours = {}DT",
                ligne.getIdLigneReservation(),
                ligne.getProduit() != null ? ligne.getProduit().getNomProduit() : "N/A",
                ligne.getQuantite(),
                ligne.getPrixUnitaire(),
                nombreDeJours,
                sousTotal);

        return sousTotal;
    }

    /**
     * Recalculer le montant total d'une réservation
     *

     * IMPORTANT :
     * - Recalcule TOUTES les lignes
     * - Ne prend PAS en compte les remises
     * - Montant BRUT avant remise
     */
    public double recalculerMontantTotal(Reservation reservation) {
        if (reservation == null) {
            log.warn("Réservation null pour le recalcul du montant");
            return 0.0;
        }

        Set<LigneReservation> lignes = reservation.getLigneReservations();

        if (lignes == null || lignes.isEmpty()) {
            log.warn("Aucune ligne pour la réservation {}",
                    reservation.getReferenceReservation());
            return 0.0;
        }

        double montantTotal = lignes.stream()
                .mapToDouble(this::calculerSousTotalLigne)
                .sum();

        log.info("Montant total recalculé pour réservation {}: {}DT ({} lignes)",
                reservation.getReferenceReservation(),
                montantTotal,
                lignes.size());

        return montantTotal;
    }

    /**
     * Recalculer le montant total ET mettre à jour la réservation
     *
     * Cette méthode :
     * 1. Sauvegarde l'ancien montant
     * 2. Calcule le nouveau montant
     * 3. Met à jour reservation.montantTotal
     * 4. Retourne l'ancien montant pour traçabilité
     */
    public double recalculerEtMettreAJourMontantTotal(Reservation reservation) {
        double ancienMontant = reservation.getMontantTotal() != null ?
                reservation.getMontantTotal() : 0.0;

        double nouveauMontant = recalculerMontantTotal(reservation);

        if(nouveauMontant > ancienMontant ) {
            if (reservation.getRemisePourcentage() != null && reservation.getRemisePourcentage() > 0) {
                double remise = nouveauMontant * (reservation.getRemisePourcentage() / 100.0);
                nouveauMontant -= remise;
            }
            if (reservation.getRemiseMontant() != null && reservation.getRemiseMontant() > 0) {
                nouveauMontant -= reservation.getRemiseMontant();
            }
            reservation.setMontantTotal(nouveauMontant);
        }else {
            reservation.setMontantTotal(nouveauMontant);
        }
        if (Math.abs(ancienMontant - nouveauMontant) > 0.01) {
            log.info("Montant total modifié : {}DT → {}DT (différence: {}DT)",
                    ancienMontant,
                    nouveauMontant,
                    nouveauMontant - ancienMontant);
        }

        return nouveauMontant;
    }
}