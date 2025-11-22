package tn.weeding.agenceevenementielle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration pour activer JPA Auditing
 * Permet d'utiliser @CreatedDate et @LastModifiedDate dans les entités
 *
 * ✅ REQUIS pour les champs dateCreation et dateModification de l'entité Livraison
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
    // La simple présence de @EnableJpaAuditing active la fonctionnalité
}






