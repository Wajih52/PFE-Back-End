package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.produit.MouvementStockResponseDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitDisponibiliteDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.produit.ProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.InstanceProduit;
import tn.weeding.agenceevenementielle.entities.MouvementStock;
import tn.weeding.agenceevenementielle.entities.Produit;
import tn.weeding.agenceevenementielle.entities.Reservation;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.ProduitException;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des produits
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProduitServiceImpl implements ProduitServiceInterface {

    private final ProduitRepository produitRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final CodeGeneratorServiceProduit codeGeneratorService;
    private final InstanceProduitRepository instanceProduitRepository;
    private final ImageService imageService;
    private final LigneReservationRepository ligneReservationRepository;
    private final ReservationRepository reservationRepository;

    private static final Integer SEUIL_CRITIQUE_DEFAUT = 5;

// ============================================
    // GESTION DES PRODUITS (CRUD) - INCHANGÉ
    // ============================================

    @Override
    public ProduitResponseDto creerProduit(ProduitRequestDto produitDto, String username) {
        log.info("📦 Création d'un nouveau produit : {} par {}", produitDto.getNomProduit(), username);

        String imagePath = null;

        // Générer un code produit à partir du nom de produit
        String codeProduit = codeGeneratorService.generateProduitCode(produitDto.getNomProduit());

        // Vérification de l'existence de l'image
        if (produitDto.getImageProduit() != null &&
                produitDto.getImageProduit().startsWith("data:image")) {
            try {
                imagePath = imageService.saveBase64Image(
                        produitDto.getImageProduit(),
                        codeProduit
                );
            } catch (Exception e) {
                log.error("❌ Erreur sauvegarde image: {}", e.getMessage());
            }
        }

        // Créer l'entité produit
        Produit produit = new Produit();
        produit.setCodeProduit(codeProduit);
        produit.setNomProduit(produitDto.getNomProduit());
        produit.setDescriptionProduit(produitDto.getDescriptionProduit());
        produit.setCategorieProduit(produitDto.getCategorieProduit());

        // Si produit de référence, la quantité est liée aux nombres des instances
        if (produitDto.getTypeProduit().equals(TypeProduit.AVEC_REFERENCE)) {
            produit.setQuantiteInitial(0);
            produit.setMaintenanceRequise(false);
        } else {
            produit.setQuantiteInitial(produitDto.getQuantiteInitial());
            produit.setMaintenanceRequise(produitDto.getMaintenanceRequise());
        }

        produit.setPrixUnitaire(produitDto.getPrixUnitaire());
        produit.setQuantiteDisponible(produitDto.getQuantiteInitial());
        produit.setTypeProduit(produitDto.getTypeProduit());
        produit.setSeuilCritique(produitDto.getSeuilCritique());
        produit.setImageProduit(imagePath);

        produit = produitRepository.save(produit);

        // Enregistrer le mouvement de création si quantité > 0
        if (produit.getQuantiteInitial() > 0 && produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
            enregistrerMouvement(
                    produit,
                    TypeMouvement.CREATION,
                    produit.getQuantiteInitial(),
                    0,
                    produit.getQuantiteInitial(),
                    "Création du produit avec stock initial",
                    username,
                    null
            );
        }

        log.info("✅ Produit créé avec succès : Code={}", produit.getCodeProduit());
        return convertToDto(produit);
    }

    @Override
    public ProduitResponseDto modifierProduit(Long id, ProduitRequestDto produitDto, String username) {
        log.info("🔧 Modification du produit ID: {} par {}", id, username);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Produit avec ID " + id + " introuvable"));

        // Mise à jour des champs modifiables
        produit.setSeuilCritique(produitDto.getSeuilCritique());
        produit.setNomProduit(produitDto.getNomProduit());
        produit.setDescriptionProduit(produitDto.getDescriptionProduit());
        produit.setCategorieProduit(produitDto.getCategorieProduit());
        produit.setPrixUnitaire(produitDto.getPrixUnitaire());
        produit.setMaintenanceRequise(produitDto.getMaintenanceRequise());

        // Gestion de l'image
        if (produitDto.getImageProduit() != null &&
                produitDto.getImageProduit().startsWith("data:image")) {
            try {
                String imagePath = imageService.saveBase64Image(
                        produitDto.getImageProduit(),
                        produit.getCodeProduit()
                );
                produit.setImageProduit(imagePath);
            } catch (Exception e) {
                log.error("❌ Erreur mise à jour image: {}", e.getMessage());
            }
        }

        produit = produitRepository.save(produit);

        log.info("✅ Produit modifié avec succès: Code={}", produit.getCodeProduit());
        return convertToDto(produit);
    }


    @Override
    public void supprimerProduit(Long id, String username) {
        log.info("🗑️ Suppression du produit ID: {} par {}", id, username);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));


        boolean exist = ligneReservationRepository.existsActiveReservationForProduit(
                id,
                LocalDate.now()
        );

        if(exist){
            throw new CustomException("Tu ne peux pas supprimer un produit qui est déjà reservé");
        }
        if(produit.getTypeProduit()==TypeProduit.AVEC_REFERENCE){
            List<InstanceProduit> instanceProduits = instanceProduitRepository.findByProduit_IdProduit(id);
            log.info("nombre instances produit trouvé {}",instanceProduits.size());
            for(InstanceProduit instanceProduit : instanceProduits){
                instanceProduit.setStatut(StatutInstance.HORS_SERVICE);
                enregistrerMouvementInstance(
                        instanceProduit.getProduit(),
                        TypeMouvement.DESACTIVATION,
                        -1,
                        "Instance :désactivé/supprimé "+StatutInstance.HORS_SERVICE,
                        username,
                        instanceProduit
                );
                produit.setQuantiteDisponible(produit.getQuantiteDisponible()-1);
               // produitRepository.save(produit);
            }
            produit.setQuantiteDisponible(0);


        }else{
            // Soft delete
            produit.setQuantiteDisponible(0);

            // Enregistrer le mouvement
            enregistrerMouvement(
                    produit,
                    TypeMouvement.DESACTIVATION,
                    0,
                    produit.getQuantiteDisponible(),
                    0,
                    "Produit désactivé/supprimé",
                    username,
                    null
            );

        }

        produitRepository.save(produit);

        log.info("✅ Produit désactivé: Code={}", produit.getCodeProduit());
    }

    @Override
    @Transactional
    public void supprimerProduitDeBase(Long id, String username) {
        log.info("🗑️ Suppression DÉFINITIVE du produit ID: {} par {}", id, username);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));

        // Vérifier les réservations actives
        boolean exist = ligneReservationRepository.existsActiveReservationForProduit(
                id,
                LocalDate.now()
        );

        if(exist) {
            throw new CustomException("Impossible de supprimer un produit qui est actuellement réservé");
        }

        // ✅ NOUVEAU: Détacher les mouvements de stock AVANT suppression
        List<MouvementStock> mouvements = mouvementStockRepository.findByProduit_IdProduit(id);

        log.info("📦 {} mouvements de stock trouvés pour ce produit", mouvements.size());

        // Détacher chaque mouvement du produit
        for (MouvementStock mouvement : mouvements) {
            mouvement.setProduit(null);  // ✅ Détacher la relation
            mouvement.setNomProduitArchive(produit.getNomProduit());
            mouvement.setCodeProduitArchive(produit.getCodeProduit());
            mouvement.setIdProduitArchive(produit.getIdProduit());
            mouvementStockRepository.save(mouvement);
        }

        // Si produit avec référence, gérer les instances
        if(produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
            List<InstanceProduit> instances = instanceProduitRepository.findByProduit_IdProduit(id);
            log.info("🔖 {} instances trouvées", instances.size());

            // Supprimer chaque instance (les mouvements d'instance seront orphelins aussi)
            for(InstanceProduit instance : instances) {
                List<MouvementStock> mouvementsInstance = mouvementStockRepository
                        .findByIdInstance(instance.getIdInstance());

                for(MouvementStock mvt : mouvementsInstance) {
                    mvt.setProduit(null);
                    mouvementStockRepository.save(mvt);
                }

                instanceProduitRepository.delete(instance);
            }
        }

        // ✅ NOUVEAU: Créer un mouvement final AVANT suppression
        MouvementStock mouvementFinal = new MouvementStock();
        mouvementFinal.setTypeMouvement(TypeMouvement.DESACTIVATION);
        mouvementFinal.setQuantite(produit.getQuantiteDisponible());
        mouvementFinal.setQuantiteApres(0);
        mouvementFinal.setQuantiteAvant(produit.getQuantiteDisponible());
        mouvementFinal.setMotif(String.format(
                "Suppression DÉFINITIVE produit %s de la BDD (stock: %d)",
                produit.getCodeProduit(),
                produit.getQuantiteDisponible()
        ));
        mouvementFinal.setEffectuePar(username);
        mouvementFinal.setDateMouvement(LocalDateTime.now());
        // On associe pas au produit qui va être supprimé
        mouvementFinal.setProduit(null);
        // Mais On garde les informations du produit
        mouvementFinal.setNomProduitArchive(produit.getNomProduit());
        mouvementFinal.setCodeProduitArchive(produit.getCodeProduit());
        mouvementFinal.setIdProduitArchive(produit.getIdProduit());

        mouvementStockRepository.save(mouvementFinal);

        log.debug("📝 Mouvement final enregistré sans référence au produit");

        // Maintenant on peut supprimer le produit en toute sécurité
        produitRepository.delete(produit);

        log.info("⚠️ Historique conservé malgré suppression produit");
        log.info("✅ Produit supprimé définitivement: Code={}", produit.getCodeProduit());
    }

    @Override
    public ProduitResponseDto reactiverProduit(Long id, Integer quantite, String username) {
        log.info("♻️ Réactivation du produit ID: {} avec quantité: {} par {}", id, quantite, username);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));



        if(produit.getTypeProduit()==TypeProduit.AVEC_REFERENCE){
            throw new CustomException("pour Réactiver produit de réference il faut réactiver ses instances");
        }

        if(produit.getQuantiteDisponible()!=0){
            throw new CustomException("produit déjà activer !!!");
        }

        Integer quantiteAvant = 0;
        produit.setQuantiteDisponible(quantite);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.REACTIVATION,
                quantite,
                quantiteAvant,
                quantite,
                "Produit réactivé",
                username,
                null
        );

        produit = produitRepository.save(produit);

        log.info("✅ Produit réactivé: Code={}", produit.getCodeProduit());
        return convertToDto(produit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitResponseDto getProduitById(Long id) {
        log.debug("🔍 Recherche produit ID: {}", id);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Produit avec ID " + id + " introuvable"));

        return convertToDto(produit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitResponseDto getProduitByCode(String code) {
        log.debug("🔍 Recherche produit Code: {}", code);

        Produit produit = produitRepository.findByCodeProduit(code)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Produit avec code " + code + " introuvable"));

        return convertToDto(produit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getAllProduits() {
        log.debug("📋 Récupération de tous les produits");

        return produitRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    // ============================================
    // RECHERCHE ET FILTRAGE (SANS PÉRIODE)
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsDisponibles() {
        log.debug("📋 Récupération des produits disponibles (stock global)");

        return produitRepository.findProduitsDisponibles().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsEnRupture() {
        log.debug("📋 Récupération des produits en rupture");

        return produitRepository.findProduitsEnRupture().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsStockCritique(Integer seuil) {
        Integer seuilEffectif = (seuil != null) ? seuil : SEUIL_CRITIQUE_DEFAUT;

        log.debug("⚠️ Récupération des produits en stock critique (seuil: {})", seuilEffectif);

        return produitRepository.findProduitsStockCritique(seuilEffectif).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> searchProduitsByNom(String nom) {
        log.debug("🔍 Recherche produits par nom: {}", nom);

        return produitRepository.searchByNom(nom).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsByCategorie(Categorie categorie) {
        log.debug("🔍 Recherche produits par catégorie: {}", categorie);

        return produitRepository.findByCategorieProduit(categorie).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsByType(TypeProduit typeProduit) {
        log.debug("🔍 Recherche produits par type: {}", typeProduit);

        return produitRepository.findByTypeProduit(typeProduit).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> searchProduits(
            Categorie categorie,
            TypeProduit typeProduit,
            Double minPrix,
            Double maxPrix,
            Boolean disponible) {

        log.debug("🔍 Recherche multicritères: cat={}, type={}, prix={}-{}, dispo={}",
                categorie, typeProduit, minPrix, maxPrix, disponible);

        return produitRepository.searchProduits(
                        categorie,
                        typeProduit,
                        minPrix,
                        maxPrix,
                        disponible
                ).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // DISPONIBILITÉ AVEC PÉRIODE
    // ============================================
    @Override
    @Transactional(readOnly = true)
    public Integer calculerQuantiteDisponibleSurPeriode(Long idProduit, LocalDate dateDebut, LocalDate dateFin) {
        log.info("🔍 Calcul quantité disponible produit ID: {} du {} au {}",
                idProduit, dateDebut, dateFin);

        // Vérifier que le produit existe
        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + idProduit + " introuvable"));
        Integer quantiteDispo = null ;

        // Vérifier que c'est un produit de quantité
        if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {

            quantiteDispo = instanceProduitRepository.countInstancesDisponiblesSurPeriode(
                    idProduit,
                    dateDebut,
                    dateFin
            );

        }else {

            // Vérification quantité disponible sur une période donnée
             quantiteDispo = produitRepository.calculerQuantiteDisponibleSurPeriode(
                    idProduit,
                    dateDebut,
                    dateFin
            );
        }
        // Gérer le cas où il n'y a aucune réservation
        if (quantiteDispo == null) {
            quantiteDispo = produit.getQuantiteDisponible();
        }

        log.info("✅ Quantité disponible calculée: {} (stock total: {})",
                quantiteDispo, produit.getQuantiteDisponible());

        return quantiteDispo;
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean verifierDisponibiliteSurPeriode(
            Long idProduit,
            Integer quantiteDemandee,
            LocalDate dateDebut,
            LocalDate dateFin) {

        log.info("🔍 Vérification disponibilité produit ID: {}, quantité: {}, période: {} -> {}",
                idProduit, quantiteDemandee, dateDebut, dateFin);

        // Vérifier que le produit existe
        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + idProduit + " introuvable"));

        // Vérifier que c'est un produit de quantité
        if (produit.getTypeProduit() != TypeProduit.EN_QUANTITE) {

           int qteInstanceDispo = instanceProduitRepository.countInstancesDisponiblesSurPeriode(
                    idProduit,
                    dateDebut,
                    dateFin
            );

            return qteInstanceDispo >= quantiteDemandee;
        }

        // vérifie la disponibilité du produit dans une période donnée
        Boolean disponible = produitRepository.estDisponibleSurPeriode(
                idProduit,
                quantiteDemandee,
                dateDebut,
                dateFin
        );

        if (disponible == null) {
            // Aucune réservation, vérifier contre le stock total
            disponible = produit.getQuantiteDisponible() >= quantiteDemandee;
        }

        if (disponible) {
            log.info("✅ Disponible: {} unités demandées", quantiteDemandee);
        } else {
            Integer quantiteDispo = calculerQuantiteDisponibleSurPeriode(idProduit, dateDebut, dateFin);
            log.warn("❌ Indisponible: {} demandées, {} disponibles", quantiteDemandee, quantiteDispo);
        }

        return disponible;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getCatalogueDisponibleSurPeriode(LocalDate dateDebut, LocalDate dateFin) {
        log.info("📋 Récupération catalogue disponible du {} au {}", dateDebut, dateFin);

        // les produits qui existent sur une période donnée
        List<Produit> produits = produitRepository.findProduitsDisponiblesSurPeriode(
                dateDebut,
                dateFin
        );

        log.info("✅ {} produits disponibles trouvés", produits.size());

        return produits.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> searchProduitsAvecPeriode(
            Categorie categorie,
            TypeProduit typeProduit,
            Double minPrix,
            Double maxPrix,
            LocalDate dateDebut,
            LocalDate dateFin) {

        log.info("🔍 Recherche avec période: cat={}, type={}, prix={}-{}, période={}-{}",
                categorie, typeProduit, minPrix, maxPrix, dateDebut, dateFin);

        // ✅ NOUVELLE REQUÊTE JPQL avec période
        List<Produit> produits = produitRepository.searchProduitsAvecPeriode(
                categorie,
                typeProduit,
                minPrix,
                maxPrix,
                dateDebut,
                dateFin
        );

        log.info("✅ {} produits trouvés", produits.size());

        return produits.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsAvecQuantiteMinSurPeriode(
            Integer quantiteMin,
            LocalDate dateDebut,
            LocalDate dateFin) {

        log.info("🔍 Recherche produits avec quantité >= {} du {} au {}",
                quantiteMin, dateDebut, dateFin);


        List<Produit> produits = produitRepository.findProduitsAvecQuantiteMinSurPeriode(
                quantiteMin,
                dateDebut,
                dateFin
        );

        log.info("✅ {} produits trouvés avec quantité suffisante", produits.size());

        return produits.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsStockCritiqueSurPeriode(
            Integer seuil,
            LocalDate dateDebut,
            LocalDate dateFin) {

        Integer seuilEffectif = (seuil != null) ? seuil : SEUIL_CRITIQUE_DEFAUT;

        log.warn("⚠️ Vérification stock critique (seuil: {}) du {} au {}",
                seuilEffectif, dateDebut, dateFin);

        // les produits en stock critique sur une période donnée
        List<Produit> produits = produitRepository.findProduitsStockCritiqueSurPeriode(
                seuilEffectif,
                dateDebut,
                dateFin
        );

        if (!produits.isEmpty()) {
            log.warn("⚠️ {} produits en stock critique sur cette période", produits.size());
        }

        return produits.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTauxOccupationSurPeriode(LocalDate dateDebut, LocalDate dateFin) {
        log.info("📊 Calcul taux d'occupation du {} au {}", dateDebut, dateFin);


        List<Object[]> resultats = produitRepository.findTauxOccupationProduitsParPeriode(
                dateDebut,
                dateFin
        );

        List<Map<String, Object>> stats = new ArrayList<>();

        for (Object[] row : resultats) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("idProduit", row[0]);
            stat.put("nomProduit", row[1]);
            stat.put("tauxOccupation", row[2]);

            Double taux = (Double) row[2];
            if (taux > 80) {
                stat.put("niveau", "TRÈS ÉLEVÉ");
                stat.put("alerte", true);
            } else if (taux > 50) {
                stat.put("niveau", "MODÉRÉ");
                stat.put("alerte", false);
            } else {
                stat.put("niveau", "FAIBLE");
                stat.put("alerte", false);
            }

            stats.add(stat);
        }

        log.info("📊 Statistiques générées pour {} produits", stats.size());

        return stats;
    }

    @Override
    public List<ProduitDisponibiliteDto> getProduitsAvecDisponibilitePourPeriode(
            LocalDate dateDebut, LocalDate dateFin) {

        log.info("🔍 Calcul disponibilité produits du {} au {}", dateDebut, dateFin);

        List<Produit> tousProduits = produitRepository.findAll();
        List<ProduitDisponibiliteDto> resultat = new ArrayList<>();

        for (Produit produit : tousProduits) {
            ProduitDisponibiliteDto dto = new ProduitDisponibiliteDto();
            dto.setIdProduit(produit.getIdProduit());
            dto.setCodeProduit(produit.getCodeProduit());
            dto.setNomProduit(produit.getNomProduit());
            dto.setTypeProduit(produit.getTypeProduit());
            dto.setQuantiteTotale(produit.getQuantiteDisponible());

            if (produit.getTypeProduit() == TypeProduit.EN_QUANTITE) {
                // Calculer quantité réservée sur la période
                Integer quantiteReservee = ligneReservationRepository
                        .calculerQuantiteReserveePourPeriode(
                                produit.getIdProduit(),
                                dateDebut,
                                dateFin
                        );

                if (quantiteReservee == null) quantiteReservee = 0;

                dto.setQuantiteReservee(quantiteReservee);
                dto.setQuantiteDisponible(produit.getQuantiteDisponible() - quantiteReservee);

            } else {
                // Pour produits AVEC_REFERENCE
                Long totalInstances = instanceProduitRepository
                        .countByProduit_IdProduit(produit.getIdProduit());

                Long instancesReservees = ligneReservationRepository
                        .countInstancesReserveesPourPeriode(
                                produit.getIdProduit(),
                                dateDebut,
                                dateFin
                        );

                if (instancesReservees == null) instancesReservees = 0L;

                dto.setQuantiteTotale(totalInstances.intValue());
                dto.setQuantiteReservee(instancesReservees.intValue());
                dto.setQuantiteDisponible(totalInstances.intValue() - instancesReservees.intValue());
            }

            resultat.add(dto);
        }

        log.info("✅ {} produits avec disponibilité calculée", resultat.size());
        return resultat;
    }
    // ============================================
    // GESTION DU STOCK (PRODUITS EN_QUANTITE)
    // ============================================

    @Override
    public ProduitResponseDto ajouterStock(Long id, Integer quantite, String motif, String username) {
        log.info("➕ Ajout de stock: produit ID={}, quantité={}, par {}", id, quantite, username);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));

        if (produit.getTypeProduit() != TypeProduit.EN_QUANTITE) {
            throw new CustomException(
                    "L'ajout de stock n'est possible que pour les produits de type EN_QUANTITE. " +
                            "Pour les produits avec référence, créez des instances.");
        }

        Integer quantiteAvant = produit.getQuantiteDisponible();
        Integer quantiteApres = quantiteAvant + quantite;

        produit.setQuantiteDisponible(quantiteApres);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.ENTREE_STOCK,
                quantite,
                quantiteAvant,
                quantiteApres,
                motif != null ? motif : "Ajout de stock",
                username,
                null
        );

        produit = produitRepository.save(produit);

        log.info("✅ Stock ajouté: {} -> {} (Code: {})",
                quantiteAvant, quantiteApres, produit.getCodeProduit());

        return convertToDto(produit);
    }

    @Override
    public ProduitResponseDto retirerStock(Long id, Integer quantite, String motif, String username) {
        log.info("➖ Retrait de stock: produit ID={}, quantité={}, par {}", id, quantite, username);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));

        if (produit.getTypeProduit() != TypeProduit.EN_QUANTITE) {
            throw new CustomException(
                    "Le retrait de stock n'est possible que pour les produits de type EN_QUANTITE");
        }

        Integer quantiteAvant = produit.getQuantiteDisponible();

        if (quantiteAvant < quantite) {
            throw new CustomException(
                    "Stock insuffisant: " + quantiteAvant + " disponibles, " + quantite + " demandés");
        }

        Integer quantiteApres = quantiteAvant - quantite;
        produit.setQuantiteDisponible(quantiteApres);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.RETRAIT_STOCK,
                quantite,
                quantiteAvant,
                quantiteApres,
                motif != null ? motif : "Retrait de stock",
                username,
                null
        );

        produit = produitRepository.save(produit);

        log.info("✅ Stock retiré: {} -> {} (Code: {})",
                quantiteAvant, quantiteApres, produit.getCodeProduit());

        return convertToDto(produit);
    }

    @Override
    public ProduitResponseDto ajusterStock(Long id, Integer nouvelleQuantite, String motif, String username) {
        log.info("🔧 Ajustement de stock: produit ID={}, nouvelle quantité={}, par {}",
                id, nouvelleQuantite, username);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));

        if (produit.getTypeProduit() != TypeProduit.EN_QUANTITE) {
            throw new CustomException(
                    "L'ajustement de stock n'est possible que pour les produits de type EN_QUANTITE");
        }

        // ✅  Vérifier la quantité maximale réservée
        Integer quantiteMaxReservee = ligneReservationRepository
                .findMaxQuantiteReserveeForProduit(id, LocalDate.now());

        if (quantiteMaxReservee == null) {
            quantiteMaxReservee = 0;
        }

        if (nouvelleQuantite < quantiteMaxReservee) {
            throw new CustomException(
                    String.format("Impossible d'ajuster le stock à %d. " +
                                    "Il y a actuellement %d unités réservées pour ce produit. " +
                                    "Le stock minimum doit être de %d unités.",
                            nouvelleQuantite, quantiteMaxReservee, quantiteMaxReservee)
            );
        }

        int quantiteAvant = produit.getQuantiteDisponible();
        int difference = nouvelleQuantite - quantiteAvant;

        produit.setQuantiteDisponible(nouvelleQuantite);

        // Enregistrer le mouvement
        TypeMouvement typeMouvement = difference > 0 ? TypeMouvement.AJOUT_STOCK : TypeMouvement.RETRAIT_STOCK;

        enregistrerMouvement(
                produit,
                typeMouvement,
                Math.abs(difference),
                quantiteAvant,
                nouvelleQuantite,
                motif != null ? motif : "Ajustement de stock",
                username,
                null
        );

        produit = produitRepository.save(produit);

        log.info("✅ Stock ajusté: {} -> {} (Δ={}, Code: {})",
                quantiteAvant, nouvelleQuantite, difference, produit.getCodeProduit());

        return convertToDto(produit);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean verifierStockCritique(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));

        boolean critique = produit.getQuantiteDisponible() <= produit.getSeuilCritique() &&
                produit.getQuantiteDisponible() > 0;

        if (critique) {
            log.warn("⚠️ Stock critique pour {}: {} unités",
                    produit.getNomProduit(), produit.getQuantiteDisponible());
        }

        return critique;
    }

    // ============================================
    // STATISTIQUES ET RAPPORTS
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public StockStatistiquesDto getStatistiquesProduit(Long id) {
        log.info("📊 Génération statistiques pour produit ID: {}", id);

        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + id + " introuvable"));

        List<MouvementStock> mouvements = mouvementStockRepository.findByProduit_IdProduitOrderByDateMouvementDesc(id);

        Integer totalEntrees = mouvementStockRepository.getTotalEntrees(id);
        Integer totalSorties = mouvementStockRepository.getTotalSorties(id);

        LocalDateTime dateDernierMouvement = mouvements.isEmpty() ? null : mouvements.get(0).getDateMouvement();

        StockStatistiquesDto stats = new StockStatistiquesDto(
                totalEntrees != null ? totalEntrees : 0,
                totalSorties != null ? totalSorties : 0,
                produit.getQuantiteDisponible(),
                mouvements.size(),
                dateDernierMouvement
        );

        log.info("📊 Statistiques générées: entrées={}, sorties={}, stock={}",
                stats.getTotalEntrees(), stats.getTotalSorties(), stats.getQuantiteDisponible());

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsLesPlusLoues() {
        log.info("📊 Récupération des produits les plus loués");

        return produitRepository.findProduitsLesPlusLoues().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsMieuxNotes(Double minNote) {
        Double noteEffective = (minNote != null) ? minNote : 4.0;

        log.info("📊 Récupération des produits avec note >= {}", noteEffective);

        return produitRepository.findProduitsMieuxNotes(noteEffective).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // HISTORIQUE DES MOUVEMENTS
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getHistoriqueMouvements(Long idProduit) {
        log.info("📜 Récupération historique des mouvements pour produit ID: {}", idProduit);

        // Vérifier que le produit existe
        produitRepository.findById(idProduit)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + idProduit + " introuvable"));

        List<MouvementStock> mouvements = mouvementStockRepository
                .findByProduit_IdProduitOrderByDateMouvementDesc(idProduit);

        log.info("📜 {} mouvements trouvés", mouvements.size());

        return mouvements.stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getMouvementsByType(TypeMouvement type) {
        log.info("📜 Récupération des mouvements de type: {}", type);

        return mouvementStockRepository.findByTypeMouvement(type).stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getMouvementsByUser(String username) {
        log.info("📜 Récupération des mouvements de l'utilisateur: {}", username);

        return mouvementStockRepository.findByEffectueParOrderByDateMouvementDesc(username).stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getMouvementsByPeriode(Date dateDebut, Date dateFin) {
        log.info("📜 Récupération des mouvements du {} au {}", dateDebut, dateFin);

        return mouvementStockRepository.findByDateMouvementBetween(dateDebut, dateFin).stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getMouvementsProduitParPeriode(
            Long idProduit,
            Date dateDebut,
            Date dateFin) {

        log.info("📜 Récupération des mouvements du produit ID: {} du {} au {}",
                idProduit, dateDebut, dateFin);

        // Vérifier que le produit existe
        produitRepository.findById(idProduit)
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + idProduit + " introuvable"));

        return mouvementStockRepository.findByProduitAndPeriode(idProduit, dateDebut, dateFin).stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }


// ============================================
    // MÉTHODES PRIVÉES - UTILITAIRES
    // ============================================

    /**
     * Enregistrer un mouvement de stock pour traçabilité
     */
    private void enregistrerMouvement(
            Produit produit,
            TypeMouvement typeMouvement,
            Integer quantite,
            Integer quantiteAvant,
            Integer quantiteApres,
            String motif,
            String effectuePar,
            Long idReservation) {

        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setTypeMouvement(typeMouvement);
        mouvement.setQuantite(quantite);
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setMotif(motif);
        mouvement.setEffectuePar(effectuePar);
        mouvement.setIdReservation(idReservation);
        mouvement.setCodeInstance(produit.getCodeProduit());
        // Si c'est une réservation, récupérer les dates
        if (idReservation != null && typeMouvement == TypeMouvement.RESERVATION) {
            try {
                Reservation reservation = reservationRepository.findById(idReservation)
                        .orElse(null);
                if (reservation != null) {
                    mouvement.setDateDebut(reservation.getDateDebut());
                    mouvement.setDateFin(reservation.getDateFin());
                }
            } catch (Exception e) {
                log.warn("⚠️ Impossible de récupérer les dates de la réservation {}", idReservation);
            }
        }

        mouvementStockRepository.save(mouvement);

        log.debug("📝 Mouvement enregistré: Type={}, Quantité={}, Motif={}",
                typeMouvement, quantite, motif);
    }
    /**
     * Enregistre un mouvement de stock pour traçabilité
     */
    private void enregistrerMouvementInstance(Produit produit, TypeMouvement type,
                                      int quantite, String motif, String username, InstanceProduit instanceProduit) {
        Integer quantiteAvant = produit.getQuantiteDisponible();
        Integer quantiteApres = quantiteAvant + quantite;

        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setTypeMouvement(type);
        mouvement.setQuantite(Math.abs(quantite));
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setMotif(motif);
        mouvement.setEffectuePar(username);
        mouvement.setDateMouvement(LocalDateTime.now());
        mouvement.setCodeInstance(instanceProduit.getNumeroSerie());
        mouvement.setIdInstance(instanceProduit.getIdInstance());

        mouvementStockRepository.save(mouvement);

        log.debug("Mouvement enregistré: {} - {} ({}→{})",
                type, motif, quantiteAvant, quantiteApres);
    }


    /**
     * Convertir une entité Produit en DTO
     */
    private ProduitResponseDto convertToDto(Produit produit) {
        ProduitResponseDto dto = new ProduitResponseDto();
        dto.setIdProduit(produit.getIdProduit());
        dto.setCodeProduit(produit.getCodeProduit());
        dto.setNomProduit(produit.getNomProduit());
        dto.setDescriptionProduit(produit.getDescriptionProduit());
        dto.setImageProduit(produit.getImageProduit());
        dto.setCategorieProduit(produit.getCategorieProduit());
        dto.setTypeProduit(produit.getTypeProduit());
        dto.setPrixUnitaire(produit.getPrixUnitaire());
        dto.setQuantiteInitial(produit.getQuantiteInitial());
        dto.setQuantiteDisponible(produit.getQuantiteDisponible());
        dto.setMaintenanceRequise(produit.getMaintenanceRequise());
        dto.setAlerteStockCritique(produit.getQuantiteDisponible()<produit.getSeuilCritique());
        dto.setEnStock(produit.getQuantiteDisponible()>0);
        dto.setDateCreation(produit.getDateCreation());
        dto.setDateDerniereModification(produit.getDateModification());
        dto.setSeuilCritique(produit.getSeuilCritique());

        // Calcul du taux d'occupation moyen (basé sur les stats)
        if (produit.getQuantiteInitial() != null && produit.getQuantiteInitial() > 0) {
            double taux = ((double) (produit.getQuantiteInitial() - produit.getQuantiteDisponible()) /
                    produit.getQuantiteInitial()) * 100;
            dto.setTauxOccupation(Math.round(taux * 100.0) / 100.0);
        }

        return dto;
    }

    /**
     * Convertir une entité MouvementStock en DTO
     */
    private MouvementStockResponseDto convertMouvementToDto(MouvementStock mouvement) {
        MouvementStockResponseDto dto = new MouvementStockResponseDto();
        dto.setIdMouvement(mouvement.getIdMouvement());
        dto.setIdProduit(mouvement.getProduit().getIdProduit());
        dto.setNomProduit(mouvement.getProduit().getNomProduit());
        dto.setCodeProduit(mouvement.getProduit().getCodeProduit());
        dto.setTypeMouvement(mouvement.getTypeMouvement());
        dto.setQuantite(mouvement.getQuantite());
        dto.setQuantiteAvant(mouvement.getQuantiteAvant());
        dto.setQuantiteApres(mouvement.getQuantiteApres());
        dto.setMotif(mouvement.getMotif());
        dto.setEffectuePar(mouvement.getEffectuePar());
        dto.setDateMouvement(mouvement.getDateMouvement());
        dto.setIdReservation(mouvement.getIdReservation());
        dto.setCodeInstance(mouvement.getCodeInstance());
        dto.setDateDebut(mouvement.getDateDebut());
        dto.setDateFin(mouvement.getDateFin());

        return dto;
    }

}

