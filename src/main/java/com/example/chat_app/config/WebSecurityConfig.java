//... inside WebSecurityConfig.java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index.html", "/api/register").permitAll()
            // ADD /api/users/** to this line
            .requestMatchers("/chat", "/api/user/me", "/ws/**", "/api/conversations/**", "/api/users/**").authenticated()
            .anyRequest().authenticated()
        )
        // ... rest of the file is the same
        .formLogin(form -> form
            .loginPage("/").permitAll()
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/chat", true)
            .failureUrl("/?error=true")
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/")
            .permitAll()
        );

    return http.build();
}