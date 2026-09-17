package com.rahul.taskmanager.service;

import com.rahul.taskmanager.dto.RegisterRequest;
import com.rahul.taskmanager.dto.RegisterResponse;
import com.rahul.taskmanager.entity.Role;
import com.rahul.taskmanager.entity.User;
import com.rahul.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .role(saved.getRole().name())
                .build();
    }
}