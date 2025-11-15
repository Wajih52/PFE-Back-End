package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.paiement.PaiementRequestDto;
import tn.weeding.agenceevenementielle.dto.paiement.PaiementResponseDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutPaiement;

import java.time.LocalDateTime;
import java.util.List;

public interface PaiementServiceInterface {
    PaiementResponseDto creerPaiement(PaiementRequestDto dto, String username);
    PaiementResponseDto validerPaiement(Long idPaiement, String username);
    PaiementResponseDto refuserPaiement(Long idPaiement, String motifRefus, String username);
    PaiementResponseDto getPaiementById(Long idPaiement);
    PaiementResponseDto getPaiementByCode(String codePaiement);
    List<PaiementResponseDto> getPaiementsByReservation(Long idReservation);
    List<PaiementResponseDto> getPaiementsByClient(Long idClient);
    List<PaiementResponseDto> getAllPaiements();
    List<PaiementResponseDto> getPaiementsByStatut(StatutPaiement statut);
    List<PaiementResponseDto> getPaiementsEnAttente();
    List<PaiementResponseDto> getPaiementsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin);
    Double calculerMontantPaye(Long idReservation);
    void supprimerPaiement(Long idPaiement, String username);
    Boolean isReservationPayeeCompletement(Long idReservation);
}
