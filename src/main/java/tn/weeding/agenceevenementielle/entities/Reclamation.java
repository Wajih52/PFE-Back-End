package tn.weeding.agenceevenementielle.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class Reclamation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idReclamation;
    @Column(name = "codeReclamation")
    String codeReclamation;
    @Temporal(TemporalType.DATE)
    Date dateReclamation;
    String objet ;
    String descriptionReclamation;
    String contactEmail;
    String ContactTelephone;
    @Enumerated(EnumType.STRING)
    StatutReclamation statutReclamation;
    @Enumerated(EnumType.STRING)
    TypeReclamation typeReclamation;

    //Reclamation 0..* ------------------ 1 Utilisateur
    @ManyToOne (optional = true) // pour dire relation facultative
    @JoinColumn(name = "idUtilisateur", nullable = true)
    Utilisateur utilisateur;
}
