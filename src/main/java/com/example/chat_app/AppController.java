package com.example.chat_app;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppController {

    // This is the critical mapping that was missing.
    // It tells Spring: "When a user goes to /chat, serve them the index.html file."
    @GetMapping("/chat")
    public String chatPage() {
        return "index"; 
    }
    
    // This endpoint lets the frontend know who is logged in.
    @GetMapping("/api/user/me")
    @ResponseBody
    public String getCurrentUser(Principal principal) {
        // Principal is automatically injected by Spring Security
        return principal != null ? principal.getName() : "";
    }
}