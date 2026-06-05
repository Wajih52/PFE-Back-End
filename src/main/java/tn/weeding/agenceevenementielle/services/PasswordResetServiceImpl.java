package tn.weeding.agenceevenementielle.services;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.authentification.PasswordResetDto;
import tn.weeding.agenceevenementielle.dto.authentification.PasswordResetRequestDto;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;


import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Durée de validité du token : 30 minutes
    private static final int TOKEN_EXPIRATION_MINUTES = 30;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl ;

    @Override
    @Transactional
    public void demanderReinitialisationMotDePasse(PasswordResetRequestDto request) {
        log.info("Demande de réinitialisation pour: {}", request.getEmail());

        // 1. Chercher l'utilisateur par email
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Email non trouvé : {}", request.getEmail());
                     return new CustomException("Aucun compte associé à cet email");});

        // 2 Vérifier si le compte est activé
        if (Boolean.FALSE.equals(utilisateur.getActivationCompte())) {
            throw new CustomException("Ce compte n'est pas encore activé. Vérifiez vos emails.");
        }

        // 3 Les utilisateurs Google peuvent créer/modifier un mot de passe
        if (Boolean.TRUE.equals(utilisateur.getGoogleAccount())) {
            log.info("Compte Google détecté : activation de l'authentification hybride pour {}",
                    utilisateur.getEmail());
            // On continue le processus normalement
            // L'utilisateur pourra créer un mot de passe ET garder Google comme option
        }

        emailService.checkEmailDelay(utilisateur);


        // 4. Générer un token unique
        String resetToken = UUID.randomUUID().toString();

        // 5. Définir l'expiration (30 minutes)
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES);

        // 6. Sauvegarder le token et l'expiration
        utilisateur.setResetPasswordToken(resetToken);
        utilisateur.setResetPasswordExpiration(expiration);
        utilisateur.setDernierEnvoiEmail(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        log.info("Token généré et sauvegardé pour : {}", utilisateur.getEmail());

        // 7. Construire le lien de réinitialisation
        String resetLink = frontendUrl + "/auth/reset-password?token=" + resetToken;

        // 6. Envoyer l'email
        String emailContent = emailService.construireEmailResetPassword(utilisateur.getPrenom(), resetLink);

        emailService.sendEmail(
                utilisateur.getEmail(),
                "Réinitialisation de votre mot de passe",
                emailContent
        );

        log.info("Email de réinitialisation envoyé à: {}", utilisateur.getEmail());
    }

    @Override
    @Transactional
    public void reinitialiserMotDePasse(PasswordResetDto request) {
        log.info("Tentative de réinitialisation avec le token");

        // 1. Chercher l'utilisateur par token
        Utilisateur utilisateur = utilisateurRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> {
                    log.warn("Token invalide ou expiré");
                    return new CustomException("Token invalide ou expiré");
                });

        // 2. Vérifier si le token a expiré
        if (utilisateur.getResetPasswordExpiration().isBefore(LocalDateTime.now())) {
            log.warn(" Token expiré pour : {}", utilisateur.getEmail());
            throw new CustomException("Le token a expiré. Veuillez refaire une demande.");
        }

        // 3. Encoder et sauvegarder le nouveau mot de passe
        String nouveauMotDePasseEncode = passwordEncoder.encode(request.getNouveauMotDePasse());
        utilisateur.setMotDePasse(nouveauMotDePasseEncode);

        // 4. Nettoyer le token (pour sécurité)
        utilisateur.setResetPasswordToken(null);
        utilisateur.setResetPasswordExpiration(null);


        // 5 Si c'était un compte Google, il devient maintenant hybride
        if (Boolean.TRUE.equals(utilisateur.getGoogleAccount())) {
            log.info("Compte Google converti en compte hybride (Google + Mot de passe) pour : {}",
                    utilisateur.getEmail());
        }

        utilisateurRepository.save(utilisateur);

        log.info("Mot de passe réinitialisé avec succès pour: {}", utilisateur.getEmail());

        // 6 Envoyer un email de confirmation
        emailService.sendEmail(
                utilisateur.getEmail(),
                "Mot de passe modifié",
                emailService.construireEmailConfirmation(utilisateur.getPrenom())
        );
    }


}
