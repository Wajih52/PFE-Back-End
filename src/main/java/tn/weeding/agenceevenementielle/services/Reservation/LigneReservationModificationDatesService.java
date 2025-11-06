package tn.weeding.agenceevenementielle.services.Reservation;

import tn.weeding.agenceevenementielle.dto.modifDateReservation.DecalerToutesLignesRequestDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModificationDatesResponseDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModifierPlusieurLignesRequestDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModifierUneLigneRequestDto;

/**
 * ==========================================
 * INTERFACE SERVICE - NOUVELLES MÉTHODES POUR MODIFICATION DE DATES
 * ==========================================
 *
 * 3 fonctionnalités principales :
 * 1️⃣ Modifier UNE ligne (granularité fine)
 * 2️⃣ Décaler TOUTES les lignes (décalage global)
 * 3️⃣ Modifier plusieurs lignes spécifiques (flexibilité maximale)
 */
public interface LigneReservationModificationDatesService {

    /**
     * 🎯 FONCTIONNALITÉ 1 : MODIFIER UNE SEULE LIGNE
     *
     * Cas d'usage :
     * - Client veut garder les chaises 2 jours de plus
     * - Ajuster juste l'éclairage car montage plus tôt
     *
     * @param idReservation ID de la réservation
     * @param idLigne ID de la ligne à modifier
     * @param request Nouvelles dates pour cette ligne
     * @param username Utilisateur effectuant la modification
     * @return Réponse avec réservation mise à jour
     */
    ModificationDatesResponseDto modifierUneLigne(
            Long idReservation,
            Long idLigne,
            ModifierUneLigneRequestDto request,
            String username
    );

    /**
     * 🎯 FONCTIONNALITÉ 2 : DÉCALER TOUTES LES LIGNES
     *
     * Cas d'usage :
     * - Événement reporté (COVID, météo, etc.)
     * - Client veut avancer/reculer tout
     *
     * @param idReservation ID de la réservation
     * @param request Nombre de jours de décalage et motif
     * @param username Utilisateur effectuant la modification
     * @return Réponse avec réservation mise à jour
     */
    ModificationDatesResponseDto decalerToutesLesLignes(
            Long idReservation,
            DecalerToutesLignesRequestDto request,
            String username
    );

    /**
     * 🎯 FONCTIONNALITÉ 3 : MODIFIER PLUSIEURS LIGNES SPÉCIFIQUES
     *
     * Cas d'usage :
     * - Réorganisation complète de la logistique
     * - Ajustement fin de plusieurs produits
     *
     * @param idReservation ID de la réservation
     * @param request Liste des modifications à effectuer
     * @param username Utilisateur effectuant la modification
     * @return Réponse avec réservation mise à jour
     */
    ModificationDatesResponseDto modifierPlusieurLignes(
            Long idReservation,
            ModifierPlusieurLignesRequestDto request,
            String username
    );
}