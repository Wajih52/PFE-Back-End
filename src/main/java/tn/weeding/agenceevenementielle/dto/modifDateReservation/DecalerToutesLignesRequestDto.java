package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ==========================================
 * DTO POUR DÉCALER TOUTES LES LIGNES D'UNE RÉSERVATION
 * Fonctionnalité 2 : Décalage global
 * ==========================================
 *
 * 📝 CAS D'USAGE :
 * - Événement reporté (COVID, météo, etc.)
 * - Client veut avancer/reculer tout l'événement
 * - Modification globale de planning
 *
 * ✅ COMPORTEMENT :
 * - Décalage de TOUTES les lignes de X jours
 * - Recalcul automatique des dates de la réservation
 * - Vérification de disponibilité pour toutes les lignes
 *
 * 💡 EXEMPLES :
 * - nombreJours = 7  → Décaler de 7 jours vers le futur
 * - nombreJours = -7 → Avancer de 7 jours (vers le passé)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecalerToutesLignesRequestDto {

    @NotNull(message = "Le nombre de jours est obligatoire")
    private Integer nombreJours;  // +7 pour avancer, -7 pour reculer

    @NotNull(message = "Le motif est obligatoire")
    private String motif;  // Ex: "Mariage reporté d'une semaine"
}