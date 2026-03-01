package com.runnit.api.controller;

import com.runnit.api.model.Achievement;
import com.runnit.api.model.User;
import com.runnit.api.repository.AchievementRepository;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementRepository achievementRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAchievements(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        try {
            Long targetUserId = userId != null ? userId : (Long) auth.getPrincipal();
            List<Map<String, Object>> achievements = achievementRepository.findByUserId(targetUserId)
                    .stream().map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/check")
    @Transactional
    public ResponseEntity<?> checkAndAward(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            long activityCount = activityRepository.countByUserId(userId);

            award(user, "first_activity", activityCount >= 1);
            award(user, "five_activities", activityCount >= 5);
            award(user, "ten_activities", activityCount >= 10);
            award(user, "fifty_activities", activityCount >= 50);

            List<Map<String, Object>> achievements = achievementRepository.findByUserId(userId)
                    .stream().map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private void award(User user, String badgeId, boolean condition) {
        if (condition && !achievementRepository.existsByUserIdAndBadgeId(user.getId(), badgeId)) {
            achievementRepository.save(Achievement.builder().user(user).badgeId(badgeId).build());
        }
    }

    private Map<String, Object> toMap(Achievement a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("badgeId", a.getBadgeId());
        map.put("earnedAt", a.getEarnedAt());
        return map;
    }
}
