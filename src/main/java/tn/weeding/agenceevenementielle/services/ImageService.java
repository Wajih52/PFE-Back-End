package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.exceptions.CustomException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Service de gestion des images
 *
 * Rôle : Convertir Base64 → Fichier physique
 *
 * Fonctionnement :
 * 1. Reçoit string Base64 : "data:image/png;base64,iVBORw0KG..."
 * 2. Extrait les données brutes (après la virgule)
 * 3. Décode Base64 → bytes[]
 * 4. Sauvegarde bytes[] dans un fichier .jpg ou .png
 * 5. Retourne le chemin : "/uploads/profiles/john_123.jpg"
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService {

    // Dossier où sauvegarder les images
    private static final String UPLOAD_DIR = "uploads"+ File.separator + "profiles"+ File.separator;

    // Taille max : 5 Mo
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    /**
     * Sauvegarde une image Base64 dans un fichier
     *
     * @param base64Image Format : "data:image/png;base64,iVBORw0KG..."
     * @param username Utilisé pour nommer le fichier
     * @return Chemin relatif : "/uploads/profiles/username_timestamp.jpg"
     */
    public String saveBase64Image(String base64Image, String username) {
        // Si pas d'image, retourner null
        if (base64Image == null || !base64Image.startsWith("data:image")) {
            return null;
        }

        try {
            // 1️⃣ Séparer "data:image/png;base64" et "iVBORw0KG..."
            String[] parts = base64Image.split(",");
            String imageData = parts[1]; // Les données après la virgule

            // 2️⃣ Décoder Base64 → bytes[]
            byte[] bytes = Base64.getDecoder().decode(imageData);

            // 3️⃣ Vérifier la taille
            if (bytes.length > MAX_SIZE) {
                throw new CustomException("Image trop volumineuse (max 5MB)");
            }

            // 4️⃣ Déterminer l'extension (png ou jpg)
            String extension = parts[0].contains("png") ? "png" : "jpg";

            // 5️⃣ Créer un nom de fichier unique
            String filename = username + "_" + System.currentTimeMillis() + "." + extension;
            // Exemple : john_1729512345678.jpg

            // 6️⃣ Créer le dossier si n'existe pas
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 7️⃣ Écrire le fichier
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, bytes);

            log.info("✅ Image sauvegardée : {}", filename);

            // 8️⃣ Retourner le chemin relatif
            return "/uploads/profiles/" + filename;

        } catch (Exception e) {
            log.error("❌ Erreur sauvegarde image", e);
            throw new CustomException("Erreur lors de la sauvegarde de l'image");
        }
    }

    /**
     * Supprimer une ancienne image
     *
     * @param imageUrl Chemin comme "/uploads/profiles/john_123.jpg"
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith("/uploads")) {
            try {
                // Enlever le "/" du début pour obtenir le chemin relatif
                Path filePath = Paths.get(imageUrl.substring(1));
                Files.deleteIfExists(filePath);
                log.info("🗑️ Image supprimée : {}", imageUrl);
            } catch (Exception e) {
                log.warn("⚠️ Impossible de supprimer l'image : {}", imageUrl);
            }
        }
    }
}