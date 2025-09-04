package com.example.chat_app;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // This is the new, correct method to find messages for a specific conversation
    List<ChatMessage> findByConversation_IdOrderByTimestampAsc(Long conversationId);
}