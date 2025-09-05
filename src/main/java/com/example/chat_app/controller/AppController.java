package com.example.chat_app.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppController {

    @GetMapping("/chat")
    public String chatPage() {
        return "forward:/index.html";
    }
    
    @GetMapping("/api/user/me")
    @ResponseBody
    public String getCurrentUser(Principal principal) {
        return principal != null ? principal.getName() : "";
    }
}