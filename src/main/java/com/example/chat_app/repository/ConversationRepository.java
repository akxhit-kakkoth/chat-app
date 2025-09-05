package com.example.chat_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat_app.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}