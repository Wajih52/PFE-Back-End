package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.chatbot.ChatConversation;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findByUtilisateurAndActiveTrueOrderByUpdatedAtDesc(Utilisateur utilisateur);
    Optional<ChatConversation> findByIdConversationAndUtilisateurAndActiveTrue(Long id, Utilisateur utilisateur);
}
