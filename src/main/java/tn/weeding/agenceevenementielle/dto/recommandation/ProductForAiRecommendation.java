package tn.weeding.agenceevenementielle.dto.recommandation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductForAiRecommendation {
    Long idProduit;
    String nomProduit;
    String categorie;
    String typeProduit;
    Double prixUnitaire;
    Integer quantiteDisponible;
}
