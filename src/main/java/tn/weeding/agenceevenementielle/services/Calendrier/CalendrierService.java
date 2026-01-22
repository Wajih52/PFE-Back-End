package tn.weeding.agenceevenementielle.services.Calendrier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.calendrier.CalendrierEventDto;
import tn.weeding.agenceevenementielle.dto.calendrier.CalendrierFiltreDto;
import tn.weeding.agenceevenementielle.dto.calendrier.CalendrierStatistiquesDto;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.StatutLivraison;
import tn.weeding.agenceevenementielle.entities.enums.StatutReservation;
import tn.weeding.agenceevenementielle.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion du calendrier
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalendrierService {

    private final ReservationRepository reservationRepo;
    private final LivraisonRepository livraisonRepo;
    private final LigneReservationRepository ligneReservationRepo;

    /**
     * Récupérer tous les événements du calendrier avec filtres
     */
    public List<CalendrierEventDto> getEvenements(CalendrierFiltreDto filtres) {
        log.info("Récupération des événements du calendrier avec filtres: {}", filtres);

        List<CalendrierEventDto> evenements = new ArrayList<>();

        // Ajouter les réservations si demandé
        if (Boolean.TRUE.equals(filtres.getInclureReservations())) {
            evenements.addAll(getReservationsCommeEvenements(filtres));
        }

        // Ajouter les livraisons si demandé
        if (Boolean.TRUE.equals(filtres.getInclureLivraisons())) {
            evenements.addAll(getLivraisonsCommeEvenements(filtres));
        }

        log.info("{} événements trouvés", evenements.size());
        return evenements;
    }

    /**
     * Convertir les réservations en événements calendrier
     */
    private List<CalendrierEventDto> getReservationsCommeEvenements(CalendrierFiltreDto filtres) {
        List<Reservation> reservations;

        // Récupérer les réservations selon les filtres
        if (filtres.getDateDebut() != null && filtres.getDateFin() != null) {
            reservations = reservationRepo.findReservationsEntreDates(
                    filtres.getDateDebut(),
                    filtres.getDateFin()
            );
        } else {
            reservations = reservationRepo.findAll();
        }

        return reservations.stream()
                .filter(res -> appliquerFiltresReservation(res, filtres))
                .map(this::convertirReservationEnEvent)
                .collect(Collectors.toList());
    }

    /**
     * Convertir les livraisons en événements calendrier
     */
    private List<CalendrierEventDto> getLivraisonsCommeEvenements(CalendrierFiltreDto filtres) {
        List<Livraison> livraisons;

        // Récupérer les livraisons selon les filtres
        if (filtres.getDateDebut() != null && filtres.getDateFin() != null) {
            livraisons = livraisonRepo.findByDateLivraisonBetween(
                    filtres.getDateDebut(),
                    filtres.getDateFin()
            );
        } else {
            livraisons = livraisonRepo.findAll();
        }

        return livraisons.stream()
                .filter(liv -> appliquerFiltresLivraison(liv, filtres))
                .map(this::convertirLivraisonEnEvent)
                .collect(Collectors.toList());
    }

    /**
     * Convertir une Reservation en CalendrierEventDto
     */
    private CalendrierEventDto convertirReservationEnEvent(Reservation reservation) {
        // Calculer les dates de début et fin (min/max des lignes)
        LocalDate dateDebut = reservation.getLigneReservations().stream()
                .map(LigneReservation::getDateDebut)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate dateFin = reservation.getLigneReservations().stream()
                .map(LigneReservation::getDateFin)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Résumé des produits
        String produitsResume = reservation.getLigneReservations().stream()
                .map(ligne -> ligne.getQuantite() + " " + ligne.getProduit().getNomProduit())
                .limit(3)
                .collect(Collectors.joining(", "));

        if (reservation.getLigneReservations().size() > 3) {
            produitsResume += "...";
        }

        // Obtenir le client
        Utilisateur client = reservation.getUtilisateur();

        return CalendrierEventDto.builder()
                .id(reservation.getIdReservation())
                .type("RESERVATION")
                .reference(reservation.getReferenceReservation())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .titre(reservation.getReferenceReservation() + " - " +
                        (client != null ? client.getNom() + " " + client.getPrenom() : "Client inconnu"))
                .description(produitsResume)
                .adresse(null) // Pas d'adresse pour une réservation simple
                .statut(reservation.getStatutReservation().name())
                .couleur(CalendrierEventDto.getCouleurParStatut("RESERVATION", reservation.getStatutReservation().name()))
                .idClient(client != null ? client.getIdUtilisateur() : null)
                .nomClient(client != null ? client.getNom() : null)
                .prenomClient(client != null ? client.getPrenom() : null)
                .emailClient(client != null ? client.getEmail() : null)
                .telephoneClient(client != null ? client.getTelephone().toString() : null)
                .nombreProduits(reservation.getLigneReservations().size())
                .produitsResume(produitsResume)
                .montantTotal(reservation.getMontantTotal())
                .montantPaye(reservation.getMontantPaye())
                .paiementComplet(reservation.isPaiementComplet())
                .build();
    }

    /**
     * Convertir une Livraison en CalendrierEventDto
     */
    private CalendrierEventDto convertirLivraisonEnEvent(Livraison livraison) {
        // Obtenir la réservation associée (via les lignes)
        Reservation reservation = livraison.getLigneReservations().stream()
                .findFirst()
                .map(LigneReservation::getReservation)
                .orElse(null);

        Utilisateur client = reservation != null ? reservation.getUtilisateur() : null;

        // Résumé des produits
        String produitsResume = livraison.getLigneReservations().stream()
                .map(ligne -> ligne.getQuantite() + " " + ligne.getProduit().getNomProduit())
                .limit(3)
                .collect(Collectors.joining(", "));

        if (livraison.getLigneReservations().size() > 3) {
            produitsResume += "...";
        }

        // Employé principal (première affectation)
        Utilisateur employe = livraison.getAffectationLivraisons().stream()
                .findFirst()
                .map(AffectationLivraison::getUtilisateur)
                .orElse(null);

        return CalendrierEventDto.builder()
                .id(livraison.getIdLivraison())
                .type("LIVRAISON")
                .reference(reservation != null ? reservation.getReferenceReservation() : "LIV-" + livraison.getIdLivraison())
                .dateDebut(livraison.getDateLivraison())
                .dateFin(livraison.getDateLivraison())
                .heure(livraison.getHeureLivraison())
                .titre(livraison.getTitreLivraison())
                .description(produitsResume)
                .adresse(livraison.getAdresserLivraison())
                .statut(livraison.getStatutLivraison().name())
                .couleur(CalendrierEventDto.getCouleurParStatut("LIVRAISON", livraison.getStatutLivraison().name()))
                .idClient(client != null ? client.getIdUtilisateur() : null)
                .nomClient(client != null ? client.getNom() : null)
                .prenomClient(client != null ? client.getPrenom() : null)
                .emailClient(client != null ? client.getEmail() : null)
                .telephoneClient(client != null ? client.getTelephone().toString() : null)
                .idEmploye(employe != null ? employe.getIdUtilisateur() : null)
                .nomEmploye(employe != null ? employe.getNom() : null)
                .prenomEmploye(employe != null ? employe.getPrenom() : null)
                .nombreProduits(livraison.getLigneReservations().size())
                .produitsResume(produitsResume)
                .build();
    }

    /**
     * Appliquer les filtres sur une réservation
     */
    private boolean appliquerFiltresReservation(Reservation reservation, CalendrierFiltreDto filtres) {
        // Filtre par client ID
        if (filtres.getIdClient() != null) {
            if (reservation.getUtilisateur() == null ||
                    !reservation.getUtilisateur().getIdUtilisateur().equals(filtres.getIdClient())) {
                return false;
            }
        }

        // Filtre par nom client
        if (filtres.getNomClient() != null && !filtres.getNomClient().isEmpty()) {
            if (reservation.getUtilisateur() == null ||
                    !reservation.getUtilisateur().getNom().toLowerCase().contains(filtres.getNomClient().toLowerCase())) {
                return false;
            }
        }

        // Filtre par statuts
        if (filtres.getStatutsReservation() != null && !filtres.getStatutsReservation().isEmpty()) {
            if (!filtres.getStatutsReservation().contains(reservation.getStatutReservation().name())) {
                return false;
            }
        }

        // Filtre par produit
        if (filtres.getIdProduit() != null) {
            boolean contientProduit = reservation.getLigneReservations().stream()
                    .anyMatch(ligne -> ligne.getProduit().getIdProduit().equals(filtres.getIdProduit()));
            if (!contientProduit) {
                return false;
            }
        }

        // Filtre paiement complet
        if (Boolean.TRUE.equals(filtres.getPaiementCompletUniquement())) {
            if (!reservation.isPaiementComplet()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Appliquer les filtres sur une livraison
     */
    private boolean appliquerFiltresLivraison(Livraison livraison, CalendrierFiltreDto filtres) {
        // Filtre par employé
        if (filtres.getIdEmploye() != null) {
            boolean employeAffecte = livraison.getAffectationLivraisons().stream()
                    .anyMatch(affectation -> affectation.getUtilisateur().getIdUtilisateur().equals(filtres.getIdEmploye()));
            if (!employeAffecte) {
                return false;
            }
        }

        // Filtre par statuts
        if (filtres.getStatutsLivraison() != null && !filtres.getStatutsLivraison().isEmpty()) {
            if (!filtres.getStatutsLivraison().contains(livraison.getStatutLivraison().name())) {
                return false;
            }
        }

        // Filtre par produit
        if (filtres.getIdProduit() != null) {
            boolean contientProduit = livraison.getLigneReservations().stream()
                    .anyMatch(ligne -> ligne.getProduit().getIdProduit().equals(filtres.getIdProduit()));
            if (!contientProduit) {
                return false;
            }
        }

        return true;
    }

    /**
     * Obtenir les statistiques du calendrier pour une période
     */
    public CalendrierStatistiquesDto getStatistiques(LocalDate dateDebut, LocalDate dateFin) {
        log.info("Calcul des statistiques pour la période {} - {}", dateDebut, dateFin);

        long nombreReservations = reservationRepo.countReservationsEntreDates(dateDebut, dateFin);
        long nombreLivraisons = livraisonRepo.countByDateLivraisonBetween(dateDebut, dateFin);

        List<Reservation> reservations = reservationRepo.findReservationsEntreDates(dateDebut, dateFin);
        double montantTotalPeriode = reservations.stream()
                .filter(reservation -> reservation.getStatutReservation() != StatutReservation.ANNULE)
                .mapToDouble(Reservation::getMontantTotal)
                .sum();

        long reservationsPayees = reservations.stream()
                .filter(Reservation::isPaiementComplet)
                .count();

        return CalendrierStatistiquesDto.builder()
                .nombreReservations(nombreReservations)
                .nombreLivraisons(nombreLivraisons)
                .montantTotalPeriode(montantTotalPeriode)
                .tauxPaiement(nombreReservations > 0 ? (double) reservationsPayees / nombreReservations * 100 : 0)
                .build();
    }
}