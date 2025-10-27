package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString


public class UtilisateurRole implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idUtilisateurRole;


    LocalDateTime dateAffectationRole;

    @Size(max = 50, message = "Le champ attribuéPar ne peut pas dépasser 50 caractères")
    String attribuePar;

    //--------------------------------------------Les Relations------------------------------------------------

    //UtilisateurRole 1..* ----------- 1 Role
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @JsonIgnore
    Role role;

    //UtilisateurRole 1..* ------------ 1 Utilisateur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "motDePasse", "utilisateurRoles"
    ,"dernierEnvoiEmail","activationToken","tokenExpiration","activationCompte","googleAccount","resetPasswordToken"
    ,"resetPasswordExpiration","doitChangerMotDePasse"})
    Utilisateur utilisateur;

    @PrePersist
    protected void onCreate() {
        dateAffectationRole = LocalDateTime.now();

    }

    @PreUpdate
    protected void onUpdate() {
        dateAffectationRole = LocalDateTime.now();
    }

}
