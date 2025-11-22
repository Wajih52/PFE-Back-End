package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Livraison implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idLivraison;
    String titreLivraison;
    String adresserLivraison;

    LocalDate dateLivraison;
    LocalTime heureLivraison;

    @Enumerated(EnumType.STRING)
    StatutLivraison statutLivraison;

    String observations;

    //Audit automatique
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime dateModification;

    //Livraison 0..1 ------------ 1..* LigneReservation
    @OneToMany(mappedBy = "livraison")
    Set<LigneReservation> ligneReservations;

    //Livraison 1 ----------- 1..* AffectationLivraison
    @OneToMany
    Set<AffectationLivraison> affectationLivraisons;
}
