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
 * Sprint 4 - Gestion des réservations
 * ==========================================

 * 🎯 RESPONSABILITÉS :
 * - Calculer le nombre de jours de location
 * - Calculer le sous-total d'une ligne
 * - Recalculer le montant total d'une réservation
 * - Gérer les remises

 * 📝 FORMULES :
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
     * 📅 Calculer le nombre de jours entre deux dates (inclusif)
     *
     * @param dateDebut Date de début
     * @param dateFin Date de fin
     * @return Nombre de jours (minimum 1)

     * Exemple :
     * - 08/11 → 10/11 = 3 jours (08, 09, 10)
     * - 08/11 → 08/11 = 1 jour
     */
    public int calculerNombreDeJours(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            log.warn("⚠️ Dates nulles pour le calcul de jours");
            return 1;
        }

        long jours = ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;

        if (jours < 1) {
            log.warn("⚠️ Nombre de jours calculé < 1 : {} (dateDebut: {}, dateFin: {})",
                    jours, dateDebut, dateFin);
            return 1;
        }

        log.debug("📅 Nombre de jours calculé : {} ({} → {})", jours, dateDebut, dateFin);
        return (int) jours;
    }

    /**
     * 💰 Calculer le sous-total d'une ligne de réservation
     *
     * @param ligne Ligne de réservation
     * @return Sous-total (quantité × prixUnitaire × nombreDeJours)

     * IMPORTANT :
     * - prixUnitaire = prix PAR JOUR PAR UNITÉ
     * - Formule : quantité × prixParJour × nombreDeJours

     * Exemple :
     * - 50 chaises × 10DT/jour × 3 jours = 1500DT
     */
    public double calculerSousTotalLigne(LigneReservation ligne) {
        if (ligne == null) {
            log.warn("⚠️ Ligne null pour le calcul du sous-total");
            return 0.0;
        }

        int nombreDeJours = calculerNombreDeJours(ligne.getDateDebut(), ligne.getDateFin());
        double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire() * nombreDeJours;

        log.debug("💰 Sous-total ligne {} ({}): {} unités × {}DT × {} jours = {}DT",
                ligne.getIdLigneReservation(),
                ligne.getProduit() != null ? ligne.getProduit().getNomProduit() : "N/A",
                ligne.getQuantite(),
                ligne.getPrixUnitaire(),
                nombreDeJours,
                sousTotal);

        return sousTotal;
    }

    /**
     * 💰 Recalculer le montant total d'une réservation
     *
     * @param reservation Réservation à recalculer
     * @return Nouveau montant total (somme de tous les sous-totaux)

     * IMPORTANT :
     * - Recalcule TOUTES les lignes
     * - Ne prend PAS en compte les remises (à gérer séparément)
     * - Montant BRUT avant remise
     */
    public double recalculerMontantTotal(Reservation reservation) {
        if (reservation == null) {
            log.warn("⚠️ Réservation null pour le recalcul du montant");
            return 0.0;
        }

        Set<LigneReservation> lignes = reservation.getLigneReservations();

        if (lignes == null || lignes.isEmpty()) {
            log.warn("⚠️ Aucune ligne pour la réservation {}",
                    reservation.getReferenceReservation());
            return 0.0;
        }

        double montantTotal = lignes.stream()
                .mapToDouble(this::calculerSousTotalLigne)
                .sum();

        log.info("💰 Montant total recalculé pour réservation {}: {}DT ({} lignes)",
                reservation.getReferenceReservation(),
                montantTotal,
                lignes.size());

        return montantTotal;
    }

    /**
     * 💰 Recalculer le montant total ET mettre à jour la réservation
     *
     * @param reservation Réservation à mettre à jour
     * @return Ancien montant total (pour comparaison)

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
            log.info("💰 Montant total modifié : {}DT → {}DT (différence: {}DT)",
                    ancienMontant,
                    nouveauMontant,
                    nouveauMontant - ancienMontant);
        }

        return nouveauMontant;
    }

    /**
     * 🎁 Calculer le montant après remise
     *
     * @param montantTotal Montant total brut
     * @param remisePourcentage Remise en pourcentage (ex: 10 pour 10%)
     * @param remiseMontant Remise en montant fixe (ex: 100 pour 100DT)
     * @return Montant final après remise

     * Règle de priorité :
     * 1. Si remiseMontant > 0 : montantTotal - remiseMontant
     * 2. Sinon si remisePourcentage > 0 : montantTotal × (1 - remisePourcentage/100)
     * 3. Sinon : montantTotal
     */
    public double calculerMontantApresRemise(
            double montantTotal,
            Double remisePourcentage,
            Double remiseMontant) {

        // Montant fixe prioritaire
        if (remiseMontant != null && remiseMontant > 0) {
            double montantFinal = Math.max(0, montantTotal - remiseMontant);
            log.debug("🎁 Remise montant fixe : {}DT - {}DT = {}DT",
                    montantTotal, remiseMontant, montantFinal);
            return montantFinal;
        }

        // Sinon pourcentage
        if (remisePourcentage != null && remisePourcentage > 0) {
            double montantRemise = montantTotal * (remisePourcentage / 100.0);
            double montantFinal = montantTotal - montantRemise;
            log.debug("🎁 Remise {} % : {}DT - {}DT = {}DT",
                    remisePourcentage, montantTotal, montantRemise, montantFinal);
            return montantFinal;
        }

        // Aucune remise
        return montantTotal;
    }

    /**
     * 📊 Calculer le détail complet des montants d'une réservation
     *
     * @param reservation Réservation
     * @return Détail des montants (utile pour affichage au client)
     */
    public DetailMontantsDto calculerDetailMontants(Reservation reservation) {
        double montantBrut = recalculerMontantTotal(reservation);

        // Ici, vous pouvez ajouter la logique pour récupérer les remises
        // depuis votre système (si stockées ailleurs)
        Double remisePourcentage = reservation.getRemisePourcentage(); // À adapter selon votre modèle
        Double remiseMontant = reservation.getRemiseMontant();     // À adapter selon votre modèle

        double montantRemise = 0.0;
        if (remiseMontant != null && remiseMontant > 0) {
            montantRemise = remiseMontant;
        } else if (remisePourcentage != null && remisePourcentage > 0) {
            montantRemise = montantBrut * (remisePourcentage / 100.0);
        }

        double montantFinal = montantBrut - montantRemise;

        return DetailMontantsDto.builder()
                .montantBrut(montantBrut)
                .remisePourcentage(remisePourcentage)
                .remiseMontant(remiseMontant)
                .montantRemiseCalcule(montantRemise)
                .montantFinal(montantFinal)
                .nombreLignes(reservation.getLigneReservations().size())
                .build();
    }

    /**
     * DTO pour les détails de montants
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DetailMontantsDto {
        private Double montantBrut;           // Somme des sous-totaux
        private Double remisePourcentage;      // Remise en %
        private Double remiseMontant;          // Remise en montant fixe
        private Double montantRemiseCalcule;   // Montant de la remise appliquée
        private Double montantFinal;           // Montant après remise
        private Integer nombreLignes;          // Nombre de lignes
    }
}