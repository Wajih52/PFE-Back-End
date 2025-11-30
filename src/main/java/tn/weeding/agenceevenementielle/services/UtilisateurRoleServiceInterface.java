package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.UtilisateurRoleResponseDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRoleWithUserDto;
import tn.weeding.agenceevenementielle.entities.UtilisateurRole;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRoleServiceInterface {

    // Récupérer tous les UtilisateurRole
    List<UtilisateurRole> getAllUtilisateurRoles();

    // Récupérer un UtilisateurRole par son ID
    Optional<UtilisateurRole> getUtilisateurRoleById(Long id);

    // Ajouter un nouveau UtilisateurRole
    UtilisateurRole addUtilisateurRole(Long idUtilisateur,Long idRole);

    // Mettre à jour un UtilisateurRole existant
    UtilisateurRole updateUtilisateurRole(Long idUtilisateurRole, Long idRole);

    // Supprimer un UtilisateurRole par son ID
    void deleteUtilisateurRole(Long id);

    // Récupérer tous les rôles d'un utilisateur
    List<UtilisateurRole> getRolesByUtilisateur(Long utilisateurId);

    // Récupérer toutes les associations utilisateurs ayant un rôle donné
    List<UtilisateurRoleWithUserDto> getAssociationUtilisateursByRole(Long roleId);

    // méthode pour récupérer les rôles avec les détails
    List<UtilisateurRoleResponseDto> getRolesWithDetailsByUtilisateur(Long utilisateurId);
}
