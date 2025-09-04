package com.example.chat_app;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Fetches all messages, ordered by timestamp (for backwards compatibility if needed)
    List<ChatMessage> findAllByOrderByTimestampAsc();

    // New method to find messages for a specific channel
    List<ChatMessage> findByChannelOrderByTimestampAsc(String channel);
}