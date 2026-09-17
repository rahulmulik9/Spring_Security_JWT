package com.rahul.taskmanager.service;

import com.rahul.taskmanager.dto.LoginRequest;
import com.rahul.taskmanager.dto.LoginResponse;
import com.rahul.taskmanager.dto.RegisterRequest;
import com.rahul.taskmanager.dto.RegisterResponse;
import com.rahul.taskmanager.entity.Role;
import com.rahul.taskmanager.entity.User;
import com.rahul.taskmanager.repository.UserRepository;
import com.rahul.taskmanager.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

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

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}