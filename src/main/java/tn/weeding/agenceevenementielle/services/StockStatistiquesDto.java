package tn.weeding.agenceevenementielle.services;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


/**
 * DTO pour les statistiques de stock d'un produit
 *
 * Contient les informations agrégées sur les mouvements de stock
 *
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
    private LocalDateTime dateDernierMouvement;

}