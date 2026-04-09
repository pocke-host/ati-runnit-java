package com.runnit.api.controller;

import com.runnit.api.model.Achievement;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.AchievementRepository;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.CommentRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementRepository achievementRepository;
    private final ActivityRepository activityRepository;
    private final CommentRepository commentRepository;
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
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
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

            List<Activity> activities = activityRepository.findAllByUserId(userId);
            long count = activities.size();
            double totalKm = activities.stream().mapToDouble(a -> (a.getDistanceMeters() != null ? a.getDistanceMeters() : 0) / 1000.0).sum();
            double maxSingleKm = activities.stream().mapToDouble(a -> (a.getDistanceMeters() != null ? a.getDistanceMeters() : 0) / 1000.0).max().orElse(0);
            long runCount = activities.stream().filter(a -> a.getSportType() == Activity.SportType.RUN).count();
            long bikeCount = activities.stream().filter(a -> a.getSportType() == Activity.SportType.BIKE).count();
            long swimCount = activities.stream().filter(a -> a.getSportType() == Activity.SportType.SWIM).count();
            int maxStreak = computeMaxStreak(activities);
            boolean hasCommented = commentRepository.existsByUser_Id(userId);

            // IDs match frontend BADGE_CATALOG exactly
            award(user, "early_adopter", true);  // Everyone who calls /check has adopted
            award(user, "first_activity",  count >= 1);
            award(user, "first_km",        totalKm >= 1);
            award(user, "5k_club",         maxSingleKm >= 5);
            award(user, "10k_club",        maxSingleKm >= 10);
            award(user, "half_marathon",   maxSingleKm >= 21.1);
            award(user, "marathon",        maxSingleKm >= 42.2);
            award(user, "total_100km",     totalKm >= 100);
            award(user, "total_500km",     totalKm >= 500);
            award(user, "activities_10",   count >= 10);
            award(user, "activities_50",   count >= 50);
            award(user, "activities_100",  count >= 100);
            award(user, "streak_3",        maxStreak >= 3);
            award(user, "streak_7",        maxStreak >= 7);
            award(user, "streak_30",       maxStreak >= 30);
            award(user, "runner",          runCount >= 10);
            award(user, "cyclist",         bikeCount >= 10);
            award(user, "swimmer",         swimCount >= 10);
            award(user, "triathlete",      runCount >= 1 && bikeCount >= 1 && swimCount >= 1);
            award(user, "social_star",     hasCommented);

            List<Map<String, Object>> achievements = achievementRepository.findByUserId(userId)
                    .stream().map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** Returns the maximum consecutive-day streak across all activities. */
    private int computeMaxStreak(List<Activity> activities) {
        if (activities.isEmpty()) return 0;
        // Deduplicate to unique calendar dates, sorted ascending
        Set<LocalDate> days = new TreeSet<>();
        for (Activity a : activities) {
            if (a.getCreatedAt() != null) days.add(a.getCreatedAt().toLocalDate());
        }
        LocalDate[] sorted = days.toArray(new LocalDate[0]);
        int maxStreak = 1;
        int current = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i].minusDays(1).equals(sorted[i - 1])) {
                current++;
                maxStreak = Math.max(maxStreak, current);
            } else {
                current = 1;
            }
        }
        return maxStreak;
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
