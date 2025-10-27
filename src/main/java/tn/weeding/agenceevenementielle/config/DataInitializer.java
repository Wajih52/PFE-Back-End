package tn.weeding.agenceevenementielle.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;
import tn.weeding.agenceevenementielle.services.CodeGeneratorService;

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
    }

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

    public void initAdminUser() {
        String adminEmail = "admin@Hive.tn";
        String adminPseudo = "WajihAdmin";

        // Vérifier si l'admin existe déjà
        Optional<Utilisateur> existingAdminByEmail = utilisateurRepository.findByEmail(adminEmail);
        Optional<Utilisateur> existingAdminByPseudo = utilisateurRepository.findByPseudo(adminPseudo);

        if (existingAdminByEmail.isEmpty() && existingAdminByPseudo.isEmpty()) {
            try {
                // Créer l'utilisateur admin
                Utilisateur admin = new Utilisateur();
                admin.setCodeUtilisateur("AD001");
                admin.setNom("Wajih");
                admin.setPrenom("Rjeb");
                admin.setPseudo(adminPseudo);
                admin.setGenre("Homme");
                admin.setTelephone(54496597L);
                admin.setAdresse("Rue Monji SLim");
                admin.setEmail(adminEmail);
                admin.setMotDePasse(passwordEncoder.encode("Wajih.19.01?"));
                admin.setDateCreation(LocalDateTime.now());
                admin.setDateModification(LocalDateTime.now());
                admin.setEtatCompte(StatutCompte.ACTIVE);
                admin.setPoste("Administration");
                admin.setDateEmbauche(new Date());

                // Date de fin de contrat : aujourd'hui + 10 ans
                LocalDate localDate = LocalDate.now().plusYears(10);
                Date dateFinContrat = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                admin.setDateFinContrat(dateFinContrat);

                admin.setStatutEmploye(StatutEmp.EnTravail);
                admin.setBio("Etudiant ingénieur");
                admin.setActivationCompte(true);
                admin.setGoogleAccount(false);
                admin.setDoitChangerMotDePasse(false);

                // Récupérer le rôle ADMIN
                Optional<Role> adminRole = roleRepository.findByNom("ADMIN");

                if (adminRole.isPresent()) {
                    // Créer l'association utilisateur-role
                    UtilisateurRole utilisateurRole = new UtilisateurRole();
                    utilisateurRole.setUtilisateur(admin);
                    utilisateurRole.setRole(adminRole.get());

                    // Initialiser la collection des rôles
                    admin.setUtilisateurRoles(new HashSet<>());
                    admin.getUtilisateurRoles().add(utilisateurRole);

                    // Sauvegarder l'utilisateur admin
                    utilisateurRepository.save(admin);
                    System.out.println("✅ Utilisateur Admin créé automatiquement : " + adminEmail);
                } else {
                    System.out.println("❌ Le rôle ADMIN n'a pas été trouvé pour créer l'utilisateur admin");
                }

            } catch (Exception e) {
                System.out.println("❌ Erreur lors de la création de l'utilisateur admin : " + e.getMessage());
            }
        } else {
            System.out.println("✅ Utilisateur Admin existe déjà");
        }
    }
}