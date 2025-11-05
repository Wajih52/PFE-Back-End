package tn.weeding.agenceevenementielle.dto.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour modifier les dates d'une réservation existante
 *
 * Utilisé par le client ou l'admin pour changer la période d'une réservation
 * Déclenche une nouvelle vérification de disponibilité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierDatesReservationDto {

    @NotNull(message = "L'ID de la réservation est obligatoire")
    private Long idReservation;

    @NotNull(message = "La nouvelle date de début est obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    private LocalDate nouvelleDateDebut;

    @NotNull(message = "La nouvelle date de fin est obligatoire")
    @Future(message = "La date de fin doit être dans le futur")
    private LocalDate nouvelleDateFin;

    /**
     * Raison de la modification
     */
    private String motifModification;
}