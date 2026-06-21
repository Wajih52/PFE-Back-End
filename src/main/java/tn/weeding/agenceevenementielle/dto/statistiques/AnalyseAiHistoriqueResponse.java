package tn.weeding.agenceevenementielle.dto.statistiques;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseAiHistoriqueResponse {
    Long id ;
    String summary ;
    LocalDateTime createdAt ;
    String generatedBy;

}
