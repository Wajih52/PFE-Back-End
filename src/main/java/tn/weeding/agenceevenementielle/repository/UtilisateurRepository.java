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

     /**
      * Compter les utilisateurs par état de compte
      * Ex: combien d'utilisateurs ACTIVE, INACTIVE, SUSPENDED
      */
     @Query("SELECT u.etatCompte, COUNT(u) FROM Utilisateur u " +
             "WHERE u.activationCompte = true " +
             "GROUP BY u.etatCompte")
     List<Object[]> countUtilisateursByEtatCompte();

     /**
      * Distribution des utilisateurs par rôle (pour graphique)
      * Retourne: [nomRole, nombreUtilisateurs]
      */
     @Query("SELECT ur.role.nom, COUNT(DISTINCT u) " +
             "FROM Utilisateur u " +
             "JOIN u.utilisateurRoles ur " +
             "WHERE u.etatCompte = 'ACTIVE' " +
             "GROUP BY ur.role.nom " +
             "ORDER BY COUNT(DISTINCT u) DESC")
     List<Object[]> countUtilisateursByRole();

     /**
      * Nouveaux utilisateurs par période (pour graphique d'évolution)
      * Retourne: [mois, année, nombreNouveauxUtilisateurs]
      */
     @Query(value = "SELECT MONTH(u.date_creation) as mois, " +
             "YEAR(u.date_creation) as annee, " +
             "COUNT(*) as nombre " +
             "FROM utilisateur u " +
             "WHERE u.actif = true " +
             "AND u.date_creation >= :dateDebut " +
             "GROUP BY YEAR(u.date_creation), MONTH(u.date_creation) " +
             "ORDER BY annee DESC, mois DESC",
             nativeQuery = true)
     List<Object[]> countNouveauxUtilisateursParMois(@Param("dateDebut") LocalDateTime dateDebut);

}
