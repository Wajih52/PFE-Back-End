package tn.weeding.agenceevenementielle.entities.chatbot;

import jakarta.persistence.*;
import lombok.*;
import tn.weeding.agenceevenementielle.entities.enums.MessageSender;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSender sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String modelUsed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idConversation", nullable = false)
    private ChatConversation conversation;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
