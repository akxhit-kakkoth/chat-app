package com.example.chat_app;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private WebSocketEventListener eventListener;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate; // For sending private messages

    @MessageMapping("/chat.sendMessage/{channel}")
    @SendTo("/topic/public/{channel}")
    public ChatMessage sendMessage(@DestinationVariable String channel, @Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(Instant.now());
        chatMessage.setChannel(channel);
        chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser/{channel}")
    @SendTo("/topic/public/{channel}")
    public ChatMessage addUser(@DestinationVariable String channel, @Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("channel", channel);
        
        eventListener.userJoined(username, channel);

        chatMessage.setTimestamp(Instant.now());
        chatMessage.setChannel(channel);
        return chatMessage;
    }
    
    @MessageMapping("/chat.typing/{channel}")
    @SendTo("/topic/public/{channel}")
    public ChatMessage handleTyping(@DestinationVariable String channel, @Payload ChatMessage chatMessage) {
        chatMessage.setType(ChatMessage.MessageType.TYPING);
        chatMessage.setChannel(channel);
        return chatMessage;
    }

    // --- NEW METHOD FOR PRIVATE MESSAGES ---
    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(Instant.now());
        chatMessage.setType(ChatMessage.MessageType.PRIVATE_MESSAGE);
        chatMessageRepository.save(chatMessage);
        
        // Send message to the recipient's private queue
        messagingTemplate.convertAndSendToUser(
            chatMessage.getRecipient(), "/queue/private", chatMessage);
    }

    @GetMapping("/messages")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@RequestParam("channel") String channel) {
        return chatMessageRepository.findByChannelOrderByTimestampAsc(channel);
    }
}