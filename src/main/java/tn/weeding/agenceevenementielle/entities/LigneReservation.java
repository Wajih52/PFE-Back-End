package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

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

    /**
     * Quantité réservée
     * - Pour produits quantitatifs (enQuantite): nombre d'unités (ex: 50 chaises)
     * - Pour produits avec référence (avecReference): nombre d'instances (ex: 3 projecteurs)
     */
    @Column(nullable = false)
    private Integer quantite;


    /**
     * Prix unitaire au moment de la réservation
     */
    @Column(nullable = false)
    private Double prixUnitaire;

    /**
     * Date de début de la réservation
     */
    @Temporal(TemporalType.DATE)
    Date dateDebut;

    /**
     * Date de fin de la réservation
     */
    @Temporal(TemporalType.DATE)
    Date dateFin;

    /**
     * Statut de la ligne de réservation
     */
    @Enumerated(EnumType.STRING)
    StatutLivraison statutLivraisonLigne;




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
     * NOUVEAU : Instances spécifiques réservées
     *
     * Utilisé UNIQUEMENT pour les produits avec référence (typeProduit = avecReference)
     * - Pour produits quantitatifs: cette liste est vide/null (NORMAL)
     * - Pour produits avec référence: contient les N instances réservées
     *
     * Exemple pour 3 projecteurs réservés:
     * instancesReservees = [PROJ-2025-001, PROJ-2025-002, PROJ-2025-003]
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "idLigneReservation")
    private Set<InstanceProduit> instancesReservees;

    /**
     * Observations sur cette ligne de réservation
     */
    @Column(length = 1000)
    private String observations;


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
        return produit != null && produit.getTypeProduit() == TypeProduit.avecReference;
    }

    /**
     * Vérifie si toutes les instances sont affectées
     * (uniquement pour produits avec référence)
     */
    public boolean toutesInstancesAffectees() {
        if (!isProduitAvecReference()) {
            return true; // N/A pour produits quantitatifs
        }
        return instancesReservees != null && instancesReservees.size() == quantite;
    }

    /**
     * Retourne le nombre d'instances affectées
     */
    public int getNombreInstancesAffectees() {
        return instancesReservees != null ? instancesReservees.size() : 0;
    }
}
