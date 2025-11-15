package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.repository.PaiementRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.time.Year;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeGeneratorService {

    private final UtilisateurRepository utilisateurRepository;
    private final PaiementRepository paiementRepository;
    private static final String PAIEMENT_PREFIX = "PAY";



    @Transactional(readOnly = true)
    public String generateNextCode(String typeRole) {

        // Déterminer le préfixe selon le rôle
        String prefix = determinePrefix(typeRole);

        // Récupérer le dernier code avec ce préfixe
        Optional<String> lastCode = utilisateurRepository.findLastCodeByPrefix(prefix);

        if (lastCode.isPresent()) {
            // Extraire le numéro et incrémenter
            String code = lastCode.get();
            int number = extractNumber(code, prefix);
            int nextNumber = number + 1;

            String newCode = String.format("%s%04d", prefix, nextNumber);
            log.debug("📊 Dernier code {} : {} → Nouveau : {}", typeRole, code, newCode);
            return newCode;

        } else {
            // Premier utilisateur de ce type
            String firstCode = prefix + "0001";
            log.debug("🆕 Premier code {} : {}", typeRole, firstCode);
            return firstCode;
        }
    }
    /**
     * Détermine le préfixe selon le type de rôle
     * Prend les deux premiers caractères du nom du rôle en majuscule
     */
    public String determinePrefix(String typeRole) {
        if (typeRole == null || typeRole.trim().isEmpty()) {
            return "XX"; // Par défaut CLIENT
        }

        String trimmedRole = typeRole.trim();

        if (trimmedRole.length() == 1) {
            // Si le rôle n'a qu'un seul caractère, on le duplique
            String singleChar = trimmedRole.toUpperCase();
            return singleChar + singleChar;
        } else {
            // Prend les deux premiers caractères en majuscule
            return trimmedRole.substring(0, 2).toUpperCase();
        }
    }

    /**
     * Extrait le numéro depuis un code utilisateur
     * Ex: "CL005" → 5, "AD012" → 12
     */
    private int extractNumber(String code, String prefix) {
        try {
            // Retirer le préfixe et parser le numéro
            String numberPart = code.substring(prefix.length());
            return Integer.parseInt(numberPart);
        } catch (Exception e) {
            log.warn("⚠️ Impossible d'extraire le numéro du code : {}. Retour à 0.", code);
            return 0;
        }
    }

    /**
     *Génerateur de code prefixe pour les paiements
     * **/

    @Transactional(readOnly = true)
    public synchronized String generatePaiementCode() {
        int currentYear = Year.now().getValue();
        String yearPrefix = PAIEMENT_PREFIX + "-" + currentYear + "-";

        Optional<String> lastCode = paiementRepository.findLastCodePaiementByYear(yearPrefix);

        if (lastCode.isPresent()) {
            String lastCodeValue = lastCode.get();
            String numberPart = lastCodeValue.substring(yearPrefix.length());
            int lastNumber = Integer.parseInt(numberPart);
            int newNumber = lastNumber + 1;
            String newCode = yearPrefix + String.format("%04d", newNumber);
            log.info("✅ Code paiement généré: {}", newCode);
            return newCode;
        } else {
            String newCode = yearPrefix + "0001";
            log.info("✅ Premier code paiement: {}", newCode);
            return newCode;
        }
    }

    @Transactional(readOnly = true)
    public boolean codeExists(String code) {
        return paiementRepository.existsByCodePaiement(code);
    }
}