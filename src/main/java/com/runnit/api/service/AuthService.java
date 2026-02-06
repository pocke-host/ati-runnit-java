package com.runnit.api.service;

import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Transactional
    public Map<String, Object> registerWithEmail(String email, String password, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = User.builder()
                .email(email)
                .displayName(displayName)
                .authProvider(User.AuthProvider.EMAIL)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userToMap(user));
        return response;
    }
    
    public Map<String, Object> loginWithEmail(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        if (user.getAuthProvider() != User.AuthProvider.EMAIL) {
            throw new RuntimeException("Please use " + user.getAuthProvider() + " to sign in");
        }
        
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userToMap(user));
        return response;
    }
    
    @Transactional
    public Map<String, Object> handleOAuthLogin(User.AuthProvider provider, String providerId, 
                                                 String email, String displayName, String avatarUrl) {
        User user = userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .displayName(displayName)
                            .avatarUrl(avatarUrl)
                            .authProvider(provider)
                            .providerId(providerId)
                            .build();
                    return userRepository.save(newUser);
                });
        
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userToMap(user));
        return response;
    }
    
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail() != null ? user.getEmail() : "");
        map.put("displayName", user.getDisplayName());
        map.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        return map;
    }
}