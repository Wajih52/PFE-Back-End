package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.chatbot.ChatConversation;
import tn.weeding.agenceevenementielle.entities.chatbot.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationOrderByCreatedAtAsc(ChatConversation conversation);

    List<ChatMessage> findTop12ByConversationOrderByCreatedAtDesc(ChatConversation conversation);
}