package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.avis.*;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutAvis;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvisServiceImpl implements AvisServiceInterface {

    private final AvisRepository avisRepository;
    private final ReservationRepository reservationRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationServiceInterface notificationService;

    // ============================================
    // CRUD CLIENT
    // ============================================

    @Override
    @Transactional
    public AvisResponseDto creerAvis(AvisCreateDto dto, String username) {
        log.info("⭐ Création d'un avis par {} pour le produit {}", username, dto.getIdProduit());

        // 1. Récupérer le client
        Utilisateur client = utilisateurRepository.findByPseudoOrEmail(username,username)
                .orElseThrow(() -> new CustomException("Client introuvable"));

        // 2. Vérifier que la réservation existe et appartient au client
        Reservation reservation = reservationRepository.findById(dto.getIdReservation())
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        if (!reservation.getUtilisateur().getIdUtilisateur().equals(client.getIdUtilisateur())) {
            throw new CustomException("Cette réservation ne vous appartient pas");
        }

        // 3. Vérifier que la réservation est terminée
        if (reservation.getStatutReservation() != StatutReservation.TERMINE) {
            throw new CustomException("Vous ne pouvez évaluer que les réservations terminées");
        }

        // 4. Vérifier que le produit fait partie de la réservation
        Produit produit = produitRepository.findById(dto.getIdProduit())
                .orElseThrow(() -> new CustomException("Produit introuvable"));

        boolean produitDansReservation = reservation.getLigneReservations().stream()
                .anyMatch(ligne -> ligne.getProduit().getIdProduit().equals(dto.getIdProduit()));

        if (!produitDansReservation) {
            throw new CustomException("Ce produit ne fait pas partie de cette réservation");
        }

        // 5. Vérifier que le client n'a pas déjà évalué ce produit pour cette réservation
        boolean dejaEvalue = avisRepository.existsByClient_IdUtilisateurAndReservation_IdReservationAndProduit_IdProduit(
                client.getIdUtilisateur(),
                dto.getIdReservation(),
                dto.getIdProduit()
        );

        if (dejaEvalue) {
            throw new CustomException("Vous avez déjà évalué ce produit pour cette réservation");
        }

        // 6. Créer l'avis
        Avis avis = new Avis();
        avis.setClient(client);
        avis.setReservation(reservation);
        avis.setProduit(produit);
        avis.setNote(dto.getNote());
        avis.setCommentaire(dto.getCommentaire());
        avis.setStatut(StatutAvis.EN_ATTENTE);
        avis.setVisible(true);

        avis = avisRepository.save(avis);

        log.info("✅ Avis créé avec succès : ID {}", avis.getIdAvis());

        return convertToDto(avis);
    }

    @Override
    @Transactional
    public AvisResponseDto modifierAvis(AvisUpdateDto dto, String username) {
        log.info("✏️ Modification de l'avis {} par {}", dto.getIdAvis(), username);

        Avis avis = avisRepository.findById(dto.getIdAvis())
                .orElseThrow(() -> new CustomException("Avis introuvable"));

        // Vérifier que l'avis appartient au client
        if (!avis.getClient().getPseudo().equals(username)) {
            throw new CustomException("Vous ne pouvez modifier que vos propres avis");
        }

        // On ne peut modifier que les avis EN_ATTENTE
        if (avis.getStatut() != StatutAvis.EN_ATTENTE) {
            throw new CustomException("Vous ne pouvez modifier que les avis en attente de modération");
        }

        avis.setNote(dto.getNote());
        avis.setCommentaire(dto.getCommentaire());

        avis = avisRepository.save(avis);

        log.info("✅ Avis modifié avec succès");

        return convertToDto(avis);
    }

    @Override
    @Transactional
    public void supprimerAvis(Long idAvis, String username) {
        log.info("🗑️ Suppression de l'avis {} par {}", idAvis, username);

        Avis avis = avisRepository.findById(idAvis)
                .orElseThrow(() -> new CustomException("Avis introuvable"));

        // Vérifier que l'avis appartient au client
        if (!avis.getClient().getPseudo().equals(username)) {
            throw new CustomException("Vous ne pouvez supprimer que vos propres avis");
        }

        // Soft delete
        avis.setVisible(false);
        avisRepository.save(avis);

        log.info("✅ Avis supprimé (soft delete)");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getMesAvis(String username) {
        log.info("📋 Récupération des avis de {}", username);

        Utilisateur client = utilisateurRepository.findByPseudoOrEmail(username,username)
                .orElseThrow(() -> new CustomException("Client introuvable"));

        List<Avis> avis = avisRepository.findByClient_IdUtilisateurOrderByDateAvisDesc(client.getIdUtilisateur());

        return avis.stream()
                .filter(Avis::getVisible)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean peutEvaluerProduit(Long idReservation, Long idProduit, String username) {
        log.info("🔍 Vérification si {} peut évaluer le produit {} (réservation {})",
                username, idProduit, idReservation);

        Utilisateur client = utilisateurRepository.findByPseudoOrEmail(username,username)
                .orElseThrow(() -> new CustomException("Client introuvable"));

        // Vérifier que la réservation existe et appartient au client
        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new CustomException("Réservation introuvable"));

        if (!reservation.getUtilisateur().getIdUtilisateur().equals(client.getIdUtilisateur())) {
            return false;
        }

        // Vérifier que la réservation est terminée
        if (reservation.getStatutReservation() != StatutReservation.TERMINE) {
            return false;
        }

        // Vérifier que le produit fait partie de la réservation
        boolean produitDansReservation = reservation.getLigneReservations().stream()
                .anyMatch(ligne -> ligne.getProduit().getIdProduit().equals(idProduit));

        if (!produitDansReservation) {
            return false;
        }

        // Vérifier qu'il n'a pas déjà évalué
        boolean dejaEvalue = avisRepository.existsByClient_IdUtilisateurAndReservation_IdReservationAndProduit_IdProduit(
                client.getIdUtilisateur(), idReservation, idProduit
        );

        return !dejaEvalue;
    }

    // ============================================
    // CONSULTATION PUBLIQUE
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAvisApprouvesByProduit(Long idProduit) {
        log.info("📋 Récupération des avis approuvés pour le produit {}", idProduit);

        List<Avis> avis = avisRepository.findAvisApprouvesByProduit(idProduit);

        return avis.stream()
                .map(this::convertToDtoPublic)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StatistiquesAvisDto getStatistiquesAvisProduit(Long idProduit) {
        log.info("📊 Calcul des statistiques d'avis pour le produit {}", idProduit);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new CustomException("Produit introuvable"));

        List<Avis> tousLesAvis = avisRepository.findByProduit_IdProduitOrderByDateAvisDesc(idProduit);
        List<Avis> avisApprouves = tousLesAvis.stream()
                .filter(a -> a.getStatut() == StatutAvis.APPROUVE && a.getVisible())
                .toList();

        Long nombreTotal = (long) tousLesAvis.size();
        Long nombreApprouves = (long) avisApprouves.size();
        Long nombreEnAttente = tousLesAvis.stream()
                .filter(a -> a.getStatut() == StatutAvis.EN_ATTENTE).count();
        Long nombreRejetes = tousLesAvis.stream()
                .filter(a -> a.getStatut() == StatutAvis.REJETE).count();

        Double moyenne = avisRepository.getMoyenneNotesByProduit(idProduit);

        // Répartition par note
        long n5 = avisApprouves.stream().filter(a -> a.getNote() == 5).count();
        long n4 = avisApprouves.stream().filter(a -> a.getNote() == 4).count();
        long n3 = avisApprouves.stream().filter(a -> a.getNote() == 3).count();
        long n2 = avisApprouves.stream().filter(a -> a.getNote() == 2).count();
        long n1 = avisApprouves.stream().filter(a -> a.getNote() == 1).count();

        return StatistiquesAvisDto.builder()
                .idProduit(idProduit)
                .nomProduit(produit.getNomProduit())
                .nombreTotalAvis(nombreTotal)
                .nombreAvisApprouves(nombreApprouves)
                .nombreAvisEnAttente(nombreEnAttente)
                .nombreAvisRejetes(nombreRejetes)
                .moyenneNotes(moyenne != null ? moyenne : 0.0)
                .nombre5Etoiles(n5)
                .nombre4Etoiles(n4)
                .nombre3Etoiles(n3)
                .nombre2Etoiles(n2)
                .nombre1Etoile(n1)
                .pourcentage5Etoiles(nombreApprouves > 0 ? (n5 * 100.0 / nombreApprouves) : 0.0)
                .pourcentage4Etoiles(nombreApprouves > 0 ? (n4 * 100.0 / nombreApprouves) : 0.0)
                .pourcentage3Etoiles(nombreApprouves > 0 ? (n3 * 100.0 / nombreApprouves) : 0.0)
                .pourcentage2Etoiles(nombreApprouves > 0 ? (n2 * 100.0 / nombreApprouves) : 0.0)
                .pourcentage1Etoile(nombreApprouves > 0 ? (n1 * 100.0 / nombreApprouves) : 0.0)
                .build();
    }

    // ============================================
    // MODÉRATION ADMIN
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAllAvis() {
        log.info("📋 Récupération de tous les avis (ADMIN)");

        List<Avis> avis = avisRepository.findAll();

        return avis.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAvisEnAttente() {
        log.info("📋 Récupération des avis en attente de modération");

        List<Avis> avis = avisRepository.findAvisEnAttente();

        return avis.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AvisResponseDto modererAvis(AvisModerationDto dto, String adminUsername) {
        log.info("⚖️ Modération de l'avis {} par {} : {}",
                dto.getIdAvis(), adminUsername, dto.getStatut());

        Avis avis = avisRepository.findById(dto.getIdAvis())
                .orElseThrow(() -> new CustomException("Avis introuvable"));

        // Vérifier que le statut est valide pour la modération
        if (dto.getStatut() != StatutAvis.APPROUVE && dto.getStatut() != StatutAvis.REJETE) {
            throw new CustomException("Statut de modération invalide");
        }

        avis.setStatut(dto.getStatut());
        avis.setCommentaireModeration(dto.getCommentaireModeration());
        avis.setDateModeration(LocalDateTime.now());

        avis = avisRepository.save(avis);

        log.info("✅ Avis {} : {}",
                dto.getStatut() == StatutAvis.APPROUVE ? "approuvé" : "rejeté",
                avis.getIdAvis());

        // Envoyer une notification au client
        NotificationRequestDto notifclient = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.SYSTEME_INFO)
                .titre("Avis Modéré")
                .message("Votre Avis a étè modérer par notre Equipe , Merci pour votre retour qui nous aide à progresser")
                .idUtilisateur(avis.getClient().getIdUtilisateur())
                .urlAction("/client/mes-avis")
                .build();

        notificationService.creerNotification(notifclient);

        return convertToDto(avis);
    }

    @Override
    @Transactional
    public void supprimerAvisDefinitivement(Long idAvis) {
        log.info("🗑️ Suppression définitive de l'avis {} (ADMIN)", idAvis);

        Avis avis = avisRepository.findById(idAvis)
                .orElseThrow(() -> new CustomException("Avis introuvable"));

        avisRepository.delete(avis);

        log.info("✅ Avis supprimé définitivement");
    }


    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAvisByStatut(StatutAvis statut) {
        log.info("📋 Récupération des avis avec le statut {}", statut);

        List<Avis> avis = avisRepository.findByStatutOrderByDateAvisDesc(statut);

        return avis.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAvisByClient(Long clientId) {
        log.info("📋 Récupération des avis du client {}", clientId);

        List<Avis> avis = avisRepository.findByClient_IdUtilisateurOrderByDateAvisDesc(clientId);

        return avis.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAllAvisByProduit(Long idProduit) {
        log.info("📋 Récupération de tous les avis du produit {} (ADMIN)", idProduit);

        List<Avis> avis = avisRepository.findByProduit_IdProduitOrderByDateAvisDesc(idProduit);

        return avis.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // RECHERCHE ET FILTRAGE
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAvisByNote(Integer note) {
        log.info("📋 Recherche des avis avec la note {}", note);

        List<Avis> avis = avisRepository.findByNoteAndStatutOrderByDateAvisDesc(
                note, StatutAvis.APPROUVE
        );

        return avis.stream()
                .filter(Avis::getVisible)
                .map(this::convertToDtoPublic)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> getAvisByPeriode(LocalDateTime debut, LocalDateTime fin) {
        log.info("📋 Recherche des avis entre {} et {}", debut, fin);

        List<Avis> avis = avisRepository.findAvisByPeriode(debut, fin);

        return avis.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvisResponseDto> searchAvisByKeyword(String keyword) {
        log.info("🔍 Recherche d'avis avec le mot-clé : {}", keyword);

        List<Avis> avis = avisRepository.searchByCommentaire(keyword);

        return avis.stream()
                .filter(a -> a.getStatut() == StatutAvis.APPROUVE && a.getVisible())
                .map(this::convertToDtoPublic)
                .collect(Collectors.toList());
    }

    // ============================================
    // STATISTIQUES GLOBALES
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public Long getNombreAvisEnAttente() {
        return avisRepository.countByStatut(StatutAvis.EN_ATTENTE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getTopProduitsParNote(Long minAvis) {
        return avisRepository.getTopProduitsByNote(minAvis);
    }

    // ============================================
    // CONVERSION
    // ============================================

    private AvisResponseDto convertToDto(Avis avis) {
        Utilisateur client = avis.getClient();
        Produit produit = avis.getProduit();
        Reservation reservation = avis.getReservation();

        return AvisResponseDto.builder()
                .idAvis(avis.getIdAvis())
                .note(avis.getNote())
                .commentaire(avis.getCommentaire())
                .dateAvis(avis.getDateAvis())
                .statut(avis.getStatut())
                .visible(avis.getVisible())
                .idClient(client.getIdUtilisateur())
                .nomClient(client.getNom())
                .prenomClient(client.getPrenom())
                .emailClient(client.getEmail())
                .idProduit(produit.getIdProduit())
                .nomProduit(produit.getNomProduit())
                .codeProduit(produit.getCodeProduit())
                .idReservation(reservation.getIdReservation())
                .numeroReservation(reservation.getReferenceReservation())
                .dateDebutReservation(reservation.getDateDebut())
                .dateFinReservation(reservation.getDateFin())
                .commentaireModeration(avis.getCommentaireModeration())
                .dateModeration(avis.getDateModeration())
                .peutEtreModifie(avis.getStatut() == StatutAvis.EN_ATTENTE)
                .peutEtreSupprime(true)
                .build();
    }

    private AvisResponseDto convertToDtoPublic(Avis avis) {
        // Version publique : masquer les infos sensibles
        Utilisateur client = avis.getClient();
        Produit produit = avis.getProduit();

        return AvisResponseDto.builder()
                .idAvis(avis.getIdAvis())
                .note(avis.getNote())
                .commentaire(avis.getCommentaire())
                .dateAvis(avis.getDateAvis())
                .statut(avis.getStatut())
                .nomClient(client.getPrenom()) // Seulement le prénom
                .prenomClient(client.getPrenom().substring(0, 1) + ".") // Initiale
                .idProduit(produit.getIdProduit())
                .nomProduit(produit.getNomProduit())
                .build();
    }
}