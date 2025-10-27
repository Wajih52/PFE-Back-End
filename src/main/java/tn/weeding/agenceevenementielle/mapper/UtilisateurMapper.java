package tn.weeding.agenceevenementielle.mapper;

import org.mapstruct.*;
import tn.weeding.agenceevenementielle.dto.UtilisateurInscriptionDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRequestDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRequestPatchDto;
import tn.weeding.agenceevenementielle.entities.Utilisateur;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE) //  ignore les champs non explicitement mappés)
public interface UtilisateurMapper {

    void updateUtilisateurFromUtilisateurResquestDto(UtilisateurRequestDto source, @MappingTarget Utilisateur target);
    void updateUtilisateurFromUtilisateurResquestPatchDto(UtilisateurRequestPatchDto source, @MappingTarget Utilisateur target);

    //pour la mise à jour partielle de l'utilisateur sans ecraser les données
    void updateUtilisateurFromInscriptionDto(UtilisateurInscriptionDto source, @MappingTarget Utilisateur target);



    //pour l'inscription du client (DTO --> Entité )
    @Mapping(target = "idUtilisateur", ignore = true)
    @Mapping(target = "codeUtilisateur", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "etatCompte", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "poste", ignore = true)
    @Mapping(target = "dateEmbauche", ignore = true)
    @Mapping(target = "dateFinContrat", ignore = true)
    @Mapping(target = "statutEmploye", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "activationToken", ignore = true)
    @Mapping(target = "tokenExpiration", ignore = true)
    @Mapping(target = "activationCompte", ignore = true)
    @Mapping(target = "utilisateurRoles", ignore = true)
    @Mapping(target = "reclamations", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "pointages", ignore = true)
    @Mapping(target = "affectationLivraisons", ignore = true)
    Utilisateur dtoInscriptionToUtilisateur(UtilisateurInscriptionDto dto);

    //  Mapper de UtilisateurRequestDto vers Utilisateur
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "activationToken", ignore = true)
    @Mapping(target = "tokenExpiration", ignore = true)
    @Mapping(target = "activationCompte", ignore = true)
    @Mapping(target = "utilisateurRoles", ignore = true)
    @Mapping(target = "reclamations", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "pointages", ignore = true)
    @Mapping(target = "affectationLivraisons", ignore = true)
    Utilisateur requestDtoToUtilisateur(UtilisateurRequestDto dto);




}
