package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.facture.FactureResponseDto;
import tn.weeding.agenceevenementielle.dto.facture.GenererFactureRequestDto;
import tn.weeding.agenceevenementielle.entities.enums.StatutFacture;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;

import java.util.List;

public interface FactureServiceInterface {

    // Génération de factures
    FactureResponseDto genererFacture(GenererFactureRequestDto request, String username);
    FactureResponseDto genererFactureAutomatique(Long idReservation, TypeFacture typeFacture, String username);

    // Consultation
    FactureResponseDto getFactureById(Long idFacture);
    FactureResponseDto getFactureByNumero(String numeroFacture);
    List<FactureResponseDto> getFacturesByReservation(Long idReservation);
    List<FactureResponseDto> getFacturesByClient(Long idClient);
    List<FactureResponseDto> getAllFactures();

    // Filtres
    List<FactureResponseDto> getFacturesByStatut(StatutFacture statut);
    List<FactureResponseDto> getFacturesByType(TypeFacture type);

    // Téléchargement PDF
    byte[] telechargerPdfFacture(Long idFacture);

    // Mise à jour statut
    FactureResponseDto updateStatutFacture(Long idFacture, StatutFacture nouveauStatut, String username);

    // Régénération
    FactureResponseDto regenererPdfFacture(Long idFacture, String username);
}