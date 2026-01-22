package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.entities.Role;
import tn.weeding.agenceevenementielle.entities.enums.StatutCompte;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.UtilisateurRole;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRoleRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2UserService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CodeGeneratorService codeGeneratorService;


    @Transactional
    public String processOAuth2User(String email, String prenom, String nom, String photoUrl) {

        log.info("🔍 Traitement de l'utilisateur Google : {}", email);

        // 1- Vérifier si l'utilisateur existe déjà
        Optional<Utilisateur> existingUser = utilisateurRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            Utilisateur utilisateur = existingUser.get();

            // Cas 1 : Compte Google existant
            if (Boolean.TRUE.equals(utilisateur.getGoogleAccount())) {

                if(utilisateur.getEtatCompte()==StatutCompte.ARCHIVE){
                    throw new CustomException("Ce Compte a étè Supprimé, merci de contacter Notre Support");
                }

                if(utilisateur.getEtatCompte()==StatutCompte.SUSPENDU){
                    throw new CustomException("Le Compte Est Bloqué, merci de contacter Notre Support");
                }

                if(utilisateur.getEtatCompte()==StatutCompte.DESACTIVE){
                    utilisateur.setEtatCompte(StatutCompte.ACTIVE);
                    utilisateurRepository.save(utilisateur);
                    log.info(" Compte Réactivé de nouveau ");
                }
                log.info(" Utilisateur Google existant : {}", utilisateur.getPseudo());
                return utilisateur.getPseudo();
            }

            // Cas 2 : Compte classique existant avec le même email
            if (Boolean.TRUE.equals(utilisateur.getActivationCompte())) {
                log.warn(" Email {} déjà utilisé par un compte classique actif", email);
                throw new CustomException(
                        "CONNEXION_EXIST_IN_CLASSIC_MODE"
                );
            }


            // Cas 3 : Compte classique non activé
            log.info(" Conversion d'un compte non activé en compte Google");
            convertToGoogleAccount(utilisateur, prenom, nom, photoUrl);
            return utilisateur.getPseudo();


        }

        // 2- Créer un nouvel utilisateur Google
        log.info(" Création d'un nouvel utilisateur Google");
        Utilisateur newUser = createGoogleUser(email, prenom, nom, photoUrl);
        return newUser.getPseudo();
    }

    /**
     * Crée un nouvel utilisateur à partir d'un compte Google
     */
    private Utilisateur createGoogleUser(String email, String prenom, String nom, String photoUrl) {

        Utilisateur utilisateur = new Utilisateur();

        // Informations de base
        utilisateur.setEmail(email);
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setImage(photoUrl);

        // Générer un pseudo unique à partir de l'email
        String basePseudo = email.split("@")[0].replaceAll("[^a-zA-Z0-9_-]", "_");
        utilisateur.setPseudo(generateUniquePseudo(basePseudo));

        // Configuration du compte Google
        utilisateur.setGoogleAccount(true);
        utilisateur.setActivationCompte(true);  // Activé immédiatement (Google a vérifié l'email)
        utilisateur.setEtatCompte(StatutCompte.ACTIVE);
        utilisateur.setCodeUtilisateur(codeGeneratorService.generateNextCode("CLIENT"));

        // Pas de mot de passe (authentification via Google)
        utilisateur.setMotDePasse(passwordEncoder.encode(UUID.randomUUID().toString())); // MDP aléatoire inutilisable

        // Sauvegarder l'utilisateur
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info(" Utilisateur créé avec le pseudo : {}", utilisateur.getPseudo());

        // Assigner le rôle CLIENT automatiquement
        assignClientRole(utilisateur);

        return utilisateur;
    }

    /**
     * Convertit un compte classique non activé en compte Google
     */
    private void convertToGoogleAccount(Utilisateur utilisateur, String prenom, String nom, String photoUrl) {

        utilisateur.setGoogleAccount(true);
        utilisateur.setActivationCompte(true);
        utilisateur.setEtatCompte(StatutCompte.ACTIVE);
        utilisateur.setActivationToken(null);
        utilisateur.setTokenExpiration(null);

        // Mettre à jour les infos depuis Google
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        if (photoUrl != null) {
            utilisateur.setImage(photoUrl);
        }

        utilisateurRepository.save(utilisateur);
        log.info(" Compte converti en compte Google : {}", utilisateur.getPseudo());
    }

    /**
     * Génère un pseudo unique si celui proposé existe déjà
     */
    private String generateUniquePseudo(String basePseudo) {
        String pseudo = basePseudo;
        int counter = 1;

        while (utilisateurRepository.existsByPseudo(pseudo)) {
            pseudo = basePseudo + counter;
            counter++;
        }

        return pseudo;
    }

    /**
     * Assigne automatiquement le rôle CLIENT à l'utilisateur
     */
    private void assignClientRole(Utilisateur utilisateur) {

        Role clientRole = roleRepository.findByNom("CLIENT")
                .orElseThrow(() -> new RuntimeException("Le rôle CLIENT n'existe pas"));

        UtilisateurRole lien = new UtilisateurRole();
        lien.setUtilisateur(utilisateur);
        lien.setRole(clientRole);
        lien.setAttribuePar("GOOGLE_OAUTH2");

        utilisateurRoleRepository.save(lien);
        log.info(" Rôle CLIENT assigné à l'utilisateur : {}", utilisateur.getPseudo());
    }
}