package tn.weeding.agenceevenementielle.dto.facture;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutFacture;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureResponseDto {

    private Long idFacture;
    private String numeroFacture;
    private TypeFacture typeFacture;
    private StatutFacture statutFacture;

    private LocalDateTime dateCreation;
    private LocalDate dateEcheance;

    private Double montantHT;
    private Double montantTVA;
    private Double montantTTC;
    private Double montantRemise;

    private String notes;
    private String conditionsPaiement;
    private String cheminPDF;

    // Infos réservation
    private Long idReservation;
    private String referenceReservation;

    // Infos client
    private String nomClient;
    private String prenomClient;
    private String emailClient;
    private String telephoneClient;

    private String generePar;
}