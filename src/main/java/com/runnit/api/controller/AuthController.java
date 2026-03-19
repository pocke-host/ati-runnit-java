package com.runnit.api.controller;

import com.runnit.api.dto.LoginRequest;
import com.runnit.api.dto.RegisterRequest;
import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.User;
import com.runnit.api.service.AuthService;
import com.runnit.api.repository.FollowRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final FollowRepository followRepository;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = authService.registerWithEmail(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                request.getRole()
        );
        setJwtCookie(response, (String) result.get("token"));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = authService.loginWithEmail(request.getEmail(), request.getPassword());
        setJwtCookie(response, (String) result.get("token"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "None"); // must match the original cookie's SameSite attribute to clear it
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = authService.getUserById(userId);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .location(user.getLocation())
                .sport(user.getSport())
                .primarySport(user.getSport())
                .bio(user.getBio())
                .isPublic(user.getIsPublic())
                .role(user.getRole())
                .onboardingComplete(user.getOnboardingComplete())
                .followerCount(followRepository.countByFollowingUserId(userId))
                .followingCount(followRepository.countByFollowerUserId(userId))
                .createdAt(user.getCreatedAt())
                .unitSystem(user.getUnitSystem())
                .build();

        return ResponseEntity.ok(userResponse);
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000));
        cookie.setAttribute("SameSite", "None"); // required for cross-origin cookie sharing (runnit.live → onrender.com)
        response.addCookie(cookie);
    }
}
