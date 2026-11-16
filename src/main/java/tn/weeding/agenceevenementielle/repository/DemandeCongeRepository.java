package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Conge;
import tn.weeding.agenceevenementielle.entities.enums.StatutDemandeConge;

import java.util.List;

@Repository
public interface DemandeCongeRepository extends JpaRepository<Conge, Long> {
    // Pour l'historique personnel de l'employé
    List<Conge> findByEmployeIdUtilisateurOrderByDateDebutDesc(Long idUtilisateur);

    // Pour l'admin (filtrer par statut ou voir tout l'historique)
    List<Conge> findAllByOrderByDateDebutDesc();
    List<Conge> findByStatutOrderByDateDebutDesc(StatutDemandeConge statut);
}
