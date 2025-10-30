package tn.weeding.agenceevenementielle.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import tn.weeding.agenceevenementielle.dto.UserCreationDataInitializer;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutCompte;
import tn.weeding.agenceevenementielle.entities.enums.StatutEmp;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        initRoles();
        initAdminUser();
        initManagerUser();
        initClientUser();
        initEmployeUser();
    }

    public void initAdminUser() {
        LocalDate localDate = LocalDate.now().plusYears(10);
        Date dateFinContrat = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        UserCreationDataInitializer adminData = new UserCreationDataInitializer(
                "AD001", "Wajih", "Rjeb", "WajihAdmin", "Homme", 54496597L,
                "Rue Monji SLim", "admin@Hive.tn", "Wajih.19.01?", "Administration",
                new Date(), dateFinContrat, StatutEmp.EnTravail, "Etudiant ingénieur", false
        );

        initUser("ADMIN", adminData);
    }

    public void initClientUser() {
        UserCreationDataInitializer clientData = new UserCreationDataInitializer(
                "CL001", "Client", "Test", "ClientTest", "Homme", 52240470L,
                "Rue de stade n°7", "client@Hive.tn", "Client.19.01?", null,
                null, null, null, "Bio client", false
        );

        initUser("CLIENT", clientData);
    }

    public void initEmployeUser() {
        LocalDate localDate = LocalDate.now().plusYears(1);
        Date dateFinContrat = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        UserCreationDataInitializer employeData = new UserCreationDataInitializer(
                "EM001", "Employe", "Test", "EmployeTest", "Femme", 54330769L,
                "Rue abou kacem chebbi", "employe@Hive.tn", "Employe.19.01?", "Responsable équipe",
                new Date(), dateFinContrat, StatutEmp.EnTravail, "Bio employe", false
        );

        initUser("EMPLOYE", employeData);
    }

    public void initManagerUser() {
        LocalDate localDate = LocalDate.now().plusYears(5);
        Date dateFinContrat = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        UserCreationDataInitializer managerData = new UserCreationDataInitializer(
                "MA001", "Manager", "Test", "ManagerTest", "Homme", 50408537L,
                "Adresse manager", "manager@Hive.tn", "Manager.19.01?", "Manager",
                new Date(), dateFinContrat, StatutEmp.EnTravail, "Bio manager", false
        );

        initUser("MANAGER", managerData);
    }

    //===============================Utilitiaires============================
    public void initRoles() {
        List<String> defaultRoles = List.of("ADMIN", "EMPLOYE", "CLIENT","MANAGER");

        for (String roleName : defaultRoles) {
            if (!roleRepository.existsByNom(roleName)) {
                Role role = new Role();
                role.setNom(roleName);
                role.setDescription("Rôle système par défaut : " + roleName);
                role.setActive(true);
                roleRepository.save(role);
                System.out.println(" Rôle ajouté automatiquement : " + roleName);
            }else{
                System.out.println("Rôle déjà existant : " + roleName);
            }
        }
    }

    public void initUser(String roleName, UserCreationDataInitializer userData) {
        // Vérifier si l'utilisateur existe déjà
        Optional<Utilisateur> existingUserByEmail = utilisateurRepository.findByEmail(userData.getEmail());
        Optional<Utilisateur> existingUserByPseudo = utilisateurRepository.findByPseudo(userData.getPseudo());

        if (existingUserByEmail.isEmpty() && existingUserByPseudo.isEmpty()) {
            try {
                Utilisateur user = new Utilisateur();
                user.setCodeUtilisateur(userData.getCodeUtilisateur());
                user.setNom(userData.getNom());
                user.setPrenom(userData.getPrenom());
                user.setPseudo(userData.getPseudo());
                user.setGenre(userData.getGenre());
                user.setTelephone(userData.getTelephone());
                user.setAdresse(userData.getAdresse());
                user.setEmail(userData.getEmail());
                user.setMotDePasse(passwordEncoder.encode(userData.getMotDePasse()));
                user.setDateCreation(LocalDateTime.now());
                user.setDateModification(LocalDateTime.now());
                user.setEtatCompte(StatutCompte.ACTIVE);
                user.setPoste(userData.getPoste());
                user.setDateEmbauche(userData.getDateEmbauche());
                user.setDateFinContrat(userData.getDateFinContrat());
                user.setStatutEmploye(userData.getStatutEmploye());
                user.setBio(userData.getBio());
                user.setActivationCompte(true);
                user.setGoogleAccount(false);
                user.setDoitChangerMotDePasse(userData.isDoitChangerMotDePasse());

                // Récupérer le rôle
                Optional<Role> role = roleRepository.findByNom(roleName);

                if (role.isPresent()) {
                    UtilisateurRole utilisateurRole = new UtilisateurRole();
                    utilisateurRole.setUtilisateur(user);
                    utilisateurRole.setRole(role.get());

                    user.setUtilisateurRoles(new HashSet<>());
                    user.getUtilisateurRoles().add(utilisateurRole);

                    utilisateurRepository.save(user);
                    System.out.println("✅ Utilisateur " + roleName + " créé automatiquement : " + userData.getEmail());
                } else {
                    System.out.println("❌ Le rôle " + roleName + " n'a pas été trouvé");
                }

            } catch (Exception e) {
                System.out.println("❌ Erreur lors de la création de l'utilisateur " + roleName + " : " + e.getMessage());
            }
        } else {
            System.out.println("✅ Utilisateur " + roleName + " existe déjà");
        }
    }



}