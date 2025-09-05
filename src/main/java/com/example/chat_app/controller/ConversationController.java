package com.example.chat_app.controller;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.chat_app.model.User;
import com.example.chat_app.repository.ConversationRepository;
import com.example.chatapp.repository.UserRepository;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    // NEW: Inject the template for sending WebSocket messages
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @GetMapping
    public ResponseEntity<Set<Conversation>> getUserConversations(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).get();
        return ResponseEntity.ok(user.getConversations());
    }

    @PostMapping("/group")
    public ResponseEntity<Conversation> createGroupConversation(@AuthenticationPrincipal UserDetails userDetails, @RequestBody CreateGroupRequest request) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).get();
        
        Conversation newGroup = new Conversation();
        newGroup.setName(request.getGroupName());
        newGroup.setType(Conversation.ConversationType.GROUP);
        newGroup.getParticipants().add(currentUser);

        for (String username : request.getParticipantUsernames()) {
            userRepository.findByUsername(username).ifPresent(user -> newGroup.getParticipants().add(user));
        }
        
        Conversation savedGroup = conversationRepository.save(newGroup);

        // NEW: Notify all participants that a new group has been created
        savedGroup.getParticipants().forEach(participant -> {
            messagingTemplate.convertAndSendToUser(participant.getUsername(), "/queue/new-conversation", savedGroup);
        });

        return ResponseEntity.ok(savedGroup);
    }
    
    // UPDATED: This now accepts a phone number and pushes updates
    @PostMapping("/personal")
    public ResponseEntity<Conversation> createPersonalConversation(@AuthenticationPrincipal UserDetails userDetails, @RequestBody String otherUserPhoneNumber) {
        User user1 = userRepository.findByUsername(userDetails.getUsername()).get();
        // UPDATED: Find user by phone number instead of username
        Optional<User> user2Opt = userRepository.findByPhoneNumber(otherUserPhoneNumber);

        if (!user2Opt.isPresent()) {
            return ResponseEntity.badRequest().body(null);
        }
        User user2 = user2Opt.get();

        for (Conversation conv : user1.getConversations()) {
            if (conv.getType() == Conversation.ConversationType.PERSONAL && conv.getParticipants().contains(user2)) {
                return ResponseEntity.ok(conv);
            }
        }

        Conversation newPersonalChat = new Conversation();
        newPersonalChat.setType(Conversation.ConversationType.PERSONAL);
        newPersonalChat.getParticipants().add(user1);
        newPersonalChat.getParticipants().add(user2);
        
        Conversation savedChat = conversationRepository.save(newPersonalChat);

        // NEW: Notify both users that a new personal chat has been created
        messagingTemplate.convertAndSendToUser(user1.getUsername(), "/queue/new-conversation", savedChat);
        messagingTemplate.convertAndSendToUser(user2.getUsername(), "/queue/new-conversation", savedChat);

        return ResponseEntity.ok(savedChat);
    }
}

// (Helper class CreateGroupRequest remains the same)
class CreateGroupRequest {
    private String groupName;
    private Set<String> participantUsernames;
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Set<String> getParticipantUsernames() { return participantUsernames; }
    public void setParticipantUsernames(Set<String> participantUsernames) { this.participantUsernames = participantUsernames; }
}