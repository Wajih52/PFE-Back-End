package tn.weeding.agenceevenementielle.controller;

import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.services.statistiques.RapportStatistiquesService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ==========================================
 * CONTROLLER RAPPORTS STATISTIQUES
 * ==========================================
 * BONUS : Téléchargement de rapports PDF et Excel
 * Accessible uniquement aux ADMIN et MANAGER
 */
@RestController
@RequestMapping("/api/statistiques/rapports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rapports Statistiques", description = "Téléchargement de rapports PDF/Excel")
@CrossOrigin(origins = "http://localhost:4200")
public class RapportStatistiquesController {

    private final RapportStatistiquesService rapportService;

    /**
     * 📄 Télécharger un rapport PDF des statistiques
     * GET /api/statistiques/rapports/pdf
     */
    @GetMapping("/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Télécharger rapport PDF",
            description = "Génère et télécharge un rapport PDF complet des statistiques"
    )
    public ResponseEntity<byte[]> telechargerRapportPDF() {
        log.info("📄 [API] Requête: téléchargement rapport PDF");

        try {
            byte[] pdfBytes = rapportService.genererRapportPDF();

            String filename = "rapport_statistiques_"
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [API] Rapport PDF généré: {} bytes", pdfBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (DocumentException e) {
            log.error("❌ [API] Erreur génération PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 📄 Télécharger un rapport PDF pour une période spécifique
     * GET /api/statistiques/rapports/pdf/periode?dateDebut=...&dateFin=...
     */
    @GetMapping("/pdf/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Télécharger rapport PDF période",
            description = "Génère un rapport PDF pour une période personnalisée"
    )
    public ResponseEntity<byte[]> telechargerRapportPDFPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        log.info("📄 [API] Requête: rapport PDF période {} - {}", dateDebut, dateFin);

        try {
            byte[] pdfBytes = rapportService.genererRapportPDFPeriode(dateDebut, dateFin);

            String filename = String.format("rapport_%s_%s.pdf",
                    dateDebut.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    dateFin.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [API] Rapport PDF période généré: {} bytes", pdfBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (DocumentException e) {
            log.error("❌ [API] Erreur génération PDF période", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 📊 Télécharger un rapport Excel des statistiques
     * GET /api/statistiques/rapports/excel
     */
    @GetMapping("/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Télécharger rapport Excel",
            description = "Génère et télécharge un rapport Excel complet des statistiques"
    )
    public ResponseEntity<byte[]> telechargerRapportExcel() {
        log.info("📊 [API] Requête: téléchargement rapport Excel");

        try {
            byte[] excelBytes = rapportService.genererRapportExcel();

            String filename = "rapport_statistiques_"
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            log.info("✅ [API] Rapport Excel généré: {} bytes", excelBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (IOException e) {
            log.error("❌ [API] Erreur génération Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}