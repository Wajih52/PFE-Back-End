package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Categorie;
import tn.weeding.agenceevenementielle.entities.Produit;
import tn.weeding.agenceevenementielle.entities.TypeProduit;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des produits
 */
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // Recherche par code produit
    Optional<Produit> findByCodeProduit(String codeProduit);

    // Vérifier si un produit existe par son code
    boolean existsByCodeProduit(String codeProduit);

    // Recherche par catégorie
    List<Produit> findByCategorieProduit(Categorie categorie);

    // Recherche par type de produit
    List<Produit> findByTypeProduit(TypeProduit typeProduit);

    // Produits nécessitant une maintenance
    List<Produit> findByMaintenanceRequise(Boolean maintenanceRequise);

    // Produits disponibles (quantité > 0)
    @Query("SELECT p FROM Produit p WHERE p.quantiteDisponible > 0")
    List<Produit> findProduitsDisponibles();

    // Produits en rupture de stock
    @Query("SELECT p FROM Produit p WHERE p.quantiteDisponible = 0")
    List<Produit> findProduitsEnRupture();

    // Produits avec alerte stock critique (avec seuil paramétrable)
    @Query("SELECT p FROM Produit p WHERE p.quantiteDisponible <= :seuil AND p.quantiteDisponible > 0")
    List<Produit> findProduitsStockCritique(@Param("seuil") Integer seuil);

    // Recherche par nom (recherche partielle, insensible à la casse)
    @Query("SELECT p FROM Produit p WHERE LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Produit> searchByNom(@Param("nom") String nom);

    // Recherche multicritères
    @Query("SELECT p FROM Produit p WHERE " +
            "(:categorie IS NULL OR p.categorieProduit = :categorie) AND " +
            "(:typeProduit IS NULL OR p.typeProduit = :typeProduit) AND " +
            "(:minPrix IS NULL OR p.prixUnitaire >= :minPrix) AND " +
            "(:maxPrix IS NULL OR p.prixUnitaire <= :maxPrix) AND " +
            "(:disponible IS NULL OR " +
            "CASE WHEN :disponible = true THEN p.quantiteDisponible > 0 ELSE p.quantiteDisponible = 0 END)")
    List<Produit> searchProduits(
            @Param("categorie") Categorie categorie,
            @Param("typeProduit") TypeProduit typeProduit,
            @Param("minPrix") Double minPrix,
            @Param("maxPrix") Double maxPrix,
            @Param("disponible") Boolean disponible
    );

    // Obtenir les produits les plus loués
    @Query("SELECT p FROM Produit p " +
            "LEFT JOIN p.ligneReservationProduit lr " +
            "GROUP BY p " +
            "ORDER BY COUNT(lr) DESC")
    List<Produit> findProduitsLesPlusLoues();

    // Obtenir les produits les mieux notés
    @Query("SELECT p FROM Produit p " +
            "LEFT JOIN p.avisProduit a " +
            "GROUP BY p " +
            "HAVING AVG(a.note) >= :minNote " +
            "ORDER BY AVG(a.note) DESC")
    List<Produit> findProduitsMieuxNotes(@Param("minNote") Double minNote);
}