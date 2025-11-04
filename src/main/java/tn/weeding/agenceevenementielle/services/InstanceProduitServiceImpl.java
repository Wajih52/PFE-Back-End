package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.produit.InstanceProduitRequestDto;
import tn.weeding.agenceevenementielle.dto.produit.InstanceProduitResponseDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.EtatPhysique;
import tn.weeding.agenceevenementielle.entities.enums.StatutInstance;
import tn.weeding.agenceevenementielle.entities.enums.TypeMouvement;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.ProduitException;
import tn.weeding.agenceevenementielle.repository.InstanceProduitRepository;
import tn.weeding.agenceevenementielle.repository.LigneReservationRepository;
import tn.weeding.agenceevenementielle.repository.MouvementStockRepository;
import tn.weeding.agenceevenementielle.repository.ProduitRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des instances de produits
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InstanceProduitServiceImpl implements InstanceProduitServiceInterface {

    private final InstanceProduitRepository instanceRepo;
    private final ProduitRepository produitRepo;
    private final CodeGeneratorServiceProduit codeGeneratorServiceProduit;
    private final MouvementStockRepository mouvementStockRepo;
    private final LigneReservationRepository ligneReservationRepo;

    // ============ CRUD DE BASE ============

    @Override
    public InstanceProduitResponseDto creerInstance(InstanceProduitRequestDto dto, String username) {
        log.info("Création d'une instance pour le produit ID: {} par {}", dto.getIdProduit(), username);

        // Vérifier que le produit existe et est de type AVEC_REFERENCE
        Produit produit = produitRepo.findById(dto.getIdProduit())
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Produit avec ID " + dto.getIdProduit() + " introuvable"));

        if (produit.getTypeProduit() != TypeProduit.AVEC_REFERENCE) {
            throw new ProduitException(
                    "Les instances ne peuvent être créées que pour les produits de type <<AVEC_REFERENCE>>");
        }

        // Vérifier l'unicité du numéro de série
        if (instanceRepo.existsByNumeroSerie(dto.getNumeroSerie())) {
            throw new ProduitException("Le numéro de série " + dto.getNumeroSerie() + " existe déjà");
        }


        // Créer l'instance
        InstanceProduit instance = InstanceProduit.builder()
                .numeroSerie(codeGeneratorServiceProduit.generateInstanceCode(produit.getNomProduit()))
                .produit(produit)
                .statut(dto.getStatut() != null ? dto.getStatut() : StatutInstance.DISPONIBLE)
                .etatPhysique(dto.getEtatPhysique() != null ? dto.getEtatPhysique() : EtatPhysique.BON_ETAT)
                .observation(dto.getObservation())
                .dateAcquisition(dto.getDateAcquisition() != null ? dto.getDateAcquisition() : LocalDate.now())
                .dateDerniereMaintenance(dto.getDateDerniereMaintenance())
                .dateProchaineMaintenance(dto.getDateProchaineMaintenance())
                .ajoutPar(username)
                .build();

        instance = instanceRepo.save(instance);

        // Enregistrer le mouvement de stock
        enregistrerMouvement(
                produit,
                TypeMouvement.AJOUT_INSTANCE,
                1,
                "Ajout instance " + instance.getNumeroSerie(),
                username,
                instance
        );

        // Mettre à jour la quantité disponible du produit
        mettreAJourQuantiteDisponible(produit);

        log.info("Instance {} créée avec succès", instance.getNumeroSerie());
        return toDto(instance);
    }

    @Override
    public InstanceProduitResponseDto modifierInstance(Long idInstance, InstanceProduitRequestDto dto, String username) {
        log.info("Modification de l'instance ID: {} par {}", idInstance, username);

        InstanceProduit instance = instanceRepo.findById(idInstance)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Instance avec ID " + idInstance + " introuvable"));

        // Vérifier l'unicité du numéro de série (si modifié)
        if (!instance.getNumeroSerie().equals(dto.getNumeroSerie())) {
            if (instanceRepo.existsByNumeroSerie(dto.getNumeroSerie())) {
                throw new ProduitException("Le numéro de série " + dto.getNumeroSerie() + " existe déjà");
            }
            instance.setNumeroSerie(dto.getNumeroSerie());
        }

        // Mettre à jour les champs
        instance.setStatut(dto.getStatut());
        instance.setEtatPhysique(dto.getEtatPhysique());
        instance.setObservation(dto.getObservation());
       // instance.setDateAcquisition(dto.getDateAcquisition());
        instance.setDateDerniereMaintenance(dto.getDateDerniereMaintenance());
        instance.setDateProchaineMaintenance(dto.getDateProchaineMaintenance());
        instance.setAjoutPar(username);

        instance = instanceRepo.save(instance);

        // Mettre à jour la quantité disponible du produit
        mettreAJourQuantiteDisponible(instance.getProduit());

        log.info("Instance {} modifiée avec succès", instance.getNumeroSerie());
        return toDto(instance);
    }

    @Override
    public void supprimerInstance(Long idInstance,String username) {
        log.info("Suppression de l'instance ID: {}", idInstance);

        InstanceProduit instance = instanceRepo.findById(idInstance)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Instance avec ID " + idInstance + " introuvable"));

        boolean estReservee = ligneReservationRepo.existsActiveReservationForInstance(
                instance.getIdInstance(),
                new Date()
        );

        if (estReservee) {
            throw new ProduitException(
                    "Impossible de supprimer l'instance " + instance.getNumeroSerie() +
                            " car elle est actuellement réservée");
        }

        Produit produit = instance.getProduit();
        instanceRepo.delete(instance);

        // Enregistrer le mouvement de stock
        enregistrerMouvement(
                produit,
                TypeMouvement.SUPPRESSION_INSTANCE,
                -1,
                "Suppression instance " + instance.getNumeroSerie(),
                username,
                instance
        );

        // Mettre à jour la quantité du produit
        mettreAJourQuantiteDisponible(produit);

        log.info("Instance supprimée avec succès");
    }

    // ============ CONSULTATION ============

    @Override
    public List<InstanceProduitResponseDto> getInstances() {
        return instanceRepo.findAll().stream()
                .map(this::toDto)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public InstanceProduitResponseDto getInstanceById(Long idInstance) {
        InstanceProduit instance = instanceRepo.findById(idInstance)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Instance avec ID " + idInstance + " introuvable"));
        return toDto(instance);
    }



    @Override
    @Transactional(readOnly = true)
    public InstanceProduitResponseDto getInstanceByNumeroSerie(String numeroSerie) {
        InstanceProduit instance = instanceRepo.findByNumeroSerie(numeroSerie)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Instance avec numéro de série " + numeroSerie + " introuvable"));
        return toDto(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceProduitResponseDto> getInstancesByProduit(Long idProduit) {
        return instanceRepo.findByProduit_IdProduit(idProduit).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceProduitResponseDto> getInstancesDisponibles(Long idProduit) {
        return instanceRepo.findInstancesDisponibles(idProduit).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<InstanceProduitResponseDto> getInstancesDisponiblesSurPeriode(Long idProduit, LocalDate dateDebut, LocalDate dateFin) {
        return instanceRepo.findInstancesDisponiblesSurPeriode(idProduit,dateDebut,dateFin).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceProduitResponseDto> getInstancesByStatut(StatutInstance statut) {
        return instanceRepo.findByStatut(statut).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }




    // ============ GESTION DES STATUTS ============

    @Override
    public InstanceProduitResponseDto changerStatut(Long idInstance, StatutInstance nouveauStatut, String username) {
        log.info("Changement du statut de l'instance ID: {} vers {} par {}", idInstance, nouveauStatut, username);

        InstanceProduit instance = instanceRepo.findById(idInstance)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Instance avec ID " + idInstance + " introuvable"));

        StatutInstance ancienStatut = instance.getStatut();
        if(ancienStatut.equals(StatutInstance.EN_MAINTENANCE)){
            throw new CustomException("Changement De Statut Simple De Maintenance ==> En Disponible est impossible");
        }

        if(ancienStatut.equals(StatutInstance.DISPONIBLE)&&nouveauStatut==StatutInstance.EN_MAINTENANCE) {
           throw new CustomException("Changement De Statut Simple De Disponible ==> En Maintenance est impossible ");
        }

        boolean estReservee = ligneReservationRepo.existsActiveReservationForInstance(
                instance.getIdInstance(),
                new Date()
        );

        if (estReservee) {
            throw new ProduitException(
                    "Impossible de mettre l'instance " + instance.getNumeroSerie() +
                            "en Etat :"+nouveauStatut+" car elle est actuellement réservée");
        }

        instance.setStatut(nouveauStatut);
        instance = instanceRepo.save(instance);

        if (nouveauStatut == StatutInstance.HORS_SERVICE || nouveauStatut == StatutInstance.PERDU) {
            enregistrerMouvement(
                    instance.getProduit(),
                    TypeMouvement.PRODUIT_ENDOMMAGE,
                    -1,
                    "Instance : "+nouveauStatut,
                    username,
                    instance
            );
        }

        // Si changement DISPONIBLE <-> autre, mettre à jour quantité
        if ((ancienStatut == StatutInstance.DISPONIBLE && nouveauStatut != StatutInstance.DISPONIBLE) ||
                (ancienStatut != StatutInstance.DISPONIBLE && nouveauStatut == StatutInstance.DISPONIBLE)) {
            mettreAJourQuantiteDisponible(instance.getProduit());
        }

        log.info("Statut de l'instance {} changé de {} vers {}",
                instance.getNumeroSerie(), ancienStatut, nouveauStatut);
        return toDto(instance);
    }

    // ============ GESTION DE LA MAINTENANCE ============

    @Override
    public InstanceProduitResponseDto envoyerEnMaintenance(Long idInstance, String motif, String username) {
        log.info("Envoi en maintenance de l'instance ID: {} par {}", idInstance, username);

        InstanceProduit instance = instanceRepo.findById(idInstance)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Instance avec ID " + idInstance + " introuvable"));

        // Vérifier que l'instance n'est pas réservée
        boolean estReservee = ligneReservationRepo.existsActiveReservationForInstance(
                instance.getIdInstance(),
                new Date()
        );

        if (estReservee) {
            throw new ProduitException(
                    "Impossible d'envoyer l'instance " + instance.getNumeroSerie() +
                            "En maintenance car elle est actuellement réservée");
        }
        instance.setStatut(StatutInstance.EN_MAINTENANCE);
        instance.setDateDerniereMaintenance(LocalDate.now());
        instance.setDateProchaineMaintenance(LocalDate.now().plusMonths(4));
        instance.setMotif(motif);
        instance = instanceRepo.save(instance);

        // Enregistrer le mouvement
        enregistrerMouvement(
                instance.getProduit(),
                TypeMouvement.MAINTENANCE,
                -1,
                "Instance envoyée en maintenance",
                username,
                instance
        );

        // Mettre à jour la quantité disponible
        mettreAJourQuantiteDisponible(instance.getProduit());

        log.info("Instance {} envoyée en maintenance",
                instance.getNumeroSerie());
        return toDto(instance);
    }

    @Override
    public InstanceProduitResponseDto retournerDeMaintenance(Long idInstance,LocalDate dateProchainMaintenance, String username) {
        log.info("Retour de maintenance de l'instance ID: {} par {}", idInstance, username);

        InstanceProduit instance = instanceRepo.findById(idInstance)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Instance avec ID " + idInstance + " introuvable"));

        instance.setStatut(StatutInstance.DISPONIBLE);
        instance.setDateDerniereMaintenance(LocalDate.now());
        instance.setDateProchaineMaintenance(dateProchainMaintenance);
        instance = instanceRepo.save(instance);

        // Enregistrer le mouvement
        enregistrerMouvement(
                instance.getProduit(),
                TypeMouvement.RETOUR_MAINTENANCE,
                1,
                "Instance retournée de maintenance",
                username,
                instance
        );

        // Mettre à jour la quantité disponible
        mettreAJourQuantiteDisponible(instance.getProduit());

        log.info("Instance {} retournée de maintenance", instance.getNumeroSerie());
        return toDto(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceProduitResponseDto> getInstancesNecessitantMaintenance() {
        return instanceRepo.findInstancesNecessitantMaintenance(LocalDate.now()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============ RÉSERVATION ============

    @Override
    public List<InstanceProduitResponseDto> reserverInstances(
            Long idProduit,
            int quantite,
            Long idLigneReservation,
            LocalDate dateDebut,
            LocalDate dateFin,
            String username) {

        log.info("Affectation de {} instances du produit ID: {} à la ligne {} (période: {}-{})",
                quantite, idProduit, idLigneReservation, dateDebut, dateFin);

        // ✅ Vérifier la disponibilité sur la PÉRIODE
        List<InstanceProduit> instancesDisponibles = instanceRepo.findInstancesDisponiblesSurPeriode(
                idProduit,
                dateDebut,
                dateFin
        );

        if (instancesDisponibles.size() < quantite) {
            throw new ProduitException(
                    "Stock insuffisant du " + dateDebut + " au " + dateFin + ": " +
                            "seulement " + instancesDisponibles.size() + " instances disponibles " +
                            "(demandé: " + quantite + ")");
        }

        // ✅ Récupérer les instances (SANS changer leur statut)
        List<InstanceProduit> instancesAAffecter = instancesDisponibles.stream()
                .limit(quantite)
                .toList();

        // ⚠️ NE PAS modifier l'instance ici !
        // La relation ManyToMany est gérée dans le ReservationService

        log.info("{} instances identifiées pour la ligne {}", quantite, idLigneReservation);

        return instancesAAffecter.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============ CRÉATION EN LOT ============

    @Override
    public List<InstanceProduitResponseDto> creerInstancesEnLot(Long idProduit, int quantite,
                                                                String prefixeNumeroSerie, String username) {
        log.info("Création de {} instances en lot pour le produit ID: {} par {}", quantite, idProduit, username);

        Produit produit = produitRepo.findById(idProduit)
                .orElseThrow(() -> new ProduitException.ProduitNotFoundException(
                        "Produit avec ID " + idProduit + " introuvable"));

        if (produit.getTypeProduit() != TypeProduit.AVEC_REFERENCE) {
            throw new ProduitException(
                    "Les instances ne peuvent être créées que pour les produits de type AVEC_REFERENCE");
        }

        // Trouver le prochain numéro disponible
        int maxNumero = instanceRepo.findByProduit_IdProduit(idProduit).stream()
                .map(InstanceProduit::getNumeroSerie)
                .filter(ns -> ns.startsWith(produit.getCodeProduit()))
                .map(ns -> {
                    try {
                        String[] parts = ns.split("-");
                        return Integer.parseInt(parts[parts.length - 1]);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(0);

        // Créer les instances
        List<InstanceProduit> instances = new java.util.ArrayList<>();
        for (int i = 1; i <= quantite; i++) {
            String numeroSerie = String.format("%s-%04d", produit.getCodeProduit(), maxNumero + i);

            InstanceProduit instance = InstanceProduit.builder()
                    .numeroSerie(numeroSerie)
                    .produit(produit)
                    .statut(StatutInstance.DISPONIBLE)
                    .etatPhysique(EtatPhysique.BON_ETAT)
                    .dateAcquisition(LocalDate.now())
                    .ajoutPar(username)
                    .build();

            instances.add(instanceRepo.save(instance));
        }

        // Enregistrer le mouvement de stock pour le lot
        enregistrerMouvement(
                produit,
                TypeMouvement.AJOUT_INSTANCE,
                quantite,
                "Ajout lot de " + quantite + " instances (" + produit.getCodeProduit() + ")",
                username,
                instances.get(0)
        );

        // Mettre à jour la quantité disponible du produit
        mettreAJourQuantiteDisponible(produit);

        log.info("{} instances créées avec succès en lot", quantite);
        return instances.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============ MÉTHODES UTILITAIRES ============

    /**
     * Met à jour la quantité disponible d'un produit AVEC_REFERENCE
     * en comptant les instances avec statut DISPONIBLE
     */
    private void mettreAJourQuantiteDisponible(Produit produit) {
        if (produit.getTypeProduit() == TypeProduit.AVEC_REFERENCE) {
            // Pour chercher les instances disponibles pour AUJOURD'HUI

            int disponibles = instanceRepo.countInstancesDisponiblesSurPeriode(
                    produit.getIdProduit(), LocalDate.now(), LocalDate.now());
            produit.setQuantiteDisponible(disponibles);
            produitRepo.save(produit);
            log.debug("Quantité disponible du produit {} mise à jour : {}",
                    produit.getNomProduit(), disponibles);
        }
    }

    /**
     * Enregistre un mouvement de stock pour traçabilité
     */
    private void enregistrerMouvement(Produit produit, TypeMouvement type,
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

        mouvementStockRepo.save(mouvement);

        log.debug("Mouvement enregistré: {} - {} ({}→{})",
                type, motif, quantiteAvant, quantiteApres);
    }

    /**
     * Convertit une entité InstanceProduit en DTO
     */
    public InstanceProduitResponseDto toDto(InstanceProduit instance) {
        InstanceProduitResponseDto dto = InstanceProduitResponseDto.builder()
                .idInstance(instance.getIdInstance())
                .numeroSerie(instance.getNumeroSerie())
                .statut(instance.getStatut())
                .etatPhysique(instance.getEtatPhysique())
                .idProduit(instance.getProduit().getIdProduit())
                .nomProduit(instance.getProduit().getNomProduit())
                .codeProduit(instance.getProduit().getCodeProduit())
                .observation(instance.getObservation())
                .dateAcquisition(instance.getDateAcquisition())
                .dateDerniereMaintenance(instance.getDateDerniereMaintenance())
                .dateProchaineMaintenance(instance.getDateProchaineMaintenance())
                .disponible(instance.isDisponiblePhysiquement())
                .maintenanceRequise(instance.maintenanceNecessaire())
                .ajoutPar(instance.getAjoutPar())
                .motif(instance.getMotif())
                .build();

        // Calculer les jours avant maintenance
        if (instance.getDateProchaineMaintenance() != null) {
            dto.setJoursAvantMaintenance(
                    (int) ChronoUnit.DAYS.between(LocalDate.now(), instance.getDateProchaineMaintenance())
            );
        }

        return dto;
    }
}