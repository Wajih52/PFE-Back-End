package tn.weeding.agenceevenementielle.dto.avis;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesAvisDto {

    private Long idProduit;
    private String nomProduit;

    private Long nombreTotalAvis;
    private Long nombreAvisApprouves;
    private Long nombreAvisEnAttente;
    private Long nombreAvisRejetes;

    private Double moyenneNotes;

    // Répartition par note
    private Long nombre5Etoiles;
    private Long nombre4Etoiles;
    private Long nombre3Etoiles;
    private Long nombre2Etoiles;
    private Long nombre1Etoile;

    // Pourcentages
    private Double pourcentage5Etoiles;
    private Double pourcentage4Etoiles;
    private Double pourcentage3Etoiles;
    private Double pourcentage2Etoiles;
    private Double pourcentage1Etoile;
}