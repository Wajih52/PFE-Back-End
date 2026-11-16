package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.reclamation.*;
import tn.weeding.agenceevenementielle.entities.Reclamation;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;
import tn.weeding.agenceevenementielle.entities.enums.TypeReclamation;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.ReclamationRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des réclamations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReclamationServiceImpl implements ReclamationServiceInterface {

    private final ReclamationRepository reclamationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final NotificationServiceInterface notificationService;


    @Override
    public ReclamationResponseDto creerReclamation(ReclamationRequestDto dto, String username) {
        log.info("Création d'une nouvelle réclamation - Type: {}", dto.getTypeReclamation());

        // Créer la réclamation
        Reclamation reclamation = Reclamation.builder()
                .codeReclamation(genererCodeReclamation())
                .dateReclamation(LocalDateTime.now())
                .objet(dto.getObjet())
                .descriptionReclamation(dto.getDescriptionReclamation())
                .contactEmail(dto.getContactEmail())
                .contactTelephone(dto.getContactTelephone())
                .typeReclamation(dto.getTypeReclamation())
                .statutReclamation(StatutReclamation.EN_ATTENTE)
                .prioriteReclamation(determinerPrioriteAutomatique(dto.getTypeReclamation()))
                .build();

        // Associer l'utilisateur si connecté
        if (username != null && !username.isEmpty()) {
            Utilisateur utilisateur = utilisateurRepository.findByPseudo(username)
                    .orElseThrow(() -> new CustomException("Utilisateur introuvable: " + username));
            reclamation.setUtilisateur(utilisateur);
            log.info("Réclamation associée à l'utilisateur: {}", username);
        } else {
            log.info("Réclamation soumise par un visiteur (email: {})", dto.getContactEmail());
        }

        // Associer une réservation si fournie
        if (dto.getIdReservation() != null) {
            Reservation reservation = reservationRepository.findById(dto.getIdReservation())
                    .orElseThrow(() -> new CustomException("Réservation introuvable: " + dto.getIdReservation()));
            reclamation.setReservation(reservation);
            log.info(" Réclamation liée à la réservation: {}", reservation.getReferenceReservation());
        }

        Reclamation saved = reclamationRepository.save(reclamation);
        log.info("Réclamation créée avec succès - Code: {}", saved.getCodeReclamation());

        // ========================================
        // NOTIFIER LES ADMINS ET MANAGERS
        // ========================================
        String messageNotif = String.format(
                "Une nouvelle réclamation (%s) de type '%s' a été soumise. " +
                        "Contact: %s.",
                saved.getCodeReclamation(),
                saved.getTypeReclamation().name(),
                saved.getContactEmail()
        );

        // Extraire l'ID de la réservation si elle existe
        Long idRes = (saved.getReservation() != null) ? saved.getReservation().getIdReservation() : null;

        notificationService.creerNotificationPourStaff(
                TypeNotification.NOUVELLE_RECLAMATION,
                "Nouvelle réclamation à traiter",
                messageNotif,
                idRes,
                "/admin/reclamations" // Redirigera l'admin vers la liste
        );

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getAllReclamations() {
        log.info(" Récupération de toutes les réclamations");
        return reclamationRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReclamationResponseDto getReclamationById(Long id) {
        log.info("Recherche de la réclamation ID: {}", id);
        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new CustomException("Réclamation introuvable avec l'ID: " + id));
        return mapToResponseDto(reclamation);
    }

    @Override
    @Transactional(readOnly = true)
    public ReclamationResponseDto getReclamationByCode(String code) {
        log.info("Recherche de la réclamation Code: {}", code);
        Reclamation reclamation = reclamationRepository.findByCodeReclamation(code)
                .orElseThrow(() -> new CustomException("Réclamation introuvable avec le code: " + code));
        return mapToResponseDto(reclamation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getReclamationsByUtilisateur(Long idUtilisateur) {
        log.info(" Récupération des réclamations de l'utilisateur ID: {}", idUtilisateur);

        // Vérifier que l'utilisateur existe
        if (!utilisateurRepository.existsById(idUtilisateur)) {
            throw new CustomException("Utilisateur introuvable avec l'ID: " + idUtilisateur);
        }

        return reclamationRepository.findByUtilisateurIdUtilisateur(idUtilisateur).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getReclamationsByEmail(String email) {
        log.info(" Récupération des réclamations pour l'email: {}", email);
        return reclamationRepository.findByContactEmail(email).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getReclamationsByStatut(StatutReclamation statut) {
        log.info("Récupération des réclamations avec statut: {}", statut);
        return reclamationRepository.findByStatutReclamation(statut).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getReclamationsByType(TypeReclamation type) {
        log.info("Récupération des réclamations de type: {}", type);
        return reclamationRepository.findByTypeReclamation(type).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getReclamationsByPriorite(PrioriteReclamation priorite) {
        log.info("Récupération des réclamations priorité: {}", priorite);
        return reclamationRepository.findByPrioriteReclamation(priorite).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getReclamationsByReservation(Long idReservation) {
        log.info("Récupération des réclamations pour la réservation ID: {}", idReservation);

        // Vérifier que la réservation existe
        if (!reservationRepository.existsById(idReservation)) {
            throw new CustomException("Réservation introuvable avec l'ID: " + idReservation);
        }

        return reclamationRepository.findByReservationIdReservation(idReservation).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ReclamationResponseDto classerReclamation(Long id, ClasserReclamationDto dto, String username) {
        log.info("Classification de la réclamation ID: {} par {}", id, username);

        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new CustomException("Réclamation introuvable avec l'ID: " + id));

        // Récupérer le code de l'utilisateur connecté
        Utilisateur utilisateur = utilisateurRepository.findByPseudo(username)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable: " + username));

        // Mettre à jour le statut et la priorité
        reclamation.setStatutReclamation(dto.getStatutReclamation());
        reclamation.setPrioriteReclamation(dto.getPrioriteReclamation());
        reclamation.setTraitePar(utilisateur.getCodeUtilisateur());

        Reclamation updated = reclamationRepository.save(reclamation);
        log.info("Réclamation classée - Statut: {}, Priorité: {}",
                dto.getStatutReclamation(), dto.getPrioriteReclamation());

        return mapToResponseDto(updated);
    }

    @Override
    public ReclamationResponseDto traiterReclamation(Long id, TraiterReclamationDto dto, String username) {
        log.info("Traitement de la réclamation ID: {} par {}", id, username);

        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> new CustomException("Réclamation introuvable avec l'ID: " + id));

        // Récupérer le code de l'utilisateur connecté
        Utilisateur utilisateur = utilisateurRepository.findByPseudo(username)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable: " + username));

        // Mettre à jour la réclamation avec la réponse
        reclamation.setStatutReclamation(dto.getStatutReclamation());
        reclamation.setPrioriteReclamation(dto.getPrioriteReclamation());
        reclamation.setReponse(dto.getReponse());
        reclamation.setDateReponse(LocalDateTime.now());
        reclamation.setTraitePar(utilisateur.getCodeUtilisateur());

        Reclamation updated = reclamationRepository.save(reclamation);
        log.info("Réclamation traitée avec succès");

        // ========================================
        // NOTIFICATION PAR E-MAIL AU VISITEUR/CLIENT
        // ========================================
        if (updated.getContactEmail() != null && !updated.getContactEmail().isEmpty()) {
            // Déterminer le prénom (s'il est connecté) ou utiliser un terme générique pour un visiteur
            String prenomVisiteur = (updated.getUtilisateur() != null) ? updated.getUtilisateur().getPrenom() : "Cher client";

            String titreEmail = "Mise à jour de votre réclamation : " + updated.getCodeReclamation();
            String contenuEmail = String.format(
                    "Votre réclamation a été traitée par notre équipe.<br><br>" +
                            "<strong>Statut :</strong> %s<br>" +
                            "<strong>Réponse de l'administration :</strong><br> %s",
                    updated.getStatutReclamation().name(),
                    updated.getReponse()
            );

            emailService.envoyerEmailNotification(
                    updated.getContactEmail(),
                    prenomVisiteur,
                    TypeNotification.SYSTEME_INFO,
                    titreEmail,
                    contenuEmail
            );
            log.info("E-mail de réponse envoyé à la suite du traitement de la réclamation à {}", updated.getContactEmail());
        }


        return mapToResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> rechercherReclamations(
            StatutReclamation statut,
            TypeReclamation type,
            PrioriteReclamation priorite,
            Long idUtilisateur) {

        log.info("Recherche multi-critères - Statut: {}, Type: {}, Priorité: {}, Utilisateur: {}",
                statut, type, priorite, idUtilisateur);

        return reclamationRepository.rechercherReclamations(statut, type, priorite, idUtilisateur).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> getReclamationsByPeriode(LocalDateTime debut, LocalDateTime fin) {
        log.info("Récupération des réclamations entre {} et {}", debut, fin);
        return reclamationRepository.findByDateReclamationBetween(debut, fin).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatut(StatutReclamation statut) {
        return reclamationRepository.countByStatutReclamation(statut);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReclamationsUrgentesNonTraitees() {
        return reclamationRepository.countReclamationsUrgentesNonTraitees();
    }

    @Override
    public void deleteReclamation(Long id) {
        log.warn("Suppression de la réclamation ID: {}", id);

        if (!reclamationRepository.existsById(id)) {
            throw new CustomException("Réclamation introuvable avec l'ID: " + id);
        }

        reclamationRepository.deleteById(id);
        log.info("Réclamation supprimée");
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Générer un code unique pour la réclamation
     */
    private String genererCodeReclamation() {
        String prefix = "REC";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String code = prefix + timestamp.substring(timestamp.length() - 8);

        // Vérifier l'unicité
        while (reclamationRepository.existsByCodeReclamation(code)) {
            try {
                Thread.sleep(1); // Attendre 1ms
                timestamp = String.valueOf(System.currentTimeMillis());
                code = prefix + timestamp.substring(timestamp.length() - 8);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return code;
    }

    /**
     * Déterminer automatiquement la priorité selon le type
     */
    private PrioriteReclamation determinerPrioriteAutomatique(TypeReclamation type) {
        return switch (type) {
            case PRODUIT_ENDOMMAGE, QUANTITE_MANQUANTE, FACTURATION -> PrioriteReclamation.HAUTE;
            case RETARD_LIVRAISON -> PrioriteReclamation.URGENTE;
            case QUALITE_SERVICE, PRODUIT_NON_CONFORME, PROBLEME_RETOUR -> PrioriteReclamation.MOYENNE;
            case AUTRE -> PrioriteReclamation.BASSE;
        };
    }

    /**
     * Mapper une entité vers un DTO de réponse
     */
    private ReclamationResponseDto mapToResponseDto(Reclamation reclamation) {
        ReclamationResponseDto dto = ReclamationResponseDto.builder()
                .idReclamation(reclamation.getIdReclamation())
                .codeReclamation(reclamation.getCodeReclamation())
                .dateReclamation(reclamation.getDateReclamation())
                .objet(reclamation.getObjet())
                .descriptionReclamation(reclamation.getDescriptionReclamation())
                .contactEmail(reclamation.getContactEmail())
                .contactTelephone(reclamation.getContactTelephone())
                .statutReclamation(reclamation.getStatutReclamation())
                .typeReclamation(reclamation.getTypeReclamation())
                .prioriteReclamation(reclamation.getPrioriteReclamation())
                .reponse(reclamation.getReponse())
                .dateReponse(reclamation.getDateReponse())
                .traitePar(reclamation.getTraitePar())
                .build();

        // Ajouter les infos utilisateur si présent
        if (reclamation.getUtilisateur() != null) {
            dto.setIdUtilisateur(reclamation.getUtilisateur().getIdUtilisateur());
            dto.setNomUtilisateur(reclamation.getUtilisateur().getNom());
            dto.setPrenomUtilisateur(reclamation.getUtilisateur().getPrenom());
        }

        // Ajouter les infos réservation si présente
        if (reclamation.getReservation() != null) {
            dto.setIdReservation(reclamation.getReservation().getIdReservation());
            dto.setCodeReservation(reclamation.getReservation().getReferenceReservation());
        }

        return dto;
    }
}