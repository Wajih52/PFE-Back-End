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
        String yearPrefix = PRODUIT_PREFIX + "-" + currentYear + "-"+generateProductPrefix(nomProduit);
        return  yearPrefix ;
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
            log.debug("📦 Dernier code produit : {} → Nouveau : {}", code, newCode);
            return newCode;

        } else {
            // Premier produit de l'année
            String firstCode = yearPrefix + "0001";
            log.debug("🆕 Premier code produit de l'année : {}", firstCode);
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
            log.warn("⚠️ Impossible d'extraire le numéro du code : {}. Retour à 0.", code);
            return 0;
        }
    }

    /**
     * Vérifie si un code produit existe déjà
     */
    @Transactional(readOnly = true)
    public boolean codeExists(String codeProduit) {
        return produitRepository.existsByCodeProduit(codeProduit);
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
                Map.entry("CHAISEPLASTIQUE", "CH"),
                Map.entry("CHAISENAPOLEON", "CHN"),
                Map.entry("TANTE", "TT"),
                Map.entry("TABLE", "TA"),
                Map.entry("TABLEBASSE", "TB"),
                Map.entry("LAMPE", "LP"),
                Map.entry("LUMINAIRE", "LU"),
                Map.entry("TAPISROUGE", "TPR"),
                Map.entry("RIDEAUBLANC", "RDB"),
                Map.entry("COUVERTUREBLANC", "CVB")
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