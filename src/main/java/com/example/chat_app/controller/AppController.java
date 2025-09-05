package com.example.chat_app.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppController {

    // You can remove the /ping endpoint now, we don't need it anymore.
    
    // THIS IS THE CORRECTED MAPPING
    @GetMapping("/chat")
    public String chatPage() {
        return "forward:/index.html"; // The fix is to forward to the static file
    }
    
    @GetMapping("/api/user/me")
    @ResponseBody
    public String getCurrentUser(Principal principal) {
        return principal != null ? principal.getName() : "";
    }
}