// ========== AuthController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.LoginRequest;
import com.runnit.api.dto.RegisterRequest;
import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.User;
import com.runnit.api.service.AuthService;
// import com.runnit.api.service.FollowService;
import com.runnit.api.repository.FollowRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final FollowRepository followRepository;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Map<String, Object> response = authService.registerWithEmail(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> response = authService.loginWithEmail(
                request.getEmail(),
                request.getPassword()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = authService.getUserById(userId);
            
            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .location(user.getLocation())
                    .sport(user.getSport())
                    .followerCount(followRepository.countByFollowingUserId(userId))
                    .followingCount(followRepository.countByFollowerUserId(userId))
                    .createdAt(user.getCreatedAt())
                    .unitSystem(user.getUnitSystem())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
