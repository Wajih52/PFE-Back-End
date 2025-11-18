package tn.weeding.agenceevenementielle.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.entities.*;
import tn.weeding.agenceevenementielle.entities.enums.TypeFacture;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorService {

    private static final String PDF_DIRECTORY = "factures/";
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY);

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

        // Créer le document
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));

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

        document.close();

        log.info("📄 PDF généré : {}", filePath);
        return filePath;
    }

    /**
     * Contenu pour facture DEVIS
     */
    private void ajouterContenuDevis(Document document, Facture facture) throws DocumentException {
        Reservation reservation = facture.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        // Titre
        Paragraph titre = new Paragraph("DEVIS", TITLE_FONT);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(20);
        document.add(titre);

        // Numéro et date
        document.add(new Paragraph("N° " + facture.getNumeroFacture(), HEADER_FONT));
        document.add(new Paragraph("Date : " + facture.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Statut : En attente de validation client", NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Informations agence et client
        ajouterInfosEntreprise(document);
        ajouterInfosClient(document, client);

        // Détails réservation
        ajouterDetailsReservation(document, reservation);

        // Tableau des produits
        ajouterTableauProduits(document, reservation);

        // Totaux
        ajouterTotaux(document, facture);

        // Conditions
        ajouterConditions(document, facture, "Ce devis est valable 30 jours et ne constitue pas une réservation ferme.");
    }

    /**
     * Contenu pour facture PRO-FORMA
     */
    private void ajouterContenuProForma(Document document, Facture facture) throws DocumentException {
        Reservation reservation = facture.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        // Titre
        Paragraph titre = new Paragraph("FACTURE PRO-FORMA", TITLE_FONT);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(20);
        document.add(titre);

        // Numéro et date
        document.add(new Paragraph("N° " + facture.getNumeroFacture(), HEADER_FONT));
        document.add(new Paragraph("Date : " + facture.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Réservation : " + reservation.getReferenceReservation(), NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Informations agence et client
        ajouterInfosEntreprise(document);
        ajouterInfosClient(document, client);

        // Détails réservation
        ajouterDetailsReservation(document, reservation);

        // Tableau des produits
        ajouterTableauProduits(document, reservation);

        // Totaux
        ajouterTotaux(document, facture);

        // Conditions
        ajouterConditions(document, facture, "Cette facture pro-forma peut être modifiée avant la livraison.");
    }

    /**
     * Contenu pour facture FINALE
     */
    private void ajouterContenuFinale(Document document, Facture facture) throws DocumentException {
        Reservation reservation = facture.getReservation();
        Utilisateur client = reservation.getUtilisateur();

        // Titre
        Paragraph titre = new Paragraph("FACTURE", TITLE_FONT);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(20);
        document.add(titre);

        // Numéro et date
        document.add(new Paragraph("N° " + facture.getNumeroFacture(), HEADER_FONT));
        document.add(new Paragraph("Date : " + facture.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Date d'échéance : " + facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        document.add(new Paragraph("Réservation : " + reservation.getReferenceReservation(), NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Informations agence et client
        ajouterInfosEntreprise(document);
        ajouterInfosClient(document, client);

        // Détails réservation
        ajouterDetailsReservation(document, reservation);

        // Tableau des produits
        ajouterTableauProduits(document, reservation);

        // Totaux
        ajouterTotaux(document, facture);

        // Informations paiement
        ajouterInfosPaiement(document, reservation);

        // Conditions
        ajouterConditions(document, facture, "Facture définitive. Paiement à régler avant la date d'échéance.");
    }

    /**
     * Ajoute les informations de l'entreprise
     */
    private void ajouterInfosEntreprise(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph("ELEGANT HIVE", HEADER_FONT));
        cell.addElement(new Paragraph("Agence d'événementiel", NORMAL_FONT));
        cell.addElement(new Paragraph("Adresse : Tunis, Tunisie", SMALL_FONT));
        cell.addElement(new Paragraph("Tél : +216 XX XXX XXX", SMALL_FONT));
        cell.addElement(new Paragraph("Email : contact@eleganthive.tn", SMALL_FONT));

        table.addCell(cell);
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    /**
     * Ajoute les informations du client
     */
    private void ajouterInfosClient(Document document, Utilisateur client) throws DocumentException {
        document.add(new Paragraph("CLIENT", HEADER_FONT));
        document.add(new Paragraph(client.getNom() + " " + client.getPrenom(), NORMAL_FONT));
        document.add(new Paragraph("Email : " + client.getEmail(), SMALL_FONT));
        document.add(new Paragraph("Tél : " + (client.getTelephone() != null ? client.getTelephone() : "N/A"), SMALL_FONT));
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
     * Ajoute le tableau des produits
     */
    private void ajouterTableauProduits(Document document, Reservation reservation) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 15, 20, 25});

        // En-têtes
        ajouterCelluleEntete(table, "Produit");
        ajouterCelluleEntete(table, "Quantité");
        ajouterCelluleEntete(table, "Prix Unitaire");
        ajouterCelluleEntete(table, "Sous-total");

        // Lignes de produits
        for (LigneReservation ligne : reservation.getLigneReservations()) {
            ajouterCellule(table, ligne.getProduit().getNomProduit());
            ajouterCellule(table, String.valueOf(ligne.getQuantite()));
            ajouterCellule(table, String.format("%.2f TND", ligne.getPrixUnitaire()));
            ajouterCellule(table, String.format("%.2f TND", ligne.getQuantite() * ligne.getPrixUnitaire()));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    /**
     * Ajoute les totaux
     */
    private void ajouterTotaux(Document document, Facture facture) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(40);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{60, 40});

        ajouterLigneTotaux(table, "Sous-total HT :", String.format("%.2f TND", facture.getMontantHT()));

        if (facture.getMontantRemise() != null && facture.getMontantRemise() > 0) {
            ajouterLigneTotaux(table, "Remise :", String.format("-%.2f TND", facture.getMontantRemise()));
        }

        ajouterLigneTotaux(table, "TVA (19%) :", String.format("%.2f TND", facture.getMontantTVA()));

        PdfPCell cell1 = new PdfPCell(new Phrase("TOTAL TTC :", HEADER_FONT));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(String.format("%.2f TND", facture.getMontantTTC()), HEADER_FONT));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
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
                String.format("%.2f TND", reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0),
                NORMAL_FONT));

        Double restant = reservation.getMontantTotal() -
                (reservation.getMontantPaye() != null ? reservation.getMontantPaye() : 0.0);
        document.add(new Paragraph("Reste à payer : " + String.format("%.2f TND", restant), NORMAL_FONT));
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
}