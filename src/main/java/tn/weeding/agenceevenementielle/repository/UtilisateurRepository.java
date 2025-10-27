package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

     boolean existsByPseudoAndActivationCompteTrue(String pseudo);
     boolean existsByPseudo(String pseudo);
     boolean existsByNom(String nom);
     boolean existsByEmail(String email);
     Optional<Utilisateur> findByPseudoOrEmail(String pseudo, String email);
     Optional<Utilisateur> findByActivationToken(String token);
     Optional<Utilisateur> findByEmail(String email);
     Optional<Utilisateur> findByPseudo(String pseudo);
     Optional<Utilisateur> findByResetPasswordToken(String token);
     Optional<Utilisateur> findByCodeUtilisateur(String codeUtilisateur);

     @Query(value = "SELECT u.codeClient FROM utilisateur u " +
             "WHERE u.codeClient LIKE CONCAT(:prefix, '%') " +
             "ORDER BY u.idUtilisateur DESC " +
             "LIMIT 1",
             nativeQuery = true)
     Optional<String> findLastCodeByPrefix(@Param("prefix") String prefix);


}
