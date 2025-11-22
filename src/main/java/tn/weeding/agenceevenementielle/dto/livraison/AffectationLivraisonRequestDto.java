package tn.weeding.agenceevenementielle.dto.livraison;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO pour affecter un employé à une livraison
 * Sprint 6 - Gestion des livraisons
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AffectationLivraisonRequestDto {

    @NotNull(message = "L'ID de la livraison est obligatoire")
    private Long idLivraison;

    @NotNull(message = "L'ID de l'employé est obligatoire")
    private Long idEmploye;

    @NotNull(message = "La date d'affectation est obligatoire")
    private LocalDate dateAffectation;

    @NotNull(message = "L'heure d'affectation est obligatoire")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime heureAffectation;

    /**
     * Notes ou instructions spécifiques pour cet employé
     */
    private String notes;
}