package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.conge.DemandeCongeRequestDto;
import tn.weeding.agenceevenementielle.dto.conge.DemandeCongeResponseDto;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.entities.Conge;
import tn.weeding.agenceevenementielle.entities.Pointage;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.StatutDemandeConge;
import tn.weeding.agenceevenementielle.entities.enums.StatutEmp;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.DemandeCongeRepository;
import tn.weeding.agenceevenementielle.repository.PointageRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CongeService {

    private final DemandeCongeRepository congeRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final PointageRepository pointageRepo;
    private final NotificationServiceInterface notificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // 1. Employé : Soumettre une demande
    public DemandeCongeResponseDto demanderConge(DemandeCongeRequestDto dto, String username) {
        Utilisateur employe = utilisateurRepo.findByPseudo(username)
                .orElseThrow(() -> new CustomException("Employé introuvable"));

        if (dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new CustomException("La date de fin doit être postérieure ou égale à la date de début");
        }

        long joursOuvrables = calculerJoursOuvrables(dto.getDateDebut(), dto.getDateFin());
        if (joursOuvrables == 0) {
            throw new CustomException("La période sélectionnée ne contient aucun jour ouvrable");
        }

        int soldeActuel = employe.getSoldeConges() != null ? employe.getSoldeConges() : 30;
        if (soldeActuel < joursOuvrables) {
            throw new CustomException("Solde insuffisant. Il vous reste " + soldeActuel + " jour(s), mais vous demandez " + joursOuvrables + " jour(s).");
        }

        Conge demande = new Conge();
        demande.setEmploye(employe);
        demande.setDateDebut(dto.getDateDebut());
        demande.setDateFin(dto.getDateFin());
        demande.setMotif(dto.getMotif());
        demande.setStatut(StatutDemandeConge.EN_ATTENTE);

        Conge savedDemande = congeRepo.save(demande);

        //  Alerter les Admins et Managers de la nouvelle demande
        String messageStaff = String.format(
                "L'employé %s %s a soumis une demande de congé de %d jour(s) ouvrable(s) (du %s au %s). Motif : %s",
                employe.getPrenom(), employe.getNom(),
                joursOuvrables,
                dto.getDateDebut().format(DATE_FORMATTER),
                dto.getDateFin().format(DATE_FORMATTER),
                dto.getMotif()
        );

        notificationService.creerNotificationPourStaff(
                TypeNotification.NOUVELLE_DEMANDE_CONGE,
                "Nouvelle demande de congé",
                messageStaff,
                null,
                "/admin/conges" // Redirige l'admin directement vers la page de validation au clic
        );

        return mapToDto(savedDemande);
    }

    // 2. Admin : Accepter ou Refuser une demande
    public DemandeCongeResponseDto traiterDemande(Long idDemande, boolean accepter) {
        Conge demande = congeRepo.findById(idDemande)
                .orElseThrow(() -> new CustomException("Demande de congé introuvable"));

        if (demande.getStatut() != StatutDemandeConge.EN_ATTENTE) {
            throw new CustomException("Cette demande a déjà été traitée");
        }

        Utilisateur employe = demande.getEmploye();
        long joursOuvrables = calculerJoursOuvrables(demande.getDateDebut(), demande.getDateFin());
        String periodeStr = String.format("du %s au %s",
                demande.getDateDebut().format(DATE_FORMATTER),
                demande.getDateFin().format(DATE_FORMATTER));

        // --- CAS 1 : DEMANDE REFUSÉE ---
        if (!accepter) {
            demande.setStatut(StatutDemandeConge.REFUSEE);
            Conge saved = congeRepo.save(demande);

            // Notification + Email à l'employé
            NotificationRequestDto notifRefus = NotificationRequestDto.builder()
                    .typeNotification(TypeNotification.SYSTEME_ALERTE) // ou TypeNotification.CONGE_REFUSE
                    .titre("Demande de congé refusée")
                    .message(String.format(
                            "Votre demande de congé %s (%d jour(s)) a été refusée par l'administration. Votre solde reste inchangé (%d jours).",
                            periodeStr, joursOuvrables, employe.getSoldeConges()))
                    .idUtilisateur(employe.getIdUtilisateur())
                    .urlAction("/employe/conges")
                    .build();

            notificationService.creerNotificationAvecEmail(notifRefus);
            return mapToDto(saved);
        }

        // CAS 2 Si acceptée : décrémenter le solde et générer les pointages
        int soldeActuel = employe.getSoldeConges() != null ? employe.getSoldeConges() : 30;
        if (soldeActuel < joursOuvrables) {
            throw new CustomException("Solde de l'employé insuffisant (" + soldeActuel + " jours restants)");
        }

        int nouveauSolde = (int) (soldeActuel - joursOuvrables);
        employe.setSoldeConges(nouveauSolde);

        // Si aujourd'hui fait partie de la période de congé, on change son statut immédiatement
        LocalDate aujourdhui = LocalDate.now();
        if (!aujourdhui.isBefore(demande.getDateDebut()) && !aujourdhui.isAfter(demande.getDateFin())) {
            employe.setStatutEmploye(StatutEmp.EnConge);
        }
        utilisateurRepo.save(employe);

        // Pré-remplir la table Pointage pour chaque jour ouvrable
        LocalDate dateCourante = demande.getDateDebut();
        while (!dateCourante.isAfter(demande.getDateFin())) {
            if (dateCourante.getDayOfWeek().getValue() < 6) { // Lundi (1) à Vendredi (5)
                boolean pointageExiste = pointageRepo.existsByUtilisateurIdUtilisateurAndDateTravail(
                        employe.getIdUtilisateur(), dateCourante);
                if (!pointageExiste) {
                    Pointage p = new Pointage();
                    p.setUtilisateur(employe);
                    p.setDateTravail(dateCourante);
                    p.setStatutPointage(StatutPointage.enConge);
                    p.setDescription("Congé : " + demande.getMotif());
                    pointageRepo.save(p);
                }
            }
            dateCourante = dateCourante.plusDays(1);
        }

        demande.setStatut(StatutDemandeConge.ACCEPTEE);
        Conge saved = congeRepo.save(demande);

        // Notification + Email à l'employé
        NotificationRequestDto notifAccept = NotificationRequestDto.builder()
                .typeNotification(TypeNotification.SYSTEME_INFO) // ou TypeNotification.CONGE_ACCEPTE
                .titre("Demande de congé acceptée")
                .message(String.format(
                        "Votre demande de congé %s (%d jour(s) ouvrable(s)) a été approuvée ! Vos pointages ont été mis à jour. Nouveau solde restant : %d jour(s).",
                        periodeStr, joursOuvrables, nouveauSolde))
                .idUtilisateur(employe.getIdUtilisateur())
                .urlAction("/employe/conges") // Redirige l'employé vers son historique au clic
                .build();

        notificationService.creerNotificationAvecEmail(notifAccept);

        return mapToDto(saved);
    }

    // 3. Admin : Modifier le quota annuel de congés d'un employé
    public void modifierSoldeEmploye(Long idEmploye, Integer nouveauSolde) {
        Utilisateur employe = utilisateurRepo.findById(idEmploye)
                .orElseThrow(() -> new CustomException("Employé introuvable"));
        employe.setSoldeConges(nouveauSolde);
        utilisateurRepo.save(employe);
    }

    // 4. Récupérer l'historique d'un employé (pour l'employé connecté)
    @Transactional(readOnly = true)
    public List<DemandeCongeResponseDto> getMesDemandes(String username) {
        Utilisateur employe = utilisateurRepo.findByPseudo(username)
                .orElseThrow(() -> new CustomException("Employé introuvable"));
        return congeRepo.findByEmployeIdUtilisateurOrderByDateDebutDesc(employe.getIdUtilisateur())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // 5. Récupérer toutes les demandes (pour l'Admin)
    @Transactional(readOnly = true)
    public List<DemandeCongeResponseDto> getToutesLesDemandes() {
        return congeRepo.findAllByOrderByDateDebutDesc()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Utilitaire : Calculer les jours hors week-end
    private long calculerJoursOuvrables(LocalDate debut, LocalDate fin) {
        long jours = 0;
        LocalDate d = debut;
        while (!d.isAfter(fin)) {
            if (d.getDayOfWeek().getValue() < 6) {
                jours++;
            }
            d = d.plusDays(1);
        }
        return jours;
    }

    private DemandeCongeResponseDto mapToDto(Conge d) {
        return DemandeCongeResponseDto.builder()
                .idDemande(d.getIdDemande())
                .idUtilisateur(d.getEmploye().getIdUtilisateur())
                .nomCompletEmploye(d.getEmploye().getPrenom() + " " + d.getEmploye().getNom())
                .posteEmploye(d.getEmploye().getPoste())
                .soldeCongesRestant(d.getEmploye().getSoldeConges() != null ? d.getEmploye().getSoldeConges() : 30)
                .dateDebut(d.getDateDebut())
                .dateFin(d.getDateFin())
                .nombreJoursOuvrables(calculerJoursOuvrables(d.getDateDebut(), d.getDateFin()))
                .motif(d.getMotif())
                .statut(d.getStatut())
                .build();
    }
}