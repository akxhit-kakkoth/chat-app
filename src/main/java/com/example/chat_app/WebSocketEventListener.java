package com.example.chat_app;

import java.time.Instant;
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

    // A map to store active users per channel. Key: channel, Value: Set of usernames
    private static final Map<String, Set<String>> channelUserMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        logger.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String channel = (String) headerAccessor.getSessionAttributes().get("channel");

        if (username != null && channel != null) {
            logger.info("User Disconnected from channel " + channel + " : " + username);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);
            chatMessage.setChannel(channel);
            chatMessage.setTimestamp(Instant.now());

            // Remove user from the channel's user set
            Set<String> usersInChannel = channelUserMap.getOrDefault(channel, new HashSet<>());
            usersInChannel.remove(username);
            
            // If the channel is empty, we can remove it from the map
            if (usersInChannel.isEmpty()) {
                channelUserMap.remove(channel);
            }

            broadcastUserList(channel);

            messagingTemplate.convertAndSend("/topic/public/" + channel, chatMessage);
        }
    }

    public void broadcastUserList(String channel) {
        Set<String> users = channelUserMap.getOrDefault(channel, new HashSet<>());
        messagingTemplate.convertAndSend("/topic/users/" + channel, users);
    }

    public void userJoined(String username, String channel) {
        // Add user to the channel's user set
        Set<String> usersInChannel = channelUserMap.computeIfAbsent(channel, k -> new HashSet<>());
        usersInChannel.add(username);
        
        broadcastUserList(channel);
    }
}