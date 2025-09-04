package com.example.chat_app;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // Usernames must be unique
    private String username;

    @JsonIgnore // Never send the password to the frontend
    private String password;

    // This tells the database that the "participants" field in the Conversation entity manages this relationship.
    @ManyToMany(mappedBy = "participants", fetch = FetchType.EAGER)
    @JsonIgnore // Avoid infinite loops when sending data
    private Set<Conversation> conversations = new HashSet<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Set<Conversation> getConversations() { return conversations; }
    public void setConversations(Set<Conversation> conversations) { this.conversations = conversations; }
}