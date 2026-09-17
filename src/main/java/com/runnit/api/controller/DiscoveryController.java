package com.runnit.api.controller;

import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/** One authenticated discovery contract for people, training, and community. */
@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {
    private final UserRepository users;
    private final FollowRepository follows;
    private final ClubRepository clubs;
    private final PlanRepository plans;
    private final TrainingFolderRepository folders;
    private final ActivityRepository activities;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, Authentication auth) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) return ResponseEntity.ok(Map.of("results", List.of()));
        Long userId = (Long) auth.getPrincipal();
        String needle = query.toLowerCase(Locale.ROOT);

        List<Map<String, Object>> result = new ArrayList<>();
        users.searchByDisplayNameOrEmail(query, PageRequest.of(0, 8)).stream()
                .filter(u -> !u.getId().equals(userId) && Boolean.TRUE.equals(u.getIsPublic()))
                .forEach(u -> result.add(userResult(u, userId)));
        clubs.findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrSportContainingIgnoreCase(query, query, query)
                .stream().filter(c -> !c.isPrivateClub()).limit(8).forEach(c -> result.add(clubResult(c)));
        plans.findByNameContainingIgnoreCaseOrGoalContainingIgnoreCase(query, query)
                .stream().filter(p -> p.isActive() || p.getUser().getId().equals(userId)).limit(8).forEach(p -> result.add(planResult(p)));
        folders.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query)
                .stream().filter(f -> f.getUser().getId().equals(userId)).limit(8).forEach(f -> result.add(folderResult(f)));

        // Activities are private by default; expose only the current athlete's matching history.
        activities.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20)).getContent().stream()
                .filter(a -> a.getSportType() != null && (a.getSportType().name().toLowerCase(Locale.ROOT).contains(needle)
                        || (a.getSource() != null && a.getSource().name().toLowerCase(Locale.ROOT).contains(needle))))
                .limit(8).forEach(a -> result.add(activityResult(a)));
        return ResponseEntity.ok(Map.of("results", result));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> recommendations(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = users.findById(userId).orElseThrow();
        String sport = user.getSport();
        List<Map<String, Object>> athleteRecs = users.findTop50ByIsPublicTrueOrderByCreatedAtDesc().stream()
                .filter(u -> !u.getId().equals(userId))
                .filter(u -> sport == null || sport.isBlank() || sport.equalsIgnoreCase(u.getSport()))
                .limit(6).map(u -> userResult(u, userId)).toList();
        List<Map<String, Object>> planRecs = plans.findAllByActiveTrue().stream()
                .filter(p -> sport == null || sport.isBlank() || sport.equalsIgnoreCase(p.getSport()))
                .limit(6).map(this::planResult).toList();
        List<Map<String, Object>> clubRecs = clubs.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> !c.isPrivateClub() && (sport == null || sport.isBlank() || sport.equalsIgnoreCase(c.getSport())))
                .limit(6).map(this::clubResult).toList();
        return ResponseEntity.ok(Map.of(
                "athletes", athleteRecs,
                "plans", planRecs,
                "clubs", clubRecs,
                "races", List.of(Map.of("title", "Find a race near you", "path", "/races"))
        ));
    }

    private Map<String, Object> userResult(User u, Long currentId) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("type", "ATHLETE"); m.put("id", u.getId());
        m.put("title", u.getDisplayName()); m.put("subtitle", u.getSport()); m.put("imageUrl", u.getAvatarUrl());
        m.put("path", "/profile/" + u.getId()); m.put("isFollowing", follows.existsByFollowerUserIdAndFollowingUserId(currentId, u.getId())); return m;
    }
    private Map<String, Object> clubResult(Club c) { return base("CLUB", c.getId(), c.getName(), c.getCity(), c.getImageUrl(), "/clubs"); }
    private Map<String, Object> planResult(Plan p) { return base("PLAN", p.getId(), p.getName(), p.getSport(), null, "/plans/" + p.getId()); }
    private Map<String, Object> folderResult(TrainingFolder f) { return base("FOLDER", f.getId(), f.getName(), f.getDescription(), null, "/training-folders/" + f.getId()); }
    private Map<String, Object> activityResult(Activity a) { return base("ACTIVITY", a.getId(), a.getSportType().name(), a.getSource() == null ? "Manual" : a.getSource().name(), null, "/activities/" + a.getId()); }
    private Map<String, Object> base(String type, Long id, String title, String subtitle, String imageUrl, String path) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("type", type); m.put("id", id); m.put("title", title); m.put("subtitle", subtitle); m.put("imageUrl", imageUrl); m.put("path", path); return m;
    }
}
