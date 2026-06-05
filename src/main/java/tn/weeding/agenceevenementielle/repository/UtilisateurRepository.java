package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

     boolean existsByPseudoAndActivationCompteTrue(String pseudo);
     boolean existsByPseudo(String pseudo);

     Optional<Utilisateur> findByPseudoOrEmail(String pseudo, String email);

     Optional<Utilisateur> findByActivationToken(String token);

     Optional<Utilisateur> findByEmail(String email);

     Optional<Utilisateur> findByPseudo(String pseudo);

     Optional<Utilisateur> findByResetPasswordToken(String token);


     @Query(value = "SELECT u.codeClient FROM utilisateur u " +
             "WHERE u.codeClient LIKE CONCAT(:prefix, '%') " +
             "ORDER BY u.idUtilisateur DESC " +
             "LIMIT 1",
             nativeQuery = true)
     Optional<String> findLastCodeByPrefix(@Param("prefix") String prefix);

     //=================================================
     // Statistiques utilisateurs (Dashboard)
     //=================================================

     /**
      * Compter les utilisateurs actifs ayant un rôle spécifique
      * Ex: compter tous les ADMIN actifs
      */
     @Query("SELECT COUNT(DISTINCT u) FROM Utilisateur u " +
             "JOIN u.utilisateurRoles ur " +
             "WHERE ur.role.nom = :nomRole " +
             "AND u.etatCompte = 'ACTIVE'")
     Long countByRoleAndActifTrue(@Param("nomRole") String nomRole);

     /**
      * Compter les nouveaux utilisateurs sur une période avec un rôle spécifique
      * Ex: nouveaux EMPLOYE inscrits ce mois
      */
     @Query("SELECT COUNT(DISTINCT u) FROM Utilisateur u " +
             "JOIN u.utilisateurRoles ur " +
             "WHERE ur.role.nom = :nomRole " +
             "AND u.dateCreation BETWEEN :debut AND :fin " +
             "AND u.etatCompte = 'ACTIVE'")
     Long countByRoleAndDateCreationBetweenAndActifTrue(
             @Param("nomRole") String nomRole,
             @Param("debut") LocalDateTime debut,
             @Param("fin") LocalDateTime fin
     );

     /**
      * Compter les utilisateurs actifs ayant l'un des rôles spécifiés
      * Ex: compter tous les ADMIN + MANAGER
      */
     @Query("SELECT COUNT(DISTINCT u) FROM Utilisateur u " +
             "JOIN u.utilisateurRoles ur " +
             "WHERE ur.role.nom IN :nomsRoles " +
             "AND u.etatCompte = 'ACTIVE'")
     Long countByRolesInAndActifTrue(@Param("nomsRoles") List<String> nomsRoles);

     /**
      * Trouver les utilisateurs actifs ayant l'un des rôles spécifiés
      * Ex: récupérer tous les ADMIN + MANAGER pour affichage
      */
     @Query("SELECT DISTINCT u FROM Utilisateur u " +
             "JOIN FETCH u.utilisateurRoles ur " +
             "WHERE ur.role.nom IN :nomsRoles " +
             "AND u.etatCompte = 'ACTIVE'")
     List<Utilisateur> findByRolesInAndActifTrue(@Param("nomsRoles") List<String> nomsRoles);

}
