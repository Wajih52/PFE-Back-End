package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.UtilisateurInscriptionDto;
import tn.weeding.agenceevenementielle.entities.Utilisateur;

public interface InscriptionServiceInterface {

    Utilisateur inscription(UtilisateurInscriptionDto dtoInscription);
    void resendActivationEmail(String email);
    void activerCompte(String token);
    String getEmailByToken(String token);
    //***********-Géneration Token *************
    Utilisateur ConfigureToken (Utilisateur utilisateur);

}
