package tn.weeding.agenceevenementielle.controller;



import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.chatbot.ChatbotRequest;
import tn.weeding.agenceevenementielle.dto.chatbot.ChatbotResponse;
import tn.weeding.agenceevenementielle.dto.chatbot.ConversationResponse;
import tn.weeding.agenceevenementielle.dto.chatbot.MessageResponse;
import tn.weeding.agenceevenementielle.services.chatbot.ChatbotService;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ChatbotResponse ask(
            @Valid @RequestBody ChatbotRequest request,
            Authentication authentication
    ) {
        return chatbotService.ask(request, authentication);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> getMyConversations(Authentication authentication) {
        return chatbotService.getMyConversations(authentication);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getConversationMessages(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return chatbotService.getConversationMessages(conversationId, authentication);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public void deleteConversation(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        chatbotService.deleteConversation(conversationId, authentication);
    }
}