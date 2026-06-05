package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"reservation", "produit", "instancesReservees"})
public class LigneReservation implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idLigneReservation;


    @Column(nullable = false)
    private Integer quantite;



    @Column(nullable = false)
    private Double prixUnitaire;


    LocalDate dateDebut;


    LocalDate dateFin;


    @Enumerated(EnumType.STRING)
    StatutLivraison statutLivraisonLigne;

    @Column(length = 1000)
    private String observations;


    //LigneReservation 1..*-------1 Reservation
    @ManyToOne
    Reservation reservation;

    //ligne Reservation 1..* --------- 1 Produit
    @ManyToOne
    Produit produit;

    //LigneReservation 1..* -------- 0..1 Livraison
    @ManyToOne
    Livraison livraison;

    /**

     * Utilisé UNIQUEMENT pour les produits avec référence (typeProduit = AVEC_REFERENCE)
     * - Pour produits quantitatifs : cette liste est vide/null
     * - Pour produits avec référence : contient les N instances réservées

     * Exemple pour 3 projecteurs réservés :
     * instancesReservees = [PROJ-2025-001, PROJ-2025-002, PROJ-2025-003]
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ligne_reservation_instances",
            joinColumns = @JoinColumn(name = "idLigneReservation"),
            inverseJoinColumns = @JoinColumn(name = "idInstance")
    )
    private Set<InstanceProduit> instancesReservees;




    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Calcule le prix total de cette ligne
     */
    public Double getPrixTotal() {
        if (prixUnitaire != null && quantite != null) {
            return prixUnitaire * quantite;
        }
        return 0.0;
    }

    /**
     * Vérifie si cette ligne concerne un produit avec référence
     */
    public boolean isProduitAvecReference() {
        return produit != null && produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE;
    }

}
