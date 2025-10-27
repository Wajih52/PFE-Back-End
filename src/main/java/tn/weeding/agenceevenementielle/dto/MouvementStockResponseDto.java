package tn.weeding.agenceevenementielle.dto;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.TypeMouvement;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MouvementStockResponseDto {

    private Long idMouvement;
    private Long idProduit;
    private String nomProduit;
    private String codeProduit;
    private TypeMouvement typeMouvement;
    private Integer quantite;
    private Integer quantiteAvant;
    private Integer quantiteApres;
    private Date dateMouvement;
    private String motif;
    private String effectuePar;
    private Long idReservation;
}