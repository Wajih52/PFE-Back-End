package tn.weeding.agenceevenementielle.dto.livraison;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de réponse pour une affectation de livraison
 * Sprint 6 - Gestion des livraisons
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AffectationLivraisonDto {

    private Long idAffectation;
    private LocalDate dateAffectation;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String notes;

    /**
     * Informations sur l'employé affecté
     */
    private Long idEmploye;
    private String nomEmploye;
    private String prenomEmploye;
    private String emailEmploye;
    private String telephoneEmploye;

    /**
     * Informations sur la livraison associée
     */
    private Long idLivraison;
    private String titreLivraison;
}