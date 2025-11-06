package tn.weeding.agenceevenementielle.dto.modifDateReservation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ==========================================
 * DTO POUR MODIFIER UNE SEULE LIGNE DE RÉSERVATION
 * Fonctionnalité 1 : Granularité fine
 * ==========================================
 *
 * 📝 CAS D'USAGE :
 * - Client veut garder les chaises 2 jours de plus
 * - Ajuster juste l'éclairage car montage plus tôt
 * - Modifier les dates d'un seul produit
 *
 * ✅ COMPORTEMENT :
 * - Mise à jour de CETTE ligne uniquement
 * - Recalcul automatique des dates de la réservation (dateDebut = min, dateFin = max)
 * - Vérification de disponibilité pour cette ligne
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierUneLigneRequestDto {

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate nouvelleDateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate nouvelleDateFin;

    /**
     * Motif optionnel pour l'historique
     */
    private String motif;
}