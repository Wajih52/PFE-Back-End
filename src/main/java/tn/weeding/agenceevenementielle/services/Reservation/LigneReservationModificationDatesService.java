package tn.weeding.agenceevenementielle.services.Reservation;

import tn.weeding.agenceevenementielle.dto.modifDateReservation.DecalerToutesLignesRequestDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModificationDatesResponseDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModifierPlusieurLignesRequestDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.ModifierUneLigneRequestDto;


public interface LigneReservationModificationDatesService {

    /**
     * MODIFIER UNE SEULE LIGNE
     *
     * Cas d'usage :
     * - Client veut garder les chaises 2 jours de plus
     *
     */
    ModificationDatesResponseDto modifierUneLigne(
            Long idReservation,
            Long idLigne,
            ModifierUneLigneRequestDto request,
            String username
    );

    /**
     *  DÉCALER TOUTES LES LIGNES
     *
     * Cas d'usage :
     * - Événement reporté (COVID, météo, etc.)
     * - Client veut avancer/reculer tout
     */
    ModificationDatesResponseDto decalerToutesLesLignes(
            Long idReservation,
            DecalerToutesLignesRequestDto request,
            String username
    );

    /**
     * MODIFIER PLUSIEURS LIGNES SPÉCIFIQUES
     *
     * Cas d'usage :
     * - Réorganisation complète de la logistique
     * - Ajustement fin de plusieurs produits
     */
    ModificationDatesResponseDto modifierPlusieurLignes(
            Long idReservation,
            ModifierPlusieurLignesRequestDto request,
            String username
    );
}