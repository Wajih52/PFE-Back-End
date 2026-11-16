package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.entities.InstanceProduit;
import tn.weeding.agenceevenementielle.repository.InstanceProduitRepository;
import tn.weeding.agenceevenementielle.repository.ProduitRepository;

import java.time.Year;
import java.util.Map;
import java.util.Optional;

/**
 * Service de génération de codes uniques pour les produits
 * Format: PRD-YYYY-XXXX (ex: PRD-2025-0001)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeGeneratorServiceProduit {

    private final ProduitRepository produitRepository;
    private final InstanceProduitRepository instanceProduitRepository;
    private static final String PRODUIT_PREFIX = "PRD";


    /**
     * Génère un code produit unique au format PRD-YYYY-XXXX
     * Exemple: PRD-2025-PR
     */
    @Transactional(readOnly = true)
    public String generateProduitCode(String nomProduit) {
        int currentYear = Year.now().getValue();
        String basePrefix = PRODUIT_PREFIX + "-" + currentYear + "-";

        // Génère le préfixe de base (ex: "CH" pour CHAISE PLASTIQUE)
        String productPrefix = generateProductPrefix(nomProduit);
        String finalCode = basePrefix + productPrefix;

        // Caractère de distinction pour résoudre les conflits (sans utiliser de chiffres)
        char suffixeDistinction = 'A';

        // On utilise findByCodeProduit pour vérifier si le code existe déjà
        while (produitRepository.findByCodeProduit(finalCode).isPresent()) {
            // On ajoute une lettre qui s'incrémente (A, B, C...)
            // Ex: PRD-2026-CHA, puis PRD-2026-CHB...
            finalCode = basePrefix + productPrefix + suffixeDistinction;
            suffixeDistinction++;

            // Sécurité anti-boucle infinie (si on épuise tout l'alphabet de A à Z)
            if (suffixeDistinction > 'Z') {
                finalCode = basePrefix + productPrefix + java.util.UUID.randomUUID().toString().substring(0, 3).toUpperCase();
                break;
            }
        }

        return finalCode;
    }

    /**
     * Génère un code produit instance unique au format PRD-YYYY-XXXX
     * Exemple: PRD-2025-PR0001
     */
    @Transactional(readOnly = true)
    public String generateInstanceCode(String nomProduit) {
        int currentYear = Year.now().getValue();
        String yearPrefix = PRODUIT_PREFIX + "-" + currentYear + "-"+generateProductPrefix(nomProduit) + "-";

        // Récupérer le dernier code de l'année en cours
        Optional<String> lastCode = instanceProduitRepository.findAll()
                .stream()
                .map(InstanceProduit::getNumeroSerie)
                .filter(code -> code != null && code.startsWith(yearPrefix))
                .max(String::compareTo);

        if (lastCode.isPresent()) {
            // Extraire le numéro et incrémenter
            String code = lastCode.get();
            int number = extractNumber(code, yearPrefix);
            int nextNumber = number + 1;
            String newCode = String.format("%s%04d", yearPrefix, nextNumber);
            log.debug("Dernier code produit : {} → Nouveau : {}", code, newCode);
            return newCode;

        } else {
            // Premier produit de l'année
            String firstCode = yearPrefix + "0001";
            log.debug("Premier code produit de l'année : {}", firstCode);
            return firstCode;
        }
    }

    /**
     * Extrait le numéro depuis un code produit
     * Ex: "PRD-2025-0005" → 5
     */
    private int extractNumber(String code, String prefix) {
        try {
            String numberPart = code.substring(prefix.length());
            return Integer.parseInt(numberPart);
        } catch (Exception e) {
            log.warn("Impossible d'extraire le numéro du code : {}. Retour à 0.", code);
            return 0;
        }
    }


    /**
     * Détermine le préfixe selon le nom de produit
     * Génère un préfixe de 2 lettres à partir du nom du produit
     */
    private String generateProductPrefix(String nomProduit) {
        if (nomProduit == null || nomProduit.trim().isEmpty()) {
            return "XX"; // Préfixe par défaut
        }

        String nomNormalise = nomProduit.trim().toUpperCase();

        // Utilisation de Map.ofEntries() pour supporter plus d'éléments
        Map<String, String> reglesSpeciales = Map.ofEntries(
                Map.entry("FRIGO", "FR"),
                Map.entry("PROJECTEUR", "PR"),
                Map.entry("CHAISE PLASTIQUE", "CH"),
                Map.entry("CHAISE NAPOLEON", "CHN"),
                Map.entry("CHAISE PLEXIGLAS", "CX"),
                Map.entry("TANTE", "TT"),
                Map.entry("TABLE", "TA"),
                Map.entry("TABLE BASSE", "TB"),
                Map.entry("LAMPE", "LP"),
                Map.entry("LUMINAIRE", "LU"),
                Map.entry("TAPIS ROUGE", "TPR"),
                Map.entry("RIDEAU BLANC", "RDB"),
                Map.entry("COUVERTURE BLANC", "CVB")
        );

        // Vérifier d'abord les règles spéciales
        for (Map.Entry<String, String> entry : reglesSpeciales.entrySet()) {
            if (nomNormalise.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Si pas de règle spéciale, prendre les 2 premières lettres
        if (nomNormalise.length() >= 2) {
            return nomNormalise.substring(0, 2);
        } else {
            // Si le nom est trop court, compléter avec X
            return String.format("%-2s", nomNormalise).replace(' ', 'X');
        }
    }


}