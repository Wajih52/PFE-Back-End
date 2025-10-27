package tn.weeding.agenceevenementielle.entities;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class Role implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idRole;
    @Column(unique = true, nullable = false)
    String nom;
    String description;
    @Column(updatable = false)
    LocalDateTime creationDate;
    LocalDateTime modificationDate;
    Boolean active=true;

    //------------------------------------------Constructeurs------------------------------------------
    public Role(String nom, String description) {
        this.nom = nom;
        this.description = description;

    }
//--------------------------------------------Les Relations------------------------------------------------

    //Role 1 -------------------- 1..* UtilisateurRole
    @OneToMany(mappedBy = "role",fetch = FetchType.LAZY,cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore
    Set<UtilisateurRole> utilisateurRoles;

    //--------------------------------------------Les override et méthodes--------------------------------------------

    @PrePersist
    protected void onCreate() {
        creationDate = LocalDateTime.now();
        modificationDate = creationDate;

    }
    @PreUpdate
    protected void onUpdate() {
        modificationDate = LocalDateTime.now();

    }

    //------------------------------------Override hashcode et equals-----------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(idRole, role.idRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRole);
    }



}
