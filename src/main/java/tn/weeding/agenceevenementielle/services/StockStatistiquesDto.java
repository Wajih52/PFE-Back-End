package tn.weeding.agenceevenementielle.services;

import java.util.Date; /**
 * DTO pour les statistiques de stock
 */
public class StockStatistiquesDto {
    public Integer totalEntrees;
    public Integer totalSorties;
    public Integer stockActuel;
    public Integer nombreMouvements;
    public Date dateDernierMouvement;

    public StockStatistiquesDto(Integer totalEntrees, Integer totalSorties, Integer stockActuel,
                                Integer nombreMouvements, Date dateDernierMouvement) {
        this.totalEntrees = totalEntrees;
        this.totalSorties = totalSorties;
        this.stockActuel = stockActuel;
        this.nombreMouvements = nombreMouvements;
        this.dateDernierMouvement = dateDernierMouvement;
    }
}
