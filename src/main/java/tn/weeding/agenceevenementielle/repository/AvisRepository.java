package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Avis;
import org.springframework.stereotype.Repository;


@Repository
public interface AvisRepository extends JpaRepository<Avis, Long> {
}
