package com.example.chat_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat_app.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByConversation_IdOrderByTimestampAsc(Long conversationId);
}