package com.example.chat_app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;
    
    // An endpoint for the frontend to get all conversations for the logged-in user
    @GetMapping
    public ResponseEntity<Set<Conversation>> getUserConversations(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).get();
        return ResponseEntity.ok(user.getConversations());
    }

    // An endpoint to create a new group chat
    @PostMapping("/group")
    public ResponseEntity<Conversation> createGroupConversation(@AuthenticationPrincipal UserDetails userDetails, @RequestBody CreateGroupRequest request) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).get();
        
        Conversation newGroup = new Conversation();
        newGroup.setName(request.getGroupName());
        newGroup.setType(Conversation.ConversationType.GROUP);
        newGroup.getParticipants().add(currentUser);

        // Find and add other participants
        for (String username : request.getParticipantUsernames()) {
            userRepository.findByUsername(username).ifPresent(user -> newGroup.getParticipants().add(user));
        }
        
        Conversation savedGroup = conversationRepository.save(newGroup);
        return ResponseEntity.ok(savedGroup);
    }
    
    // An endpoint to create a new one-on-one personal chat
    @PostMapping("/personal")
    public ResponseEntity<Conversation> createPersonalConversation(@AuthenticationPrincipal UserDetails userDetails, @RequestBody String otherUsername) {
        User user1 = userRepository.findByUsername(userDetails.getUsername()).get();
        Optional<User> user2Opt = userRepository.findByUsername(otherUsername);

        if (!user2Opt.isPresent()) {
            return ResponseEntity.badRequest().body(null); // User not found
        }
        User user2 = user2Opt.get();

        // Check if a personal chat already exists between these two users
        for (Conversation conv : user1.getConversations()) {
            if (conv.getType() == Conversation.ConversationType.PERSONAL && conv.getParticipants().contains(user2)) {
                return ResponseEntity.ok(conv); // Return existing conversation
            }
        }

        // If not, create a new one
        Conversation newPersonalChat = new Conversation();
        newPersonalChat.setType(Conversation.ConversationType.PERSONAL);
        newPersonalChat.getParticipants().add(user1);
        newPersonalChat.getParticipants().add(user2);
        
        Conversation savedChat = conversationRepository.save(newPersonalChat);
        return ResponseEntity.ok(savedChat);
    }
}

// Helper classes for request bodies
class CreateGroupRequest {
    private String groupName;
    private Set<String> participantUsernames;
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Set<String> getParticipantUsernames() { return participantUsernames; }
    public void setParticipantUsernames(Set<String> participantUsernames) { this.participantUsernames = participantUsernames; }
}