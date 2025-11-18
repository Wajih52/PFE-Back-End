package tn.weeding.agenceevenementielle.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;

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

    public MouvementStock(Produit produit, TypeMouvement typeMouvement,Integer quantite, String motif, String effectuePar) {
        this.produit = produit;
        this.typeMouvement = typeMouvement;
        this.quantite = quantite;
        this.motif = motif;
        this.effectuePar = effectuePar;
    }

    public MouvementStock(Produit produit, TypeMouvement typeMouvement, Integer quantite, Integer quantiteAvant, Integer quantiteApres, String motif, String effectuePar) {
        this.motif = motif;
        this.quantiteApres = quantiteApres;
        this.quantiteAvant = quantiteAvant;
        this.quantite = quantite;
        this.effectuePar = effectuePar;
        this.typeMouvement = typeMouvement;
        this.produit = produit;
    }



    @ManyToOne
    @JoinColumn(name = "idProduit")
    private Produit produit;

    // Champs pour garder l'info même si produit supprimé
    private String nomProduitArchive;
    private String codeProduitArchive;
    private Long idProduitArchive;

    @Enumerated(EnumType.STRING)
    private TypeMouvement typeMouvement;


    // Quantité du mouvement (toujours positif)
    private Integer quantite;

    // Stock avant et après (pour produits EN_QUANTITE)
    private Integer quantiteAvant;
    private Integer quantiteApres;

    // Pour produits AVEC_REFERENCE
    private String codeInstance;
    private Long idInstance;

    // Informations sur le mouvement
    private String motif;

    private String effectuePar;

    // Réservation associée (si applicable)
    private String referenceReservation;
    private Long idReservation;

    // Pour les réservations, ajouter les dates de période
    private LocalDate dateDebut;
    private LocalDate dateFin;

    private LocalDateTime dateMouvement;






    @PrePersist
    protected void onCreate() {
        dateMouvement = LocalDateTime.now();
    }
}