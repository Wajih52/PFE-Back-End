package tn.weeding.agenceevenementielle.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.produit.InstanceProduitResponseDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationRequestDto;
import tn.weeding.agenceevenementielle.dto.reservation.LigneReservationResponseDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutInstance;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.TypeProduit;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.exceptions.ReservationException;
import tn.weeding.agenceevenementielle.repository.InstanceProduitRepository;
import tn.weeding.agenceevenementielle.repository.LigneReservationRepository;
import tn.weeding.agenceevenementielle.repository.ProduitRepository;
import tn.weeding.agenceevenementielle.repository.ReservationRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ==========================================
 * SERVICE POUR LA GESTION DES LIGNES DE RÉSERVATION
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 *
 * Responsabilités:
 * - CRUD des lignes de réservation (produits dans le panier)
 * - Vérification de disponibilité AVANT création
 * - Affectation automatique des instances (produits avec référence)
 * - Gestion du stock (décrémentation/incrémentation)
 * - Calcul des montants (sous-totaux)
 * - Modification des quantités et dates
 * - Suppression avec libération du stock
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LigneReservationServiceImpl implements LigneReservationServiceInterface {

    private final LigneReservationRepository ligneReservationRepo;
    private final ReservationRepository reservationRepo;
    private final ProduitRepository produitRepo;
    private final InstanceProduitRepository instanceProduitRepo;
    private final InstanceProduitServiceInterface instanceProduitService;

    // ============================================
    // PARTIE 1: CRÉATION ET AJOUT DE LIGNES
    // ============================================

    /**
     * 📝 Créer une nouvelle ligne de réservation
     * Vérifie la disponibilité et affecte automatiquement les instances si nécessaire
     */
    @Override
    public LigneReservationResponseDto creerLigneReservation(LigneReservationRequestDto dto, Long idReservation, String username) {
        log.info("🛒 Ajout d'une ligne de réservation: Produit ID {} (Qté: {})", dto.getIdProduit(), dto.getQuantite());

        // Vérifier que la réservation existe
        Reservation reservation = reservationRepo.findById(idReservation)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Réservation avec ID " + idReservation + " introuvable"));

        // Vérifier que le produit existe
        Produit produit = produitRepo.findById(dto.getIdProduit())
                .orElseThrow(() -> new CustomException(
                        "Produit avec ID " + dto.getIdProduit() + " introuvable"));

        // Vérifier la disponibilité
        verifierDisponibilite(produit, dto.getQuantite(), dto.getDateDebut(), dto.getDateFin());

        // Créer la ligne de réservation
        LigneReservation ligne = new LigneReservation();
        ligne.setReservation(reservation);
        ligne.setProduit(produit);
        ligne.setQuantite(dto.getQuantite());
        ligne.setPrixUnitaire(produit.getPrixUnitaire());
        ligne.setDateDebut(dto.getDateDebut());
        ligne.setDateFin(dto.getDateFin());
        ligne.setObservations(dto.getObservations());
        ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);

        // Sauvegarder la ligne
        ligne = ligneReservationRepo.save(ligne);
        log.info("✅ Ligne créée avec ID: {}", ligne.getIdLigneReservation());

        // Gérer le stock selon le type de produit
        if (produit.getTypeProduit() == TypeProduit.avecReference) {
            // Affecter automatiquement les instances disponibles
            affecterInstancesAutomatiquement(ligne, produit, dto.getQuantite(), username);
        } else {
            // Décrémenter le stock pour les produits quantitatifs
            produit.setQuantiteDisponible(produit.getQuantiteDisponible() - dto.getQuantite());
            produitRepo.save(produit);
            log.info("📉 Stock décrémenté: {} -> {} (Produit: {})",
                    produit.getQuantiteDisponible() + dto.getQuantite(),
                    produit.getQuantiteDisponible(),
                    produit.getNomProduit());
        }

        return toDto(ligne);
    }

    /**
     * 🔄 Affecter automatiquement les N premières instances disponibles
     */
    private void affecterInstancesAutomatiquement(LigneReservation ligne, Produit produit, Integer quantite, String username) {
        log.info("🔗 Affectation automatique de {} instances du produit ID: {}", quantite, produit.getIdProduit());

        try {
            // Utiliser le service InstanceProduit pour réserver les instances
            List<InstanceProduitResponseDto> instancesReservees = instanceProduitService.reserverInstances(
                    produit.getIdProduit(),
                    quantite,
                    ligne.getIdLigneReservation(),
                    username
            );

            // Récupérer les entités pour les associer à la ligne
            Set<InstanceProduit> instances = instancesReservees.stream()
                    .map(dto -> instanceProduitRepo.findById(dto.getIdInstance())
                            .orElseThrow(() -> new CustomException("Instance introuvable")))
                    .collect(Collectors.toSet());

            ligne.setInstancesReservees(instances);
            ligneReservationRepo.save(ligne);

            log.info("✅ {} instances affectées avec succès à la ligne {}",
                    instancesReservees.size(), ligne.getIdLigneReservation());

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'affectation des instances: {}", e.getMessage());
            throw new CustomException("Impossible d'affecter les instances: " + e.getMessage());
        }
    }

    // ============================================
    // PARTIE 2: CONSULTATION DES LIGNES
    // ============================================

    /**
     * 📋 Récupérer une ligne par son ID
     */
    @Override
    public LigneReservationResponseDto getLigneReservationById(Long id) {
        log.info("🔍 Recherche de la ligne de réservation ID: {}", id);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));

        return toDto(ligne);
    }

    /**
     * 📋 Récupérer toutes les lignes d'une réservation
     */
    @Override
    public List<LigneReservationResponseDto> getLignesReservationByReservation(Long idReservation) {
        log.info("📋 Récupération des lignes de la réservation ID: {}", idReservation);

        // Vérifier que la réservation existe
        if (!reservationRepo.existsById(idReservation)) {
            throw new ReservationException.ReservationNotFoundException(
                    "Réservation avec ID " + idReservation + " introuvable");
        }

        List<LigneReservation> lignes = ligneReservationRepo.findByReservation_IdReservation(idReservation);

        return lignes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 📋 Récupérer les lignes contenant un produit spécifique
     */
    @Override
    public List<LigneReservationResponseDto> getLignesReservationByProduit(Long idProduit) {
        log.info("📋 Recherche des lignes contenant le produit ID: {}", idProduit);

        List<LigneReservation> lignes = ligneReservationRepo.findByProduit_IdProduit(idProduit);

        return lignes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 📋 Récupérer les lignes par statut de livraison
     */
    @Override
    public List<LigneReservationResponseDto> getLignesReservationByStatut(StatutLivraison statut) {
        log.info("📋 Recherche des lignes avec statut: {}", statut);

        List<LigneReservation> lignes = ligneReservationRepo.findByStatutLivraisonLigne(statut);

        return lignes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // PARTIE 3: MODIFICATION DES LIGNES
    // ============================================

    /**
     * ✏️ Modifier une ligne de réservation
     * Permet de changer la quantité, les dates ou les observations
     */
    @Override
    public LigneReservationResponseDto modifierLigneReservation(Long id, LigneReservationRequestDto dto, String username) {
        log.info("✏️ Modification de la ligne de réservation ID: {}", id);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));

        Produit produit = ligne.getProduit();
        Integer ancienneQuantite = ligne.getQuantite();
        Integer nouvelleQuantite = dto.getQuantite();

        // Si la quantité change, gérer le stock
        if (!ancienneQuantite.equals(nouvelleQuantite)) {
            log.info("🔄 Changement de quantité: {} -> {}", ancienneQuantite, nouvelleQuantite);

            if (produit.getTypeProduit() == TypeProduit.avecReference) {
                // Gérer les instances
                gererChangementQuantiteAvecInstances(ligne, ancienneQuantite, nouvelleQuantite, username);
            } else {
                // Gérer le stock quantitatif
                int difference = nouvelleQuantite - ancienneQuantite;

                if (difference > 0) {
                    // Augmentation: vérifier la disponibilité
                    verifierDisponibilite(produit, difference, dto.getDateDebut(), dto.getDateFin());
                    produit.setQuantiteDisponible(produit.getQuantiteDisponible() - difference);
                } else {
                    // Diminution: libérer le stock
                    produit.setQuantiteDisponible(produit.getQuantiteDisponible() + Math.abs(difference));
                }
                produitRepo.save(produit);
            }
        }

        // Mettre à jour les autres champs
        ligne.setQuantite(nouvelleQuantite);
        ligne.setDateDebut(dto.getDateDebut());
        ligne.setDateFin(dto.getDateFin());
        ligne.setObservations(dto.getObservations());

        ligne = ligneReservationRepo.save(ligne);
        log.info("✅ Ligne modifiée avec succès");

        return toDto(ligne);
    }

    /**
     * 🔄 Gérer le changement de quantité pour les produits avec référence
     */
    private void gererChangementQuantiteAvecInstances(LigneReservation ligne, Integer ancienneQte, Integer nouvelleQte, String username) {
        int difference = nouvelleQte - ancienneQte;

        if (difference > 0) {
            // Augmentation: affecter plus d'instances
            log.info("➕ Affectation de {} instances supplémentaires", difference);
            affecterInstancesAutomatiquement(ligne, ligne.getProduit(), difference, username);

        } else if (difference < 0) {
            // Diminution: libérer des instances
            int nombreALiberer = Math.abs(difference);
            log.info("➖ Libération de {} instances", nombreALiberer);

            List<InstanceProduit> instancesActuelles = new ArrayList<>(ligne.getInstancesReservees());
            List<InstanceProduit> instancesALiberer = instancesActuelles.subList(0, Math.min(nombreALiberer, instancesActuelles.size()));

            for (InstanceProduit instance : instancesALiberer) {
                instanceProduitService.libererInstance(instance.getIdInstance(), username);
            }

            // Mettre à jour la liste des instances
            instancesActuelles.removeAll(instancesALiberer);
            ligne.setInstancesReservees(new HashSet<>(instancesActuelles));
        }
    }

    /**
     * 🔄 Mettre à jour le statut de livraison d'une ligne
     */
    @Override
    public LigneReservationResponseDto updateStatutLivraison(Long id, StatutLivraison nouveauStatut) {
        log.info("🔄 Changement de statut de livraison pour la ligne ID: {} -> {}", id, nouveauStatut);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));

        ligne.setStatutLivraisonLigne(nouveauStatut);
        ligne = ligneReservationRepo.save(ligne);

        // Si passage à EN_LIVRAISON, mettre à jour le statut des instances
        if (nouveauStatut == StatutLivraison.EN_COURS && ligne.getInstancesReservees() != null) {
            for (InstanceProduit instance : ligne.getInstancesReservees()) {
                instance.setStatut(StatutInstance.EN_LIVRAISON);
                instanceProduitRepo.save(instance);
            }
            log.info("📦 {} instances passées en EN_LIVRAISON", ligne.getInstancesReservees().size());
        }

        // Si passage à LIVRE, mettre à jour le statut des instances
        if (nouveauStatut == StatutLivraison.LIVREE && ligne.getInstancesReservees() != null) {
            for (InstanceProduit instance : ligne.getInstancesReservees()) {
                instance.setStatut(StatutInstance.EN_UTILISATION);
                instanceProduitRepo.save(instance);
            }
            log.info("✅ {} instances passées en EN_UTILISATION", ligne.getInstancesReservees().size());
        }

        return toDto(ligne);
    }

    // ============================================
    // PARTIE 4: SUPPRESSION DES LIGNES
    // ============================================

    /**
     * ❌ Supprimer une ligne de réservation
     * Libère automatiquement le stock et les instances
     */
    @Override
    public void supprimerLigneReservation(Long id, String username) {
        log.info("🗑️ Suppression de la ligne de réservation ID: {}", id);

        LigneReservation ligne = ligneReservationRepo.findById(id)
                .orElseThrow(() -> new ReservationException.ReservationNotFoundException(
                        "Ligne de réservation avec ID " + id + " introuvable"));

        Produit produit = ligne.getProduit();

        // Libérer le stock selon le type de produit
        if (produit.getTypeProduit() == TypeProduit.avecReference) {
            // Libérer les instances
            if (ligne.getInstancesReservees() != null) {
                for (InstanceProduit instance : ligne.getInstancesReservees()) {
                    try {
                        instanceProduitService.libererInstance(instance.getIdInstance(), username);
                    } catch (Exception e) {
                        log.error("⚠️ Erreur lors de la libération de l'instance {}: {}",
                                instance.getNumeroSerie(), e.getMessage());
                    }
                }
                log.info("✅ {} instances libérées", ligne.getInstancesReservees().size());
            }
        } else {
            // Libérer le stock quantitatif
            produit.setQuantiteDisponible(produit.getQuantiteDisponible() + ligne.getQuantite());
            produitRepo.save(produit);
            log.info("📈 Stock libéré: +{} (Total: {})", ligne.getQuantite(), produit.getQuantiteDisponible());
        }

        // Supprimer la ligne
        ligneReservationRepo.delete(ligne);
        log.info("✅ Ligne supprimée avec succès");
    }

    // ============================================
    // PARTIE 5: STATISTIQUES ET CALCULS
    // ============================================

    /**
     * 💰 Calculer le montant total d'une réservation
     */
    @Override
    public Double calculerMontantTotalReservation(Long idReservation) {
        log.info("💰 Calcul du montant total pour la réservation ID: {}", idReservation);

        List<LigneReservation> lignes = ligneReservationRepo.findByReservation_IdReservation(idReservation);

        double montantTotal = lignes.stream()
                .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
                .sum();

        log.info("💵 Montant total calculé: {} TND", montantTotal);
        return montantTotal;
    }

    /**
     * 📊 Obtenir les statistiques d'une réservation
     */
    @Override
    public Map<String, Object> getStatistiquesReservation(Long idReservation) {
        log.info("📊 Récupération des statistiques de la réservation ID: {}", idReservation);

        List<LigneReservation> lignes = ligneReservationRepo.findByReservation_IdReservation(idReservation);

        Map<String, Object> stats = new HashMap<>();
        stats.put("nombreLignes", lignes.size());
        stats.put("nombreProduitsTotal", lignes.stream().mapToInt(LigneReservation::getQuantite).sum());
        stats.put("montantTotal", calculerMontantTotalReservation(idReservation));
        stats.put("produitsParCategorie", grouperParCategorie(lignes));

        return stats;
    }

    /**
     * 📊 Grouper les produits par catégorie
     */
    private Map<String, Integer> grouperParCategorie(List<LigneReservation> lignes) {
        return lignes.stream()
                .collect(Collectors.groupingBy(
                        ligne -> ligne.getProduit().getCategorieProduit().toString(),
                        Collectors.summingInt(LigneReservation::getQuantite)
                ));
    }

    // ============================================
    // PARTIE 6: VÉRIFICATIONS ET VALIDATIONS
    // ============================================

    /**
     * ✅ Vérifier la disponibilité d'un produit
     */
    private void verifierDisponibilite(Produit produit, Integer quantiteDemandee, Date dateDebut, Date dateFin) {
        log.info("🔍 Vérification de disponibilité pour {} unités de {}", quantiteDemandee, produit.getNomProduit());

        if (produit.getTypeProduit() == TypeProduit.avecReference) {
            // Pour les produits avec référence, vérifier le nombre d'instances disponibles
            int instancesDisponibles = instanceProduitRepo.countInstancesDisponibles(produit.getIdProduit());

            if (instancesDisponibles < quantiteDemandee) {
                throw new CustomException(String.format(
                        "❌ Instances insuffisantes pour %s. Demandé: %d, Disponible: %d instances",
                        produit.getNomProduit(), quantiteDemandee, instancesDisponibles));
            }
        } else {
            // Pour les produits quantitatifs, vérifier le stock
            if (produit.getQuantiteDisponible() < quantiteDemandee) {
                throw new CustomException(String.format(
                        "❌ Stock insuffisant pour %s. Demandé: %d, Disponible: %d",
                        produit.getNomProduit(), quantiteDemandee, produit.getQuantiteDisponible()));
            }
        }

        log.info("✅ Disponibilité confirmée");
    }

    // ============================================
    // PARTIE 7: CONVERSION DTO
    // ============================================

    /**
     * 🔄 Convertir une entité en DTO
     */
    private LigneReservationResponseDto toDto(LigneReservation ligne) {
        Produit produit = ligne.getProduit();

        LigneReservationResponseDto dto = LigneReservationResponseDto.builder()
                .idLigneReservation(ligne.getIdLigneReservation())
                .idProduit(produit.getIdProduit())
                .nomProduit(produit.getNomProduit())
                .codeProduit(produit.getCodeProduit())
                .imageProduit(produit.getImageProduit())
                .quantite(ligne.getQuantite())
                .prixUnitaire(ligne.getPrixUnitaire())
                .sousTotal(ligne.getQuantite() * ligne.getPrixUnitaire())
                .dateDebut(ligne.getDateDebut())
                .dateFin(ligne.getDateFin())
                .statutLivraisonLigne(ligne.getStatutLivraisonLigne())
                .observations(ligne.getObservations())
                .build();

        // Ajouter les instances si produit avec référence
        if (ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {
            List<String> numerosSeries = ligne.getInstancesReservees().stream()
                    .map(InstanceProduit::getNumeroSerie)
                    .collect(Collectors.toList());
            dto.setNumerosSeries(numerosSeries);
        }

        // Ajouter les infos de livraison si présente
        if (ligne.getLivraison() != null) {
            dto.setIdLivraison(ligne.getLivraison().getIdLivraison());
            dto.setTitreLivraison(ligne.getLivraison().getTitreLivraison());
        }

        return dto;
    }
}