package tn.weeding.agenceevenementielle.entities;

import jakarta.persistence.*;
import lombok.Data;
import tn.weeding.agenceevenementielle.entities.enums.StatutDemandeConge;

import java.time.LocalDate;

@Entity
@Data
public class Conge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDemande;

    @ManyToOne
    @JoinColumn(name = "idUtilisateur")
    private Utilisateur employe;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String motif;

    @Enumerated(EnumType.STRING)
    private StatutDemandeConge statut;
}
