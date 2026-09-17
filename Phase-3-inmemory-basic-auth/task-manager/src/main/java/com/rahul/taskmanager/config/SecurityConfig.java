package com.rahul.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var alice = User.builder()
                .username("Ajay")
                .password(passwordEncoder.encode("password123"))
                .roles("USER")
                .build();

        var bob = User.builder()
                .username("Amit")
                .password(passwordEncoder.encode("password123"))
                .roles("USER")
                .build();

        var admin = User.builder()
                .username("Rahul")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(alice, bob, admin);
    }
}