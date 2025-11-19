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
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.FactureRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        log.info("🔄 Mise à jour de la facture ID: {}", idFacture);

        Facture facture = factureRepository.findById(idFacture)
                .orElseThrow(() -> new CustomException("Facture introuvable"));

        // Vérifier que la facture peut être mise à jour
        if (facture.getStatutFacture() == StatutFacture.PAYEE) {
            throw new CustomException("Impossible de modifier une facture payée");
        }

        if (facture.getStatutFacture() == StatutFacture.ANNULEE) {
            throw new CustomException("Impossible de modifier une facture annulée");
        }

        // Vérifier que c'est un DEVIS ou PRO_FORMA
        if (facture.getTypeFacture() == TypeFacture.FINALE &&
                facture.getReservation().getStatutLivraisonRes() == StatutLivraison.LIVREE) {
            throw new CustomException("Impossible de modifier une facture finale après livraison");
        }

        // Mettre à jour les montants
        facture = mettreAJourMontantsFacture(facture, facture.getReservation());

        // Régénérer le PDF
        try {
            String cheminPDF = pdfGeneratorService.genererPdfFacture(facture);
            facture.setCheminPDF(cheminPDF);
            facture = factureRepository.save(facture);

            log.info("✅ Facture mise à jour : {}", facture.getNumeroFacture());
            return convertToDto(facture);
        } catch (DocumentException | IOException e) {
            log.error("❌ Erreur mise à jour PDF : {}", e.getMessage());
            throw new CustomException("Erreur lors de la mise à jour du PDF");
        }
    }

    // ===== MÉTHODES PRIVÉES =====

    private Facture creerFacture(Reservation reservation, TypeFacture typeFacture, String username) {
        // Générer le numéro
        String numeroFacture = codeGeneratorService.genererNumeroFacture(typeFacture);

        // 1️⃣ Calculer le montant total AVANT remise (montant brut des lignes)
        double montantTotalSansRemise = 0.0;
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            // Calculer le nombre de jours
            long nbrJours = java.time.temporal.ChronoUnit.DAYS.between(
                    ligne.getDateDebut(),
                    ligne.getDateFin()
            ) + 1;

            montantTotalSansRemise += ligne.getQuantite() * ligne.getPrixUnitaire() * nbrJours;
        }

        // 2️⃣ Calculer le montant de la remise
        double montantRemise = 0.0;

        if (reservation.getRemiseMontant() != null && reservation.getRemiseMontant() > 0) {
            // Remise en MONTANT FIXE
            montantRemise = reservation.getRemiseMontant();
        }
        else if (reservation.getRemisePourcentage() != null && reservation.getRemisePourcentage() > 0) {
            // Remise en POURCENTAGE
            montantRemise = montantTotalSansRemise * (reservation.getRemisePourcentage() / 100.0);
        }

        // 3️⃣ Calculer le montant APRÈS remise (c'est le montantTotal de la réservation)
        double montantTotalApresRemise = montantTotalSansRemise - montantRemise;

        // Vérification : le montantTotal de la réservation devrait correspondre
        // (on utilise quand même celui de la réservation pour la cohérence)
        double montantTTC = reservation.getMontantTotal();

        // 4️⃣ Calculer le HT et la TVA depuis le montant TTC (après remise)
        double montantHT = montantTTC / (1 + TVA_TAUX);
        double montantTVA = montantTTC - montantHT;

        // 5️⃣ Déterminer le statut initial
        StatutFacture statut = determinerStatutInitial(typeFacture);

        // 6️⃣ Calculer la date d'échéance (30 jours par défaut pour facture finale)
        LocalDate dateEcheance = typeFacture == TypeFacture.FINALE
                ? LocalDate.now().plusDays(30)
                : null;

        // 7️⃣ Créer la facture
        Facture facture = new Facture();
        facture.setReservation(reservation);
        facture.setNumeroFacture(numeroFacture);
        facture.setTypeFacture(typeFacture);
        facture.setStatutFacture(statut);
        facture.setDateCreation(LocalDateTime.now());
        facture.setDateEcheance(dateEcheance);

        // Montants
        facture.setMontantHT(montantHT);
        facture.setMontantTVA(montantTVA);
        facture.setMontantRemise(montantRemise);
        facture.setMontantTTC(montantTTC);

        log.info("💰 Calculs facture : Total sans remise={}DT, Remise={}DT, HT={}DT, TVA={}DT, TTC={}DT",
                montantTotalSansRemise, montantRemise, montantHT, montantTVA, montantTTC);

        return facture;
    }

    private StatutFacture determinerStatutInitial(TypeFacture typeFacture) {
        return switch (typeFacture) {
            case PRO_FORMA -> StatutFacture.EN_ATTENTE_LIVRAISON;
            case FINALE -> StatutFacture.A_REGLER;
            default -> StatutFacture.EN_ATTENTE_VALIDATION_CLIENT;
        };
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

    /**
     * 🆕 Méthode helper pour mettre à jour les montants d'une facture
     */
    private Facture mettreAJourMontantsFacture(Facture facture, Reservation reservation) {
        // Recalculer les montants (même logique que dans creerFacture)
        double montantTotalSansRemise = 0.0;
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            long nbrJours = ChronoUnit.DAYS.between(
                    ligne.getDateDebut(),
                    ligne.getDateFin()
            ) + 1;
            montantTotalSansRemise += ligne.getQuantite() * ligne.getPrixUnitaire() * nbrJours;
        }

        double montantRemise = 0.0;
        if (reservation.getRemiseMontant() != null && reservation.getRemiseMontant() > 0) {
            montantRemise = reservation.getRemiseMontant();
        } else if (reservation.getRemisePourcentage() != null && reservation.getRemisePourcentage() > 0) {
            montantRemise = montantTotalSansRemise * (reservation.getRemisePourcentage() / 100.0);
        }

        double montantTTC = reservation.getMontantTotal();
        double montantHT = montantTTC / 1.19;
        double montantTVA = montantTTC - montantHT;

        facture.setMontantHT(montantHT);
        facture.setMontantTVA(montantTVA);
        facture.setMontantRemise(montantRemise);
        facture.setMontantTTC(montantTTC);

        return facture;
    }
}