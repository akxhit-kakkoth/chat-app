package com.example.chat_app;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    // Map to store active users per conversation. Key: conversationId, Value: Set of usernames
    private static final Map<Long, Set<String>> conversationUserMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        Long conversationId = (Long) headerAccessor.getSessionAttributes().get("conversationId");

        if (username != null && conversationId != null) {
            logger.info("User Disconnected from conversation " + conversationId + " : " + username);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);
            
            Set<String> usersInConv = conversationUserMap.getOrDefault(conversationId, new HashSet<>());
            usersInConv.remove(username);
            
            if (usersInConv.isEmpty()) {
                conversationUserMap.remove(conversationId);
            }

            broadcastUserList(conversationId);
            messagingTemplate.convertAndSend(String.format("/topic/conversation/%d", conversationId), chatMessage);
        }
    }

    public void broadcastUserList(Long conversationId) {
        Set<String> users = conversationUserMap.getOrDefault(conversationId, new HashSet<>());
        messagingTemplate.convertAndSend(String.format("/topic/users/%d", conversationId), users);
    }

    public void userJoined(String username, Long conversationId) {
        Set<String> usersInConv = conversationUserMap.computeIfAbsent(conversationId, k -> new HashSet<>());
        usersInConv.add(username);
        broadcastUserList(conversationId);
    }
}