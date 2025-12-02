package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.statistiques.DashboardStatistiquesDto;
import tn.weeding.agenceevenementielle.dto.statistiques.DashboardStatistiquesDto.*;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ==========================================
 * SERVICE DE STATISTIQUES DASHBOARD
 * ==========================================
 * Responsabilités :
 * - Calculer tous les KPIs pour le dashboard
 * - Générer les données pour les graphiques
 * - Analyser les tendances et évolutions
 * - Fournir des insights métier
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardStatistiquesService {

    private final ReservationRepository reservationRepo;
    private final PaiementRepository paiementRepo;
    private final ProduitRepository produitRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final LigneReservationRepository ligneReservationRepo;
    private final ReclamationRepository reclamationRepo;
    private final LivraisonRepository livraisonRepo;
    private final AvisRepository avisRepo;
    private final PointageRepository pointageRepo;
    private final AffectationLivraisonRepository affectationRepo;

    /**
     * Obtenir toutes les statistiques du dashboard
     */
    public DashboardStatistiquesDto getDashboardStatistiques() {
        log.info("📊 Calcul des statistiques dashboard globales");

        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        LocalDate finMois = aujourdhui.withDayOfMonth(aujourdhui.lengthOfMonth());

        LocalDate debutMoisPrecedent = debutMois.minusMonths(1);
        LocalDate finMoisPrecedent = debutMoisPrecedent.withDayOfMonth(debutMoisPrecedent.lengthOfMonth());

        return DashboardStatistiquesDto.builder()
                // KPIs principaux
                .chiffreAffairesTotal(calculerCATotal())
                .chiffreAffairesMoisActuel(calculerCAMois(debutMois, finMois))
                .evolutionCAMensuel(calculerEvolutionCAMensuel(debutMois, finMois, debutMoisPrecedent, finMoisPrecedent))
                .nombreTotalReservations(compterReservationsTotal())
                .nombreReservationsConfirmees(compterReservationsParStatut(StatutReservation.CONFIRME))
                .nombreDevisEnAttente(compterReservationsParStatut(StatutReservation.EN_ATTENTE))
                .nombreClients(compterClients())
                .nouveauxClientsMois(compterNouveauxClients(debutMois, finMois))
                .panierMoyen(calculerPanierMoyen())
                .tauxConversion(calculerTauxConversion())

                // Alertes
                .produitsStockCritique(compterProduitsStockCritique())
                .reclamationsEnCours(compterReclamationsEnCours())
                .paiementsEnRetard(compterPaiementsEnRetard())
                .livraisonsAujourdhui(compterLivraisonsJour(aujourdhui))
                .retoursAujourdhui(compterRetoursJour(aujourdhui))

                // Graphiques
                .evolutionCA12Mois(getEvolutionCA12Mois())
                .repartitionReservationsParStatut(getRepartitionReservationsParStatut())
                .topProduitsLoues(getTopProduitsLoues(10))
                .topProduitsCA(getTopProduitsCA(10))
                .caParCategorie(getCAParCategorie())
                .evolutionReservations12Mois(getEvolutionReservations12Mois())
                .moyenneNotesParCategorie(getMoyenneNotesParCategorie())

                // Statistiques employés
                .nombreEmployesActifs(compterEmployesActifs())
                .tauxPresenceMoyen(calculerTauxPresenceMoyen(debutMois, finMois))
                .topEmployesLivraisons(getTopEmployesLivraisons(5))

                // Période
                .dateDebut(debutMois)
                .dateFin(finMois)
                .build();
    }

    /**
     * Obtenir les statistiques pour une période spécifique
     */
    public DashboardStatistiquesDto getDashboardStatistiquesPeriode(LocalDate dateDebut, LocalDate dateFin) {
        log.info("📊 Calcul des statistiques dashboard pour la période: {} - {}", dateDebut, dateFin);

        return DashboardStatistiquesDto.builder()
                .chiffreAffairesTotal(calculerCAMois(dateDebut, dateFin))
                .nombreTotalReservations(compterReservationsPeriode(dateDebut, dateFin))
                .nombreReservationsConfirmees(compterReservationsConfirmeesPeriode(dateDebut, dateFin))
                .panierMoyen(calculerPanierMoyenPeriode(dateDebut, dateFin))
                .topProduitsLoues(getTopProduitsLouesPeriode(dateDebut, dateFin, 10))
                .caParCategorie(getCAParCategoriePeriode(dateDebut, dateFin))
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .build();
    }

    // ============================================
    // CALCULS DES KPIs
    // ============================================

    private Double calculerCATotal() {
        List<Paiement> paiements = paiementRepo.findByValidePar_IsNotNull();
        return paiements.stream()
                .mapToDouble(Paiement::getMontantPaiement)
                .sum();
    }

    private Double calculerCAMois(LocalDate debut, LocalDate fin) {
        List<Paiement> paiements = paiementRepo.findByDatePaiementBetweenAndValidePar_IsNotNull(
                debut.atStartOfDay(),
                fin.atTime(23, 59, 59)
        );
        return paiements.stream()
                .mapToDouble(Paiement::getMontantPaiement)
                .sum();
    }

    private Double calculerEvolutionCAMensuel(LocalDate debutMoisActuel, LocalDate finMoisActuel,
                                              LocalDate debutMoisPrecedent, LocalDate finMoisPrecedent) {
        Double caMoisActuel = calculerCAMois(debutMoisActuel, finMoisActuel);
        Double caMoisPrecedent = calculerCAMois(debutMoisPrecedent, finMoisPrecedent);

        if (caMoisPrecedent == 0) return 0.0;

        return ((caMoisActuel - caMoisPrecedent) / caMoisPrecedent) * 100;
    }

    private Long compterReservationsTotal() {
        return  reservationRepo.count();
    }

    private Long compterReservationsParStatut(StatutReservation statut) {
        return  reservationRepo.countByStatutReservation(statut);
    }

    private Long compterReservationsPeriode(LocalDate debut, LocalDate fin) {
        return reservationRepo.countByDateCreationBetween(
                debut.atStartOfDay(),
                fin.atTime(23, 59, 59)
        );
    }

    private Long compterReservationsConfirmeesPeriode(LocalDate debut, LocalDate fin) {
        return reservationRepo.countByStatutReservationAndDateCreationBetween(
                StatutReservation.CONFIRME,
                debut.atStartOfDay(),
                fin.atTime(23, 59, 59)
        );
    }

    private Long compterClients() {
        return utilisateurRepo.countByRoleAndActifTrue("CLIENT");
    }

    private Long compterNouveauxClients(LocalDate debut, LocalDate fin) {
        return utilisateurRepo.countByRoleAndDateCreationBetweenAndActifTrue(
                "CLIENT",
                debut.atStartOfDay(),
                fin.atTime(23, 59, 59)
        );
    }

    private Double calculerPanierMoyen() {
        List<Reservation> reservationsConfirmees = reservationRepo
                .findByStatutReservation(StatutReservation.CONFIRME);

        if (reservationsConfirmees.isEmpty()) return 0.0;

        double somme = reservationsConfirmees.stream()
                .mapToDouble(Reservation::getMontantTotal)
                .sum();

        return somme / reservationsConfirmees.size();
    }

    private Double calculerPanierMoyenPeriode(LocalDate debut, LocalDate fin) {
        List<Reservation> reservations = reservationRepo
                .findByStatutReservationAndDateCreationBetween(
                        StatutReservation.CONFIRME,
                        debut.atStartOfDay(),
                        fin.atTime(23, 59, 59)
                );

        if (reservations.isEmpty()) return 0.0;

        double somme = reservations.stream()
                .mapToDouble(Reservation::getMontantTotal)
                .sum();

        return somme / reservations.size();
    }

    private Double calculerTauxConversion() {
        long totalDevis = compterReservationsParStatut(StatutReservation.EN_ATTENTE);
        long totalConfirmes = compterReservationsParStatut(StatutReservation.CONFIRME);

        long total = totalDevis + totalConfirmes;
        if (total == 0) return 0.0;

        return ((double) totalConfirmes / total) * 100;
    }

    // ============================================
    // CALCULS DES ALERTES
    // ============================================

    private Long compterProduitsStockCritique() {
        return produitRepo.countProduitsEnAlerteCritiqueAujourdhui();
    }

    private Long compterReclamationsEnCours() {
        return reclamationRepo.countByStatutReclamationIn(
                Arrays.asList(StatutReclamation.EN_ATTENTE, StatutReclamation.EN_COURS)
        );
    }

    private long compterPaiementsEnRetard() {
        LocalDate aujourdhui = LocalDate.now();
        return reservationRepo.findReservationsAvecPaiementsEnRetard(aujourdhui).size();
    }

    private Long compterLivraisonsJour(LocalDate date) {
        return livraisonRepo.countByDateLivraison(date);
    }

    private long compterRetoursJour(LocalDate date) {
        return ligneReservationRepo.countByDateFinAndStatutLivraisonLigneIn(
                date,
                Arrays.asList(StatutLivraison.LIVREE, StatutLivraison.RETOUR)
        );
    }

    // ============================================
    // DONNÉES POUR LES GRAPHIQUES
    // ============================================

    private List<MoisChiffreAffairesDto> getEvolutionCA12Mois() {
        LocalDate aujourdhui = LocalDate.now();
        List<MoisChiffreAffairesDto> evolution = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth mois = YearMonth.from(aujourdhui.minusMonths(i));
            LocalDate debut = mois.atDay(1);
            LocalDate fin = mois.atEndOfMonth();

            Double ca = calculerCAMois(debut, fin);

            String nomMois = mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                    + " " + mois.getYear();

            evolution.add(MoisChiffreAffairesDto.builder()
                    .mois(nomMois)
                    .chiffreAffaires(ca)
                    .annee((long)mois.getYear())
                    .moisNumero((long)mois.getMonthValue())
                    .build());
        }

        return evolution;
    }

    private Map<String, Long> getRepartitionReservationsParStatut() {
        Map<String, Long> repartition = new LinkedHashMap<>();

        for (StatutReservation statut : StatutReservation.values()) {
            long count = compterReservationsParStatut(statut);
            repartition.put(statut.name(), count);
        }

        return repartition;
    }

    private List<TopProduitDto> getTopProduitsLoues(int limit) {
        List<Object[]> results = ligneReservationRepo.findTopProduitsLoues(limit);

        return results.stream()
                .map(row -> {
                    Long idProduit = (Long) row[0];
                    Produit produit = produitRepo.findById(idProduit).orElse(null);

                    if (produit == null) return null;

                    return TopProduitDto.builder()
                            .idProduit(idProduit)
                            .nomProduit(produit.getNomProduit())
                            .codeProduit(produit.getCodeProduit())
                            .imageProduit(produit.getImageProduit())
                            .nombreLocations(((Number) row[2]).longValue())
                            .chiffreAffaires(((Number) row[3]).doubleValue())
                            .moyenneNotes(calculerMoyenneNotesProduit(idProduit))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<TopProduitDto> getTopProduitsLouesPeriode(LocalDate debut, LocalDate fin, int limit) {
        List<Object[]> results = ligneReservationRepo.findTopProduitsLouesPeriode(debut, fin, limit);

        return results.stream()
                .map(row -> {
                    Long idProduit = (Long) row[0];
                    Produit produit = produitRepo.findById(idProduit).orElse(null);

                    if (produit == null) return null;

                    return TopProduitDto.builder()
                            .idProduit(idProduit)
                            .nomProduit(produit.getNomProduit())
                            .codeProduit(produit.getCodeProduit())
                            .imageProduit(produit.getImageProduit())
                            .nombreLocations(((Number) row[2]).longValue())
                            .chiffreAffaires(((Number) row[3]).doubleValue())
                            .moyenneNotes(calculerMoyenneNotesProduit(idProduit))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<TopProduitDto> getTopProduitsCA(int limit) {
        List<Object[]> results = ligneReservationRepo.findTopProduitsParCA(limit);

        return results.stream()
                .map(row -> {
                    Long idProduit = (Long) row[0];
                    Produit produit = produitRepo.findById(idProduit).orElse(null);

                    if (produit == null) return null;

                    return TopProduitDto.builder()
                            .idProduit(idProduit)
                            .nomProduit(produit.getNomProduit())
                            .codeProduit(produit.getCodeProduit())
                            .imageProduit(produit.getImageProduit())
                            .chiffreAffaires(((Number) row[2]).doubleValue())
                            .nombreLocations(compterLocationsProduit(idProduit))
                            .moyenneNotes(calculerMoyenneNotesProduit(idProduit))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Double> getCAParCategorie() {
        List<Object[]> results = ligneReservationRepo.findCAParCategorie();

        Map<String, Double> caParCat = new LinkedHashMap<>();
        for (Object[] row : results) {
            String categorie = row[0].toString();
            Double ca = ((Number) row[1]).doubleValue();
            caParCat.put(categorie, ca);
        }

        return caParCat;
    }

    private Map<String, Double> getCAParCategoriePeriode(LocalDate debut, LocalDate fin) {
        List<Object[]> results = ligneReservationRepo.findCAParCategoriePeriode(debut, fin);

        Map<String, Double> caParCat = new LinkedHashMap<>();
        for (Object[] row : results) {
            String categorie = row[0].toString();
            Double ca = ((Number) row[1]).doubleValue();
            caParCat.put(categorie, ca);
        }

        return caParCat;
    }

    private List<MoisNombreReservationsDto> getEvolutionReservations12Mois() {
        LocalDate aujourdhui = LocalDate.now();
        List<MoisNombreReservationsDto> evolution = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth mois = YearMonth.from(aujourdhui.minusMonths(i));
            LocalDate debut = mois.atDay(1);
            LocalDate fin = mois.atEndOfMonth();

            Long nombre = compterReservationsPeriode(debut, fin);

            String nomMois = mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                    + " " + mois.getYear();

            evolution.add(MoisNombreReservationsDto.builder()
                    .mois(nomMois)
                    .nombreReservations(nombre)
                    .annee((long)mois.getYear())
                    .moisNumero((long)mois.getMonthValue())
                    .build());
        }

        return evolution;
    }

    private Map<String, Double> getMoyenneNotesParCategorie() {
        List<Object[]> results = avisRepo.findMoyenneNotesParCategorie();

        Map<String, Double> moyennes = new LinkedHashMap<>();
        for (Object[] row : results) {
            String categorie = row[0].toString();
            Double moyenne = ((Number) row[1]).doubleValue();
            moyennes.put(categorie, Math.round(moyenne * 10.0) / 10.0);
        }

        return moyennes;
    }

    // ============================================
    // STATISTIQUES EMPLOYÉS
    // ============================================

    private Long compterEmployesActifs() {
        return utilisateurRepo.countByRolesInAndActifTrue(
                Arrays.asList("EMPLOYE", "MANAGER", "ADMIN")
        );
    }

    private Double calculerTauxPresenceMoyen(LocalDate debut, LocalDate fin) {
        List<Utilisateur> employes = utilisateurRepo.findByRolesInAndActifTrue(
                Arrays.asList("EMPLOYE", "MANAGER")
        );

        if (employes.isEmpty()) return 0.0;

        double sommeTaux = 0.0;
        int compteur = 0;

        for (Utilisateur employe : employes) {
            List<Pointage> pointages = pointageRepo.findByUtilisateur_IdUtilisateurAndDateTravailBetween(
                    employe.getIdUtilisateur(),
                    debut,
                    fin
            );

            long joursPresents = pointages.stream()
                    .filter(p -> p.getHeureDebut() != null)
                    .count();

            long joursOuvrables = calculerJoursOuvrables(debut, fin);
            if (joursOuvrables > 0) {
                double taux = ((double) joursPresents / joursOuvrables) * 100;
                sommeTaux += taux;
                compteur++;
            }
        }

        return compteur > 0 ? sommeTaux / compteur : 0.0;
    }

    private List<TopEmployeDto> getTopEmployesLivraisons(int limit) {
        List<Object[]> results = affectationRepo.findTopEmployesParNombreLivraisons(limit);

        LocalDate debutMois = LocalDate.now().withDayOfMonth(1);
        LocalDate finMois = debutMois.withDayOfMonth(debutMois.lengthOfMonth());

        return results.stream()
                .map(row -> {
                    Long idEmploye = (Long) row[0];
                    Utilisateur employe = utilisateurRepo.findById(idEmploye).orElse(null);

                    if (employe == null) return null;

                    Long nombreLivraisons = ((Number) row[1]).longValue();

                    // Calculer le taux de présence
                    List<Pointage> pointages = pointageRepo.findByUtilisateur_IdUtilisateurAndDateTravailBetween(
                            idEmploye,
                            debutMois,
                            finMois
                    );

                    long joursPresents = pointages.stream()
                            .filter(p -> p.getHeureDebut() != null)
                            .count();

                    long joursOuvrables = calculerJoursOuvrables(debutMois, finMois);
                    double tauxPresence = joursOuvrables > 0
                            ? ((double) joursPresents / joursOuvrables) * 100
                            : 0.0;

                    return TopEmployeDto.builder()
                            .idEmploye(idEmploye)
                            .nomComplet(employe.getPrenom() + " " + employe.getNom())
                            .email(employe.getEmail())
                            .nombreLivraisons(nombreLivraisons)
                            .tauxPresence(Math.round(tauxPresence * 10.0) / 10.0)
                            .imageProfil(employe.getImage())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    private Double calculerMoyenneNotesProduit(Long idProduit) {
        List<Avis> avis = avisRepo.getAvisApprouvesByProduit(idProduit);

        if (avis.isEmpty()) return 0.0;

        double somme = avis.stream()
                .mapToDouble(Avis::getNote)
                .sum();

        return Math.round((somme / avis.size()) * 10.0) / 10.0;
    }

    private Long compterLocationsProduit(Long idProduit) {
        return ligneReservationRepo.countByProduit_IdProduitAndReservation_StatutReservation(
                idProduit,
                StatutReservation.CONFIRME
        );
    }

    private long calculerJoursOuvrables(LocalDate debut, LocalDate fin) {
        long jours = 0;
        LocalDate date = debut;

        while (!date.isAfter(fin)) {
            // Exclure samedi (6) et dimanche (7)
            if (date.getDayOfWeek().getValue() < 6) {
                jours++;
            }
            date = date.plusDays(1);
        }

        return jours;
    }
}