package tn.weeding.agenceevenementielle.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.weeding.agenceevenementielle.entities.enums.StatutCompte;
import tn.weeding.agenceevenementielle.entities.enums.StatutEmp;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UtilisateurResponseDto {
    Long idUtilisateur;
    String codeUtilisateur;
    String nom;
    String prenom;
    String pseudo;
    String genre ;
    Long telephone;
    String adresse;
    String email;
    String image;
    StatutCompte etatCompte;
    String poste;
    LocalDateTime dateCreation;
    Date dateEmbauche;
    Date dateFinContrat;
    StatutEmp statutEmploye;
    String bio ;
    boolean doitChangerMotDePasse ;
    private Set<String> roles;
}
