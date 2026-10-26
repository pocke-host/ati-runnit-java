package com.runnit.api.controller;

import com.runnit.api.dto.MomentResponse;
import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.User;
import com.runnit.api.model.Activity;
import com.runnit.api.model.RaceBookmark;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.RaceBookmarkRepository;
import com.runnit.api.service.MomentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ActivityRepository activityRepository;
    private final MomentService momentService;
    private final RaceBookmarkRepository raceBookmarkRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id, Authentication auth) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Long viewerId = auth == null || !(auth.getPrincipal() instanceof Long)
                    ? null : (Long) auth.getPrincipal();
            if (!canViewUser(user, viewerId)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(toFullResponse(user, id.equals(viewerId)));
        } catch (RuntimeException e) {
            log.warn("User profile not found: id={}", id);
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (body.containsKey("displayName"))        user.setDisplayName((String) body.get("displayName"));
            if (body.containsKey("location"))           user.setLocation((String) body.get("location"));
            if (body.containsKey("sport"))              user.setSport((String) body.get("sport"));
            if (body.containsKey("primarySport"))       user.setSport((String) body.get("primarySport"));
            if (body.containsKey("avatarUrl"))          user.setAvatarUrl((String) body.get("avatarUrl"));
            if (body.containsKey("bio"))                user.setBio((String) body.get("bio"));
            if (body.containsKey("isPublic"))           user.setIsPublic((Boolean) body.get("isPublic"));
            // Role and onboarding state are server-controlled. Never accept either
            // field from a self-service profile update: allowing this would let a
            // normal athlete promote themselves to admin or bypass onboarding.

            userRepository.save(user);
            return ResponseEntity.ok(toFullResponse(user));
        } catch (RuntimeException e) {
            log.error("Failed to update profile: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            userRepository.deleteById(userId);
            log.info("Account deleted: userId={}", userId);
            return ResponseEntity.ok(Map.of("message", "Account deleted"));
        } catch (RuntimeException e) {
            log.error("Failed to delete account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (body.containsKey("unitSystem")) user.setUnitSystem(body.get("unitSystem"));
            if (body.containsKey("goal")) user.setOnboardingGoal(body.get("goal"));
            if (body.containsKey("level")) user.setExperienceLevel(body.get("level"));
            if (body.containsKey("trainingDays")) user.setTrainingDays(body.get("trainingDays"));

            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "unitSystem", user.getUnitSystem(),
                    "goal", user.getOnboardingGoal() == null ? "" : user.getOnboardingGoal(),
                    "level", user.getExperienceLevel() == null ? "" : user.getExperienceLevel(),
                    "trainingDays", user.getTrainingDays() == null ? "" : user.getTrainingDays()));
        } catch (RuntimeException e) {
            log.error("Failed to update preferences: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search users by display name. Email is intentionally excluded from results
     * to prevent leaking user email addresses through the search API.
     */
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
                            .user(u.getUser())
                            .displayName(u.getDisplayName())
                            // email intentionally omitted from search results
                            .avatarUrl(u.getAvatarUrl())
                            .sport(u.getSport())
                            .bio(u.getBio())
                            .followerCount(followRepository.countByFollowingUserId(u.getId()))
                            .isFollowing(followRepository.existsByFollowerUserIdAndFollowingUserId(currentUserId, u.getId()))
                            .build())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            log.error("User search failed: query={} error={}", query, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Match a device's contact emails against existing accounts, so a user can find
     * real-world friends who are already on Runnit. Emails only (no phone number is
     * collected/stored anywhere in this app) — the client sends whatever emails the
     * Contact Picker / manual entry surfaced, we return which ones have accounts.
     */
    @PostMapping("/match-contacts")
    public ResponseEntity<?> matchContacts(@RequestBody Map<String, List<String>> body, Authentication auth) {
        try {
            Long currentUserId = (Long) auth.getPrincipal();
            List<String> emails = body.getOrDefault("emails", List.of()).stream()
                    .filter(e -> e != null && !e.isBlank())
                    .map(e -> e.trim().toLowerCase())
                    .distinct()
                    .limit(1000)
                    .collect(Collectors.toList());
            if (emails.isEmpty()) return ResponseEntity.ok(List.of());

            List<UserResponse> matches = userRepository.findByEmailInIgnoreCase(emails).stream()
                    .filter(u -> !u.getId().equals(currentUserId))
                    .map(u -> UserResponse.builder()
                            .id(u.getId())
                            .displayName(u.getDisplayName())
                            .avatarUrl(u.getAvatarUrl())
                            .sport(u.getSport())
                            .isFollowing(followRepository.existsByFollowerUserIdAndFollowingUserId(currentUserId, u.getId()))
                            .build())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(matches);
        } catch (RuntimeException e) {
            log.error("Contact match failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            log.error("Failed to fetch user moments: userId={} error={}", id, e.getMessage(), e);
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/race-history")
    public ResponseEntity<?> getRaceHistory(@PathVariable Long id, Authentication auth) {
        try {
            User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
            Long viewerId = auth == null || !(auth.getPrincipal() instanceof Long)
                    ? null : (Long) auth.getPrincipal();
            if (!canViewUser(user, viewerId)) {
                return ResponseEntity.notFound().build();
            }
            List<Map<String, Object>> activities = activityRepository
                    .findByUserIdOrderByCreatedAtDesc(id, PageRequest.of(0, 200)).getContent().stream()
                    .filter(a -> a.getSportType() == Activity.SportType.RUN && isRaceDistance(a.getDistanceMeters()))
                    .map(this::raceActivityMap).collect(Collectors.toList());
            List<Map<String, Object>> bookmarks = raceBookmarkRepository.findByUserIdOrderByRaceDateAsc(id).stream()
                    .map(this::raceBookmarkMap).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("activities", activities, "bookmarks", bookmarks));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isRaceDistance(Integer meters) {
        if (meters == null) return false;
        double km = meters / 1000.0;
        return List.of(5.0, 10.0, 21.0975, 42.195).stream()
                .anyMatch(d -> Math.abs(km - d) / d <= 0.05);
    }

    private Map<String, Object> raceActivityMap(Activity a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId()); map.put("distanceMeters", a.getDistanceMeters());
        map.put("durationSeconds", a.getDurationSeconds()); map.put("performedAt", a.getPerformedAt());
        map.put("createdAt", a.getCreatedAt()); map.put("raceName", null);
        return map;
    }

    private Map<String, Object> raceBookmarkMap(RaceBookmark bm) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", bm.getId()); map.put("raceName", bm.getRaceName());
        map.put("raceDate", bm.getRaceDate() != null ? bm.getRaceDate().toString() : null);
        map.put("raceType", bm.getRaceType()); map.put("city", bm.getCity()); map.put("state", bm.getState());
        map.put("raceUrl", bm.getRaceUrl()); return map;
    }

    @PostMapping("/me/push-token")
    public ResponseEntity<?> savePushToken(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String token    = body.get("token");
            String platform = body.get("platform");
            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "token is required"));
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setPushToken(token);
            user.setPushPlatform(platform);
            userRepository.save(user);
            log.info("Push token saved: userId={} platform={}", userId, platform);
            return ResponseEntity.ok(Map.of("message", "Push token saved"));
        } catch (RuntimeException e) {
            log.error("Failed to save push token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/me/push-token")
    public ResponseEntity<?> deletePushToken(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setPushToken(null);
            user.setPushPlatform(null);
            userRepository.save(user);
            log.info("Push token removed: userId={}", userId);
            return ResponseEntity.ok(Map.of("message", "Push token removed"));
        } catch (RuntimeException e) {
            log.error("Failed to remove push token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private UserResponse toFullResponse(User user) {
        return toFullResponse(user, true);
    }

    private UserResponse toFullResponse(User user, boolean includePrivateFields) {
        String subscriptionStatus = user.getSubscriptionStatus();
        String subscriptionTier = user.getSubscriptionTier() != null
                ? user.getSubscriptionTier()
                : deriveSubscriptionTier(subscriptionStatus);

        return UserResponse.builder()
                .id(user.getId())
                .email(includePrivateFields ? user.getEmail() : null)
                .user(user.getUser())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .location(user.getLocation())
                .sport(user.getSport())
                .primarySport(user.getSport())
                .bio(user.getBio())
                .isPublic(user.getIsPublic())
                .role(includePrivateFields ? user.getRole() : null)
                .onboardingComplete(includePrivateFields ? user.getOnboardingComplete() : null)
                .unitSystem(user.getUnitSystem())
                .archetype(user.getArchetype())
                .onboardingGoal(includePrivateFields ? user.getOnboardingGoal() : null)
                .experienceLevel(includePrivateFields ? user.getExperienceLevel() : null)
                .trainingDays(includePrivateFields ? user.getTrainingDays() : null)
                .targetEventId(includePrivateFields ? user.getTargetEventId() : null)
                .targetEventProvider(includePrivateFields ? user.getTargetEventProvider() : null)
                .targetEventName(includePrivateFields ? user.getTargetEventName() : null)
                .targetEventDate(includePrivateFields ? user.getTargetEventDate() : null)
                .targetEventSport(includePrivateFields ? user.getTargetEventSport() : null)
                .targetEventUrl(includePrivateFields ? user.getTargetEventUrl() : null)
                .targetEventCity(includePrivateFields ? user.getTargetEventCity() : null)
                .targetEventState(includePrivateFields ? user.getTargetEventState() : null)
                .subscriptionStatus(includePrivateFields ? subscriptionStatus : null)
                .subscriptionTier(includePrivateFields ? subscriptionTier : null)
                .followerCount(followRepository.countByFollowingUserId(user.getId()))
                .followingCount(followRepository.countByFollowerUserId(user.getId()))
                .activityCount(activityRepository.countByUserId(user.getId()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private boolean canViewUser(User user, Long viewerId) {
        return viewerId != null && user.getId().equals(viewerId)
                || Boolean.TRUE.equals(user.getIsPublic());
    }

    private String deriveSubscriptionTier(String subscriptionStatus) {
        if (subscriptionStatus == null) return "free";
        return switch (subscriptionStatus) {
            case "active", "trialing" -> "premium";
            default -> "free";
        };
    }
}
