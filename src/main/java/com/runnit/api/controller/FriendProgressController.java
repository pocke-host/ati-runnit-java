package com.runnit.api.controller;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

/** Progress summaries for people the athlete already follows; never exposes public discovery data. */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendProgressController {
    private final FollowRepository follows;
    private final UserRepository users;
    private final ActivityRepository activities;

    @GetMapping("/progress")
    public List<Map<String, Object>> progress(@RequestParam(defaultValue = "7") int days, Authentication auth) {
        int window = Math.max(1, Math.min(days, 31));
        Long currentId = (Long) auth.getPrincipal();
        LocalDateTime since = LocalDateTime.now().minusDays(window);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long friendId : follows.findFollowingUserIds(currentId)) {
            User friend = users.findById(friendId).orElse(null);
            if (friend == null || !Boolean.TRUE.equals(friend.getIsPublic())) continue;
            List<Activity> recent = activities.findByUserIdSince(friendId, since);
            long seconds = recent.stream().mapToLong(a -> a.getDurationSeconds() == null ? 0 : a.getDurationSeconds()).sum();
            long meters = recent.stream().mapToLong(a -> a.getDistanceMeters() == null ? 0 : a.getDistanceMeters()).sum();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", friend.getId());
            row.put("displayName", friend.getDisplayName());
            row.put("avatarUrl", friend.getAvatarUrl());
            row.put("activityCount", recent.size());
            row.put("durationMinutes", Math.round(seconds / 60.0));
            row.put("distanceMeters", meters);
            row.put("days", window);
            result.add(row);
        }
        result.sort(Comparator.comparingLong((Map<String, Object> row) -> ((Number) row.get("durationMinutes")).longValue()).reversed());
        return result;
    }
}
