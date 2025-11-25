package tn.weeding.agenceevenementielle.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.PrioriteReclamation;
import tn.weeding.agenceevenementielle.entities.enums.StatutReclamation;
import tn.weeding.agenceevenementielle.entities.enums.TypeReclamation;

import java.io.Serializable;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Reclamation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idReclamation;

    @Column(name = "codeReclamation", unique = true, nullable = false)
    String codeReclamation;

    LocalDateTime dateReclamation;

    @Column(length = 200)
    String objet ;

    @Column(nullable = false,length = 1000)
    String descriptionReclamation;

    @Column(nullable = false)
    String contactEmail;


    String contactTelephone;

    @Enumerated(EnumType.STRING)
    StatutReclamation statutReclamation;

    @Enumerated(EnumType.STRING)
    TypeReclamation typeReclamation;

    @Enumerated(EnumType.STRING)
    @Column( nullable = false)
    private PrioriteReclamation prioriteReclamation;

    // Réponse de l'admin/employé
    @Column( length = 1000)
    private String reponse;


    private LocalDateTime dateReponse;

    // Traité par (code de l'utilisateur admin/employé)
    private String traitePar;

    //Reclamation 0..* ------------------ 1 Utilisateur
    @ManyToOne  // pour dire relation facultative
    @JoinColumn(name = "idUtilisateur", nullable = true)
    Utilisateur utilisateur;

    // Peut être liée à une réservation (facultatif)
    @ManyToOne
    @JoinColumn(name = "idReservation", nullable = true)
    private Reservation reservation;
}
