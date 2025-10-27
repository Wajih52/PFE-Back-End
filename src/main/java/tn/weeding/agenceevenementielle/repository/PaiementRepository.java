package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Paiement;
import org.springframework.stereotype.Repository;


@Repository

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
}
