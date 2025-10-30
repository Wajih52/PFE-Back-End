package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.*;
import tn.weeding.agenceevenementielle.dto.authentification.ChangePasswordDto;
import tn.weeding.agenceevenementielle.entities.Utilisateur;

import java.util.List;

public interface UtilisateurServiceInterface {


    //--------------------------Crud Utilisateur ---------------------------------------
    UtilisateurResponseDto ajouterUtilisateur(UtilisateurRequestDto utilisateurRequestDto);
    UtilisateurResponseDto modifierUtilisateurPut(Long id,UtilisateurRequestDto utilisateurRequestDto);
    UtilisateurResponseDto modifierUtilisateurPatch(Long id, UtilisateurRequestPatchDto utilisateurRequestPatchDto);
    void supprimerUtilisateur(Long id);
    UtilisateurResponseDto afficherUtilisateur(Long id);
    List<UtilisateurResponseDto> afficherUtilisateurs();
    UtilisateurResponseDto afficherParPseudo(String pseudo);


    UtilisateurResponseDto modifierImage(Long id, String base64Image);

    void changerMotDePasse(Long id, ChangePasswordDto dto);

    void desactiverCompte(Long id);
    void activerCompteAvecLogin(Utilisateur utilisateur);
    void activerCompte(Long id);
    void suspenduCompte(Long id);
    void archiverCompte(Long id);

    //-------------------Inscription Utilisateur ----------------------------

}
