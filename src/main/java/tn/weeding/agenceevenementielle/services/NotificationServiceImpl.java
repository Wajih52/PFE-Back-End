package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.notification.NotificationRequestDto;
import tn.weeding.agenceevenementielle.dto.notification.NotificationResponseDto;
import tn.weeding.agenceevenementielle.entities.Notification;
import tn.weeding.agenceevenementielle.entities.Role;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.mapper.NotificationMapper;
import tn.weeding.agenceevenementielle.repository.NotificationRepository;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationServiceInterface {

    private final NotificationRepository notificationRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final NotificationMapper notificationMapper;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    @Override
    public NotificationResponseDto creerNotification(NotificationRequestDto dto) {
        log.info("Création d'une notification type: {} pour utilisateur ID: {}",
                dto.getTypeNotification(), dto.getIdUtilisateur());

        // Vérifier que l'utilisateur existe
        Utilisateur utilisateur = utilisateurRepo.findById(dto.getIdUtilisateur())
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        // Créer la notification
        Notification notification = Notification.builder()
                .typeNotification(dto.getTypeNotification())
                .titre(dto.getTitre())
                .message(dto.getMessage())
                .utilisateur(utilisateur)
                .lue(false)
                .idReservation(dto.getIdReservation())
                .idLivraison(dto.getIdLivraison())
                .idPaiement(dto.getIdPaiement())
                .idProduit(dto.getIdProduit())
                .urlAction(dto.getUrlAction())
                .build();

        notification = notificationRepo.save(notification);
        log.info("Notification créée avec ID: {}", notification.getIdNotification());

        return notificationMapper.toDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsByUtilisateur(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepo.findById(idUtilisateur)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        List<Notification> notifications = notificationRepo
                .findByUtilisateurOrderByDateCreationDesc(utilisateur);

        return notifications.stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsNonLues(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepo.findById(idUtilisateur)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        List<Notification> notifications = notificationRepo
                .findByUtilisateurAndLueFalseOrderByDateCreationDesc(utilisateur);

        return notifications.stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long compterNotificationsNonLues(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepo.findById(idUtilisateur)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        return notificationRepo.countByUtilisateurAndLueFalse(utilisateur);
    }

    @Override
    public NotificationResponseDto marquerCommeLue(Long idNotification, Long idUtilisateur) {
        Notification notification = notificationRepo.findById(idNotification)
                .orElseThrow(() -> new CustomException("Notification introuvable"));

        // Vérifier que la notification appartient à l'utilisateur
        if (!notification.getUtilisateur().getIdUtilisateur().equals(idUtilisateur)) {
            throw new CustomException("Vous n'avez pas accès à cette notification");
        }

        notification.setLue(true);
        notification.setDateLecture(LocalDateTime.now());
        notification = notificationRepo.save(notification);

        log.info("Notification {} marquée comme lue", idNotification);
        return notificationMapper.toDto(notification);
    }

    @Override
    public void marquerToutesCommeLues(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepo.findById(idUtilisateur)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        notificationRepo.marquerToutesCommeLues(utilisateur, LocalDateTime.now());
        log.info("Toutes les notifications de l'utilisateur {} marquées comme lues", idUtilisateur);
    }

    @Override
    public void supprimerNotification(Long idNotification, Long idUtilisateur) {
        Notification notification = notificationRepo.findById(idNotification)
                .orElseThrow(() -> new CustomException("Notification introuvable"));

        if (!notification.getUtilisateur().getIdUtilisateur().equals(idUtilisateur)) {
            throw new CustomException("Vous n'avez pas accès à cette notification");
        }

        notificationRepo.delete(notification);
        log.info(" Notification {} supprimée", idNotification);
    }

    // ============ MÉTHODES HELPER ============

    @Override
    public void creerNotificationStockCritique(Long idProduit, String nomProduit, Integer quantiteRestante) {
        // Envoyer à tous les admins et managers
        List<Utilisateur> admins = utilisateurRepo.findAll().stream()
                .filter(u -> u.getUtilisateurRoles().stream()
                        .anyMatch(ur -> ur.getRole().getNom().equals("ADMIN") ||
                                ur.getRole().getNom().equals("MANAGER")))
                .toList();

        for (Utilisateur admin : admins) {
            NotificationRequestDto dto = NotificationRequestDto.builder()
                    .typeNotification(TypeNotification.STOCK_CRITIQUE)
                    .titre("Stock critique")
                    .message(String.format("Le produit '%s' a atteint un niveau critique : %d unités restantes.",
                            nomProduit, quantiteRestante))
                    .idUtilisateur(admin.getIdUtilisateur())
                    .idProduit(idProduit)
                    .urlAction("/admin/produits/")
                    .build();

            creerNotification(dto);
        }
    }

    /**
     * Notifier tous les Manager et admins
     */
    public void creerNotificationPourStaff(TypeNotification type, String titre, String message,
                                           Long idReservation, String urlAction) {
        List<Utilisateur> staff = utilisateurRepo.findAll().stream()
                .filter(u -> u.getUtilisateurRoles().stream()
                        .anyMatch(ur -> ur.getRole().getNom().equals("ADMIN") ||
                                ur.getRole().getNom().equals("MANAGER")))
                .toList();

        for (Utilisateur employe : staff) {
            NotificationRequestDto dto = NotificationRequestDto.builder()
                    .typeNotification(type)
                    .titre(titre)
                    .message(message)
                    .idUtilisateur(employe.getIdUtilisateur())
                    .idReservation(idReservation)
                    .urlAction(urlAction)
                    .build();

            creerNotification(dto);
        }
    }

    /**
     * Notifier au choix (recepteur)
     */
    public void creerNotificationPour(TypeNotification type, String titre, String message,
                                           Long idReservation, String urlAction,String recepteur) {
        List<Utilisateur> staff = utilisateurRepo.findAll().stream()
                .filter(u -> u.getUtilisateurRoles().stream()
                        .anyMatch(ur -> ur.getRole().getNom().equals(recepteur) ))

                .toList();

        for (Utilisateur employe : staff) {
            NotificationRequestDto dto = NotificationRequestDto.builder()
                    .typeNotification(type)
                    .titre(titre)
                    .message(message)
                    .idUtilisateur(employe.getIdUtilisateur())
                    .idReservation(idReservation)
                    .urlAction(urlAction)
                    .build();

            creerNotification(dto);
        }
    }




    /**
     * Créer une notification ET envoyer l'email
     */
    @Override
    public void creerNotificationAvecEmail(NotificationRequestDto dto) {
        // 1. Créer la notification en BD
        NotificationResponseDto notification = creerNotification(dto);

        // 2. Envoyer l'email
        Utilisateur utilisateur = utilisateurRepo.findById(dto.getIdUtilisateur())
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        emailService.envoyerEmailNotification(
                utilisateur.getEmail(),
                utilisateur.getPrenom(),
                dto.getTypeNotification(),
                dto.getTitre(),
                dto.getMessage()
        );

        log.info("Notification créée ET email envoyé à {}", utilisateur.getEmail());
    }
}