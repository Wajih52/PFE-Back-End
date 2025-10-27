package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Pointage;
import org.springframework.stereotype.Repository;


@Repository

public interface PointageRepository extends JpaRepository<Pointage, Long> {
}
