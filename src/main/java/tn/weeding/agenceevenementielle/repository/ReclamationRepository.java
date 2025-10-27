package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Reclamation;
import org.springframework.stereotype.Repository;


@Repository

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
}
