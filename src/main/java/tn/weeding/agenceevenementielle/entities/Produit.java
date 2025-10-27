package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"avisProduit", "ligneReservationProduit", "mouvementStockProduit", "instances"})


public class Produit implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idProduit;
    @Column(unique = true, length = 100)
    private String codeProduit;

    @Column(nullable = false, length = 100)
    private String nomProduit;

    @Column(length = 1000)
    private String descriptionProduit;

    private String imageProduit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorieProduit;

    @Column(nullable = false)
    private Double prixUnitaire;

    /**
     * Quantité initiale totale
     * - Pour enQuantite: stock initial (ex: 50 chaises)
     * - Pour avecReference: nombre total d'instances (ex: 15 projecteurs)
     */
    @Column(nullable = false)
    private Integer quantiteInitial;

    /**
     * Quantité actuellement disponible
     * - Pour enQuantite: nombre d'unités disponibles (géré manuellement)
     * - Pour avecReference: nombre d'instances DISPONIBLES (calculé automatiquement)
     */
    @Column(nullable = false)
    private Integer quantiteDisponible;

    private Boolean maintenanceRequise = false;

    /**
     * Seuil critique de stock
     * Déclenche une alerte quand quantiteDisponible <= seuilCritique
     */
    private Integer seuilCritique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeProduit typeProduit;

    @Column
    private LocalDateTime dateCreation;

    @Column
    private LocalDateTime dateModification;


    @PrePersist
    public void onCreate (){
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }
    @PreUpdate
    public void onUpdate (){
        dateModification = LocalDateTime.now();
    }


    //====================================== Relations ========================

    /**
     * Instances individuelles (uniquement pour typeProduit = avecReference)
     * Permet le suivi détaillé de chaque unité physique
     */
    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstanceProduit> instances;

    // Produit 1 -------- 1..* Avis
    @OneToMany (mappedBy = "produit")
    Set<Avis> avisProduit;

    //Produit 1..* ------ 1 LigneReservation
    @OneToMany(mappedBy = "produit")
    Set<LigneReservation> ligneReservationProduit;

    @OneToMany(mappedBy = "produit")
    private Set<MouvementStock> mouvementStockProduit;

    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Vérifie si le produit est en stock critique
     */
    public boolean isStockCritique() {
        if (seuilCritique == null) {
            seuilCritique = (typeProduit == TypeProduit.avecReference) ? 2 : 5;
        }
        return quantiteDisponible != null && quantiteDisponible <= seuilCritique;
    }

    /**
     * Vérifie si le produit est disponible
     */
    public boolean isDisponible() {
        return quantiteDisponible != null && quantiteDisponible > 0;
    }

    /**
     * Pour les produits avecReference: compte le nombre d'instances disponibles
     */
    public int getNombreInstancesDisponibles() {
        if (typeProduit == TypeProduit.avecReference && instances != null) {
            return (int) instances.stream()
                    .filter(InstanceProduit::isDisponible)
                    .count();
        }
        return quantiteDisponible != null ? quantiteDisponible : 0;
    }
}
