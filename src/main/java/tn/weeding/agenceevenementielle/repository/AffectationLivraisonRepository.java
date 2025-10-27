package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.AffectationLivraison;

@Repository
public interface AffectationLivraisonRepository extends JpaRepository<AffectationLivraison, Long> {
}
