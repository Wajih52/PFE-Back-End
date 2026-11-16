package tn.weeding.agenceevenementielle.dto.conge;

import lombok.Builder;
import lombok.Data;
import tn.weeding.agenceevenementielle.entities.enums.StatutDemandeConge;
import java.time.LocalDate;

@Data
@Builder
public class DemandeCongeResponseDto {
    private Long idDemande;
    private Long idUtilisateur;
    private String nomCompletEmploye;
    private String posteEmploye;
    private Integer soldeCongesRestant;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private long nombreJoursOuvrables;
    private String motif;
    private StatutDemandeConge statut;
}
