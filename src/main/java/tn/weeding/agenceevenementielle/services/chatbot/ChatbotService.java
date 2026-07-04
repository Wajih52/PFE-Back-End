package tn.weeding.agenceevenementielle.services.chatbot;



import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.weeding.agenceevenementielle.dto.chatbot.ChatbotRequest;
import tn.weeding.agenceevenementielle.dto.chatbot.ChatbotResponse;
import tn.weeding.agenceevenementielle.dto.chatbot.ConversationResponse;
import tn.weeding.agenceevenementielle.dto.chatbot.MessageResponse;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.chatbot.ChatConversation;
import tn.weeding.agenceevenementielle.entities.chatbot.ChatMessage;
import tn.weeding.agenceevenementielle.entities.enums.MessageSender;
import tn.weeding.agenceevenementielle.repository.ChatConversationRepository;
import tn.weeding.agenceevenementielle.repository.ChatMessageRepository;
import tn.weeding.agenceevenementielle.repository.UtilisateurRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final OpenRouterChatbotService openRouterChatbotService;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public ChatbotResponse ask(ChatbotRequest request, Authentication authentication) {

        Utilisateur utilisateur = getConnectedUser(authentication);

        ChatConversation conversation = getOrCreateConversation(request, utilisateur);

        saveUserMessage(conversation, request.getMessage());

        List<ChatMessage> history = getLastMessagesForAi(conversation);

        List<Map<String, String>> messages = buildMessagesForOpenRouter(history);

        String aiAnswer = openRouterChatbotService.ask(messages);

        if (aiAnswer.startsWith("Erreur OpenRouter")
                || aiAnswer.startsWith("Impossible de générer")
                || aiAnswer.startsWith("Réponse OpenRouter")) {
            return new ChatbotResponse(conversation.getIdConversation(), aiAnswer);
        }

        saveAssistantMessage(conversation, aiAnswer);

        conversation.touch();
        conversationRepository.save(conversation);

        return new ChatbotResponse(conversation.getIdConversation(), aiAnswer);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(Authentication authentication) {

        Utilisateur utilisateur = getConnectedUser(authentication);

        return conversationRepository.findByUtilisateurAndActiveTrueOrderByUpdatedAtDesc(utilisateur)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getConversationMessages(Long conversationId, Authentication authentication) {

        Utilisateur utilisateur = getConnectedUser(authentication);

        ChatConversation conversation = conversationRepository.findByIdConversationAndUtilisateurAndActiveTrue(conversationId, utilisateur)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable."));

        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public void deleteConversation(Long conversationId, Authentication authentication) {

        Utilisateur utilisateur = getConnectedUser(authentication);

        ChatConversation conversation = conversationRepository.findByIdConversationAndUtilisateurAndActiveTrue(conversationId, utilisateur)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable."));

        conversation.setActive(false);
        conversation.touch();

        conversationRepository.save(conversation);
    }

    private ChatConversation getOrCreateConversation(ChatbotRequest request, Utilisateur utilisateur) {

        if (request.getConversationId() != null) {
            return conversationRepository.findByIdConversationAndUtilisateurAndActiveTrue(request.getConversationId(), utilisateur)
                    .orElseThrow(() -> new RuntimeException("Conversation introuvable."));
        }

        ChatConversation conversation = ChatConversation.builder()
                .title(generateTitle(request.getMessage()))
                .utilisateur(utilisateur)
                .active(true)
                .build();

        return conversationRepository.save(conversation);
    }

    private void saveUserMessage(ChatConversation conversation, String content) {

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(MessageSender.USER)
                .content(content)
                .modelUsed(null)
                .build();

        messageRepository.save(message);
    }

    private void saveAssistantMessage(ChatConversation conversation, String content) {

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(MessageSender.ASSISTANT)
                .content(content)
                .modelUsed(openRouterChatbotService.getModel())
                .build();

        messageRepository.save(message);
    }

    private List<ChatMessage> getLastMessagesForAi(ChatConversation conversation) {

        List<ChatMessage> lastMessages =
                messageRepository.findTop12ByConversationOrderByCreatedAtDesc(conversation);

        Collections.reverse(lastMessages);

        return lastMessages;
    }

    private String generateTitle(String message) {

        String cleanedMessage = message.trim();

        if (cleanedMessage.length() <= 50) {
            return cleanedMessage;
        }

        return cleanedMessage.substring(0, 50) + "...";
    }

    private ConversationResponse toConversationResponse(ChatConversation conversation) {
        return new ConversationResponse(
                conversation.getIdConversation(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getSender(),
                message.getContent(),
                message.getModelUsed(),
                message.getCreatedAt()
        );
    }

    private Utilisateur getConnectedUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié.");
        }

        String login = authentication.getName();

        return utilisateurRepository.findByPseudoOrEmail(login,login)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));
    }


    private List<Map<String, String>> buildMessagesForOpenRouter(List<ChatMessage> history) {

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", openRouterChatbotService.buildSystemPrompt()
        ));

        for (ChatMessage message : history) {
            messages.add(Map.of(
                    "role", message.getSender() == MessageSender.USER ? "user" : "assistant",
                    "content", message.getContent()
            ));
        }

        return messages;
    }
}