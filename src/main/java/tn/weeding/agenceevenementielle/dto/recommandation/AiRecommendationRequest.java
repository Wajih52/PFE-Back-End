package tn.weeding.agenceevenementielle.dto.recommandation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AiRecommendationRequest {



        @NotBlank(message = "Le type d'événement est obligatoire")
        private String eventType;

        @Min(value = 1, message = "Le nombre d'invités doit être supérieur à 0")
        private int guestCount;

        @NotBlank(message = "Le budget est obligatoire")
        private String budget;

        @NotBlank(message = "Le lieu est obligatoire")
        private String locationType;

        @NotNull(message = "La date de début est obligatoire")
        private LocalDate startDate;

        @NotNull(message = "La date de fin est obligatoire")
        private LocalDate endDate;

        private String notes;
}
