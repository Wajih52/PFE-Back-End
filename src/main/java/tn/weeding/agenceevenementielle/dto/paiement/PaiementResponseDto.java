package tn.weeding.agenceevenementielle.dto.paiement;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.ModePaiement;
import tn.weeding.agenceevenementielle.entities.enums.StatutPaiement;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaiementResponseDto {
    private Long idPaiement;
    private String codePaiement;
    private Long idReservation;
    private String referenceReservation;
    private Double montantPaiement;
    private ModePaiement modePaiement;
    private StatutPaiement statutPaiement;
    private LocalDateTime datePaiement;
    private LocalDateTime dateValidation;
    private String descriptionPaiement;
    private String referenceExterne;
    private String validePar;
    private String nomClient;
    private String prenomClient;
    private String emailClient;
    private Double montantTotalReservation;
    private Double montantDejaPayeAvant;
    private Double montantRestantApres;
    private Boolean paiementComplet;
}