package com.example.chat_app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity with our API/WebSocket app
            .authorizeHttpRequests(auth -> auth
                // Allow access to static resources, registration, and the WebSocket endpoint
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/ws/**").permitAll()
                .requestMatchers("/api/register").permitAll()
                // All other requests must be authenticated
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Use a custom login page (which we will build in index.html)
                .loginPage("/").permitAll()
                .loginProcessingUrl("/login") // Spring Security will handle POSTs to this URL
                .defaultSuccessUrl("/chat", true) // Redirect to a virtual /chat URL on success
                .failureUrl("/?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}