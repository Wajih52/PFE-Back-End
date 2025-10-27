package tn.weeding.agenceevenementielle.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Utilisateur implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUtilisateur")
    Long idUtilisateur;


    @Column(name = "codeClient", nullable = false, unique = true)
    @Size(max = 20)
    String codeUtilisateur;



    @Column(name = "nom")

    @Size(max = 50)
    String nom;


    @Column(name = "prenom")
    String prenom;


    @Column(name = "pseudo",unique = true)
    @Size(max = 20)
    String pseudo;


    @Column(name = "genre")
    String genre ;


    @Column(name = "telephone")
    Long telephone;


    @Column(name = "adresse")
    @Size(max = 200)
    String adresse;


    @Column(name = "email", unique = true)
    @Size(max = 100)
    String email;


    @Column(name = "motDePasse")
    @Size(max = 100)
    String motDePasse;


    @Column(name = "dateCreation")
    LocalDateTime dateCreation;


    LocalDateTime dateModification;

    @Size(max = 255)
    String image;


    @Enumerated(EnumType.STRING)
    @Column(name = "etatCompte",nullable = false)
    StatutCompte etatCompte;


    @Column(name = "poste")
    @Size(max = 100)
    String poste;


    @Column(name = "dateEmbauche")
    @Temporal(TemporalType.DATE)
    Date dateEmbauche;


    @Column(name = "dateFinContrat")
    @Temporal(TemporalType.DATE)
    Date dateFinContrat;


    @Enumerated(EnumType.STRING)
    @Column(name = "statutEmploye")
    StatutEmp statutEmploye;


    @Column(name = "bio")
    @Size(max = 500)
    String bio ;


    @Column(name = "dernierEnvoiEmail")
    private LocalDateTime dernierEnvoiEmail;

    /**
     * Pour l'activation du compte aprés inscription
     * **/
    @Column(name = "activationToken", unique = true)
    @Size(max = 255)
    private String activationToken;


    private LocalDateTime tokenExpiration;


    @Column(name = "activationCompte",nullable = false)
    Boolean activationCompte;


    // pour savoir si l'utilisateur utilise Google OAuth2
    private Boolean googleAccount = false;

    /**
     * Pour la reinitialization du compte en cas d'oublie mot de passe
     * **/

    @Column(name = "resetPasswordToken")
    private String resetPasswordToken;

    @Column(name = "resetPasswordExpiration")
    private LocalDateTime resetPasswordExpiration;

    @Column(name = "doitChangerMotDePasse")
    private Boolean doitChangerMotDePasse = false;


    //Utilisateur 1 ---------------- 1..* UtilisateurRole
    @OneToMany(mappedBy = "utilisateur",cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore
    Set<UtilisateurRole> utilisateurRoles = new HashSet<>();

    //Utilisateur 1 ----------------- 0..* Reclamation
    @OneToMany (mappedBy = "utilisateur")
    @JsonIgnore
    Set<Reclamation> reclamations;

    //Utilisateur 1 ---------- 0..* Reservation
    @OneToMany(mappedBy = "utilisateur")
    @JsonIgnore
    Set<Reservation> reservations;

    //Utilisateur 1 ------------ 0..* Pointage
    @OneToMany(mappedBy = "utilisateur")
    @JsonIgnore
    Set<Pointage> pointages;

    //Utilisateur 1 --------------- 1..* AffectationLivraison

    @OneToMany (mappedBy = "utilisateur")
    @JsonIgnore
    Set<AffectationLivraison> affectationLivraisons;


    @PrePersist
    public void onCreate (){
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }
    @PreUpdate
    public void onUpdate (){
        dateModification = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Utilisateur{" +
                "idUtilisateur=" + idUtilisateur +
                ", codeUtilisateur='" + codeUtilisateur + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", pseudo='" + pseudo + '\'' +
                ", genre='" + genre + '\'' +
                ", telephone=" + telephone +
                ", adresse='" + adresse + '\'' +
                ", email='" + email + '\'' +
                ", motDePasse='[Protégé]'" + '\'' +
                ", dateCreation=" + dateCreation +
                ", image='" + image + '\'' +
                ", etatCompte=" + etatCompte +
                ", poste='" + poste + '\'' +
                ", dateEmbauche=" + dateEmbauche +
                ", dateFinContrat=" + dateFinContrat +
                ", statutEmploye=" + statutEmploye +
                ", bio='" + bio + '\'' +
                ", activationToken='" + activationToken + '\'' +
                ", tokenExpiration=" + tokenExpiration +
                ", activationCompte=" + activationCompte +
                '}';
    }

}
