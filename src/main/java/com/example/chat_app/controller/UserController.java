package com.example.chat_app.controller;

import com.example.chat_app.dto.UserDTO;
import com.example.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<UserDTO> users = userRepository.findByPhoneNumberContaining(query)
            .stream()
            .map(user -> new UserDTO(user.getUsername(), user.getPhoneNumber()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}