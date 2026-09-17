package com.rahul.taskmanager.service;

import com.rahul.taskmanager.dto.*;
import com.rahul.taskmanager.entity.Role;
import com.rahul.taskmanager.entity.User;
import com.rahul.taskmanager.repository.UserRepository;
import com.rahul.taskmanager.security.JwtUtil;
import com.rahul.taskmanager.security.TokenBlacklistService;
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
    private final TokenBlacklistService tokenBlacklistService;

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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found for refresh token: " + username));

        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name());

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    public void logout(String accessToken) {
        tokenBlacklistService.blacklist(accessToken);
    }
}