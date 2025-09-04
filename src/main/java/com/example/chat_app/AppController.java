package com.example.chat_app;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppController {

    // This method tells Spring to serve our main HTML file when a user goes to /chat
    @GetMapping("/chat")
    public String chatPage() {
        return "index"; // This tells Spring to find and return index.html
    }
    
    // This new endpoint will be called by our JavaScript to get the logged-in user's name
    @GetMapping("/api/user/me")
    @ResponseBody
    public String getCurrentUser(Principal principal) {
        // Principal is automatically injected by Spring Security
        return principal != null ? principal.getName() : "";
    }
}