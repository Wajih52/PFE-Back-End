package tn.weeding.agenceevenementielle.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.config.AuthenticationFacade;
import tn.weeding.agenceevenementielle.dto.UtilisateurResponseDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRoleResponseDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRoleWithUserDto;
import tn.weeding.agenceevenementielle.entities.Role;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.UtilisateurRole;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRoleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class UtilisateurRoleServiceImpl implements UtilisateurRoleServiceInterface{

    private UtilisateurRoleRepository utilisateurRoleRepository;
    private UtilisateurRepository utilisateurRepository;
    private RoleRepository roleRepository;
    private AuthenticationFacade authenticationFacade;

    @Transactional(readOnly = true)
    @Override
    public List<UtilisateurRole> getAllUtilisateurRoles() {
        return utilisateurRoleRepository.findAll();
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<UtilisateurRole> getUtilisateurRoleById(Long id) {
        return utilisateurRoleRepository.findById(id);
    }

    @Override
    @Transactional
    public UtilisateurRole addUtilisateurRole(Long idUtilisateur, Long idRole) {
        //vérification Utilisateur
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(()->new CustomException("Utilisateur introuvable"));

        //verification Role
        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new CustomException("Rôle introuvable"));

        boolean exists = utilisateurRoleRepository.existsByUtilisateurAndRole(utilisateur,role);
        if(exists){
            throw new CustomException("L'utilisateur " + utilisateur.getPseudo() +
                    " possède déjà le rôle " + role.getNom());
        }

        // Récupérer le code de l'utilisateur connecté
        String pseudoConnecte = authenticationFacade.getCurrentUserPseudo();
        Utilisateur utilisateurConnecte = utilisateurRepository.findByPseudo(pseudoConnecte)
                .orElseThrow(() -> new CustomException("Utilisateur connecté introuvable"));

        String codeUtilisateurConnecte = utilisateurConnecte.getCodeUtilisateur();


        //associer et créer

        UtilisateurRole utilisateurRole = new UtilisateurRole();
        utilisateurRole.setUtilisateur(utilisateur);
        utilisateurRole.setRole(role);
        utilisateurRole.setAttribuePar(codeUtilisateurConnecte);
        utilisateurRole.setDateAffectationRole(LocalDateTime.now());

        //sauvegarder
         UtilisateurRole saved = utilisateurRoleRepository.save(utilisateurRole);

        log.info("✅ Rôle '{}' attribué à '{}' par '{}'",
                role.getNom(),
                utilisateur.getPseudo(),
                codeUtilisateurConnecte
        );

        return saved ;
    }

    @Override
    public UtilisateurRole updateUtilisateurRole(Long idUtilisateurRole, Long idRole) {
        UtilisateurRole utilisateurRole = utilisateurRoleRepository.findById(idUtilisateurRole)
                .orElseThrow(()-> new IllegalArgumentException("UtilisateurRole not found avec l'id "+idUtilisateurRole));

        // Récupérer l'utilisateur et le rôle si on veut les changer
        Utilisateur utilisateur = utilisateurRole.getUtilisateur();
        Role role = utilisateurRole.getRole();

        if (idRole != null) {
            role = roleRepository.findById(idRole)
                    .orElseThrow(() -> new CustomException("Rôle introuvable"));
        }

        // Vérifier si la combinaison existe déjà
        boolean exists = utilisateurRoleRepository.existsByUtilisateurAndRole(utilisateur, role);
        if (exists && (!utilisateurRole.getUtilisateur().equals(utilisateur) || !utilisateurRole.getRole().equals(role))) {
            throw new CustomException("Cette combinaison utilisateur/rôle existe déjà !");
        }

        // Modifier les champs
        utilisateurRole.setUtilisateur(utilisateur);
        utilisateurRole.setRole(role);

        // ✅ Récupérer automatiquement le code de l'utilisateur connecté
        String pseudoConnecte = authenticationFacade.getCurrentUserPseudo();
        Utilisateur utilisateurConnecte = utilisateurRepository.findByPseudo(pseudoConnecte)
                .orElseThrow(() -> new CustomException("Utilisateur connecté introuvable"));

        utilisateurRole.setAttribuePar(utilisateurConnecte.getCodeUtilisateur());

        UtilisateurRole updated = utilisateurRoleRepository.save(utilisateurRole);

        log.info("✅ Relation utilisateur-rôle {} mise à jour par '{}'",
                idUtilisateurRole,
                utilisateurConnecte.getCodeUtilisateur()
        );

        //Sauvegarder
        return updated;
    }

    @Override
    @Transactional
    public void deleteUtilisateurRole(Long id) {
        if(!utilisateurRoleRepository.existsById(id)) {
            throw new IllegalArgumentException("UtilisateurRole not found avec l'id "+id);
        }
        utilisateurRoleRepository.deleteById(id);
    }
    @Transactional(readOnly = true)
    @Override
    public List<UtilisateurRole> getRolesByUtilisateur(Long utilisateurId) {
        return utilisateurRoleRepository.findByUtilisateurIdUtilisateur(utilisateurId);
    }
    @Transactional(readOnly = true)
    @Override
    public List<UtilisateurRoleWithUserDto> getAssociationUtilisateursByRole(Long roleId) {
        List<UtilisateurRole> associations = utilisateurRoleRepository.findByRoleIdRole(roleId);
        return associations.stream()
                .map(ur -> UtilisateurRoleWithUserDto.builder()
                        .idUtilisateurRole(ur.getIdUtilisateurRole())
                        .dateAffectationRole(ur.getDateAffectationRole())
                        .attribuePar(ur.getAttribuePar())
                        // Rôle
                        .idRole(ur.getRole().getIdRole())
                        .nomRole(ur.getRole().getNom())
                        .descriptionRole(ur.getRole().getDescription())
                        // Utilisateur (seulement les infos nécessaires)
                        .idUtilisateur(ur.getUtilisateur().getIdUtilisateur())
                        .pseudo(ur.getUtilisateur().getPseudo())
                        .nom(ur.getUtilisateur().getNom())
                        .prenom(ur.getUtilisateur().getPrenom())
                        .email(ur.getUtilisateur().getEmail())
                        .codeUtilisateur(ur.getUtilisateur().getCodeUtilisateur())
                        .telephone(ur.getUtilisateur().getTelephone().toString())
                        .image(ur.getUtilisateur().getImage())
                        .etatCompte(ur.getUtilisateur().getEtatCompte() != null
                                ? ur.getUtilisateur().getEtatCompte().toString()
                                : null)
                        .build())
                .toList();
    }

    @Override
    public List<UtilisateurRoleResponseDto> getRolesWithDetailsByUtilisateur(Long utilisateurId) {
        // Vérifier que l'utilisateur existe
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable avec l'ID : " + utilisateurId));

        // Récupérer les relations utilisateur-rôle
        List<UtilisateurRole> utilisateurRoles = utilisateurRoleRepository.findByUtilisateurIdUtilisateur(utilisateurId);

        // Mapper vers DTO
        return utilisateurRoles.stream()
                .map(ur -> new UtilisateurRoleResponseDto(
                        ur.getIdUtilisateurRole(),
                        ur.getRole().getIdRole(),
                        ur.getRole().getNom(),
                        ur.getRole().getDescription(),
                        ur.getDateAffectationRole(),
                        ur.getAttribuePar(),
                        ur.getUtilisateur().getIdUtilisateur(),
                        ur.getUtilisateur().getPseudo()
                ))
                .toList();
    }

}
