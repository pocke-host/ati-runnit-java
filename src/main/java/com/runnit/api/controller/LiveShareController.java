package com.runnit.api.controller;

import com.runnit.api.model.LiveShare;
import com.runnit.api.model.User;
import com.runnit.api.repository.LiveShareRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/live-shares")
@RequiredArgsConstructor
public class LiveShareController {

    private final LiveShareRepository liveShareRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createShare(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LiveShare share = new LiveShare();
            share.setToken(UUID.randomUUID().toString());
            share.setUserId(userId);
            share.setDisplayName(user.getDisplayName());
            share.setSportType((String) body.getOrDefault("sportType", "RUN"));
            share.setIsActive(true);
            share.setStartedAt(LocalDateTime.now());
            share.setUpdatedAt(LocalDateTime.now());

            LiveShare saved = liveShareRepository.save(share);
            String shareUrl = "https://runnit.live/live/" + saved.getToken();

            return ResponseEntity.ok(Map.of("token", saved.getToken(), "shareUrl", shareUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{token}")
    public ResponseEntity<?> updateLocation(
            @PathVariable String token,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            LiveShare share = liveShareRepository.findByTokenAndUserId(token, userId)
                    .orElseThrow(() -> new RuntimeException("Share not found"));

            if (body.get("lat") != null) share.setLat(((Number) body.get("lat")).doubleValue());
            if (body.get("lng") != null) share.setLng(((Number) body.get("lng")).doubleValue());
            if (body.get("elapsedSeconds") != null) share.setElapsedSeconds(((Number) body.get("elapsedSeconds")).intValue());
            if (body.get("distanceMeters") != null) share.setDistanceMeters(((Number) body.get("distanceMeters")).intValue());
            share.setUpdatedAt(LocalDateTime.now());

            liveShareRepository.save(share);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getShare(@PathVariable String token) {
        return liveShareRepository.findByToken(token)
                .map(share -> ResponseEntity.ok(toPublicMap(share)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<?> stopShare(@PathVariable String token, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            LiveShare share = liveShareRepository.findByTokenAndUserId(token, userId)
                    .orElseThrow(() -> new RuntimeException("Share not found"));
            share.setIsActive(false);
            share.setUpdatedAt(LocalDateTime.now());
            liveShareRepository.save(share);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toPublicMap(LiveShare share) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", share.getToken());
        m.put("displayName", share.getDisplayName());
        m.put("sportType", share.getSportType());
        m.put("lat", share.getLat());
        m.put("lng", share.getLng());
        m.put("elapsedSeconds", share.getElapsedSeconds());
        m.put("distanceMeters", share.getDistanceMeters());
        m.put("isActive", share.getIsActive());
        m.put("startedAt", share.getStartedAt() != null ? share.getStartedAt().toString() : null);
        m.put("updatedAt", share.getUpdatedAt() != null ? share.getUpdatedAt().toString() : null);
        return m;
    }
}
