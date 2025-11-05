package tn.weeding.agenceevenementielle.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;



/**
 * DTO pour valider une période de dates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatePeriodeDto {

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;
}