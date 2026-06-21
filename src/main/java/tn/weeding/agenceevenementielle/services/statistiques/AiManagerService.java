package tn.weeding.agenceevenementielle.services.statistiques;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.dto.statistiques.AiManagerSummaryResponse;
import tn.weeding.agenceevenementielle.dto.statistiques.AnalyseAiHistoriqueResponse;
import tn.weeding.agenceevenementielle.entities.AnalyseAiHistorique;
import tn.weeding.agenceevenementielle.entities.Produit;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiManagerService {

    private final ReservationRepository reservationRepository;
    private final ProduitRepository produitRepository;
    private final PaiementRepository paiementRepository;
    private final LivraisonRepository livraisonRepository;
    private final ReclamationRepository reclamationRepository;
    private final OpenRouterService openRouterService ;

    private final AnalyseAiHistoriqueRepository analyseAiHistoriqueRepository;

    public AiManagerSummaryResponse generateSummary() {

        LocalDate today = LocalDate.now();

        long devisEnAttente =
                reservationRepository.countByStatutReservation(StatutReservation.EN_ATTENTE);

        int reservationsEnCours =
                reservationRepository.findReservationsEnCours().size();

        int reservationsAVenir =
                reservationRepository.findReservationsConfirmeesAVenir().size();

        int reservationsPaiementsEnRetard =
                reservationRepository.findReservationsAvecPaiementsEnRetard(today).size();

        Double chiffreAffairesTotal =
                reservationRepository.calculateChiffreAffairesTotal();

        if (chiffreAffairesTotal == null) {
            chiffreAffairesTotal = 0.0;
        }

        Long produitsCritiquesAujourdhui =
                produitRepository.countProduitsEnAlerteCritiqueAujourdhui();

        int produitsEnRupture =
                produitRepository.findProduitsEnRupture().size();

        List<String> topProduitsLoues =
                produitRepository.findProduitsLesPlusLoues()
                        .stream()
                        .limit(5)
                        .map(Produit::getNomProduit)
                        .toList();

        List<String> produitsForteOccupation =
                produitRepository.findTauxOccupationProduitsParPeriode(
                                today,
                                today.plusDays(30)
                        )
                        .stream()
                        .limit(5)
                        .map(row -> row[1] + " : " + row[2] + "%")
                        .toList();

        Long paiementsEnAttente =
                paiementRepository.countPaiementsEnAttente();

        Long livraisonsAujourdhui =
                livraisonRepository.countByDateLivraison(today);

        Long livraisonsDemain =
                livraisonRepository.countByDateLivraison(today.plusDays(1));

        long reclamationsEnAttente =
                reclamationRepository.countByStatutReclamation(StatutReclamation.EN_ATTENTE);

        long reclamationsUrgentes =
                reclamationRepository.countReclamationsUrgentesNonTraitees();

        String prompt = """
        Tu es un assistant IA pour un manager d'une agence événementielle.

        Analyse les indicateurs suivants et donne :
        1. un résumé de la situation générale
        2. les risques opérationnels, financiers et logistiques
        3. les actions prioritaires à effectuer
        4. une recommandation de gestion pour les prochains jours

        Réponds en français, avec un style professionnel, clair et concis.
        Ne fais pas de longues phrases inutiles.
        Présente la réponse sous forme de sections.

        Données commerciales :
        - Devis en attente : %d
        - Réservations en cours : %d
        - Réservations confirmées à venir : %d
        - Réservations avec paiement en retard : %d
        - Chiffre d'affaires total confirmé : %.2f DT

        Données stock :
        - Produits en alerte critique aujourd'hui : %d
        - Produits en rupture : %d
        - Top produits les plus loués : %s
        - Produits avec plus fort taux d'occupation sur les 30 prochains jours : %s

        Données logistiques :
        - Livraisons prévues aujourd'hui : %d
        - Livraisons prévues demain : %d

        Données support client :
        - Réclamations en attente : %d
        - Réclamations urgentes non traitées : %d

        Données financières :
        - Paiements en attente de validation : %d
        """.formatted(
                devisEnAttente,
                reservationsEnCours,
                reservationsAVenir,
                reservationsPaiementsEnRetard,
                chiffreAffairesTotal,
                produitsCritiquesAujourdhui,
                produitsEnRupture,
                topProduitsLoues,
                produitsForteOccupation,
                livraisonsAujourdhui,
                livraisonsDemain,
                reclamationsEnAttente,
                reclamationsUrgentes,
                paiementsEnAttente
        );

        String aiResponse = openRouterService.ask(prompt);

        AnalyseAiHistorique historique = AnalyseAiHistorique.builder()
                .summary(aiResponse)
                .createdAt(LocalDateTime.now())
                .generatedBy(getConnectedUserName())
                .build();

        analyseAiHistoriqueRepository.save(historique);

        return new AiManagerSummaryResponse(aiResponse);
    }

    public List<AnalyseAiHistoriqueResponse> getHistory() {
        return analyseAiHistoriqueRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(history -> new AnalyseAiHistoriqueResponse(
                        history.getId(),
                        history.getSummary(),
                        history.getCreatedAt(),
                        history.getGeneratedBy()
                ))
                .toList();
    }

    // pouur avoir le nom de l'utilisateur connecté
    private String getConnectedUserName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }

            return "SYSTEM";
        } catch (Exception e) {
            return "SYSTEM";
        }
    }


}