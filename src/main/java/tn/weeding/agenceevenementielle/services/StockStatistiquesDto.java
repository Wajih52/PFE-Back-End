package tn.weeding.agenceevenementielle.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * ✅ DTO pour les statistiques de stock d'un produit
 *
 * Contient les informations agrégées sur les mouvements de stock
 * Utilisé pour le tableau de bord et les rapports
 *
 * Sprint 3 : Gestion des produits et du stock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockStatistiquesDto {

    /**
     * Total des entrées de stock (somme de toutes les quantités entrées)
     * Inclut : CREATION, AJOUT_STOCK, ENTREE_STOCK, RETOUR_RESERVATION,
     *          RETOUR_MAINTENANCE, AJOUT_INSTANCE, RETOUR
     */
    private Integer totalEntrees;

    /**
     * Total des sorties de stock (somme de toutes les quantités sorties)
     * Inclut : RETRAIT_STOCK, SORTIE_RESERVATION, MAINTENANCE,
     *          PRODUIT_ENDOMMAGE, SUPPRESSION_INSTANCE, RESERVATION, LIVRAISON
     */
    private Integer totalSorties;

    /**
     * Quantité actuellement disponible en stock
     * Correspond au champ quantiteDisponible du produit
     */
    private Integer quantiteDisponible;

    /**
     * Nombre total de mouvements enregistrés
     * Utile pour évaluer l'activité du produit
     */
    private Integer nombreMouvements;

    /**
     * Date du dernier mouvement effectué
     * Permet de voir si le produit est actif ou dormant
     */
    private Date dateDernierMouvement;

    // ============================================
    // MÉTHODES CALCULÉES (optionnelles)
    // ============================================

    /**
     * Calcule le solde net des mouvements (entrées - sorties)
     * Devrait correspondre à la quantité disponible si les données sont cohérentes
     *
     * @return Différence entre entrées et sorties
     */
    public Integer getSoldeNet() {
        return (totalEntrees != null ? totalEntrees : 0) -
                (totalSorties != null ? totalSorties : 0);
    }

    /**
     * Calcule le taux de rotation du stock (sorties / quantité disponible)
     * Plus le taux est élevé, plus le produit est souvent utilisé
     *
     * @return Taux de rotation en pourcentage
     */
    public Double getTauxRotation() {
        if (quantiteDisponible == null || quantiteDisponible == 0) {
            return 0.0;
        }

        double sorties = totalSorties != null ? totalSorties : 0;
        return (sorties / quantiteDisponible) * 100.0;
    }

    /**
     * Calcule le taux d'utilisation (sorties / entrées)
     * Permet de voir quel pourcentage du stock entré a été utilisé
     *
     * @return Taux d'utilisation en pourcentage
     */
    public Double getTauxUtilisation() {
        if (totalEntrees == null || totalEntrees == 0) {
            return 0.0;
        }

        double sorties = totalSorties != null ? totalSorties : 0;
        return (sorties / totalEntrees) * 100.0;
    }

    /**
     * Vérifie si le produit est actif (au moins un mouvement récent)
     * Considère un produit actif s'il a eu un mouvement dans les 30 derniers jours
     *
     * @return true si le produit est actif, false sinon
     */
    public Boolean isActif() {
        if (dateDernierMouvement == null) {
            return false;
        }

        long diffEnMillis = new Date().getTime() - dateDernierMouvement.getTime();
        long diffEnJours = diffEnMillis / (1000 * 60 * 60 * 24);

        return diffEnJours <= 30;
    }

    /**
     * Obtient un résumé textuel des statistiques
     * Utilisé pour les logs et l'affichage rapide
     *
     * @return Description textuelle des stats
     */
    public String getResume() {
        return String.format(
                "Stats: %d entrées, %d sorties, %d disponible, %d mouvements",
                totalEntrees != null ? totalEntrees : 0,
                totalSorties != null ? totalSorties : 0,
                quantiteDisponible != null ? quantiteDisponible : 0,
                nombreMouvements != null ? nombreMouvements : 0
        );
    }

    /**
     * Obtient le niveau d'activité du produit
     * Basé sur le nombre de mouvements
     *
     * @return "FAIBLE", "MOYEN" ou "ÉLEVÉ"
     */
    public String getNiveauActivite() {
        if (nombreMouvements == null || nombreMouvements == 0) {
            return "AUCUNE";
        } else if (nombreMouvements < 5) {
            return "FAIBLE";
        } else if (nombreMouvements < 20) {
            return "MOYEN";
        } else {
            return "ÉLEVÉ";
        }
    }

    /**
     * Vérifie si les données sont cohérentes
     * Le solde net devrait être proche de la quantité disponible
     *
     * @return true si cohérent (différence < 10%), false sinon
     */
    public Boolean isCoherent() {
        if (quantiteDisponible == null || quantiteDisponible == 0) {
            return getSoldeNet() == 0;
        }

        int solde = getSoldeNet();
        double difference = Math.abs(solde - quantiteDisponible);
        double tolerance = quantiteDisponible * 0.1; // 10% de tolérance

        return difference <= tolerance;
    }

    /**
     * Obtient un message d'alerte si les données sont incohérentes
     *
     * @return Message d'alerte ou null si tout est OK
     */
    public String getMessageAlerte() {
        if (!isCoherent()) {
            return String.format(
                    "⚠️ Incohérence détectée : solde calculé (%d) ≠ quantité disponible (%d)",
                    getSoldeNet(),
                    quantiteDisponible
            );
        }
        return null;
    }
}