package tn.weeding.agenceevenementielle.services;

import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.facture.FactureResponseDto;
import tn.weeding.agenceevenementielle.dto.facture.GenererFactureRequestDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutFacture;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.FactureRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FactureServiceImpl implements FactureServiceInterface {

    private final FactureRepository factureRepository;
    private final ReservationRepository reservationRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final PdfGeneratorService pdfGeneratorService;

    private static final double TVA_TAUX = 0.19; // 19% TVA

    @Override
    public FactureResponseDto genererFacture(GenererFactureRequestDto request, String username) {
        log.info("📄 Génération de facture {} pour réservation {}", request.getTypeFacture(), request.getIdReservation());

        // Vérifier que la réservation existe
        Reservation reservation = reservationRepository.findById(request.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        // Vérifier qu'une facture du même type n'existe pas déjà
        if (factureRepository.existsByReservation_IdReservationAndTypeFacture(
                request.getIdReservation(), request.getTypeFacture())) {
            throw new CustomException("Une facture de type " + request.getTypeFacture() + " existe déjà pour cette réservation");
        }

        // Créer la facture
        Facture facture = creerFacture(reservation, request.getTypeFacture(), username);
        facture.setNotes(request.getNotes());
        facture.setConditionsPaiement(request.getConditionsPaiement());

        // Sauvegarder
        facture = factureRepository.save(facture);

        // Générer le PDF
        try {
            String cheminPDF = pdfGeneratorService.genererPdfFacture(facture);
            facture.setCheminPDF(cheminPDF);
            facture = factureRepository.save(facture);
        } catch (DocumentException | IOException e) {
            log.error("❌ Erreur génération PDF : {}", e.getMessage());
            throw new CustomException("Erreur lors de la génération du PDF : " + e.getMessage());
        }

        log.info("✅ Facture générée : {}", facture.getNumeroFacture());
        return convertToDto(facture);
    }

    @Override
    public FactureResponseDto genererFactureAutomatique(Long idReservation, TypeFacture typeFacture, String username) {
        GenererFactureRequestDto request = GenererFactureRequestDto.builder()
                .idReservation(idReservation)
                .typeFacture(typeFacture)
                .build();

        return genererFacture(request, username);
    }

    @Override
    public FactureResponseDto getFactureById(Long idFacture) {
        Facture facture = factureRepository.findById(idFacture)
                .orElseThrow(() -> new CustomException("Facture introuvable"));
        return convertToDto(facture);
    }

    @Override
    public FactureResponseDto getFactureByNumero(String numeroFacture) {
        Facture facture = factureRepository.findByNumeroFacture(numeroFacture)
                .orElseThrow(() -> new CustomException("Facture introuvable"));
        return convertToDto(facture);
    }

    @Override
    public List<FactureResponseDto> getFacturesByReservation(Long idReservation) {
        return factureRepository.findByReservation_IdReservation(idReservation)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FactureResponseDto> getFacturesByClient(Long idClient) {
        return factureRepository.findByReservation_Utilisateur_IdUtilisateur(idClient)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FactureResponseDto> getAllFactures() {
        return factureRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FactureResponseDto> getFacturesByStatut(StatutFacture statut) {
        return factureRepository.findByStatutFacture(statut)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FactureResponseDto> getFacturesByType(TypeFacture type) {
        return factureRepository.findByTypeFacture(type)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] telechargerPdfFacture(Long idFacture) {
        Facture facture = factureRepository.findById(idFacture)
                .orElseThrow(() -> new CustomException("Facture introuvable"));

        if (facture.getCheminPDF() == null) {
            throw new CustomException("PDF non disponible pour cette facture");
        }

        try {
            return Files.readAllBytes(Paths.get(facture.getCheminPDF()));
        } catch (IOException e) {
            log.error("❌ Erreur lecture PDF : {}", e.getMessage());
            throw new CustomException("Erreur lors de la lecture du PDF");
        }
    }

    @Override
    public FactureResponseDto updateStatutFacture(Long idFacture, StatutFacture nouveauStatut, String username) {
        Facture facture = factureRepository.findById(idFacture)
                .orElseThrow(() -> new CustomException("Facture introuvable"));

        facture.setStatutFacture(nouveauStatut);
        facture = factureRepository.save(facture);

        log.info("✅ Statut facture mis à jour : {} -> {}", facture.getNumeroFacture(), nouveauStatut);
        return convertToDto(facture);
    }

    @Override
    public FactureResponseDto regenererPdfFacture(Long idFacture, String username) {
        Facture facture = factureRepository.findById(idFacture)
                .orElseThrow(() -> new CustomException("Facture introuvable"));

        try {
            String cheminPDF = pdfGeneratorService.genererPdfFacture(facture);
            facture.setCheminPDF(cheminPDF);
            facture = factureRepository.save(facture);

            log.info("✅ PDF régénéré pour facture : {}", facture.getNumeroFacture());
            return convertToDto(facture);
        } catch (DocumentException | IOException e) {
            log.error("❌ Erreur régénération PDF : {}", e.getMessage());
            throw new CustomException("Erreur lors de la régénération du PDF");
        }
    }

    // ===== MÉTHODES PRIVÉES =====

    private Facture creerFacture(Reservation reservation, TypeFacture typeFacture, String username) {
        // Générer le numéro
        String numeroFacture = codeGeneratorService.genererNumeroFacture(typeFacture);

        // Calculer les montants
        Double montantHT = reservation.getMontantTotal() / (1 + TVA_TAUX);
        Double montantTVA = reservation.getMontantTotal() - montantHT;
        Double montantRemise = reservation.getRemiseMontant();

        // Déterminer le statut initial
        StatutFacture statut = determinerStatutInitial(typeFacture);

        // Calculer la date d'échéance (30 jours par défaut pour facture finale)
        LocalDate dateEcheance = typeFacture == TypeFacture.FINALE
                ? LocalDate.now().plusDays(30)
                : null;

        return Facture.builder()
                .numeroFacture(numeroFacture)
                .typeFacture(typeFacture)
                .statutFacture(statut)
                .montantHT(montantHT)
                .montantTVA(montantTVA)
                .montantTTC(reservation.getMontantTotal())
                .montantRemise(montantRemise)
                .dateEcheance(dateEcheance)
                .reservation(reservation)
                .generePar(username)
                .build();
    }

    private StatutFacture determinerStatutInitial(TypeFacture typeFacture) {
        switch (typeFacture) {
            case DEVIS:
                return StatutFacture.EN_ATTENTE_VALIDATION_CLIENT;
            case PRO_FORMA:
                return StatutFacture.EN_ATTENTE_LIVRAISON;
            case FINALE:
                return StatutFacture.A_REGLER;
            default:
                return StatutFacture.EN_ATTENTE_VALIDATION_CLIENT;
        }
    }

    private FactureResponseDto convertToDto(Facture facture) {
        Reservation reservation = facture.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        return FactureResponseDto.builder()
                .idFacture(facture.getIdFacture())
                .numeroFacture(facture.getNumeroFacture())
                .typeFacture(facture.getTypeFacture())
                .statutFacture(facture.getStatutFacture())
                .dateCreation(facture.getDateCreation())
                .dateEcheance(facture.getDateEcheance())
                .montantHT(facture.getMontantHT())
                .montantTVA(facture.getMontantTVA())
                .montantTTC(facture.getMontantTTC())
                .montantRemise(facture.getMontantRemise())
                .notes(facture.getNotes())
                .conditionsPaiement(facture.getConditionsPaiement())
                .cheminPDF(facture.getCheminPDF())
                .idReservation(reservation.getIdReservation())
                .referenceReservation(reservation.getReferenceReservation())
                .nomClient(client.getNom())
                .prenomClient(client.getPrenom())
                .emailClient(client.getEmail())
                .telephoneClient(client.getTelephone().toString())
                .generePar(facture.getGenerePar())
                .build();
    }
}