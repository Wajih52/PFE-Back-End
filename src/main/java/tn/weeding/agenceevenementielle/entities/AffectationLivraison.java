package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
public class AffectationLivraison implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idAffectationLivraison;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    LocalDate dateAffectationLivraison;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    LocalTime heureAffectation;

    String notes;

    //AffectationLivraison 1..* --------------- 1 Livraison
    @ManyToOne
    Livraison livraison;

    //AffectationLivraison 1..* ------------- 1 Utilisateur
    @ManyToOne
    Utilisateur utilisateur;
}
