package tn.weeding.agenceevenementielle.dto.chatbot;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    Long id;
    String title ;
    LocalDateTime createdAt ;
    LocalDateTime updatedAt ;
}
