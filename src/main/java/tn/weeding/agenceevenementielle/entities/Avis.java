package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class Avis  implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idAvis;
    @Temporal(TemporalType.DATE)
    Date dateAvis;
    Integer note;
    String commentaire;

    //Avis 1..* -------- 1 Produit
    @ManyToOne
    Produit produit ;
}
