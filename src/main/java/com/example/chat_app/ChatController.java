package com.example.chat_app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private WebSocketEventListener eventListener;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage/{conversationId}")
    public void sendMessage(@DestinationVariable Long conversationId, @Payload ChatMessage chatMessage) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        chatMessage.setConversation(conversation);
        chatMessage.setTimestamp(Instant.now());
        chatMessageRepository.save(chatMessage);
        
        // Send message to the conversation topic
        messagingTemplate.convertAndSend(String.format("/topic/conversation/%d", conversationId), chatMessage);
    }

    @MessageMapping("/chat.addUser/{conversationId}")
    public void addUser(@DestinationVariable Long conversationId, @Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("conversationId", conversationId);
        
        eventListener.userJoined(username, conversationId);

        chatMessage.setType(ChatMessage.MessageType.JOIN);
        messagingTemplate.convertAndSend(String.format("/topic/conversation/%d", conversationId), chatMessage);
    }
    
    @MessageMapping("/chat.typing/{conversationId}")
    public void handleTyping(@DestinationVariable Long conversationId, @Payload ChatMessage chatMessage) {
        chatMessage.setType(ChatMessage.MessageType.TYPING);
        messagingTemplate.convertAndSend(String.format("/topic/conversation/%d", conversationId), chatMessage);
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable Long conversationId) {
        return chatMessageRepository.findByConversation_IdOrderByTimestampAsc(conversationId);
    }
}