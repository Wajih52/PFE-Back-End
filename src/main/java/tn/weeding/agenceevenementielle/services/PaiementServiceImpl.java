package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.paiement.PaiementRequestDto;
import tn.weeding.agenceevenementielle.dto.paiement.PaiementResponseDto;
import tn.weeding.agenceevenementielle.entities.Facture;
import tn.weeding.agenceevenementielle.entities.Paiement;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.FactureRepository;
import tn.weeding.agenceevenementielle.repository.LivraisonRepository;
import tn.weeding.agenceevenementielle.repository.PaiementRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaiementServiceImpl implements PaiementServiceInterface{

    private final PaiementRepository paiementRepository;
    private final ReservationRepository reservationRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final FactureRepository factureRepository ;


    @Override
    public PaiementResponseDto creerPaiement(PaiementRequestDto dto, String username) {
        log.info("💳 Création d'un paiement pour la réservation ID: {} par {}", dto.getIdReservation(), username);

        Reservation reservation = reservationRepository.findById(dto.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable avec l'ID: " + dto.getIdReservation()));

        if (reservation.getStatutReservation() != StatutReservation.CONFIRME &&
                reservation.getStatutReservation() != StatutReservation.TERMINE) {
            throw new CustomException("Impossible d'ajouter un paiement à une réservation non confirmée ou annulée");
        }

        Double montantDejaPayeValide = calculerMontantPaye(dto.getIdReservation());
        Double montantRestant = reservation.getMontantTotal() - montantDejaPayeValide;

        if (dto.getMontantPaiement() > montantRestant + 0.01) {
            throw new CustomException(String.format(
                    "Le montant du paiement (%.2f TND) dépasse le montant restant à payer (%.2f TND)",
                    dto.getMontantPaiement(), montantRestant));
        }

        String codePaiement = codeGeneratorService.generatePaiementCode();

        Paiement paiement = Paiement.builder()
                .codePaiement(codePaiement)
                .datePaiement(LocalDateTime.now())
                .montantPaiement(dto.getMontantPaiement())
                .modePaiement(dto.getModePaiement())
                .statutPaiement(StatutPaiement.EN_ATTENTE)
                .descriptionPaiement(dto.getDescriptionPaiement())
                .referenceExterne(dto.getReferenceExterne())
                .reservation(reservation)
                .build();

        Paiement savedPaiement = paiementRepository.save(paiement);

        log.info("✅ Paiement créé avec succès: {} - Montant: {} TND", codePaiement, dto.getMontantPaiement());

        return convertToResponseDto(savedPaiement, montantDejaPayeValide);
    }

    @Override
    public PaiementResponseDto validerPaiement(Long idPaiement, String username) {
        log.info("✅ Validation du paiement ID: {} par {}", idPaiement, username);

        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        if (paiement.getStatutPaiement() != StatutPaiement.EN_ATTENTE) {
            throw new CustomException("Seuls les paiements en attente peuvent être validés");
        }

        paiement.setStatutPaiement(StatutPaiement.VALIDE);
        paiement.setValidePar(username);
        paiement.setDateValidation(LocalDateTime.now());

        Paiement savedPaiement = paiementRepository.save(paiement);

        mettreAJourMontantPayeReservation(paiement.getReservation().getIdReservation());
        verifierEtMettreAJourStatutReservation(paiement.getReservation().getIdReservation());

        log.info("✅ Paiement validé: {} - {} TND", paiement.getCodePaiement(), paiement.getMontantPaiement());

        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation()) - paiement.getMontantPaiement();

        return convertToResponseDto(savedPaiement, montantDejaPayeAvant);
    }

    @Override
    public PaiementResponseDto refuserPaiement(Long idPaiement, String motifRefus, String username) {
        log.info("❌ Refus du paiement ID: {} par {}", idPaiement, username);

        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        if (paiement.getStatutPaiement() != StatutPaiement.EN_ATTENTE) {
            throw new CustomException("Seuls les paiements en attente peuvent être refusés");
        }

        paiement.setStatutPaiement(StatutPaiement.REFUSE);
        paiement.setValidePar(username);
        paiement.setDateValidation(LocalDateTime.now());
        paiement.setMotifRefus(motifRefus);

        Paiement savedPaiement = paiementRepository.save(paiement);

        log.info("❌ Paiement refusé: {}", paiement.getCodePaiement());

        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation());

        return convertToResponseDto(savedPaiement, montantDejaPayeAvant);
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementResponseDto getPaiementById(Long idPaiement) {
        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation());

        if (paiement.getStatutPaiement() == StatutPaiement.VALIDE) {
            montantDejaPayeAvant -= paiement.getMontantPaiement();
        }

        return convertToResponseDto(paiement, montantDejaPayeAvant);
    }

    @Override
    @Transactional(readOnly = true)
    public PaiementResponseDto getPaiementByCode(String codePaiement) {
        Paiement paiement = paiementRepository.findByCodePaiement(codePaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec le code: " + codePaiement));

        Double montantDejaPayeAvant = calculerMontantPaye(paiement.getReservation().getIdReservation());

        if (paiement.getStatutPaiement() == StatutPaiement.VALIDE) {
            montantDejaPayeAvant -= paiement.getMontantPaiement();
        }

        return convertToResponseDto(paiement, montantDejaPayeAvant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByReservation(Long idReservation) {
        if (!reservationRepository.existsById(idReservation)) {
            throw new CustomException("Réservation introuvable avec l'ID: " + idReservation);
        }

        List<Paiement> paiements = paiementRepository.findByReservationIdReservationOrderByDatePaiementDesc(idReservation);

        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByClient(Long idClient) {
        List<Paiement> paiements = paiementRepository.findByClientIdOrderByDatePaiementDesc(idClient);
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getAllPaiements() {
        List<Paiement> paiements = paiementRepository.findAll();
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByStatut(StatutPaiement statut) {
        List<Paiement> paiements = paiementRepository.findByStatutPaiementOrderByDatePaiementDesc(statut);
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsEnAttente() {
        List<Paiement> paiements = paiementRepository.findPaiementsEnAttente();
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaiementResponseDto> getPaiementsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Paiement> paiements = paiementRepository.findPaiementsByPeriode(dateDebut, dateFin);
        return paiements.stream()
                .map(p -> {
                    Double montantDejaPayeAvant = calculerMontantPaye(p.getReservation().getIdReservation());

                    if (p.getStatutPaiement() == StatutPaiement.VALIDE) {
                        montantDejaPayeAvant -= p.getMontantPaiement();
                    }

                    return convertToResponseDto(p, montantDejaPayeAvant);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculerMontantPaye(Long idReservation) {
        Double montant = paiementRepository.calculerMontantPayeValidePourReservation(idReservation);
        return montant != null ? montant : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isReservationPayeeCompletement(Long idReservation) {
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable avec l'ID: " + idReservation));

        Double montantPaye = calculerMontantPaye(idReservation);
        Double montantTotal = reservation.getMontantTotal();

        return montantPaye >= (montantTotal - 0.01);
    }

    @Override
    public void supprimerPaiement(Long idPaiement, String username) {
        log.info("🗑️ Suppression du paiement ID: {} par {}", idPaiement, username);

        Paiement paiement = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new CustomException("Paiement introuvable avec l'ID: " + idPaiement));

        if (paiement.getStatutPaiement() == StatutPaiement.VALIDE) {
            throw new CustomException("Impossible de supprimer un paiement validé. Contactez un administrateur pour un remboursement.");
        }

        paiement.setStatutPaiement(StatutPaiement.REFUSE);
        paiement.setDescriptionPaiement(
                (paiement.getDescriptionPaiement() != null ? paiement.getDescriptionPaiement() + " | " : "") +
                        "Paiement supprimé par: " + username
        );

        paiementRepository.save(paiement);

        log.info("✅ Paiement supprimé (annulé): {}", paiement.getCodePaiement());
    }

    private void mettreAJourMontantPayeReservation(Long idReservation) {
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        Double montantPaye = calculerMontantPaye(idReservation);
        reservation.setMontantPaye(montantPaye);

        reservationRepository.save(reservation);

        log.info("💰 Montant payé mis à jour pour réservation {}: {} TND", reservation.getReferenceReservation(), montantPaye);
    }

    private void verifierEtMettreAJourStatutReservation(Long idReservation) {
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        Boolean paiementComplet = isReservationPayeeCompletement(idReservation);

        if (paiementComplet) {
            log.info("✅ Paiement complet pour la réservation: {}", reservation.getReferenceReservation());

            if (reservation.getStatutPaiement() == StatutPaiementRes.EN_ATTENTE_PAIEMENT||
            reservation.getStatutPaiement()==StatutPaiementRes.PARTIELLEMENT_PAYE) {
                reservation.setStatutPaiement(StatutPaiementRes.TOTALEMENT_PAYE);
                Optional<Facture> facture =factureRepository.findByReservation_IdReservationAndTypeFacture(reservation.getIdReservation(), TypeFacture.FINALE)
                        .stream().findFirst();
                if (facture.isPresent()){
                    facture.get().setStatutFacture(StatutFacture.PAYEE);
                    factureRepository.save(facture.get());
                }
                reservationRepository.save(reservation);

                log.info("📝 Statut Paiement réservation mis à jour: EN_ATTENTE_PAIEMENT → CONFIRME");
            }
        }else{
            reservation.setStatutPaiement(StatutPaiementRes.PARTIELLEMENT_PAYE);
            reservationRepository.save(reservation);
        }
    }

    private PaiementResponseDto convertToResponseDto(Paiement paiement, Double montantDejaPayeAvant) {
        Reservation reservation = paiement.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        Double montantTotalReservation = reservation.getMontantTotal();
        Double montantRestantApres = montantTotalReservation -
                (montantDejaPayeAvant +
                        (paiement.getStatutPaiement() == StatutPaiement.VALIDE ?
                                paiement.getMontantPaiement() : 0));

        Boolean paiementComplet = montantRestantApres <= 0.01;

        return PaiementResponseDto.builder()
                .idPaiement(paiement.getIdPaiement())
                .codePaiement(paiement.getCodePaiement())
                .idReservation(reservation.getIdReservation())
                .referenceReservation(reservation.getReferenceReservation())
                .montantPaiement(paiement.getMontantPaiement())
                .modePaiement(paiement.getModePaiement())
                .statutPaiement(paiement.getStatutPaiement())
                .datePaiement(paiement.getDatePaiement())
                .dateValidation(paiement.getDateValidation())
                .descriptionPaiement(paiement.getDescriptionPaiement())
                .motifRefus(paiement.getMotifRefus())
                .referenceExterne(paiement.getReferenceExterne())
                .validePar(paiement.getValidePar())
                .nomClient(client.getNom())
                .prenomClient(client.getPrenom())
                .emailClient(client.getEmail())
                .montantTotalReservation(montantTotalReservation)
                .montantDejaPayeAvant(montantDejaPayeAvant)
                .montantRestantApres(montantRestantApres)
                .paiementComplet(paiementComplet)
                .build();
    }
}
