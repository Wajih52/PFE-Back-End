package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Role;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.UtilisateurRole;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository

public interface UtilisateurRoleRepository extends JpaRepository<UtilisateurRole, Long> {

    // Récupérer tous les rôles attribués à un utilisateur
    List<UtilisateurRole> findByUtilisateurIdUtilisateur(Long utilisateurId);

    // Récupérer tous les utilisateurs ayant un rôle précis
    List<UtilisateurRole> findByRoleIdRole(Long roleId);

    // Vérifier si un utilisateur a déjà un rôle spécifique
    boolean existsByUtilisateurAndRole(Utilisateur utilisateur, Role role);

}
