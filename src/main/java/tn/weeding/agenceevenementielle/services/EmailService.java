package tn.weeding.agenceevenementielle.services;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.config.AppConfig;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;
import tn.weeding.agenceevenementielle.exceptions.CustomException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EmailService {
    private static final int EMAIL_DELAY_MINUTES = 2;
    private JavaMailSenderImpl mailSender;

    private final AppConfig appConfig ;


    /**
     * Utilisée pour la réinitialisation de mot de passe
     */
    @Async
    public void sendEmail(String destinataire, String sujet, String contenuHtml) {
        try {
            System.out.println("Envoi d'email vers : " + destinataire);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true); // true = HTML

            mailSender.send(mimeMessage);
            System.out.println("Email envoyé avec succès à " + destinataire);

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Vérifier le délai depuis le dernier envoi d'email
     */
    public void checkEmailDelay(Utilisateur utilisateur) {
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
     * Construire le contenu de l'email de réinitialisation
     */
    public String construireEmailReinitialisation(String prenom, String resetLink, boolean isGoogleAccount) {
        String message = isGoogleAccount ?
                "Vous utilisez actuellement Google pour vous connecter. " +
                        "En créant un mot de passe, vous pourrez vous connecter de deux façons : " +
                        "avec Google OU avec votre mot de passe." :
                "Vous avez demandé à réinitialiser votre mot de passe.";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #667eea;"> Réinitialisation de mot de passe</h2>
           
                    <p>Bonjour %s,</p>
            
                    <p>%s</p>
            
                    <p>Cliquez sur le bouton ci-dessous pour %s :</p>
            
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s"
                           style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                  color: white;
                                  padding: 12px 30px;
                                  text-decoration: none;
                                  border-radius: 5px;
                                  display: inline-block;">
                            %s
                        </a>
                    </div>
            
                    <p style="color: #666; font-size: 14px;">
                         Ce lien expire dans 30 minutes.
                    </p>
            
                    <p style="color: #666; font-size: 14px;">
                        Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.
                    </p>
            
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
            
                    <p style="color: #999; font-size: 12px;">
                        Elegant Hive - Agence Événementielle<br>
                        © 2025 Tous droits réservés
                    </p>
                </div>
            </body>
            </html>
            """,
                prenom,
                message,
                isGoogleAccount ? "créer votre mot de passe" : "réinitialiser votre mot de passe",
                resetLink,
                isGoogleAccount ? "Créer mon mot de passe" : "Réinitialiser mon mot de passe"
        );
    }

    /**
     * Construire l'email de confirmation
     */
    public String construireEmailConfirmation(String prenom) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #28a745;"> Mot de passe modifié</h2>
            
                    <p>Bonjour %s,</p>
            
                    <p>Votre mot de passe a été modifié avec succès.</p>
            
                    <p>Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.</p>
            
                    <p style="color: #666; font-size: 14px;">
                        ️ Si vous n'êtes pas à l'origine de cette modification,
                        contactez immédiatement notre support.
                    </p>
            
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
            
                    <p style="color: #999; font-size: 12px;">
                        Elegant Hive - Agence Événementielle<br>
                        © 2025 Tous droits réservés
                    </p>
                </div>
            </body>
            </html>
            """,
                prenom
        );
    }


    public String construireEmailResetPassword(String prenom, String resetLink) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f5f5f0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background: linear-gradient(135deg, #000000 0%%, #2c2c2c 100%%);
                        color: #d4af37;
                        padding: 30px;
                        text-align: center;
                        border-radius: 10px 10px 0 0;
                    }
                    .content {
                        background: #f8f4e9;
                        padding: 30px;
                        border-radius: 0 0 10px 10px;
                        color: #333;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background: #d4af37;
                        color: #000000;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                        font-weight: bold;
                        border: 2px solid #b8941f;
                    }
                    .button:hover {
                        background: #b8941f;
                        color: #000000;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                    ul {
                        color: #333;
                    }
                    strong {
                        color: #000000;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1> Réinitialisation de mot de passe</h1>
                    </div>
                    <div class="content">
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Vous avez demandé à réinitialiser votre mot de passe.</p>
                        <p>Cliquez sur le bouton ci-dessous pour continuer :</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Réinitialiser mon mot de passe</a>
                        </div>
                        <p><strong> Important :</strong></p>
                        <ul>
                            <li>Ce lien est valide pendant <strong>30 minutes</strong></li>
                            <li>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email</li>
                        </ul>
                        <p>Si le bouton ne fonctionne pas, copiez ce lien :</p>
                        <p style="word-break: break-all; color: #d4af37; font-weight: bold;">%s</p>
                    </div>
                    <div class="footer">
                        <p>Agence Événementielle - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """, prenom, resetLink, resetLink);
    }
@Async
    public void envoyerEmailNouvelEmploye(String email, String prenom, String pseudo, String motDePasseTemporaire) {
        String contenu = construireEmailNouvelEmploye(prenom, pseudo, motDePasseTemporaire);
        sendEmail(email, " Bienvenue chez Elegant Hive - Vos identifiants", contenu);
    }

    private String construireEmailNouvelEmploye(String prenom, String pseudo, String motDePasse) {
        // Encoder les identifiants pour l'URL
        String loginUrl = String.format(
                appConfig.getFrontendUrl() + "/auth/first-login?pseudo=%s&token=%s",
                pseudo,
                java.net.URLEncoder.encode(motDePasse, java.nio.charset.StandardCharsets.UTF_8)
        );

        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; color: #333; background: #f5f5f0; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: linear-gradient(135deg, #000 0%%, #2c2c2c 100%%); color: #d4af37; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                .content { background: #f8f4e9; padding: 30px; border-radius: 0 0 10px 10px; }
                .credentials { background: #fff; padding: 20px; border-left: 4px solid #d4af37; margin: 20px 0; border-radius: 5px; }
                .warning { background: #fff3cd; padding: 15px; border-left: 4px solid #ffc107; margin: 15px 0; border-radius: 5px; }
                .button { display: inline-block; padding: 15px 40px; background: #d4af37; color: #000; text-decoration: none; border-radius: 5px; margin: 20px 0; font-weight: bold; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                .button:hover { background: #b8941f; }
                code { background: #f0f0f0; padding: 5px 10px; border-radius: 3px; font-family: monospace; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1> Bienvenue chez Elegant Hive !</h1>
                </div>
                <div class="content">
                    <p>Bonjour <strong>%s</strong>,</p>
               
                    <p>Votre compte employé a été créé avec succès ! 🎊</p>
                    
                    <div class="credentials">
                        <p><strong> Identifiant (Pseudo) :</strong> <code>%s</code></p>
                        <p><strong> Mot de passe temporaire :</strong> <code>%s</code></p>
                    </div>
                    
                    <div style="text-align: center;">
                        <a href="%s/auth/login" class="button">
                             Se connecter maintenant
                        </a>
                    </div>
                    
                    <div class="warning">
                        <p><strong> IMPORTANT :</strong></p>
                        <ul style="margin: 10px 0; padding-left: 20px;">
                            <li>Ce mot de passe est <strong>temporaire</strong></li>
                            <li>Vous devrez le changer à votre première connexion</li>
                            <li>Ne partagez jamais vos identifiants</li>
                            <li>Ce lien expire dans 24 heures</li>
                        </ul>
                    </div>
                    
                    <p style="color: #666; font-size: 14px; margin-top: 30px;">
                        Si vous n'avez pas demandé ce compte, contactez immédiatement notre support.
                    </p>
                    
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
                    
                    <p style="color: #999; font-size: 12px; text-align: center;">
                        Elegant Hive - Agence Événementielle<br>
                        © 2025 Tous droits réservés
                    </p>
                </div>
            </div>
        </body>
        </html>
        """, prenom, pseudo, motDePasse, appConfig.getFrontendUrl());
    }

    /**
     * Envoyer un email de notification générique
     */
    @Async
    public void envoyerEmailNotification(String destinataire, String prenom,
                                         TypeNotification type, String titre, String message) {
        String contenu = construireEmailNotification(prenom, type, titre, message);
        sendEmail(destinataire, type.getIcone() + " " + titre, contenu);
    }

    /**
     * Construire le contenu HTML d'un email de notification
     */
    private String construireEmailNotification(String prenom, TypeNotification type,
                                               String titre, String message) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                .container { background-color: white; max-width: 600px; margin: 0 auto; 
                            padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .header { text-align: center; border-bottom: 3px solid #d4af37; padding-bottom: 20px; margin-bottom: 30px; }
                .notification-type { display: inline-block; background-color: #f8f9fa; padding: 10px 20px; 
                                    border-radius: 5px; margin: 20px 0; font-size: 16px; }
                .content { color: #333; line-height: 1.8; font-size: 15px; }
                .message { background-color: #f8f9fa; padding: 20px; border-left: 4px solid #d4af37; 
                          margin: 20px 0; border-radius: 5px; }
                .button { display: inline-block; background-color: #d4af37; color: white; 
                         padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                .footer { text-align: center; margin-top: 30px; padding-top: 20px; 
                         border-top: 1px solid #ddd; color: #666; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1 style="color: #d4af37; margin: 0;">Elegant Hive</h1>
                    <p style="color: #666; margin: 10px 0;">Agence Événementielle</p>
                </div>
                <div class="content">
                    <p>Bonjour <strong>%s</strong>,</p>
                    <div class="message">
                        %s
                    </div>
                    <p>Connectez-vous à votre espace client pour plus de détails.</p>
                    <a href="%s/profile" class="button">
                        Accéder à mon espace
                    </a>
                </div>
                <div class="footer">
                    <p>Elegant Hive - Agence Événementielle</p>
                    <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre</p>
                </div>
            </div>
        </body>
        </html>
        """, prenom, message, appConfig.getFrontendUrl());
    }

    }

