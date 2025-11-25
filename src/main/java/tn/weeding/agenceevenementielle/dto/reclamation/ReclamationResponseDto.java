package tn.weeding.agenceevenementielle.dto.reclamation;

import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;
import tn.weeding.agenceevenementielle.entities.enums.TypeReclamation;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour une réclamation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReclamationResponseDto {

    private Long idReclamation;
    private String codeReclamation;
    private LocalDateTime dateReclamation;
    private String objet;
    private String descriptionReclamation;
    private String contactEmail;
    private String contactTelephone;
    private StatutReclamation statutReclamation;
    private TypeReclamation typeReclamation;
    private PrioriteReclamation prioriteReclamation;

    // Réponse
    private String reponse;
    private LocalDateTime dateReponse;
    private String traitePar;

    // Informations utilisateur (si connecté)
    private Long idUtilisateur;
    private String nomUtilisateur;
    private String prenomUtilisateur;

    // Informations réservation (si applicable)
    private Long idReservation;
    private String codeReservation;
}