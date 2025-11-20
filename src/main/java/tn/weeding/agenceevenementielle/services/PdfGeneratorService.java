package tn.weeding.agenceevenementielle.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.entities.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorService {

    private static final String PDF_DIRECTORY = "factures/";
    private static final String LOGO_PATH = "src/main/resources/static/logo-elegant-hive.png"; // À ajuster selon ton chemin

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);
    private static final double TVA_TAUX = 0.19; // 19% TVA

    /**
     * Génère le PDF de la facture et retourne le chemin du fichier
     */
    public String genererPdfFacture(Facture facture) throws DocumentException, IOException {
        // Créer le répertoire si nécessaire
        Path directory = Paths.get(PDF_DIRECTORY);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        // Nom du fichier PDF
        String fileName = facture.getNumeroFacture().replace("/", "-") + ".pdf";
        String filePath = PDF_DIRECTORY + fileName;
        Path path = Paths.get(filePath);

        // Créer le document
        Document document = null;
        FileOutputStream fos = null;
        PdfWriter writer = null;

        try {
            fos = new FileOutputStream(filePath);
            document = new Document(PageSize.A4, 50, 50, 50, 50);
            writer = PdfWriter.getInstance(document, fos);

            document.open();

            // Contenu selon le type de facture
            switch (facture.getTypeFacture()) {
                case DEVIS:
                    ajouterContenuDevis(document, facture);
                    break;
                case PRO_FORMA:
                    ajouterContenuProForma(document, facture);
                    break;
                case FINALE:
                    ajouterContenuFinale(document, facture);
                    break;
            }

            //  Fermer dans le bon ordre
            document.close();
            writer.close();
            fos.close();

            log.info("📄 PDF généré avec succès : {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("❌ Erreur génération PDF : {}", e.getMessage());

            // Nettoyer en cas d'erreur
            if (document != null && document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    log.error("Erreur fermeture stream");
                }
            }

            // Supprimer le fichier corrompu
            try {
                if (Files.exists(path)) {
                    Files.delete(path);
                }
            } catch (IOException ex) {
                // Ignore
            }

            throw new RuntimeException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    /**
     * Contenu pour facture DEVIS
     */
    private void ajouterContenuDevis(Document document, Facture facture) throws DocumentException, IOException {
        Reservation reservation = facture.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        // Titre
        Paragraph titre = new Paragraph("DEVIS", TITLE_FONT);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(20);
        document.add(titre);

        // Logo + Infos entreprise ET client (côte à côte)
        ajouterEnteteFacture(document, client);

        // Numéro et date
        document.add(new Paragraph("N° " + facture.getNumeroFacture(), HEADER_FONT));
        document.add(new Paragraph("Date : " + facture.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Statut : En attente de validation client", NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Détails réservation
        ajouterDetailsReservation(document, reservation);

        // Tableau des produits
        ajouterTableauProduits(document, reservation);

        // Totaux
        ajouterTotaux(document, reservation);

        // Conditions
        ajouterConditions(document, facture, "Ce devis est valable 30 jours et ne constitue pas une réservation ferme.");
    }

    /**
     * Contenu pour facture PRO-FORMA
     */
    private void ajouterContenuProForma(Document document, Facture facture) throws DocumentException, IOException {
        Reservation reservation = facture.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        // Titre
        Paragraph titre = new Paragraph("FACTURE PRO-FORMA", TITLE_FONT);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(20);
        document.add(titre);

        // Logo + Infos entreprise ET client
        ajouterEnteteFacture(document, client);

        // Numéro et date
        document.add(new Paragraph("N° " + facture.getNumeroFacture(), HEADER_FONT));
        document.add(new Paragraph("Date : " + facture.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Réservation : " + reservation.getReferenceReservation(), NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Détails réservation
        ajouterDetailsReservation(document, reservation);

        // Tableau des produits
        ajouterTableauProduits(document, reservation);

        // Totaux CORRIGES
        ajouterTotaux(document, reservation);

        // Conditions
        ajouterConditions(document, facture, "Cette facture pro-forma peut être modifiée avant la livraison.");
    }

    /**
     * Contenu pour facture FINALE
     */
    private void ajouterContenuFinale(Document document, Facture facture) throws DocumentException, IOException {
        Reservation reservation = facture.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        // Titre
        Paragraph titre = new Paragraph("FACTURE", TITLE_FONT);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(20);
        document.add(titre);

        // Logo + Infos entreprise ET client
        ajouterEnteteFacture(document, client);

        // Numéro et date
        document.add(new Paragraph("N° " + facture.getNumeroFacture(), HEADER_FONT));
        document.add(new Paragraph("Date : " + facture.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Date d'échéance : " + facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Réservation : " + reservation.getReferenceReservation(), NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Détails réservation
        ajouterDetailsReservation(document, reservation);

        // Tableau des produits
        ajouterTableauProduits(document, reservation);

        // Totaux CORRIGES
        ajouterTotaux(document, reservation);

        // Informations paiement
        ajouterInfosPaiement(document, reservation);

        // Conditions
        ajouterConditions(document, facture, "Facture définitive. Paiement à régler avant la date d'échéance.");
    }

    /**
     * 🆕 CORRECTION : Ajouter l'en-tête avec logo à gauche et infos client à droite
     */
    private void ajouterEnteteFacture(Document document, Utilisateur client) throws DocumentException, IOException {
        // Table à 2 colonnes : gauche (entreprise + logo), droite (client)
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});

        // ===== COLONNE GAUCHE : LOGO + INFO ENTREPRISE =====
        PdfPCell cellGauche = new PdfPCell();
        cellGauche.setBorder(Rectangle.NO_BORDER);
        cellGauche.setPaddingRight(10);

        // Essayer d'ajouter le logo
        try {
            Image logo = Image.getInstance(LOGO_PATH);
            logo.scaleToFit(80, 80);
            cellGauche.addElement(logo);
        } catch (Exception e) {
            log.warn("⚠️ Logo introuvable : {}", LOGO_PATH);
            // Si le logo n'existe pas, afficher juste le nom
        }

        cellGauche.addElement(new Paragraph("ELEGANT HIVE", HEADER_FONT));
        cellGauche.addElement(new Paragraph("Agence événementiel", NORMAL_FONT));
        cellGauche.addElement(new Paragraph("Adresse : Mahdia, Tunisie", SMALL_FONT));
        cellGauche.addElement(new Paragraph("Tél : +216 98 661 402", SMALL_FONT));
        cellGauche.addElement(new Paragraph("Email : contact@eleganthive.tn", SMALL_FONT));

        // ===== COLONNE DROITE : INFO CLIENT =====
        PdfPCell cellDroite = new PdfPCell();
        cellDroite.setBorder(Rectangle.NO_BORDER);
        cellDroite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellDroite.setPaddingLeft(10);

        Paragraph clientTitle = new Paragraph("CLIENT", HEADER_FONT);
        clientTitle.setAlignment(Element.ALIGN_RIGHT);
        cellDroite.addElement(clientTitle);

        Paragraph clientNom = new Paragraph(client.getNom() + " " + client.getPrenom(), NORMAL_FONT);
        clientNom.setAlignment(Element.ALIGN_RIGHT);
        cellDroite.addElement(clientNom);

        Paragraph clientEmail = new Paragraph("Email : " + client.getEmail(), SMALL_FONT);
        clientEmail.setAlignment(Element.ALIGN_RIGHT);
        cellDroite.addElement(clientEmail);

        Paragraph clientTel = new Paragraph("Tél : " + (client.getTelephone() != null ? client.getTelephone() : "N/A"), SMALL_FONT);
        clientTel.setAlignment(Element.ALIGN_RIGHT);
        cellDroite.addElement(clientTel);

        table.addCell(cellGauche);
        table.addCell(cellDroite);

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    /**
     * Ajoute les détails de la réservation
     */
    private void ajouterDetailsReservation(Document document, Reservation reservation) throws DocumentException {
        document.add(new Paragraph("DÉTAILS DE LA RÉSERVATION", HEADER_FONT));
        document.add(new Paragraph("Référence : " + reservation.getReferenceReservation(), NORMAL_FONT));
        document.add(new Paragraph("Période : du " +
                reservation.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " au " +
                reservation.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                NORMAL_FONT));
        document.add(Chunk.NEWLINE);
    }

    /**
     * 🔧 CORRECTION : Ajoute le tableau des produits avec 5 colonnes
     */
    private void ajouterTableauProduits(Document document, Reservation reservation) throws DocumentException {
        PdfPTable table = new PdfPTable(5); // 5 colonnes au lieu de 4
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 12, 15, 18, 20}); // Ajuster les largeurs

        // En-têtes
        ajouterCelluleEntete(table, "Produit");
        ajouterCelluleEntete(table, "Quantité");
        ajouterCelluleEntete(table, "Prix Unit.");
        ajouterCelluleEntete(table, "Période");
        ajouterCelluleEntete(table, "Sous-total");

        // Lignes de produits
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            ajouterCellule(table, ligne.getProduit().getNomProduit());
            ajouterCellule(table, String.valueOf(ligne.getQuantite()));
            ajouterCellule(table, String.format("%.2f DT", ligne.getPrixUnitaire()));

            // Calculer nombre jours
            long nbrJours = calculerNombreJours(ligne.getDateDebut(), ligne.getDateFin());
            String periode = ligne.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM")) +
                    " → " +
                    ligne.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM")) +
                    " (" + nbrJours + "j)";
            ajouterCellule(table, periode);

            // Sous-total = Prix unitaire × Quantité × Nombre de jours
            double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire() * nbrJours;
            ajouterCellule(table, String.format("%.2f DT", sousTotal));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    /**
     *  Ajouter les totaux
     *
     * Logique :
     * 1. Calculer le montant total SANS remise (somme des lignes)
     * 2. Calculer le HT depuis ce total
     * 3. Afficher la remise (montant OU pourcentage)
     * 4. Calculer le total APRÈS remise
     * 5. Ajouter la TVA
     */
    private void ajouterTotaux(Document document, Reservation reservation) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{60, 40});

        // 1️⃣ Calculer le montant total AVANT remise (somme brute des lignes)
        double montantTotalSansRemise = 0.0;
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            long nbrJours = calculerNombreJours(ligne.getDateDebut(), ligne.getDateFin());
            montantTotalSansRemise += ligne.getQuantite() * ligne.getPrixUnitaire() * nbrJours;
        }

        // 2️⃣ Calculer le HT depuis le total TTC AVANT remise
        double montantHT_SansRemise = montantTotalSansRemise / (1 + TVA_TAUX);

        // 3- Afficher montantHT_SansRemise
        ajouterLigneTotaux(table, "Sous-total HT :", String.format("%.2f DT", montantHT_SansRemise));

        // 4- Calculer et afficher montantTVA (TVA sur le montant sans remise)
        double montantTVA = montantHT_SansRemise * TVA_TAUX;
        ajouterLigneTotaux(table, String.format("TVA (%.0f%%) :", TVA_TAUX * 100), String.format("%.2f DT", montantTVA));

        // 5- Afficher montantTotalSansRemise
        ajouterLigneTotaux(table, "Total TTC (sans remise) :", String.format("%.2f DT", montantTotalSansRemise));

        // 6- Calculer et afficher montantRemise
        double montantRemise = 0.0;

        if (reservation.getRemiseMontant() != null && reservation.getRemiseMontant() > 0) {
            // Remise en MONTANT
            montantRemise = reservation.getRemiseMontant();
            ajouterLigneTotaux(table, "Remise :", String.format("-%.2f DT", montantRemise));
        }
        else if (reservation.getRemisePourcentage() != null && reservation.getRemisePourcentage() > 0) {
            // Remise en POURCENTAGE
            montantRemise = montantTotalSansRemise * (reservation.getRemisePourcentage() / 100.0);
            ajouterLigneTotaux(table,
                    String.format("Remise (%.0f%%) :", reservation.getRemisePourcentage()),
                    String.format("-%.2f DT", montantRemise));
        }

        // 7- Calculer et afficher montantTotalApresRemise
        double montantTotalApresRemise = montantTotalSansRemise - montantRemise;

        PdfPCell cell1 = new PdfPCell(new Phrase("TOTAL TTC :", HEADER_FONT));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(String.format("%.2f DT", montantTotalApresRemise), HEADER_FONT));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell2);

        document.add(table);
        document.add(Chunk.NEWLINE);
    }
    /**
     * Ajoute les informations de paiement
     */
    private void ajouterInfosPaiement(Document document, Reservation reservation) throws DocumentException {
        document.add(new Paragraph("INFORMATIONS DE PAIEMENT", HEADER_FONT));
        document.add(new Paragraph("Montant payé : " +
                String.format("%.2f DT", reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0),
                NORMAL_FONT));

        Double restant = reservation.getMontantTotal() -
                (reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0);
        document.add(new Paragraph("Reste à payer : " + String.format("%.2f DT", restant), NORMAL_FONT));
        document.add(Chunk.NEWLINE);
    }

    /**
     * Ajoute les conditions
     */
    private void ajouterConditions(Document document, Facture facture, String messageSpecifique) throws DocumentException {
        document.add(new Paragraph("CONDITIONS", HEADER_FONT));
        document.add(new Paragraph(messageSpecifique, SMALL_FONT));

        if (facture.getConditionsPaiement() != null) {
            document.add(new Paragraph(facture.getConditionsPaiement(), SMALL_FONT));
        }

        if (facture.getNotes() != null) {
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("NOTES", HEADER_FONT));
            document.add(new Paragraph(facture.getNotes(), SMALL_FONT));
        }
    }

    // ===== Méthodes utilitaires =====

    private void ajouterCelluleEntete(PdfPTable table, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, HEADER_FONT));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void ajouterCellule(PdfPTable table, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, NORMAL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void ajouterLigneTotaux(PdfPTable table, String label, String montant) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, NORMAL_FONT));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(montant, NORMAL_FONT));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell2);
    }

    private long calculerNombreJours(LocalDate dateDebut, LocalDate dateFin) {
        return ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;  // +1 pour inclure le dernier jour
    }
}