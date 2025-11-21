package tn.weeding.agenceevenementielle.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.livraison.*;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.*;
import tn.weeding.agenceevenementielle.exceptions.CustomException;
import tn.weeding.agenceevenementielle.repository.*;

import java.io.ByteArrayOutputStream;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des livraisons
 * Sprint 6 - Gestion des livraisons
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LivraisonServiceImpl implements LivraisonServiceInterface {

    private final LivraisonRepository livraisonRepo;
    private final AffectationLivraisonRepository affectationRepo;
    private final LigneReservationRepository ligneReservationRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final UtilisateurRoleRepository utilisateurRoleRepo ;

    // ============================================
    // CRUD LIVRAISONS
    // ============================================

    @Override
    public LivraisonResponseDto creerLivraison(LivraisonRequestDto dto, String username) {
        log.info("🚚 Création d'une nouvelle livraison: {}", dto.getTitreLivraison());

        // Vérifier que les lignes de réservation existent et sont confirmées
        List<LigneReservation> lignes = ligneReservationRepo.findAllById(dto.getIdLignesReservation());

        if (lignes.size() != dto.getIdLignesReservation().size()) {
            throw new CustomException("Certaines lignes de réservation sont introuvables");
        }

        // Vérifier que toutes les lignes appartiennent à des réservations confirmées
        for (LigneReservation ligne : lignes) {
            if (ligne.getReservation().getStatutReservation() != StatutReservation.CONFIRME) {
                throw new CustomException(
                        "La ligne ID " + ligne.getIdLigneReservation() +
                                " appartient à une réservation non confirmée"
                );
            }

            // Vérifier que la ligne n'est pas déjà affectée à une autre livraison
            if (ligne.getLivraison() != null) {
                throw new CustomException(
                        "La ligne ID " + ligne.getIdLigneReservation() +
                                " est déjà affectée à la livraison " + ligne.getLivraison().getIdLivraison()
                );
            }
        }

        // Créer la livraison
        Livraison livraison = new Livraison();
        livraison.setTitreLivraison(dto.getTitreLivraison());
        livraison.setAdresserLivraison(dto.getAdresseLivraison());
        livraison.setDateLivraison(dto.getDateLivraison());
        livraison.setHeureLivraison(dto.getHeureLivraison());
        livraison.setStatutLivraison(StatutLivraison.EN_ATTENTE);
        livraison.setAffectationLivraisons(new HashSet<>());

        livraison = livraisonRepo.save(livraison);
        log.info("✅ Livraison créée avec ID: {}", livraison.getIdLivraison());

        // Associer les lignes de réservation à la livraison
        for (LigneReservation ligne : lignes) {
            ligne.setLivraison(livraison);
            // Mettre le statut de la ligne en EN_ATTENTE si elle ne l'est pas déjà
            if (ligne.getStatutLivraisonLigne() != StatutLivraison.EN_ATTENTE) {
                ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
            }
            ligneReservationRepo.save(ligne);
        }

        log.info("📦 {} lignes de réservation associées à la livraison", lignes.size());

        return toDto(livraison);
    }

    @Override
    public LivraisonResponseDto modifierLivraison(Long idLivraison, LivraisonRequestDto dto, String username) {
        log.info("✏️ Modification de la livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        // Vérifier que la livraison n'est pas déjà livrée
        if (livraison.getStatutLivraison() == StatutLivraison.LIVREE) {
            throw new CustomException("Impossible de modifier une livraison déjà livrée");
        }

        // Mettre à jour les informations de base
        livraison.setTitreLivraison(dto.getTitreLivraison());
        livraison.setAdresserLivraison(dto.getAdresseLivraison());
        livraison.setDateLivraison(dto.getDateLivraison());
        livraison.setHeureLivraison(dto.getHeureLivraison());

        // Si les lignes de réservation ont changé
        if (dto.getIdLignesReservation() != null && !dto.getIdLignesReservation().isEmpty()) {
            // Récupérer les anciennes lignes et les dissocier
            List<LigneReservation> anciennesLignes =
                    ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

            for (LigneReservation ligne : anciennesLignes) {
                ligne.setLivraison(null);
                ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
                ligneReservationRepo.save(ligne);
            }

            // Associer les nouvelles lignes
            List<LigneReservation> nouvellesLignes =
                    ligneReservationRepo.findAllById(dto.getIdLignesReservation());

            for (LigneReservation ligne : nouvellesLignes) {
                // Vérifier que la ligne n'est pas déjà affectée ailleurs
                if (ligne.getLivraison() != null && !ligne.getLivraison().getIdLivraison().equals(idLivraison)) {
                    throw new CustomException(
                            "La ligne ID " + ligne.getIdLigneReservation() +
                                    " est déjà affectée à une autre livraison"
                    );
                }

                ligne.setLivraison(livraison);
                ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
                ligneReservationRepo.save(ligne);
            }

            log.info("🔄 Lignes de réservation mises à jour pour la livraison");
        }

        livraison = livraisonRepo.save(livraison);
        log.info("✅ Livraison modifiée avec succès");

        return toDto(livraison);
    }

    @Override
    @Transactional(readOnly = true)
    public LivraisonResponseDto getLivraisonById(Long idLivraison) {
        log.info("📋 Récupération de la livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        return toDto(livraison);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getAllLivraisons() {
        log.info("📋 Récupération de toutes les livraisons");

        return livraisonRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByStatut(StatutLivraison statut) {
        log.info("📋 Récupération des livraisons avec statut: {}", statut);

        return livraisonRepo.findByStatutLivraison(statut).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByDate(LocalDate date) {
        log.info("📋 Récupération des livraisons du: {}", date);
        return livraisonRepo.findByDateLivraison(date).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsBetweenDates(LocalDate dateDebut, LocalDate dateFin) {
        log.info("📋 Récupération des livraisons entre {} et {}", dateDebut, dateFin);

        return livraisonRepo.findLivraisonsBetweenDates(dateDebut, dateFin).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsAujourdhui() {
        log.info("📋 Récupération des livraisons d'aujourd'hui");

        return livraisonRepo.findLivraisonsAujourdhui().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByEmploye(Long idEmploye) {
        log.info("📋 Récupération des livraisons de l'employé ID: {}", idEmploye);

        return livraisonRepo.findLivraisonsByEmploye(idEmploye).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivraisonResponseDto> getLivraisonsByReservation(Long idReservation) {
        log.info("📋 Récupération des livraisons de la réservation ID: {}", idReservation);

        return livraisonRepo.findLivraisonsByReservation(idReservation).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void supprimerLivraison(Long idLivraison, String username) {
        log.info("🗑️ Suppression de la livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        // Vérifier que la livraison n'est pas déjà livrée
        if (livraison.getStatutLivraison() == StatutLivraison.LIVREE) {
            throw new CustomException("Impossible de supprimer une livraison déjà livrée");
        }

        // Dissocier les lignes de réservation
        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);
        for (LigneReservation ligne : lignes) {
            ligne.setLivraison(null);
            ligne.setStatutLivraisonLigne(StatutLivraison.EN_ATTENTE);
            ligneReservationRepo.save(ligne);
        }

        // Supprimer les affectations
        affectationRepo.deleteByLivraison_IdLivraison(idLivraison);

        // Supprimer la livraison
        livraisonRepo.delete(livraison);

        log.info("✅ Livraison supprimée avec succès");
    }

    // ============================================
    // GESTION DES STATUTS
    // ============================================

    @Override
    public LivraisonResponseDto changerStatutLivraison(Long idLivraison, StatutLivraison nouveauStatut, String username) {
        log.info("🔄 Changement de statut de la livraison ID {} -> {}", idLivraison, nouveauStatut);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        StatutLivraison ancienStatut = livraison.getStatutLivraison();
        livraison.setStatutLivraison(nouveauStatut);

        // Mettre à jour les statuts des lignes de réservation associées
        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

        for (LigneReservation ligne : lignes) {
            ligne.setStatutLivraisonLigne(nouveauStatut);
            ligneReservationRepo.save(ligne);
        }

        livraison = livraisonRepo.save(livraison);

        log.info("✅ Statut changé de {} à {} pour {} lignes", ancienStatut, nouveauStatut, lignes.size());

        return toDto(livraison);
    }

    @Override
    public LivraisonResponseDto marquerLivraisonEnCours(Long idLivraison, String username) {
        log.info("🚚 Marquage de la livraison ID {} comme EN_COURS", idLivraison);
        return changerStatutLivraison(idLivraison, StatutLivraison.EN_COURS, username);
    }

    @Override
    public LivraisonResponseDto marquerLivraisonLivree(Long idLivraison, String username) {
        log.info("✅ Marquage de la livraison ID {} comme LIVREE", idLivraison);

        // Changer le statut de la livraison et des lignes
        LivraisonResponseDto response = changerStatutLivraison(idLivraison, StatutLivraison.LIVREE, username);

        // Mettre à jour le statut de la réservation si toutes les lignes sont livrées
        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

        if (!lignes.isEmpty()) {
            Reservation reservation = lignes.get(0).getReservation();

            // Vérifier si toutes les lignes de la réservation sont livrées
            List<LigneReservation> toutesLignes =
                    ligneReservationRepo.findByReservation_IdReservation(reservation.getIdReservation());

            boolean toutesLivrees = toutesLignes.stream()
                    .allMatch(l -> l.getStatutLivraisonLigne() == StatutLivraison.LIVREE);

            if (toutesLivrees && reservation.getStatutReservation() == StatutReservation.CONFIRME) {
                // Mettre la réservation en EN_COURS
                reservation.setStatutReservation(StatutReservation.EN_COURS);
                // Le save sera fait automatiquement par JPA grâce à la cascade
                log.info("📋 Réservation {} passée EN_COURS (toutes les lignes sont livrées)",
                        reservation.getReferenceReservation());
            }
        }

        return response;
    }

    // ============================================
    // AFFECTATION D'EMPLOYÉS
    // ============================================

    @Override
    public AffectationLivraisonDto affecterEmploye(AffectationLivraisonRequestDto dto, String username) {
        log.info("👤 Affectation de l'employé ID {} à la livraison ID {}",
                dto.getIdEmploye(), dto.getIdLivraison());

        // Vérifier que la livraison existe
        Livraison livraison = livraisonRepo.findById(dto.getIdLivraison())
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + dto.getIdLivraison()));

        // Vérifier que l'employé existe et a le rôle approprié
        Utilisateur employe = utilisateurRepo.findById(dto.getIdEmploye())
                .orElseThrow(() -> new CustomException("Employé introuvable avec ID: " + dto.getIdEmploye()));

        List<UtilisateurRole> utilisateurRoles = utilisateurRoleRepo.findByUtilisateurIdUtilisateur(dto.getIdEmploye());
        boolean estEmploye = utilisateurRoles.stream()
                .anyMatch(utilisateurRole -> utilisateurRole.getRole().getNom().equals("EMPLOYE") ||
                        utilisateurRole.getRole().getNom().equals("ADMIN") ||
                        utilisateurRole.getRole().getNom().equals("MANAGER"));

        if (!estEmploye) {
            throw new CustomException("L'utilisateur doit avoir le rôle EMPLOYE, ADMIN ou MANAGER");
        }

        // Vérifier que l'employé n'est pas déjà affecté à cette livraison
        if (affectationRepo.existsByLivraisonAndEmploye(dto.getIdLivraison(), dto.getIdEmploye())) {
            throw new CustomException("L'employé est déjà affecté à cette livraison");
        }

        // Créer l'affectation
        AffectationLivraison affectation = new AffectationLivraison();
        affectation.setLivraison(livraison);
        affectation.setUtilisateur(employe);
        affectation.setDateAffectationLivraison(dto.getDateAffectation());
        affectation.setHeureDebut(dto.getHeureDebut());
        affectation.setHeureFin(dto.getHeureFin());

        affectation = affectationRepo.save(affectation);

        log.info("✅ Employé {} affecté à la livraison {}", employe.getEmail(), livraison.getTitreLivraison());

        return toAffectationDto(affectation);
    }

    @Override
    public void retirerEmploye(Long idAffectation, String username) {
        log.info("🗑️ Retrait de l'affectation ID: {}", idAffectation);

        AffectationLivraison affectation = affectationRepo.findById(idAffectation)
                .orElseThrow(() -> new CustomException("Affectation introuvable avec ID: " + idAffectation));

        affectationRepo.delete(affectation);

        log.info("✅ Affectation supprimée avec succès");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffectationLivraisonDto> getAffectationsByLivraison(Long idLivraison) {
        log.info("📋 Récupération des affectations de la livraison ID: {}", idLivraison);

        return affectationRepo.findByLivraison_IdLivraison(idLivraison).stream()
                .map(this::toAffectationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffectationLivraisonDto> getAffectationsByEmploye(Long idEmploye) {
        log.info("📋 Récupération des affectations de l'employé ID: {}", idEmploye);

        return affectationRepo.findByUtilisateur_IdUtilisateur(idEmploye).stream()
                .map(this::toAffectationDto)
                .collect(Collectors.toList());
    }

    // ============================================
    // BON DE LIVRAISON (PDF)
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public byte[] genererBonLivraison(Long idLivraison) {
        log.info("📄 Génération du bon de livraison ID: {}", idLivraison);

        Livraison livraison = livraisonRepo.findById(idLivraison)
                .orElseThrow(() -> new CustomException("Livraison introuvable avec ID: " + idLivraison));

        List<LigneReservation> lignes = ligneReservationRepo.findByLivraison_IdLivraison(idLivraison);

        if (lignes.isEmpty()) {
            throw new CustomException("Aucune ligne de réservation associée à cette livraison");
        }

        Reservation reservation = lignes.get(0).getReservation();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Titre
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("BON DE LIVRAISON", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Informations de livraison
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

            Paragraph infoLivraison = new Paragraph();
            infoLivraison.add(new Chunk("Numéro de livraison: ", headerFont));
            infoLivraison.add(new Chunk("LIV-" + livraison.getIdLivraison() + "\n", normalFont));
            infoLivraison.add(new Chunk("Date: ", headerFont));
            infoLivraison.add(new Chunk(livraison.getDateLivraison().toString() + "\n", normalFont));
            infoLivraison.add(new Chunk("Heure: ", headerFont));
            infoLivraison.add(new Chunk(livraison.getHeureLivraison().toString() + "\n", normalFont));
            infoLivraison.add(new Chunk("Adresse: ", headerFont));
            infoLivraison.add(new Chunk(livraison.getAdresserLivraison() + "\n", normalFont));
            infoLivraison.setSpacingAfter(20);
            document.add(infoLivraison);

            // Informations client
            Paragraph infoClient = new Paragraph();
            infoClient.add(new Chunk("Client: ", headerFont));
            infoClient.add(new Chunk(reservation.getUtilisateur().getNom() + " " +
                    reservation.getUtilisateur().getPrenom() + "\n", normalFont));
            infoClient.add(new Chunk("Réservation: ", headerFont));
            infoClient.add(new Chunk(reservation.getReferenceReservation() + "\n", normalFont));
            infoClient.setSpacingAfter(20);
            document.add(infoClient);

            // Table des produits
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 3, 2, 2, 2});

            // En-têtes
            addTableHeader(table, "Qté");
            addTableHeader(table, "Produit");
            addTableHeader(table, "Date début");
            addTableHeader(table, "Date fin");
            addTableHeader(table, "Références");

            // Lignes de produits
            for (LigneReservation ligne : lignes) {
                table.addCell(String.valueOf(ligne.getQuantite()));
                table.addCell(ligne.getProduit().getNomProduit());
                table.addCell(ligne.getDateDebut().toString());
                table.addCell(ligne.getDateFin().toString());

                // Ajouter les références si produit avec référence
                if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE &&
                        ligne.getInstancesReservees() != null && !ligne.getInstancesReservees().isEmpty()) {
                    String refs = ligne.getInstancesReservees().stream()
                            .map(InstanceProduit::getNumeroSerie)
                            .collect(Collectors.joining(", "));
                    table.addCell(refs);
                } else {
                    table.addCell("-");
                }
            }

            document.add(table);

            // Employés affectés
            List<AffectationLivraison> affectations =
                    affectationRepo.findByLivraison_IdLivraison(idLivraison);

            if (!affectations.isEmpty()) {
                Paragraph employesTitle = new Paragraph("\nEmployés affectés:", headerFont);
                employesTitle.setSpacingBefore(20);
                document.add(employesTitle);

                for (AffectationLivraison aff : affectations) {
                    Paragraph emp = new Paragraph("- " + aff.getUtilisateur().getNom() + " " +
                            aff.getUtilisateur().getPrenom() + " (" +
                            aff.getHeureDebut() + " - " + aff.getHeureFin() + ")", normalFont);
                    document.add(emp);
                }
            }

            // Signature
            Paragraph signature = new Paragraph();
            signature.setSpacingBefore(50);
            signature.add(new Chunk("Signature du client: ______________________\n\n", normalFont));
            signature.add(new Chunk("Date et heure de réception: ______________________", normalFont));
            document.add(signature);

            document.close();

            log.info("✅ Bon de livraison généré avec succès");
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("❌ Erreur lors de la génération du PDF: {}", e.getMessage());
            throw new CustomException("Erreur lors de la génération du bon de livraison: " + e.getMessage());
        }
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerTitle, headerFont));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.setPadding(5);
        table.addCell(header);
    }

    // ============================================
    // STATISTIQUES
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public Long countByStatut(StatutLivraison statut) {
        return livraisonRepo.countByStatut(statut);
    }

    // ============================================
    // MÉTHODES DE CONVERSION (MAPPERS)
    // ============================================

    private LivraisonResponseDto toDto(Livraison livraison) {
        LivraisonResponseDto dto = new LivraisonResponseDto();
        dto.setIdLivraison(livraison.getIdLivraison());
        dto.setTitreLivraison(livraison.getTitreLivraison());
        dto.setAdresseLivraison(livraison.getAdresserLivraison());

        // Convertir Date en LocalDate
        dto.setDateLivraison(livraison.getDateLivraison());

        // Convertir Time en LocalTime
        dto.setHeureLivraison(livraison.getHeureLivraison());

        dto.setStatutLivraison(livraison.getStatutLivraison());

        // Récupérer les lignes associées
        List<LigneReservation> lignes =
                ligneReservationRepo.findByLivraison_IdLivraison(livraison.getIdLivraison());

        dto.setLignesReservation(lignes.stream()
                .map(this::toLigneLivraisonDto)
                .collect(Collectors.toList()));

        // Calculer le nombre total d'articles
        dto.setNombreTotalArticles(lignes.stream()
                .mapToInt(LigneReservation::getQuantite)
                .sum());

        // ✅  Vérifier si toutes les lignes proviennent de la MÊME réservation
        if (!lignes.isEmpty()) {
            Set<Long> reservationIds = lignes.stream()
                    .map(ligne -> ligne.getReservation().getIdReservation())
                    .collect(Collectors.toSet());

            // Si une seule réservation → on remplit les infos au niveau de la livraison
            if (reservationIds.size() == 1) {
                Reservation reservation = lignes.get(0).getReservation();
                dto.setNomClient(reservation.getUtilisateur().getNom());
                dto.setPrenomClient(reservation.getUtilisateur().getPrenom());
                dto.setReferenceReservation(reservation.getReferenceReservation());
            } else {
                // Plusieurs réservations → indiquer "Plusieurs clients"
                dto.setNomClient("Plusieurs clients");
                dto.setPrenomClient("");
                dto.setReferenceReservation("Multiples");
            }
        }

        // Récupérer les affectations
        dto.setAffectations(affectationRepo.findByLivraison_IdLivraison(livraison.getIdLivraison())
                .stream()
                .map(this::toAffectationDto)
                .collect(Collectors.toList()));

        return dto;
    }

    private LivraisonResponseDto.LigneLivraisonDto toLigneLivraisonDto(LigneReservation ligne) {
        LivraisonResponseDto.LigneLivraisonDto dto = new LivraisonResponseDto.LigneLivraisonDto();
        dto.setIdLigne(ligne.getIdLigneReservation());
        dto.setNomProduit(ligne.getProduit().getNomProduit());
        dto.setQuantite(ligne.getQuantite());
        dto.setDateDebut(ligne.getDateDebut());
        dto.setDateFin(ligne.getDateFin());
        dto.setStatutLivraisonLigne(ligne.getStatutLivraisonLigne());
        dto.setTypeProduit(ligne.getProduit().getTypeProduit().toString());

        // Si produit avec référence, ajouter les instances
        if (ligne.getProduit().getTypeProduit() == TypeProduit.AVEC_REFERENCE &&
                ligne.getInstancesReservees() != null) {
            dto.setInstancesReservees(ligne.getInstancesReservees().stream()
                    .map(InstanceProduit::getNumeroSerie)
                    .collect(Collectors.toList()));
        }

        // Infos de la réservation
        Reservation reservation = ligne.getReservation();
        if (reservation != null) {
            dto.setIdReservation(reservation.getIdReservation());
            dto.setReferenceReservation(reservation.getReferenceReservation());

            // Infos du client
            Utilisateur client = reservation.getUtilisateur();
            if (client != null) {
                dto.setIdClient(client.getIdUtilisateur());
                dto.setNomClient(client.getNom());
                dto.setPrenomClient(client.getPrenom());
                dto.setEmailClient(client.getEmail());
            }
        }


        return dto;
    }

    private AffectationLivraisonDto toAffectationDto(AffectationLivraison affectation) {
        AffectationLivraisonDto dto = new AffectationLivraisonDto();
        dto.setIdAffectation(affectation.getIdAffectationLivraison());

        dto.setDateAffectation(affectation.getDateAffectationLivraison());
        dto.setHeureDebut(affectation.getHeureDebut());
        dto.setHeureFin(affectation.getHeureFin());

        // Infos employé
        dto.setIdEmploye(affectation.getUtilisateur().getIdUtilisateur());
        dto.setNomEmploye(affectation.getUtilisateur().getNom());
        dto.setPrenomEmploye(affectation.getUtilisateur().getPrenom());
        dto.setEmailEmploye(affectation.getUtilisateur().getEmail());
        dto.setTelephoneEmploye(affectation.getUtilisateur().getTelephone().toString());

        // Infos livraison
        dto.setIdLivraison(affectation.getLivraison().getIdLivraison());
        dto.setTitreLivraison(affectation.getLivraison().getTitreLivraison());

        return dto;
    }
}