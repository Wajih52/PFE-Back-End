package tn.weeding.agenceevenementielle.entities;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Représente une instance physique d'un produit avec référence
 * Permet le suivi individuel de chaque unité (ex: chaque projecteur)
 *
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "produit")
@Builder
public class InstanceProduit implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInstance;

    /**
     * Numéro de série unique de l'instance
     * Ex: "PROJ-2025-001", "CAM-4K-042"
     */
    @Column(unique = true, nullable = false, length = 50)
    private String numeroSerie;

    /**
     * Référence au produit parent (modèle du catalogue)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idProduit", nullable = false)
    private Produit produit;

    /**
     * Statut de l'instance
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutInstance statut;

    /**
     * État physique de l'équipement
     */
    @Enumerated(EnumType.STRING)
    private EtatPhysique etatPhysique;

    /**
     * FK nullable vers LigneReservation
     * Si NULL → instance disponible
     * Si NOT NULL → instance réservée dans cette ligne
     */
    private Long idLigneReservation;

    /**
     * Observations générales
     */
    @Column(length = 1000)
    private String observations;

    /**
     * Date d'acquisition de l'instance
     */
    private LocalDate dateAcquisition;

    /**
     * Date de dernière maintenance
     */
    private LocalDate dateDerniereMaintenance;

    /**
     * Date de prochaine maintenance prévue
     */
    private LocalDate dateProchaineMaintenance;

    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Vérifie si l'instance est actuellement réservée
     */
    public boolean isReservee() {
        return idLigneReservation != null &&
                (statut == StatutInstance.RESERVE || statut == StatutInstance.EN_LIVRAISON);
    }

    /**
     * Vérifie si l'instance est disponible pour réservation
     */
    public boolean isDisponible() {
        return statut == StatutInstance.DISPONIBLE && idLigneReservation == null;
    }

    /**
     * Vérifie si une maintenance est nécessaire
     */
    public boolean maintenanceNecessaire() {
        return dateProchaineMaintenance != null
                && dateProchaineMaintenance.isBefore(LocalDate.now())
                && statut != StatutInstance.EN_MAINTENANCE;
    }
}