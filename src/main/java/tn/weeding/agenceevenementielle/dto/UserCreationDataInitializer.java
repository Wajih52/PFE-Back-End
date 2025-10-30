package tn.weeding.agenceevenementielle.dto;

import lombok.Data;
import lombok.Getter;
import tn.weeding.agenceevenementielle.entities.enums.StatutEmp;

import java.util.Date;
@Data
@Getter
public class UserCreationDataInitializer {
    private String codeUtilisateur;
    private String nom;
    private String prenom;
    private String pseudo;
    private String genre;
    private Long telephone;
    private String adresse;
    private String email;
    private String motDePasse;
    private String poste;
    private Date dateEmbauche;
    private Date dateFinContrat;
    private StatutEmp statutEmploye;
    private String bio;
    private boolean doitChangerMotDePasse;

    // Constructeurs, getters et setters
    public UserCreationDataInitializer(String codeUtilisateur, String nom, String prenom, String pseudo,
                            String genre, Long telephone, String adresse, String email,
                            String motDePasse, String poste, Date dateEmbauche,
                            Date dateFinContrat, StatutEmp statutEmploye, String bio,
                            boolean doitChangerMotDePasse) {
        this.codeUtilisateur = codeUtilisateur;
        this.nom = nom;
        this.prenom = prenom;
        this.pseudo = pseudo;
        this.genre = genre;
        this.telephone = telephone;
        this.adresse = adresse;
        this.email = email;
        this.motDePasse = motDePasse;
        this.poste = poste;
        this.dateEmbauche = dateEmbauche;
        this.dateFinContrat = dateFinContrat;
        this.statutEmploye = statutEmploye;
        this.bio = bio;
        this.doitChangerMotDePasse = doitChangerMotDePasse;
    }
}
