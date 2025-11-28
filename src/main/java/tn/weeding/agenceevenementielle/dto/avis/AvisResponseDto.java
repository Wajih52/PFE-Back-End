package tn.weeding.agenceevenementielle.dto.avis;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutAvis;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisResponseDto {

    private Long idAvis;
    private Integer note;
    private String commentaire;
    private LocalDateTime dateAvis;
    private StatutAvis statut;
    private Boolean visible;

    // Info du client
    private Long idClient;
    private String nomClient;
    private String prenomClient;
    private String emailClient;

    // Info du produit
    private Long idProduit;
    private String nomProduit;
    private String codeProduit;

    // Info réservation
    private Long idReservation;
    private String numeroReservation;
    private LocalDate dateDebutReservation;
    private LocalDate dateFinReservation;

    // Info modération (visible uniquement pour ADMIN)
    private String commentaireModeration;
    private LocalDateTime dateModeration;

    // Métadonnées
    private Boolean peutEtreModifie; // Le client peut-il modifier son avis ?
    private Boolean peutEtreSupprime; // Le client peut-il supprimer son avis ?
}