package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Reservation;
import org.springframework.stereotype.Repository;


@Repository

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
