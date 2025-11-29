package tn.weeding.agenceevenementielle.dto.calendrier;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO pour représenter un événement dans le calendrier
 * Peut représenter soit une réservation, soit une livraison
 * Sprint 7 - Gestion du calendrier
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendrierEventDto {

    // ============================================
    // IDENTIFIANTS
    // ============================================
    private Long id;
    private String type; // "RESERVATION" ou "LIVRAISON"
    private String reference; // RES-2025-0001 ou LIV-...

    // ============================================
    // INFORMATIONS TEMPORELLES
    // ============================================
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime heure; // Pour les livraisons principalement

    // ============================================
    // INFORMATIONS DE L'ÉVÉNEMENT
    // ============================================
    private String titre;
    private String description;
    private String adresse;

    // ============================================
    // STATUTS
    // ============================================
    private String statut; // Statut de la réservation ou livraison
    private String couleur; // Couleur pour le calendrier (hex)

    // ============================================
    // INFORMATIONS CLIENT
    // ============================================
    private Long idClient;
    private String nomClient;
    private String prenomClient;
    private String emailClient;
    private String telephoneClient;

    // ============================================
    // INFORMATIONS EMPLOYÉ (pour livraisons)
    // ============================================
    private Long idEmploye;
    private String nomEmploye;
    private String prenomEmploye;

    // ============================================
    // INFORMATIONS PRODUITS
    // ============================================
    private Integer nombreProduits;
    private String produitsResume; // Ex: "10 Chaises, 2 Tables..."

    // ============================================
    // INFORMATIONS FINANCIÈRES (réservations)
    // ============================================
    private Double montantTotal;
    private Double montantPaye;
    private Boolean paiementComplet;

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Obtenir la couleur selon le type et le statut
     */
    public static String getCouleurParStatut(String type, String statut) {
        if ("RESERVATION".equals(type)) {
            return switch (statut) {
                case "EN_ATTENTE" -> "#FFA726"; // Orange (devis)
                case "CONFIRME" -> "#66BB6A"; // Vert (confirmé)
                case "ANNULE" -> "#EF5350"; // Rouge
                case "TERMINE" -> "#42A5F5"; // Bleu
                default -> "#9E9E9E"; // Gris
            };
        } else if ("LIVRAISON".equals(type)) {
            return switch (statut) {
                case "NOT_TODAY" -> "#BDBDBD"; // Gris clair
                case "EN_ATTENTE" -> "#FFB74D"; // Orange clair
                case "EN_COURS" -> "#FDD835"; // Jaune
                case "LIVREE" -> "#66BB6A"; // Vert
                case "EN_RETOUR" -> "#AB47BC"; // Violet
                case "RETOURNEE" -> "#29B6F6"; // Bleu ciel
                default -> "#9E9E9E";
            };
        }
        return "#9E9E9E";
    }

    /**
     * Obtenir l'icône selon le type
     */
    public static String getIconeParType(String type) {
        return switch (type) {
            case "RESERVATION" -> "📅";
            case "LIVRAISON" -> "🚚";
            default -> "📌";
        };
    }
}