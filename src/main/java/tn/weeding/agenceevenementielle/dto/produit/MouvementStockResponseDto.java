package tn.weeding.agenceevenementielle.dto.produit;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;

import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MouvementStockResponseDto {

    private Long idMouvement;
    private TypeMouvement typeMouvement;
    private Integer quantite;
    private Integer quantiteAvant;
    private Integer quantiteApres;
    private Date dateMouvement;
    private String motif;
    private String effectuePar;
    private Long idReservation;

    private Long idProduit;
    private String nomProduit;
    private String codeProduit;

    // === Informations de l'instance (si applicable) ===
    private Long idInstance;
    private String numeroSerie;
    private String codeInstance;
}