package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.MouvementStockResponseDto;
import tn.weeding.agenceevenementielle.dto.ProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.ProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.InstanceProduitRepository;
import tn.weeding.agenceevenementielle.repository.MouvementStockRepository;
import tn.weeding.agenceevenementielle.repository.ProduitRepository;

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
    private final InstanceProduitRepository instanceProduitRepository ;

    private static final Integer SEUIL_CRITIQUE_DEFAUT = 5;
    private final ImageService imageService;

    // ============ GESTION DES PRODUITS ============

    @Override
    public ProduitResponseDto creerProduit(ProduitRequestDto produitDto, String username) {
        log.info("Création d'un nouveau produit : {}", produitDto.getNomProduit());
        String imagePath = null;
        String codeProduit = codeGeneratorService.generateProduitCode(produitDto.getNomProduit());

        if (produitDto.getImageProduit() != null &&
                produitDto.getImageProduit().startsWith("data:image")) {
            try {
                 imagePath = imageService.saveBase64Image(
                        produitDto.getImageProduit(),
                        codeProduit
                );
            } catch (Exception e) {
                log.error("❌ Erreur sauvegarde image ");
            }
        }
        // Créer l'entité produit
        Produit produit = new Produit();
        produit.setCodeProduit(codeProduit);
        produit.setNomProduit(produitDto.getNomProduit());
        produit.setDescriptionProduit(produitDto.getDescriptionProduit());
        produit.setCategorieProduit(produitDto.getCategorieProduit());

        //si un produit de réference donc la quantité est rélié aux nombres des instances
        if(produitDto.getTypeProduit().equals(TypeProduit.avecReference)){
            produit.setQuantiteInitial(0);
        }else{
            produit.setQuantiteInitial(produitDto.getQuantiteInitial());
        }

        produit.setPrixUnitaire(produitDto.getPrixUnitaire());
        produit.setQuantiteInitial(produitDto.getQuantiteInitial());
        produit.setQuantiteDisponible(produitDto.getQuantiteInitial());
        produit.setTypeProduit(produitDto.getTypeProduit());
        //pour un produit de réference la maintenance est rélié aux instances
        if(produitDto.getTypeProduit().equals(TypeProduit.avecReference)){
            produit.setMaintenanceRequise(false);
        }else{
            produit.setMaintenanceRequise(produitDto.getMaintenanceRequise());
        }

        produit.setSeuilCritique(produitDto.getSeuilCritique());
        produit.setImageProduit(imagePath);

        // Sauvegarder le produit
        Produit savedProduit = produitRepository.save(produit);

        // Enregistrer le mouvement de stock initial
        if (produitDto.getQuantiteInitial() > 0) {
            enregistrerMouvement(
                    savedProduit,
                    TypeMouvement.ENTREE_STOCK,
                    produitDto.getQuantiteInitial(),
                    0,
                    produitDto.getQuantiteInitial(),
                    "Création initiale du produit",
                    username,
                    null
            );
        }



        log.info("Produit créé avec succès : Code={}", savedProduit.getCodeProduit());
        return convertToDto(savedProduit);
    }

    @Override
    public ProduitResponseDto modifierProduit(Long idProduit, ProduitRequestDto produitDto, String username) {
        String imagePath = null;
        log.info("Modification du produit ID : {}", idProduit);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));


        // Vérifier si la quantité initiale a changé
        Integer ancienneQuantiteInitiale = produit.getQuantiteInitial();
        Integer nouvelleQuantiteInitiale = produitDto.getQuantiteInitial();



        // Mettre à jour les informations du produit

        List<InstanceProduit> instanceProduits =
                instanceProduitRepository.findByProduit_IdProduit(produit.getIdProduit());

        //si changement de nom donc géneration de nouveau code
        if((!produitDto.getNomProduit().equals(produit.getNomProduit())) && instanceProduits.isEmpty()){
            String code = codeGeneratorService.generateProduitCode(produitDto.getNomProduit());
            produit.setCodeProduit(code);
            produit.setNomProduit(produitDto.getNomProduit());
            imagePath= imageService.modifierImage(produitDto.getImageProduit(),produit.getImageProduit(),code);
        }else{
            produit.setNomProduit(produitDto.getNomProduit());
            imagePath= imageService.modifierImage(produitDto.getImageProduit(),produit.getImageProduit(),produit.getCodeProduit());
        }

        produit.setDescriptionProduit(produitDto.getDescriptionProduit());
        produit.setCategorieProduit(produitDto.getCategorieProduit());
        produit.setPrixUnitaire(produitDto.getPrixUnitaire());



        if(!instanceProduits.isEmpty()&& produitDto.getTypeProduit().equals(TypeProduit.enQuantite)){
            throw new CustomException("Tu ne peux pas modifier Le type du produit :" +
                    produit.getCodeProduit()+
                    " car il contient des instances "
                    );
        }

        produit.setTypeProduit(produitDto.getTypeProduit());

        if( hasInstancesProblematiques(instanceProduits)&& produitDto.getTypeProduit().equals(TypeProduit.avecReference)){
            produit.setMaintenanceRequise(true);
        } else if(produitDto.getTypeProduit().equals(TypeProduit.avecReference)&& !hasInstancesProblematiques(instanceProduits)) {
            produit.setMaintenanceRequise(false);
        }else {
            produit.setMaintenanceRequise(produitDto.getMaintenanceRequise());
        }




        produit.setImageProduit(imagePath);

        // Si la quantité initiale a changé, ajuster le stock disponible
        if (!ancienneQuantiteInitiale.equals(nouvelleQuantiteInitiale)) {
            Integer difference = nouvelleQuantiteInitiale - ancienneQuantiteInitiale;
            Integer ancienneQuantiteDisponible = produit.getQuantiteDisponible();
            Integer nouvelleQuantiteDisponible = ancienneQuantiteDisponible + difference;

            produit.setQuantiteInitial(nouvelleQuantiteInitiale);
            produit.setQuantiteDisponible(nouvelleQuantiteDisponible);

            // Enregistrer le mouvement
            enregistrerMouvement(
                    produit,
                    TypeMouvement.AJUSTEMENT_INVENTAIRE,
                    Math.abs(difference),
                    ancienneQuantiteDisponible,
                    nouvelleQuantiteDisponible,
                    "Ajustement suite à modification de la quantité initiale",
                    username,
                    null
            );
        }

        Produit savedProduit = produitRepository.save(produit);
        log.info("Produit modifié avec succès : Code={}", savedProduit.getCodeProduit());

        return convertToDto(savedProduit);
    }

    @Override
    public void supprimerProduit(Long idProduit, String username) {
        log.info("Suppression du produit ID : {}", idProduit);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));


        // Vérifier si le produit a des réservations actives
        // TODO: Implémenter la vérification des réservations actives

        // Suppression logique : mettre la quantité disponible à 0
        Integer ancienneQuantite = produit.getQuantiteDisponible();
        produit.setQuantiteInitial(0);
        produit.setQuantiteDisponible(0);

        produitRepository.save(produit);

        // Enregistrer le mouvement
        if (ancienneQuantite > 0) {
            enregistrerMouvement(
                    produit,
                    TypeMouvement.AJUSTEMENT_INVENTAIRE,
                    ancienneQuantite,
                    ancienneQuantite,
                    0,
                    "Suppression logique du produit",
                    username,
                    null
            );
        }

        log.info("Produit supprimé (logiquement) avec succès : Code={}", produit.getCodeProduit());
    }

    @Override
    public void supprimerProduitDeBase(Long idProduit, String username) {
        log.info("Suppression du produit ID : {} De la base de données", idProduit);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        imageService.deleteImage(produit.getImageProduit());
        // Vérifier si le produit a des réservations actives
        // TODO: Implémenter la vérification des réservations actives

        produitRepository.delete(produit);
    }


    @Override
    public void desactiverProduit(Long idProduit, String username) {
        log.info("Désactivation du produit ID : {}", idProduit);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();
        produit.setQuantiteDisponible(0);

        produitRepository.save(produit);

        // Enregistrer le mouvement
        if (ancienneQuantite > 0) {
            enregistrerMouvement(
                    produit,
                    TypeMouvement.AJUSTEMENT_INVENTAIRE,
                    ancienneQuantite,
                    ancienneQuantite,
                    0,
                    "Désactivation du produit",
                    username,
                    null
            );
        }

        log.info("Produit désactivé avec succès : Code={}", produit.getCodeProduit());
    }

    @Override
    public ProduitResponseDto reactiverProduit(Long idProduit, Integer quantite, String username) {
        log.info("Réactivation du produit ID : {} avec quantité : {}", idProduit, quantite);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();
        produit.setQuantiteDisponible(quantite);

        Produit savedProduit = produitRepository.save(produit);

        // Enregistrer le mouvement
        enregistrerMouvement(
                savedProduit,
                TypeMouvement.ENTREE_STOCK,
                quantite,
                ancienneQuantite,
                quantite,
                "Réactivation du produit",
                username,
                null
        );

        log.info("Produit réactivé avec succès : Code={}", savedProduit.getCodeProduit());
        return convertToDto(savedProduit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitResponseDto getProduitById(Long idProduit) {
        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));
        return convertToDto(produit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitResponseDto getProduitByCode(String codeProduit) {
        Produit produit = produitRepository.findByCodeProduit(codeProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec le code : " + codeProduit));
        return convertToDto(produit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getAllProduits() {
        return produitRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsDisponibles() {
        return produitRepository.findProduitsDisponibles()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsEnRupture() {
        return produitRepository.findProduitsEnRupture()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsStockCritique(Integer seuil) {
        Integer seuilEffectif = (seuil != null) ? seuil : SEUIL_CRITIQUE_DEFAUT;
        return produitRepository.findProduitsStockCritique(seuilEffectif)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> searchProduitsByNom(String nom) {
        return produitRepository.searchByNom(nom)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsByCategorie(Categorie categorie) {
        return produitRepository.findByCategorieProduit(categorie)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsByType(TypeProduit typeProduit) {
        return produitRepository.findByTypeProduit(typeProduit)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> searchProduits(Categorie categorie, TypeProduit typeProduit,
                                                   Double minPrix, Double maxPrix, Boolean disponible) {
        return produitRepository.searchProduits(categorie, typeProduit, minPrix, maxPrix, disponible)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsLesPlusLoues() {
        return produitRepository.findProduitsLesPlusLoues()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitResponseDto> getProduitsMieuxNotes(Double minNote) {
        Double noteEffective = (minNote != null) ? minNote : 4.0;
        return produitRepository.findProduitsMieuxNotes(noteEffective)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ============ GESTION DU STOCK ============

    @Override
    public ProduitResponseDto ajusterStock(Long idProduit, Integer quantite, String motif, String username) {
        log.info("Ajustement du stock pour le produit ID : {}, quantité : {}", idProduit, quantite);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();
        Integer nouvelleQuantite = ancienneQuantite + quantite;

        if (nouvelleQuantite < 0) {
            throw new RuntimeException("La quantité disponible ne peut pas être négative");
        }

        produit.setQuantiteDisponible(nouvelleQuantite);
        Produit savedProduit = produitRepository.save(produit);

        // Enregistrer le mouvement
        TypeMouvement typeMouvement = quantite > 0 ? TypeMouvement.ENTREE_STOCK : TypeMouvement.AJUSTEMENT_INVENTAIRE;
        enregistrerMouvement(
                savedProduit,
                typeMouvement,
                Math.abs(quantite),
                ancienneQuantite,
                nouvelleQuantite,
                motif,
                username,
                null
        );

        log.info("Stock ajusté avec succès pour le produit : Code={}", savedProduit.getCodeProduit());
        return convertToDto(savedProduit);
    }

    @Override
    public void decrementerStock(Long idProduit, Integer quantite, Long idReservation, String username) {
        log.info("Décrémentation du stock pour le produit ID : {}, quantité : {}", idProduit, quantite);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();

        if (ancienneQuantite < quantite) {
            throw new RuntimeException("Stock insuffisant pour le produit : " + produit.getNomProduit());
        }

        Integer nouvelleQuantite = ancienneQuantite - quantite;
        produit.setQuantiteDisponible(nouvelleQuantite);

        produitRepository.save(produit);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.SORTIE_RESERVATION,
                quantite,
                ancienneQuantite,
                nouvelleQuantite,
                "Réservation confirmée",
                username,
                idReservation
        );

        log.info("Stock décrémenté avec succès pour le produit : Code={}", produit.getCodeProduit());
    }

    @Override
    public void incrementerStock(Long idProduit, Integer quantite, Long idReservation, String username) {
        log.info("Incrémentation du stock pour le produit ID : {}, quantité : {}", idProduit, quantite);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();
        Integer nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantiteDisponible(nouvelleQuantite);
        produitRepository.save(produit);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.RETOUR_RESERVATION,
                quantite,
                ancienneQuantite,
                nouvelleQuantite,
                "Retour après location",
                username,
                idReservation
        );

        log.info("Stock incrémenté avec succès pour le produit : Code={}", produit.getCodeProduit());
    }

    @Override
    public void marquerProduitEndommage(Long idProduit, Integer quantite, String motif, String username) {
        log.info("Marquage de produit endommagé ID : {}, quantité : {}", idProduit, quantite);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();

        if (ancienneQuantite < quantite) {
            throw new RuntimeException("Quantité invalide pour marquer comme endommagé");
        }

        Integer nouvelleQuantite = ancienneQuantite - quantite;
        produit.setQuantiteDisponible(nouvelleQuantite);

        produitRepository.save(produit);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.PRODUIT_ENDOMMAGE,
                quantite,
                ancienneQuantite,
                nouvelleQuantite,
                motif,
                username,
                null
        );

        log.info("Produit marqué comme endommagé : Code={}", produit.getCodeProduit());
    }

    @Override
    public void mettreEnMaintenance(Long idProduit, Integer quantite, String motif, String username) {
        log.info("Mise en maintenance du produit ID : {}, quantité : {}", idProduit, quantite);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();

        if (ancienneQuantite < quantite) {
            throw new RuntimeException("Quantité invalide pour mise en maintenance");
        }

        Integer nouvelleQuantite = ancienneQuantite - quantite;
        produit.setQuantiteDisponible(nouvelleQuantite);
        produit.setMaintenanceRequise(true);

        produitRepository.save(produit);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.MAINTENANCE,
                quantite,
                ancienneQuantite,
                nouvelleQuantite,
                motif,
                username,
                null
        );

        log.info("Produit mis en maintenance : Code={}", produit.getCodeProduit());
    }

    @Override
    public void retournerDeMaintenance(Long idProduit, Integer quantite, String motif, String username) {
        log.info("Retour de maintenance du produit ID : {}, quantité : {}", idProduit, quantite);

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        Integer ancienneQuantite = produit.getQuantiteDisponible();
        Integer nouvelleQuantite = ancienneQuantite + quantite;

        produit.setQuantiteDisponible(nouvelleQuantite);
        produit.setMaintenanceRequise(false);

        produitRepository.save(produit);

        // Enregistrer le mouvement
        enregistrerMouvement(
                produit,
                TypeMouvement.RETOUR_MAINTENANCE,
                quantite,
                ancienneQuantite,
                nouvelleQuantite,
                motif,
                username,
                null
        );

        log.info("Produit retourné de maintenance : Code={}", produit.getCodeProduit());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifierDisponibilite(Long idProduit, Integer quantiteRequise) {
        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        return produit.getQuantiteDisponible() >= quantiteRequise;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifierStockCritique(Long idProduit) {
        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        return produit.getQuantiteDisponible() > 0 &&
                produit.getQuantiteDisponible() <= SEUIL_CRITIQUE_DEFAUT;
    }

    // ============ HISTORIQUE DES MOUVEMENTS ============

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getHistoriqueMouvements(Long idProduit) {
        return mouvementStockRepository.findByProduitIdOrderByDateMouvementDesc(idProduit)
                .stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getMouvementsByType(TypeMouvement typeMouvement) {
        return mouvementStockRepository.findByTypeMouvementOrderByDateMouvementDesc(typeMouvement)
                .stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getMouvementsByUser(String username) {
        return mouvementStockRepository.findByEffectueParOrderByDateMouvementDesc(username)
                .stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getMouvementsByPeriode(Date dateDebut, Date dateFin) {
        return mouvementStockRepository.findByPeriode(dateDebut, dateFin)
                .stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MouvementStockResponseDto> getRecentMouvements(Integer limit) {
        return mouvementStockRepository.findRecentMouvements()
                .stream()
                .limit(limit != null ? limit : 10)
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockStatistiquesDto getStatistiquesProduit(Long idProduit) {
        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID : " + idProduit));

        List<MouvementStock> mouvements = mouvementStockRepository
                .findByProduitIdOrderByDateMouvementDesc(idProduit);

        Integer totalEntrees = mouvementStockRepository.getTotalEntrees(idProduit);
        Integer totalSorties = mouvementStockRepository.getTotalSorties(idProduit);

        Date dateDernierMouvement = mouvements.isEmpty() ? null : mouvements.get(0).getDateMouvement();

        return new StockStatistiquesDto(
                totalEntrees != null ? totalEntrees : 0,
                totalSorties != null ? totalSorties : 0,
                produit.getQuantiteDisponible(),
                mouvements.size(),
                dateDernierMouvement
        );
    }

    // ============ MÉTHODES PRIVÉES ============

    /**
     * Enregistrer un mouvement de stock
     */
    private void enregistrerMouvement(Produit produit, TypeMouvement typeMouvement, Integer quantite,
                                      Integer quantiteAvant, Integer quantiteApres, String motif,
                                      String effectuePar, Long idReservation) {
        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setTypeMouvement(typeMouvement);
        mouvement.setQuantite(quantite);
        mouvement.setQuantiteAvant(quantiteAvant);
        mouvement.setQuantiteApres(quantiteApres);
        mouvement.setMotif(motif);
        mouvement.setEffectuePar(effectuePar);
        mouvement.setIdReservation(idReservation);

        mouvementStockRepository.save(mouvement);

        log.debug("Mouvement de stock enregistré : Type={}, Quantité={}", typeMouvement, quantite);
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
        dto.setPrixUnitaire(produit.getPrixUnitaire());
        dto.setQuantiteInitial(produit.getQuantiteInitial());
        dto.setQuantiteDisponible(produit.getQuantiteDisponible());
        dto.setMaintenanceRequise(produit.getMaintenanceRequise());
        dto.setTypeProduit(produit.getTypeProduit());
        dto.setSeuilCritique(produit.getSeuilCritique());
        dto.setDateDerniereModification(produit.getDateModification());
        dto.setDateCreation(produit.getDateCreation());


        // Calculer les indicateurs
        dto.setEnStock(produit.getQuantiteDisponible() > 0);
        dto.setAlerteStockCritique(produit.getQuantiteDisponible() > 0 &&
                produit.getQuantiteDisponible() <= SEUIL_CRITIQUE_DEFAUT);

        // Statistiques (optionnel - peut être lourd en performance)
        if (produit.getLigneReservationProduit() != null) {
            dto.setNombreReservations(produit.getLigneReservationProduit().size());
        }

        if (produit.getAvisProduit() != null && !produit.getAvisProduit().isEmpty()) {
            double moyenneNotes = produit.getAvisProduit().stream()
                    .mapToInt(Avis::getNote)
                    .average()
                    .orElse(0.0);
            dto.setMoyenneNotes(moyenneNotes);
            dto.setNombreAvis(produit.getAvisProduit().size());
        }

        return dto;
    }

    /**
     * Convertir une entité MouvementStock en DTO
     */
    private MouvementStockResponseDto convertMouvementToDto(MouvementStock mouvement) {
        MouvementStockResponseDto dto = MouvementStockResponseDto.builder()
                .idMouvement(mouvement.getIdMouvement())
                .idProduit(mouvement.getProduit().getIdProduit())
                .nomProduit(mouvement.getProduit().getNomProduit())
                .codeProduit(mouvement.getProduit().getCodeProduit())
                .typeMouvement(mouvement.getTypeMouvement())
                .quantite(mouvement.getQuantite())
                .quantiteAvant(mouvement.getQuantiteAvant())
                .quantiteApres(mouvement.getQuantiteApres())
                .dateMouvement(mouvement.getDateMouvement())
                .motif(mouvement.getMotif())
                .effectuePar(mouvement.getEffectuePar())
                .idReservation(mouvement.getIdReservation())
                .codeInstance(mouvement.getCodeInstance())
                .build();

        // Si le mouvement concerne une instance, récupérer les infos complètes
        if (mouvement.getCodeInstance() != null) {
            instanceProduitRepository.findByNumeroSerie(mouvement.getCodeInstance())
                    .ifPresent(instance -> {
                        dto.setIdInstance(instance.getIdInstance());
                        dto.setNumeroSerie(instance.getNumeroSerie());
                    });
        }

        return  dto ;
    }

    /**
     * Vérifie s'il existe des instances endommagées ou hors service
     */
    public boolean hasInstancesProblematiques(List<InstanceProduit> instances) {
        if (instances == null || instances.isEmpty()) {
            return false;
        }

        return instances.stream()
                .anyMatch(instance ->
                        instance.getEtatPhysique() == EtatPhysique.ENDOMMAGE ||
                                instance.getStatut() == StatutInstance.HORS_SERVICE ||
                                instance.getStatut() == StatutInstance.EN_MAINTENANCE
                );
    }

    /**
     * Retourne la liste des instances problématiques
     */
    public List<InstanceProduit> getInstancesProblematiques(List<InstanceProduit> instances) {
        if (instances == null || instances.isEmpty()) {
            return Collections.emptyList();
        }

        return instances.stream()
                .filter(instance ->
                        instance.getEtatPhysique() == EtatPhysique.ENDOMMAGE ||
                                instance.getStatut() == StatutInstance.HORS_SERVICE ||
                                instance.getStatut() == StatutInstance.EN_MAINTENANCE
                )
                .collect(Collectors.toList());
    }

    /**
     * Compte le nombre d'instances problématiques
     */
    public long countInstancesProblematiques(List<InstanceProduit> instances) {
        if (instances == null || instances.isEmpty()) {
            return 0;
        }

        return instances.stream()
                .filter(instance ->
                        instance.getEtatPhysique() == EtatPhysique.ENDOMMAGE ||
                                instance.getStatut() == StatutInstance.HORS_SERVICE ||
                                instance.getStatut() == StatutInstance.EN_MAINTENANCE
                )
                .count();
    }
}