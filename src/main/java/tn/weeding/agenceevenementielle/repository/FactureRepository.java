package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Facture;
import tn.weeding.agenceevenementielle.entities.enums.StatutFacture;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    Optional<Facture> findByNumeroFacture(String numeroFacture);

    List<Facture> findByReservation_IdReservation(Long idReservation);

    List<Facture> findByReservation_IdReservationAndTypeFacture(Long idReservation, TypeFacture typeFacture);

    List<Facture> findByStatutFacture(StatutFacture statutFacture);

    List<Facture> findByTypeFacture(TypeFacture typeFacture);

    List<Facture> findByReservation_Utilisateur_IdUtilisateur(Long idClient);

    boolean existsByReservation_IdReservationAndTypeFacture(Long idReservation, TypeFacture typeFacture);
}