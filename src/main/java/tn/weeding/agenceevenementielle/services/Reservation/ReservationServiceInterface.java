package tn.weeding.agenceevenementielle.services.Reservation;

import tn.weeding.agenceevenementielle.dto.modifDateReservation.DateConstraintesDto;
import tn.weeding.agenceevenementielle.dto.modifDateReservation.DatePeriodeDto;
import tn.weeding.agenceevenementielle.dto.reservation.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * ==========================================
 * INTERFACE DU SERVICE DE RÉSERVATION
 * Sprint 4 - Gestion des réservations (incluant devis)
 * ==========================================
 *
 * Ce service gère le cycle complet:
 * 1. Création de devis par le client
 * 2. Modification et validation du devis par l'admin
 * 3. Acceptation/Refus du devis par le client
 * 4. Gestion des réservations confirmées
 */
public interface ReservationServiceInterface {

    // ============ GESTION DES DEVIS (Client) ============

    /**
     * Étape 1: Le CLIENT crée un DEVIS
     * - Sélectionne des produits avec dates
     * - Vérification automatique de la disponibilité
     * - Calcul du montant total
     * - Statut: EnAttente
     *
     * @param devisRequest Détails du devis (produits, quantités, dates)
     * @param idUtilisateur ID du client
     * @param username Username du client (pour traçabilité)
     * @return Le devis créé avec statut "En Attente"
     */
    ReservationResponseDto creerDevis(DevisRequestDto devisRequest, Long idUtilisateur, String username);

    /**
     * Vérifier la disponibilité d'un produit sur une période AVANT de créer le devis
     * Utile pour afficher la disponibilité en temps réel au client
     *
     * @param verificationDto Détails de la vérification
     * @return Réponse avec disponibilité et quantité disponible
     */
    DisponibiliteResponseDto verifierDisponibilite(VerificationDisponibiliteDto verificationDto);

    /**
     * Vérifier la disponibilité de plusieurs produits en une fois
     *
     * @param verifications Liste des vérifications
     * @return Liste des disponibilités
     */
    List<DisponibiliteResponseDto> verifierDisponibilites(List<VerificationDisponibiliteDto> verifications);

    // ============ GESTION DES DEVIS (Admin) ============

    /**
     * Étape 2: L'ADMIN modifie et valide un DEVIS
     * - Peut modifier les prix unitaires
     * - Peut appliquer des remises (% ou montant fixe)
     * - Recalcule automatiquement le montant total
     * - Le devis reste en statut "EnAttente" jusqu'à la validation client
     *
     * @param modificationDto Modifications à appliquer
     * @param username Username de l'admin
     * @return Le devis modifié
     */
    ReservationResponseDto modifierDevisParAdmin(DevisModificationDto modificationDto, String username);

    /**
     * L'admin peut annuler un devis (si le client ne répond pas, etc.)
     *
     * @param idReservation ID du devis
     * @param motif Motif de l'annulation
     * @param username Username de l'admin
     */
    void annulerDevisParAdmin(Long idReservation, String motif, String username);

    // ============ VALIDATION PAR LE CLIENT ============

    /**
     * Étape 3: Le CLIENT valide ou refuse le devis modifié par l'admin
     * - Si accepté: Statut passe à "confirme" + Réservation des instances/quantités
     * - Si refusé: Statut passe à "annule"
     *
     * @param validationDto Décision du client
     * @param username Username du client
     * @return La réservation confirmée ou annulée
     */
    ReservationResponseDto validerDevisParClient(ValidationDevisDto validationDto, String username);

    // ============ CONSULTATION DES RÉSERVATIONS ============

    /**
     * Récupérer une réservation par son ID
     */
    ReservationResponseDto getReservationById(Long idReservation);

    /**
     * Récupérer une réservation par sa référence
     */
    ReservationResponseDto getReservationByReference(String referenceReservation);

    /**
     * Récupérer toutes les réservations d'un client
     */
    List<ReservationResponseDto> getReservationsByClient(Long idUtilisateur);

    /**
     * Récupérer les devis en attente d'un client
     */
    List<ReservationResponseDto> getDevisEnAttenteByClient(Long idUtilisateur);

    /**
     * Récupérer toutes les réservations (Admin)
     */
    List<ReservationResponseDto> getAllReservations();

    /**
     * Récupérer toutes les réservations par statut
     */
    List<ReservationResponseDto> getReservationsByStatut(StatutReservation statut);

    /**
     * Récupérer tous les devis en attente (Admin)
     */
    List<ReservationResponseDto> getAllDevisEnAttente();

    // ============ RECHERCHE AVANCÉE ============

    /**
     * Recherche multicritères de réservations
     */
    List<ReservationResponseDto> searchReservations(ReservationSearchDto searchDto);

    /**
     * Récupérer les réservations dans une période
     */
    List<ReservationResponseDto> getReservationsByPeriode(Date dateDebut, Date dateFin);

    /**
     * Récupérer les réservations confirmées à venir
     */
    List<ReservationResponseDto> getReservationsAVenir();

    /**
     * Récupérer les réservations en cours (actuellement actives)
     */
    List<ReservationResponseDto> getReservationsEnCours();

    /**
     * Récupérer les réservations passées
     */
    List<ReservationResponseDto> getReservationsPassees();

    // ============ MODIFICATION ET ANNULATION ============

    /**
     * Le client peut annuler sa réservation (si pas encore livrée)
     * Libère automatiquement le stock/instances
     */
    void annulerReservationParClient(Long idReservation, String motif, String username);



    /**
     * 📅 Vérifier si des nouvelles dates sont disponibles pour une réservation
     *
     * Permet au client de vérifier AVANT de modifier
     * Ne modifie rien, juste vérifie
     */
    VerificationModificationDatesDto verifAvantModifDateReservation (Long idReservation, DatePeriodeDto nouvellesDates);

    // ============ STATISTIQUES ============

    /**
     * Obtenir les statistiques globales des réservations
     */
    ReservationSummaryDto getStatistiquesReservations();

    /**
     * Obtenir les statistiques des réservations d'un client
     */
    ReservationSummaryDto getStatistiquesReservationsClient(Long idUtilisateur);

    /**
     * Calculer le chiffre d'affaires sur une période
     */
    Double calculateChiffreAffairesPeriode(Date dateDebut, Date dateFin);

    // ============ ALERTES ET NOTIFICATIONS ============

    /**
     * Récupérer les réservations qui commencent dans N jours
     * (pour notifications de livraison)
     */
    List<ReservationResponseDto> getReservationsCommencantDansNJours(int nbreJours);

    /**
     * Récupérer les réservations dont la date de fin approche
     * (pour notifications de retour)
     */
    List<ReservationResponseDto> getReservationsFinissantDansNJours(int nbreJours);

    /**
     * Récupérer les devis expirés (en attente depuis trop longtemps)
     * (pour relance client)
     */
    List<ReservationResponseDto> getDevisExpires(int nbreJours);

    List<ReservationResponseDto> getDevisExpiresToday();

    /**
     * Récupérer les réservations avec paiement incomplet
     */
    List<ReservationResponseDto> getReservationsAvecPaiementIncomplet();

    /**
     * Obtenir les contraintes de dates pour l'affichage au client
     * (Utile pour le frontend)
     */
    DateConstraintesDto getContraintesDates();
}


