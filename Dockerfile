# ===================================
# ÉTAPE 1 : BUILD - Construction du JAR
# ===================================
FROM maven:3.9.9-eclipse-temurin-17 AS build

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers de configuration Maven (pom.xml)
# On copie d'abord le pom.xml pour profiter du cache Docker
COPY pom.xml .

# Télécharger les dépendances (cette couche sera mise en cache)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Construire l'application (skip tests pour accélérer le build)
# Le JAR sera créé dans /app/target/
RUN mvn clean package -DskipTests

# ===================================
# ÉTAPE 2 : RUNTIME - Image de production
# ===================================
FROM eclipse-temurin:17-jre-alpine

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

# Définir le répertoire de travail
WORKDIR /app

# Copier le JAR depuis l'étape de build
# Le nom du JAR correspond à ton pom.xml : agenceEvenementielle-0.0.1-SNAPSHOT.jar
COPY --from=build /app/target/agenceEvenementielle-0.0.1-SNAPSHOT.jar app.jar

# Créer le répertoire pour les uploads (images produits, avatars, etc.)
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Changer l'utilisateur
USER spring:spring

# Port exposé (8080 par défaut pour Spring Boot)
EXPOSE 8080

# Variables d'environnement par défaut (seront écrasées par docker-compose)
ENV SPRING_PROFILES_ACTIVE=docker

# Point d'entrée : démarrer l'application
# -Djava.security.egd=file:/dev/./urandom : améliore la génération de nombres aléatoires
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

# Healthcheck pour vérifier si l'application est prête
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/monitoring/health || exit 1