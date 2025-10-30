package tn.weeding.agenceevenementielle.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.authentification.ChangePasswordDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRequestDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurRequestPatchDto;
import tn.weeding.agenceevenementielle.dto.UtilisateurResponseDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutCompte;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.mapper.UtilisateurMapper;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class UtilisateurServiceImpl implements UtilisateurServiceInterface {


    private final EmailService emailService;
    private  ImageService imageService;
    private UtilisateurRepository utilisateurRepository;
     private UtilisateurMapper utilisateurMapper;
     private BCryptPasswordEncoder passwordEncoder;
     private RoleRepository roleRepository;
    private CodeGeneratorService codeGeneratorService;




    @Override
    public UtilisateurResponseDto ajouterUtilisateur(UtilisateurRequestDto utilisateurRequestDto) {
        Optional<Utilisateur> existByEmail = utilisateurRepository.findByEmail(utilisateurRequestDto.getEmail());
            Utilisateur utilisateur ;
        if(existByEmail.isPresent()) {
            utilisateur = existByEmail.get();

            // si le compte est activé déjà donc on sort
            if(utilisateur.getActivationCompte()){
                throw new CustomException("Email Déjà utilisé par un autre utilisateur");
            }
            // Vérifier si le nouveau pseudo est déjà pris par un autre utilisateur (pas lui-même)
            Optional<Utilisateur> utilisateurAvecPseudo = utilisateurRepository.findByPseudo(utilisateurRequestDto.getPseudo());
            if (utilisateurAvecPseudo.isPresent() && !utilisateurAvecPseudo.get().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
                throw new CustomException("Pseudo déjà utilisé par un autre utilisateur !");
            }
            utilisateurMapper.updateUtilisateurFromUtilisateurResquestDto(utilisateurRequestDto,utilisateur);


        }else {
            if (utilisateurRepository.existsByPseudoAndActivationCompteTrue(utilisateurRequestDto.getPseudo())) {
                throw new CustomException("Pseudo déjà utilisé par un autre compte!");
            }


             utilisateur = utilisateurMapper.requestDtoToUtilisateur(utilisateurRequestDto);
        }

        //parametres communs (pour les deux cas ==> update or create )
            utilisateur.setTokenExpiration(null);
            utilisateur.setActivationCompte(true);
          //  utilisateur.setEtatCompte(StatutCompte.ACTIVE);



//--------------------------------vérification existance role ------------------------------------
            String typeRole = utilisateurRequestDto.getRole() != null ? utilisateurRequestDto.getRole():"EMPLOYE";

            utilisateur.setCodeUtilisateur(codeGeneratorService.generateNextCode(typeRole));
            Role role = roleRepository.findByNom(typeRole)
                    .orElseThrow(()-> new CustomException("ROle non trouvé"));
            //---------------------------------faire lassociation ---------------------------
            UtilisateurRole utilisateurRole = new UtilisateurRole() ;
            utilisateurRole.setUtilisateur(utilisateur);
            utilisateurRole.setRole(role);
            //-------------------------ajouter l'association-----------------------
                // ⚠️ Vérifier que la collection n’est pas nulle
                if (utilisateur.getUtilisateurRoles() == null) {
                    utilisateur.setUtilisateurRoles(new HashSet<>());
                }
            utilisateur.getUtilisateurRoles().add(utilisateurRole);

        // 🔑 Générer mot de passe temporaire
        String motDePasseTemporaire = UUID.randomUUID().toString().substring(0, 12);

        // 📧 Envoyer email si c'est un employé/admin
        if (role != null ) {
            emailService.envoyerEmailNouvelEmploye(
                    utilisateur.getEmail(),
                    utilisateur.getPrenom(),
                    utilisateur.getPseudo(),
                    motDePasseTemporaire
            );
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseTemporaire));


        //  Marquer comme "doit changer mot de passe"
        utilisateur.setDoitChangerMotDePasse(true);

        utilisateur.setImage(null);


        utilisateur = utilisateurRepository.save(utilisateur);

        // Puis ajouter l'image si elle existe
        if (utilisateurRequestDto.getImage() != null &&
                utilisateurRequestDto.getImage().startsWith("data:image")) {
            try {
                String imagePath = imageService.saveBase64Image(
                        utilisateurRequestDto.getImage(),
                        utilisateur.getPseudo()
                );
                utilisateur.setImage(imagePath);
                utilisateur = utilisateurRepository.save(utilisateur);
            } catch (Exception e) {
                log.error("❌ Erreur sauvegarde image pour utilisateur {}: {}",
                        utilisateur.getIdUtilisateur(), e.getMessage());
            }
        }
            return utilisateurToResponseDto(utilisateur);
    }

    @Override
    public UtilisateurResponseDto modifierUtilisateurPut(Long id,UtilisateurRequestDto utilisateurRequestDto) {
        Optional<Utilisateur> existing = utilisateurRepository.findById(id);
        Utilisateur utilisateur ;

        if(existing.isPresent()) {
            utilisateur = existing.get();
            verifEmailAndPseudo(utilisateur,utilisateurRequestDto);
            BeanUtils.copyProperties(utilisateurRequestDto, utilisateur);
            utilisateur.setIdUtilisateur(id);
            utilisateur.setMotDePasse(passwordEncoder.encode(utilisateurRequestDto.getMotDePasse()));
            utilisateur= utilisateurRepository.save(utilisateur)  ;
            return utilisateurToResponseDto(utilisateur);

        }else{
            throw new CustomException("utilisateur introuvable");
        }


    }

    @Override
    public UtilisateurResponseDto modifierUtilisateurPatch(Long id, UtilisateurRequestPatchDto utilisateurRequestPatchDto) {
        log.info("🔄 Modification partielle de l'utilisateur ID: {}", id);
        Optional<Utilisateur> existing = utilisateurRepository.findById(id);
        Utilisateur utilisateur ;
        if(existing.isPresent()) {
              utilisateur = existing.get();
              verifEmailAndPseudoPatch(utilisateur,utilisateurRequestPatchDto);
              utilisateurMapper.updateUtilisateurFromUtilisateurResquestPatchDto(utilisateurRequestPatchDto,utilisateur);
              utilisateur = utilisateurRepository.save(utilisateur);
            return utilisateurToResponseDto(utilisateur);
        }else {
            throw new CustomException("Utilisateur non trouvable");
        }
    }

    @Override
    @Transactional
    public void desactiverCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        utilisateur.setEtatCompte(StatutCompte.DESACTIVE);
        utilisateurRepository.save(utilisateur);

        log.info("✅ Compte désactivé : {}", utilisateur.getEmail());
    }

    /**
     * Activer le compte automatiquement lorsqu'il se connecte de nouveau
     * */
    @Override
    public void activerCompteAvecLogin(Utilisateur utilisateur) {
        if (utilisateur.getEtatCompte() == StatutCompte.DESACTIVE) {
            log.info("🔄 Réactivation automatique du compte : {}", utilisateur.getEmail());
            utilisateur.setEtatCompte(StatutCompte.ACTIVE);
            utilisateurRepository.save(utilisateur);
        }
    }

    @Override
    public void activerCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        utilisateur.setEtatCompte(StatutCompte.ACTIVE);
        utilisateurRepository.save(utilisateur);

        log.info("✅ Compte Activé : {}", utilisateur.getEmail());
    }

    @Override
    public void suspenduCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        utilisateur.setEtatCompte(StatutCompte.SUSPENDU);
        utilisateurRepository.save(utilisateur);

        log.info("✅ Compte Suspendu Par Administration : {}", utilisateur.getEmail());
    }

    @Override
    public void archiverCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        utilisateur.setEtatCompte(StatutCompte.ARCHIVE);
        utilisateurRepository.save(utilisateur);

        log.info("✅ Compte Archivé avec succés : {}", utilisateur.getEmail());
    }


    @Override
    @Transactional
    public UtilisateurResponseDto modifierImage(Long id, String base64Image) {
        log.info("📷 Modification image pour utilisateur ID: {}", id);

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        // Cas 1 : Nouvelle image (Base64)
        if (base64Image != null && base64Image.startsWith("data:image")) {

            // Supprimer l'ancienne image
            if (utilisateur.getImage() != null) {
                imageService.deleteImage(utilisateur.getImage());
            }

            // Sauvegarder la nouvelle
            String imagePath = imageService.saveBase64Image(base64Image, utilisateur.getPseudo());
            utilisateur.setImage(imagePath);

            log.info("✅ Nouvelle image sauvegardée: {}", imagePath);
        }
        // Cas 2 : Supprimer l'image (null)
        else if (base64Image == null) {
            if (utilisateur.getImage() != null) {
                imageService.deleteImage(utilisateur.getImage());
                utilisateur.setImage(null);
            }
            log.info("🗑️ Image supprimée");
        }
        // Cas 3 : Garder l'ancienne (URL existante)

        return utilisateurToResponseDto(utilisateurRepository.save(utilisateur));
    }

    @Override
    @Transactional
    public void changerMotDePasse(Long id, ChangePasswordDto dto) {
        log.info("🔑 Changement mot de passe pour utilisateur ID: {}", id);

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(dto.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new CustomException("Ancien mot de passe incorrect");
        }

        // Hasher et sauvegarder le nouveau
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);

        log.info("✅ Mot de passe modifié avec succès");

    }


    @Override
    public void supprimerUtilisateur(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(()->new CustomException("Utilisateur non trouvable"));
        imageService.deleteImage(utilisateur.getImage());
        utilisateurRepository.deleteById(id);
    }

    @Override
    public UtilisateurResponseDto afficherUtilisateur(Long id) {
        return utilisateurToResponseDto(utilisateurRepository.findById(id)
                .orElseThrow(()->new CustomException("Utilisateur non trouvable")));
    }

    @Override
    public List<UtilisateurResponseDto> afficherUtilisateurs() {
        return utilisateurRepository.findAll().stream()
                .map(this::utilisateurToResponseDto)
                .toList();
    }

    @Override
    public UtilisateurResponseDto afficherParPseudo(String pseudo) {
        Utilisateur utilisateur = utilisateurRepository.findByPseudo(pseudo)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));
        return this.utilisateurToResponseDto(utilisateur);
    }




    UtilisateurResponseDto utilisateurToResponseDto(Utilisateur utilisateur) {
        if (utilisateur == null) return null;

        UtilisateurResponseDto dtoResponse = new UtilisateurResponseDto();
        dtoResponse.setIdUtilisateur(utilisateur.getIdUtilisateur());
        dtoResponse.setCodeUtilisateur(utilisateur.getCodeUtilisateur());
        dtoResponse.setNom(utilisateur.getNom());
        dtoResponse.setPrenom(utilisateur.getPrenom());
        dtoResponse.setPseudo(utilisateur.getPseudo());
        dtoResponse.setGenre(utilisateur.getGenre());
        dtoResponse.setTelephone(utilisateur.getTelephone());
        dtoResponse.setAdresse(utilisateur.getAdresse());
        dtoResponse.setEmail(utilisateur.getEmail());
        dtoResponse.setImage(utilisateur.getImage());
        dtoResponse.setEtatCompte(utilisateur.getEtatCompte());
        dtoResponse.setPoste(utilisateur.getPoste());
        dtoResponse.setDateEmbauche(utilisateur.getDateEmbauche());
        dtoResponse.setDateFinContrat(utilisateur.getDateFinContrat());
        dtoResponse.setStatutEmploye(utilisateur.getStatutEmploye());
        dtoResponse.setDateCreation(utilisateur.getDateCreation());
        dtoResponse.setBio(utilisateur.getBio());
        dtoResponse.setRoles(utilisateur.getUtilisateurRoles().stream().
                map(ur-> ur.getRole().getNom())
                .collect(Collectors.toSet()));

        return dtoResponse;
    }

    void verifEmailAndPseudo (Utilisateur utilisateur,UtilisateurRequestDto utilisateurRequestDto){

        //vérifier is email est déjà pris par une autre utilisateur
        Optional<Utilisateur> utilisateurAvecEmail = utilisateurRepository.findByEmail(utilisateurRequestDto.getEmail());
        if (utilisateurAvecEmail.isPresent() && !utilisateurAvecEmail.get().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
            throw new CustomException("Email déjà utilisé par un autre utilisateur !");
        }
        // Vérifier si le nouveau pseudo est déjà pris par un autre utilisateur (pas lui-même)
        Optional<Utilisateur> utilisateurAvecPseudo = utilisateurRepository.findByPseudo(utilisateurRequestDto.getPseudo());
        if (utilisateurAvecPseudo.isPresent() && !utilisateurAvecPseudo.get().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
            throw new CustomException("Pseudo déjà utilisé par un autre utilisateur !");
        }
    }

    void verifEmailAndPseudoPatch (Utilisateur utilisateur,UtilisateurRequestPatchDto utilisateurRequestPatchDto){

        //vérifier is email est déjà pris par une autre utilisateur
        Optional<Utilisateur> utilisateurAvecEmail = utilisateurRepository.findByEmail(utilisateurRequestPatchDto.getEmail());
        if (utilisateurAvecEmail.isPresent() && !utilisateurAvecEmail.get().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
            throw new CustomException("📧 Email déjà utilisé par un autre utilisateur !");
        }
        // Vérifier si le nouveau pseudo est déjà pris par un autre utilisateur (pas lui-même)
        Optional<Utilisateur> utilisateurAvecPseudo = utilisateurRepository.findByPseudo(utilisateurRequestPatchDto.getPseudo());
        if (utilisateurAvecPseudo.isPresent() && !utilisateurAvecPseudo.get().getIdUtilisateur().equals(utilisateur.getIdUtilisateur())) {
            throw new CustomException("Pseudo déjà utilisé par un autre utilisateur !");
        }
    }

}
