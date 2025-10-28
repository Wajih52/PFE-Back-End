package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entité représentant l'historique des mouvements de stock
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MouvementStock implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMouvement;

    @ManyToOne
    @JoinColumn(name = "idProduit")
    private Produit produit;

    @Enumerated(EnumType.STRING)
    private TypeMouvement typeMouvement;

    private Integer quantite;
    private Integer quantiteAvant;
    private Integer quantiteApres;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateMouvement;

    private String motif;
    private String effectuePar;
    private Long idReservation;

    private String codeInstance ;

    @PrePersist
    protected void onCreate() {
        dateMouvement = new Date();
    }
}