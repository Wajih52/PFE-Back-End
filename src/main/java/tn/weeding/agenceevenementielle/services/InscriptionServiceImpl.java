package tn.weeding.agenceevenementielle.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.UtilisateurInscriptionDto;
import tn.weeding.agenceevenementielle.entities.Role;
import tn.weeding.agenceevenementielle.entities.enums.StatutCompte;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.UtilisateurRole;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.mapper.UtilisateurMapper;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRoleRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class InscriptionServiceImpl implements InscriptionServiceInterface {

    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final RoleRepository roleRepository;
    private final CodeGeneratorService codeGeneratorService;

    private static final String ACTIVATION_LINK_BASE = "http://localhost:8080/inscriptions/activation?token=";
    private static final int EMAIL_DELAY_MINUTES = 2;

    // ==================== INSCRIPTION ====================

    @Override
    @Transactional
    public Utilisateur inscription(UtilisateurInscriptionDto dtoInscription) {
        // Vérification email
        Optional<Utilisateur> existByEmail = utilisateurRepository.findByEmail(dtoInscription.getEmail());
        if (existByEmail.isPresent()) {
            return handleExistingEmailInscription(existByEmail.get(), dtoInscription);
        }

        // Vérification pseudo
        handleExistingPseudo(dtoInscription.getPseudo());

        // Créer un nouvel utilisateur
        return createNewUser(dtoInscription);
    }

    // ==================== RENVOI EMAIL ACTIVATION ====================

    /**
     * Renvoyer l'email d'activation
     */
    @Override
    @Transactional
    public void resendActivationEmail(String email) {
        // 1. Récupérer l'utilisateur
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("Utilisateur introuvable"));

        // 2. Vérifier si le compte est déjà activé
        if (utilisateur.getActivationCompte()) {
            throw new CustomException("Ce compte est déjà activé");
        }

        // 3. Vérifier le délai depuis le dernier envoi
        checkEmailDelay(utilisateur);

        // 4. Générer un nouveau token si expiré
        if (isTokenExpired(utilisateur)) {
            utilisateur = ConfigureToken(utilisateur);
        }

        // 5. Envoyer l'email
        sendActivationEmail(utilisateur, "(Resend) ");

        // 6. Mettre à jour la date du dernier envoi
        utilisateur.setDernierEnvoiEmail(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        log.info("✅ Email d'activation renvoyé pour : {}", email);
    }

    //======================Activer Compte =====================
    @Override
    @Transactional
    public void activerCompte(String token) {
        Utilisateur utilisateur = utilisateurRepository.findByActivationToken(token)
                .orElseThrow(() -> new CustomException("Token invalide"));

        if (utilisateur.getTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new CustomException("Token expiré");
        }

        utilisateur.setEtatCompte(StatutCompte.ACTIVE);
        utilisateur.setActivationCompte(true);
        utilisateur.setActivationToken(null);
        utilisateur.setTokenExpiration(null);
        utilisateurRepository.save(utilisateur);
    }

    @Override
    public String getEmailByToken(String token) {
        return utilisateurRepository.findByActivationToken(token)
                .map(Utilisateur::getEmail)
                .orElse(null);
    }
    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Gérer le cas où l'email existe déjà
     */
    private Utilisateur handleExistingEmailInscription(Utilisateur utilisateur, UtilisateurInscriptionDto dto) {
        // Si compte activé → Erreur
        if (utilisateur.getActivationCompte()) {
            throw new CustomException("Email déjà utilisé !");
        }

        // Compte non activé mais token encore valide → Renvoyer email
        if (!isTokenExpired(utilisateur)) {
            checkEmailDelay(utilisateur);
            sendActivationEmail(utilisateur, "");
            utilisateur.setDernierEnvoiEmail(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
            throw new CustomException("Un lien d'activation vous a déjà été envoyé. Vérifiez votre boîte email !");
        }

        // Token expiré → Mettre à jour les infos et renvoyer
        return updateAndResendActivation(utilisateur, dto);
    }

    /**
     * Mettre à jour un utilisateur existant et renvoyer l'email
     */
    private Utilisateur updateAndResendActivation(Utilisateur utilisateur, UtilisateurInscriptionDto dto) {
        // Vérifier si le nouveau pseudo est libre
        if (!utilisateur.getPseudo().equals(dto.getPseudo())) {
            handleExistingPseudo(dto.getPseudo());
        }

        // Mettre à jour les informations
        utilisateurMapper.updateUtilisateurFromInscriptionDto(dto, utilisateur);
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur = ConfigureToken(utilisateur);
        utilisateurRepository.save(utilisateur);

        // Envoyer l'email
        sendActivationEmail(utilisateur, "Resend-");

        return utilisateur;
    }

    /**
     * Gérer le cas où le pseudo existe déjà
     */
    private void handleExistingPseudo(String pseudo) {
        Optional<Utilisateur> existByPseudo = utilisateurRepository.findByPseudo(pseudo);
        if (existByPseudo.isPresent()) {
            Utilisateur utilisateur = existByPseudo.get();

            // Si compte activé → Erreur
            if (utilisateur.getActivationCompte()) {
                throw new CustomException("Ce pseudo est déjà utilisé !");
            }

            // Si compte NON activé → SUPPRIMER
            log.info("🗑️ Suppression du compte non activé avec pseudo: {}", utilisateur.getPseudo());
            utilisateurRepository.delete(utilisateur);
        }
    }

    /**
     * Créer un nouvel utilisateur
     */
    private Utilisateur createNewUser(UtilisateurInscriptionDto dto) {
        // Mapper le DTO
        Utilisateur utilisateur = utilisateurMapper.dtoInscriptionToUtilisateur(dto);

        // Encoder le mot de passe
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));

        // Configurer le token
        utilisateur = ConfigureToken(utilisateur);

        // Compléter les champs
        utilisateur.setEtatCompte(StatutCompte.SUSPENDU);
        utilisateur.setActivationCompte(false);
        utilisateur.setCodeUtilisateur(codeGeneratorService.generateNextCode("CLIENT"));

        // Sauvegarder
        utilisateurRepository.save(utilisateur);

        // Assigner le rôle CLIENT
        assignClientRole(utilisateur);

        // Envoyer l'email
        sendActivationEmail(utilisateur, "");

        return utilisateur;
    }

    /**
     * Assigner le rôle CLIENT automatiquement
     */
    private void assignClientRole(Utilisateur utilisateur) {
        UtilisateurRole lien = new UtilisateurRole();
        lien.setUtilisateur(utilisateur);

        Role clientRole = roleRepository.findByNom("CLIENT")
                .orElseThrow(() -> new RuntimeException("Le rôle CLIENT n'existe pas"));

        lien.setRole(clientRole);
        lien.setAttribuePar("SYSTEM");
        utilisateurRoleRepository.save(lien);
    }

    /**
     * Vérifier le délai depuis le dernier envoi d'email
     */
    private void checkEmailDelay(Utilisateur utilisateur) {
        if (utilisateur.getDernierEnvoiEmail() != null) {
            long minutesDepuisDernierEnvoi = ChronoUnit.MINUTES.between(
                    utilisateur.getDernierEnvoiEmail(),
                    LocalDateTime.now()
            );

            if (minutesDepuisDernierEnvoi < EMAIL_DELAY_MINUTES) {
                long tempsRestant = EMAIL_DELAY_MINUTES - minutesDepuisDernierEnvoi;
                throw new CustomException(
                        "Un email vient d'être envoyé. Veuillez patienter " +
                                tempsRestant + " minute(s) avant de redemander."
                );
            }
        }
    }

    /**
     * Vérifier si le token est expiré
     */
    private boolean isTokenExpired(Utilisateur utilisateur) {
        return utilisateur.getTokenExpiration() == null ||
                utilisateur.getTokenExpiration().isBefore(LocalDateTime.now());
    }

    /**
     * Envoyer l'email d'activation
     */
    private void sendActivationEmail(Utilisateur utilisateur, String prefix) {
        String link = ACTIVATION_LINK_BASE + utilisateur.getActivationToken();
        String emailContent = construireEmailActivation(utilisateur.getPrenom(), link);

        emailService.sendEmail(
                utilisateur.getEmail(),
                prefix + "Activation de votre compte - Elegant Hive",
                emailContent
        );

        log.info("📧 Email d'activation envoyé à : {}", utilisateur.getEmail());
    }

    // ==================== UTILITAIRES ====================

    @Override
    public Utilisateur ConfigureToken(Utilisateur utilisateur) {
        String newToken = UUID.randomUUID().toString();
        utilisateur.setActivationToken(newToken);
        utilisateur.setTokenExpiration(LocalDateTime.now().plusMinutes(5));
        return utilisateur;
    }

    public String construireEmailActivation(String prenom, String lienActivation) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                        border-radius: 10px 10px 0 0;
                    }
                    .content {
                        background: #f9f9f9;
                        padding: 30px;
                        border-radius: 0 0 10px 10px;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background: #667eea;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                    .highlight {
                        background: #fff3cd;
                        padding: 15px;
                        border-left: 4px solid #ffc107;
                        margin: 15px 0;
                        border-radius: 4px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✨ Bienvenue chez Elegant Hive !</h1>
                    </div>
                    <div class="content">
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Merci de vous être inscrit sur notre plateforme d'agence événementielle ! 🎉</p>
                        <p>Pour finaliser votre inscription et accéder à tous nos services, veuillez activer votre compte en cliquant sur le bouton ci-dessous :</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Activer mon compte</a>
                        </div>
                        <div class="highlight">
                            <p><strong>⚠️ Important :</strong></p>
                            <ul style="margin: 5px 0;">
                                <li>Ce lien est valide pendant <strong>5 minutes</strong></li>
                                <li>Après activation, vous pourrez vous connecter immédiatement</li>
                            </ul>
                        </div>
                        <p>Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :</p>
                        <p style="word-break: break-all; color: #11998e; font-size: 12px;">%s</p>
                        <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                        <p style="font-size: 13px; color: #666;">
                            Si vous n'avez pas créé de compte sur notre plateforme, vous pouvez ignorer cet email en toute sécurité.
                        </p>
                    </div>
                    <div class="footer">
                        <p><strong>Elegant Hive - Agence Événementielle</strong></p>
                        <p>Tous droits réservés © 2025</p>
                    </div>
                </div>
            </body>
            </html>
            """, prenom, lienActivation, lienActivation);
    }
}