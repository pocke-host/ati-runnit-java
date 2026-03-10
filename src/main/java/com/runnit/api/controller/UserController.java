package com.runnit.api.controller;

import com.runnit.api.dto.MomentResponse;
import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.service.MomentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ActivityRepository activityRepository;
    private final MomentService momentService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(toFullResponse(user));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (body.containsKey("displayName"))       user.setDisplayName((String) body.get("displayName"));
            if (body.containsKey("location"))          user.setLocation((String) body.get("location"));
            if (body.containsKey("sport"))             user.setSport((String) body.get("sport"));
            if (body.containsKey("primarySport"))      user.setSport((String) body.get("primarySport"));
            if (body.containsKey("avatarUrl"))         user.setAvatarUrl((String) body.get("avatarUrl"));
            if (body.containsKey("bio"))               user.setBio((String) body.get("bio"));
            if (body.containsKey("isPublic"))          user.setIsPublic((Boolean) body.get("isPublic"));
            if (body.containsKey("role"))              user.setRole((String) body.get("role"));
            if (body.containsKey("onboardingComplete")) user.setOnboardingComplete((Boolean) body.get("onboardingComplete"));

            userRepository.save(user);
            return ResponseEntity.ok(toFullResponse(user));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            userRepository.deleteById(userId);
            return ResponseEntity.ok(Map.of("message", "Account deleted"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (body.containsKey("unitSystem")) user.setUnitSystem(body.get("unitSystem"));

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("unitSystem", user.getUnitSystem()));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String query,
            Authentication auth) {
        try {
            Long currentUserId = (Long) auth.getPrincipal();
            List<User> users = userRepository.searchByDisplayNameOrEmail(query, PageRequest.of(0, 20));
            List<UserResponse> results = users.stream()
                    .filter(u -> !u.getId().equals(currentUserId))
                    .map(u -> UserResponse.builder()
                            .id(u.getId())
                            .displayName(u.getDisplayName())
                            .email(u.getEmail())
                            .avatarUrl(u.getAvatarUrl())
                            .build())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}/moments")
    public ResponseEntity<?> getUserMoments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long currentUserId = (Long) auth.getPrincipal();
            Page<MomentResponse> moments = momentService.getUserMoments(id, currentUserId, page, size);
            return ResponseEntity.ok(moments);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    private UserResponse toFullResponse(User user) {
        return UserResponse.builder()
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
                .unitSystem(user.getUnitSystem())
                .followerCount(followRepository.countByFollowingUserId(user.getId()))
                .followingCount(followRepository.countByFollowerUserId(user.getId()))
                .activityCount(activityRepository.countByUserId(user.getId()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
