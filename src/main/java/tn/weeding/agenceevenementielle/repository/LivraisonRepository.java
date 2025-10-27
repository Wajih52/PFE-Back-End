package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Livraison;
import org.springframework.stereotype.Repository;


@Repository

public interface LivraisonRepository extends JpaRepository<Livraison, Long> {
}
