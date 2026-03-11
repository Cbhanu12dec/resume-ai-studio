package com.resumeai.auth.service;

import com.resumeai.auth.dto.*;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry:3600}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry:604800}")
    private long refreshTokenExpiry;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(request.getRefreshToken())
                    .getPayload();
            String userId = claims.getSubject();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return buildAuthResponse(user);
        } catch (Exception e) {
            throw new RuntimeException("Invalid refresh token");
        }
    }

    public UserDto getProfile(String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        User user = userRepository.findById(claims.getSubject())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        long now = System.currentTimeMillis();
        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String accessToken = Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpiry * 1000))
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(user.getId())
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenExpiry * 1000))
                .signWith(key)
                .compact();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiry)
                .user(toDto(user))
                .build();
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
