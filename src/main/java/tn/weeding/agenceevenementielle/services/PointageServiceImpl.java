package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.pointage.PointageRequestDto;
import tn.weeding.agenceevenementielle.dto.pointage.PointageResponseDto;
import tn.weeding.agenceevenementielle.dto.pointage.StatistiquesPointageDto;
import tn.weeding.agenceevenementielle.entities.Pointage;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.StatutPointage;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.PointageRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des pointages
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PointageServiceImpl implements PointageServiceInterface {

    private final PointageRepository pointageRepo;
    private final UtilisateurRepository utilisateurRepo;

    // Heure de début de travail standard (8h00)
    private static final LocalTime HEURE_DEBUT_STANDARD = LocalTime.of(8, 0);

    // Marge de tolérance pour les retards (15 minutes)
    private static final int TOLERANCE_RETARD_MINUTES = 15;

    // ============ POINTAGE EMPLOYÉ ============

    @Override
    public PointageResponseDto pointerArrivee(String username) {
        log.info("⏰ Pointage arrivée pour: {}", username);

        Utilisateur utilisateur = getUtilisateur(username);
        LocalDate aujourdhui = LocalDate.now();


        // Vérifier si un pointage existe déjà pour aujourd'hui
        if (pointageRepo.existsByUtilisateurIdUtilisateurAndDateTravail(
                utilisateur.getIdUtilisateur(), aujourdhui)) {
            throw new CustomException("Vous avez déjà pointé votre arrivée aujourd'hui");
        }

        LocalTime maintenant = LocalTime.now();

        // Calculer si l'employé est en retard
        boolean estEnRetard = maintenant.isAfter(HEURE_DEBUT_STANDARD.plusMinutes(TOLERANCE_RETARD_MINUTES));
        int minutesRetard = estEnRetard ?
                (int) ChronoUnit.MINUTES.between(HEURE_DEBUT_STANDARD, maintenant) : 0;

        // Créer le pointage
        Pointage pointage = new Pointage();
        pointage.setUtilisateur(utilisateur);
        pointage.setDateTravail(aujourdhui);
        pointage.setHeureDebut(maintenant);
        pointage.setStatutPointage(estEnRetard ? StatutPointage.enRetard : StatutPointage.present);

        if (estEnRetard) {
            pointage.setDescription("Retard de " + minutesRetard + " minutes");
        }

        pointage = pointageRepo.save(pointage);

        log.info("✅ Pointage arrivée enregistré - Statut: {}", pointage.getStatutPointage());

        return mapToResponseDto(pointage);
    }

    @Override
    public PointageResponseDto pointerDepart(String username) {
        log.info("⏰ Pointage départ pour: {}", username);

        Utilisateur utilisateur = getUtilisateur(username);
        LocalDate aujourdhui = LocalDate.now();


        // Récupérer le pointage du jour
        Pointage pointage = pointageRepo.findByUtilisateurIdUtilisateurAndDateTravail(
                        utilisateur.getIdUtilisateur(), aujourdhui)
                .orElseThrow(() -> new CustomException(
                        "Vous devez d'abord pointer votre arrivée"));

        if (pointage.getHeureFin() != null) {
            throw new CustomException("Vous avez déjà pointé votre départ aujourd'hui");
        }

        LocalTime maintenant = LocalTime.now();
        pointage.setHeureFin(maintenant);

        // Calculer le total d'heures travaillées
        LocalTime heureDebut = pointage.getHeureDebut();
        double totalHeures = ChronoUnit.MINUTES.between(heureDebut, maintenant) / 60.0;
        pointage.setTotalHeures(totalHeures);

        pointage = pointageRepo.save(pointage);

        log.info("✅ Pointage départ enregistré - Total heures: {}", totalHeures);

        return mapToResponseDto(pointage);
    }

    @Override
    public PointageResponseDto getPointageDuJour(String username) {
        Utilisateur utilisateur = getUtilisateur(username);
        LocalDate aujourdhui = LocalDate.now();

        return pointageRepo.findByUtilisateurIdUtilisateurAndDateTravail(
                        utilisateur.getIdUtilisateur(), aujourdhui)
                .map(this::mapToResponseDto)
                .orElse(null);
    }

    @Override
    public List<PointageResponseDto> getMesPointages(String username, LocalDate dateDebut, LocalDate dateFin) {
        Utilisateur utilisateur = getUtilisateur(username);



        return pointageRepo.findByUtilisateurAndPeriode(utilisateur.getIdUtilisateur(), dateDebut, dateFin)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public StatistiquesPointageDto getMesStatistiques(String username, LocalDate dateDebut, LocalDate dateFin) {
        Utilisateur utilisateur = getUtilisateur(username);
        return calculerStatistiques(utilisateur.getIdUtilisateur(), dateDebut, dateFin);
    }

    // ============ GESTION ADMIN/MANAGER ============

    @Override
    public PointageResponseDto creerPointageManuel(PointageRequestDto dto, String username) {
        log.info("📝 Création pointage manuel par: {}", username);

        Utilisateur employe = utilisateurRepo.findById(dto.getIdUtilisateur())
                .orElseThrow(() -> new CustomException("Employé introuvable"));

        // Vérifier si un pointage existe déjà
        if (pointageRepo.existsByUtilisateurIdUtilisateurAndDateTravail(
                employe.getIdUtilisateur(), dto.getDateTravail())) {
            throw new CustomException("Un pointage existe déjà pour cette date");
        }

        Pointage pointage = new Pointage();
        pointage.setUtilisateur(employe);
        pointage.setDateTravail(dto.getDateTravail());
        pointage.setStatutPointage(dto.getStatutPointage() != null ?
                dto.getStatutPointage() : StatutPointage.present);
        pointage.setDescription(dto.getDescription());

        if (dto.getHeureDebut() != null) {
            pointage.setHeureDebut(dto.getHeureDebut());
        }

        if (dto.getHeureFin() != null) {
            pointage.setHeureFin(dto.getHeureFin());

            // Calculer les heures si les deux sont présents
            if (dto.getHeureDebut() != null) {
                double totalHeures = ChronoUnit.MINUTES.between(
                        dto.getHeureDebut(), dto.getHeureFin()) / 60.0;
                pointage.setTotalHeures(totalHeures);
            }
        }

        pointage = pointageRepo.save(pointage);

        log.info("✅ Pointage manuel créé pour l'employé ID: {}", employe.getIdUtilisateur());

        return mapToResponseDto(pointage);
    }

    @Override
    public PointageResponseDto modifierPointage(Long idPointage, PointageRequestDto dto, String username) {
        log.info("✏️ Modification pointage ID: {} par: {}", idPointage, username);

        Pointage pointage = pointageRepo.findById(idPointage)
                .orElseThrow(() -> new CustomException("Pointage introuvable"));

        if (dto.getStatutPointage() != null) {
            pointage.setStatutPointage(dto.getStatutPointage());
        }

        if (dto.getHeureDebut() != null) {
            pointage.setHeureDebut(dto.getHeureDebut());
        }

        if (dto.getHeureFin() != null) {
            pointage.setHeureFin(dto.getHeureFin());
        }

        if (dto.getDescription() != null) {
            pointage.setDescription(dto.getDescription());
        }

        // Recalculer les heures si nécessaire
        if (pointage.getHeureDebut() != null && pointage.getHeureFin() != null) {
            LocalTime debut = pointage.getHeureDebut();
            LocalTime fin = pointage.getHeureFin();
            double totalHeures = ChronoUnit.MINUTES.between(debut, fin) / 60.0;
            pointage.setTotalHeures(totalHeures);
        }

        pointage = pointageRepo.save(pointage);

        log.info("✅ Pointage modifié");

        return mapToResponseDto(pointage);
    }

    @Override
    public void supprimerPointage(Long idPointage, String username) {
        log.info("🗑️ Suppression pointage ID: {} par: {}", idPointage, username);

        if (!pointageRepo.existsById(idPointage)) {
            throw new CustomException("Pointage introuvable");
        }

        pointageRepo.deleteById(idPointage);

        log.info("✅ Pointage supprimé");
    }

    @Override
    public List<PointageResponseDto> getPointagesEmploye(Long idEmploye, LocalDate dateDebut, LocalDate dateFin) {

        return pointageRepo.findByUtilisateurAndPeriode(idEmploye, dateDebut, dateFin)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public StatistiquesPointageDto getStatistiquesEmploye(Long idEmploye, LocalDate dateDebut, LocalDate dateFin) {
        return calculerStatistiques(idEmploye, dateDebut, dateFin);
    }

    // ============ VUES GLOBALES ============

    @Override
    public List<PointageResponseDto> getPointagesAujourdhui() {
        LocalDate aujourdhui = LocalDate.now();

        return pointageRepo.findPointagesAujourdhui(aujourdhui)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PointageResponseDto> getTousLesPointages(LocalDate dateDebut, LocalDate dateFin) {

        return pointageRepo.findByPeriode(dateDebut, dateFin)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PointageResponseDto> getPointagesByStatut(StatutPointage statut, LocalDate date) {

        return pointageRepo.findByStatutPointageAndDateTravail(statut, date)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getEmployesAbsents(LocalDate date) {

        return pointageRepo.findEmployesAbsents(date);
    }

    @Override
    @Scheduled(cron = "0 0 20 * * ?") // Tous les jours à 20h00
    public void marquerAbsentsAutomatiquement() {
        log.info("🤖 Tâche automatique: Marquage des absents du jour");

        LocalDate aujourdhui = LocalDate.now();
        List<Long> idsAbsents = getEmployesAbsents(aujourdhui);

        if (idsAbsents.isEmpty()) {
            log.info("✅ Aucun absent à marquer");
            return;
        }



        for (Long idEmploye : idsAbsents) {
            Utilisateur employe = utilisateurRepo.findById(idEmploye).orElse(null);
            if (employe == null) continue;

            Pointage pointageAbsent = new Pointage();
            pointageAbsent.setUtilisateur(employe);
            pointageAbsent.setDateTravail(aujourdhui);
            pointageAbsent.setStatutPointage(StatutPointage.absent);
            pointageAbsent.setDescription("Marqué absent automatiquement");

            pointageRepo.save(pointageAbsent);
        }

        log.info("✅ {} employé(s) marqué(s) absent(s)", idsAbsents.size());
    }

    // ============ MÉTHODES UTILITAIRES ============

    private Utilisateur getUtilisateur(String username) {
        return utilisateurRepo.findByPseudo(username)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable: " + username));
    }

    private StatistiquesPointageDto calculerStatistiques(Long idUtilisateur, LocalDate dateDebut, LocalDate dateFin) {
        Utilisateur utilisateur = utilisateurRepo.findById(idUtilisateur)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));


        // Compter par statut
        Long joursPresents = pointageRepo.countByUtilisateurAndStatutAndPeriode(
                idUtilisateur, StatutPointage.present, dateDebut, dateFin);
        Long joursAbsents = pointageRepo.countByUtilisateurAndStatutAndPeriode(
                idUtilisateur, StatutPointage.absent, dateDebut, dateFin);
        Long joursEnRetard = pointageRepo.countByUtilisateurAndStatutAndPeriode(
                idUtilisateur, StatutPointage.enRetard, dateDebut, dateFin);
        Long joursEnConge = pointageRepo.countByUtilisateurAndStatutAndPeriode(
                idUtilisateur, StatutPointage.enConge, dateDebut, dateFin);

        // Total heures
        Double totalHeures = pointageRepo.sumTotalHeuresByUtilisateurAndPeriode(
                idUtilisateur, dateDebut, dateFin);

        // Calculs
        long totalJours = ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
        double tauxPresence = totalJours > 0 ?
                ((joursPresents + joursEnRetard) * 100.0 / totalJours) : 0;
        double tauxRetard = (joursPresents + joursEnRetard) > 0 ?
                (joursEnRetard * 100.0 / (joursPresents + joursEnRetard)) : 0;
        double moyenneHeures = (joursPresents + joursEnRetard) > 0 ?
                (totalHeures / (joursPresents + joursEnRetard)) : 0;

        return StatistiquesPointageDto.builder()
                .idUtilisateur(idUtilisateur)
                .nomComplet(utilisateur.getNom() + " " + utilisateur.getPrenom())
                .poste(utilisateur.getPoste())
                .joursPresents(joursPresents.intValue())
                .joursAbsents(joursAbsents.intValue())
                .joursEnRetard(joursEnRetard.intValue())
                .joursEnConge(joursEnConge.intValue())
                .totalHeuresTravaillees(totalHeures)
                .moyenneHeuresParJour(Math.round(moyenneHeures * 100.0) / 100.0)
                .tauxPresence(Math.round(tauxPresence * 100.0) / 100.0)
                .tauxRetard(Math.round(tauxRetard * 100.0) / 100.0)
                .totalRetards(joursEnRetard.intValue())
                .periodeDebut(dateDebut.toString())
                .periodeFin(dateFin.toString())
                .build();
    }

    private PointageResponseDto mapToResponseDto(Pointage pointage) {

        boolean estEnRetard = pointage.getStatutPointage() == StatutPointage.enRetard;
        int minutesRetard = 0;
        if (estEnRetard && pointage.getHeureDebut() != null) {
            minutesRetard = (int) ChronoUnit.MINUTES.between(HEURE_DEBUT_STANDARD, pointage.getHeureDebut());
        }

        return PointageResponseDto.builder()
                .idPointage(pointage.getIdPointage())
                .dateTravail(pointage.getDateTravail())
                .heureDebut(pointage.getHeureDebut())
                .heureFin(pointage.getHeureFin())
                .statutPointage(pointage.getStatutPointage())
                .totalHeures(pointage.getTotalHeures())
                .description(pointage.getDescription())
                .idUtilisateur(pointage.getUtilisateur().getIdUtilisateur())
                .nomUtilisateur(pointage.getUtilisateur().getNom())
                .prenomUtilisateur(pointage.getUtilisateur().getPrenom())
                .pseudoUtilisateur(pointage.getUtilisateur().getPseudo())
                .poste(pointage.getUtilisateur().getPoste())
                .estEnRetard(estEnRetard)
                .minutesRetard(minutesRetard)
                .estComplet(pointage.getHeureDebut() != null && pointage.getHeureFin() != null)
                .build();
    }
}
