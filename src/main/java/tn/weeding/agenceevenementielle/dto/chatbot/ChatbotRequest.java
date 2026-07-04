package tn.weeding.agenceevenementielle.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {

    Long conversationId;

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères")
    String message ;
}
