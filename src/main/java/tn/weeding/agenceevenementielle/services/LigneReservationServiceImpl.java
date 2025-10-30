package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.reservation.LigneModificationDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.services.LigneReservationServiceInterface;

import java.util.List;

public class LigneReservationServiceImpl implements LigneReservationServiceInterface {
    @Override
    public LigneReservationResponseDto creerLigneReservation(LigneReservationRequestDto ligneDto, Long idReservation, String username) {
        return null;
    }

    @Override
    public List<LigneReservationResponseDto> getLignesByReservation(Long idReservation) {
        return List.of();
    }

    @Override
    public LigneReservationResponseDto getLigneById(Long idLigne) {
        return null;
    }

    @Override
    public LigneReservationResponseDto modifierLigne(Long idLigne, LigneModificationDto modificationDto, String username) {
        return null;
    }

    @Override
    public void supprimerLigne(Long idLigne, String username) {

    }

    @Override
    public LigneReservationResponseDto assignerInstances(Long idLigne, List<Long> idsInstances, String username) {
        return null;
    }

    @Override
    public void libererInstances(Long idLigne, String username) {

    }

    @Override
    public List<LigneReservationResponseDto> getLignesByProduit(Long idProduit) {
        return List.of();
    }

    @Override
    public List<LigneReservationResponseDto> getLignesSansLivraison() {
        return List.of();
    }

    @Override
    public List<LigneReservationResponseDto> getLivraisonsAujourdhui() {
        return List.of();
    }

    @Override
    public List<LigneReservationResponseDto> getRetoursAujourdhui() {
        return List.of();
    }

    @Override
    public List<LigneReservationResponseDto> getRetoursEnRetard() {
        return List.of();
    }
}
