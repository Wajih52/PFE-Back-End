package tn.weeding.agenceevenementielle.dto.statistiques;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO pour les statistiques globales du dashboard
 * Regroupe toutes les métriques essentielles pour la prise de décision
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatistiquesDto {

    // ============ KPIs PRINCIPAUX ============

    /**
     * Chiffre d'affaires total (tous les paiements validés)
     */
    private Double chiffreAffairesTotal;

    /**
     * CA du mois en cours
     */
    private Double chiffreAffairesMoisActuel;

    /**
     * Évolution du CA par rapport au mois précédent (en %)
     */
    private Double evolutionCAMensuel;

    /**
     * Nombre total de réservations
     */
    private Long nombreTotalReservations;

    /**
     * Nombre de réservations confirmées
     */
    private Long nombreReservationsConfirmees;

    /**
     * Nombre de devis en attente
     */
    private Long nombreDevisEnAttente;

    /**
     * Nombre total de clients
     */
    private Long nombreClients;

    /**
     * Nombre de nouveaux clients ce mois
     */
    private Long nouveauxClientsMois;

    /**
     * Panier moyen (CA total / nombre de réservations confirmées)
     */
    private Double panierMoyen;

    /**
     * Taux de conversion devis → réservation (en %)
     */
    private Double tauxConversion;

    // ============ ALERTES ============

    /**
     * Nombre de produits en stock critique
     */
    private Long produitsStockCritique;

    /**
     * Nombre de réclamations en cours
     */
    private Long reclamationsEnCours;

    /**
     * Nombre de paiements en retard
     */
    private Long paiementsEnRetard;

    /**
     * Nombre de livraisons du jour
     */
    private Long livraisonsAujourdhui;

    /**
     * Nombre de retours attendus aujourd'hui
     */
    private Long retoursAujourdhui;

    // ============ GRAPHIQUES ============

    /**
     * Évolution du CA sur les 12 derniers mois
     * Map<"Janvier 2025", 15000.0>
     */
    private List<MoisChiffreAffairesDto> evolutionCA12Mois;

    /**
     * Répartition des réservations par statut
     * Map<"CONFIRME", 45>
     */
    private Map<String, Long> repartitionReservationsParStatut;

    /**
     * Top 10 des produits les plus loués
     */
    private List<TopProduitDto> topProduitsLoues;

    /**
     * Top 10 des produits par CA généré
     */
    private List<TopProduitDto> topProduitsCA;

    /**
     * Répartition du CA par catégorie de produits
     * Map<"MOBILIER", 25000.0>
     */
    private Map<String, Double> caParCategorie;

    /**
     * Évolution du nombre de réservations sur les 12 derniers mois
     */
    private List<MoisNombreReservationsDto> evolutionReservations12Mois;

    /**
     * Moyennes des notes par catégorie de produits
     */
    private Map<String, Double> moyenneNotesParCategorie;

    // ============ STATISTIQUES EMPLOYÉS ============

    /**
     * Nombre total d'employés actifs
     */
    private Long nombreEmployesActifs;

    /**
     * Taux de présence moyen ce mois (%)
     */
    private Double tauxPresenceMoyen;

    /**
     * Top 5 employés par nombre de livraisons effectuées
     */
    private List<TopEmployeDto> topEmployesLivraisons;

    // ============ PÉRIODE D'ANALYSE ============

    /**
     * Date de début de la période analysée
     */
    private LocalDate dateDebut;

    /**
     * Date de fin de la période analysée
     */
    private LocalDate dateFin;

    // ============ DTOs INTERNES ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoisChiffreAffairesDto {
        private String mois;           // "Janvier 2025"
        private Double chiffreAffaires;
        private Long annee;
        private Long moisNumero;    // 1-12
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoisNombreReservationsDto {
        private String mois;
        private Long nombreReservations;
        private Long annee;
        private Long moisNumero;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduitDto {
        private Long idProduit;
        private String nomProduit;
        private String codeProduit;
        private String imageProduit;
        private Long nombreLocations;
        private Double chiffreAffaires;
        private Double moyenneNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopEmployeDto {
        private Long idEmploye;
        private String nomComplet;
        private String email;
        private Long nombreLivraisons;
        private Double tauxPresence;
        private String imageProfil;
    }
}