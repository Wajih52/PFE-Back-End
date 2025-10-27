package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.LigneReservation;
import org.springframework.stereotype.Repository;


@Repository

public interface LigneReservationRepository extends JpaRepository<LigneReservation, Long> {
}
