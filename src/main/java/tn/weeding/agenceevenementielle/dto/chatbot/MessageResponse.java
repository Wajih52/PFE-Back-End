package tn.weeding.agenceevenementielle.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.weeding.agenceevenementielle.entities.enums.MessageSender;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    Long id;
    MessageSender sender;
    String content;
    String modelUsed;
    LocalDateTime createdAt ;
}
