package com.runnit.api.controller;

import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.service.AthleteArchetypeService;
import com.runnit.api.service.AthleteArchetypeService.Archetype;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/me/archetype")
@RequiredArgsConstructor
public class AthleteArchetypeController {

    private final AthleteArchetypeService archetypeService;
    private final UserRepository userRepository;

    /**
     * GET /api/users/me/archetype
     * Returns the stored archetype, computing and persisting it on first access.
     */
    @GetMapping
    public ResponseEntity<?> getArchetype(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String stored = user.getArchetype();
            if (stored == null) {
                // First access — compute and persist
                Archetype computed = archetypeService.compute(userId);
                user.setArchetype(computed.name());
                userRepository.save(user);
                stored = computed.name();
            }

            Archetype archetype = Archetype.valueOf(stored);
            return ResponseEntity.ok(toMap(archetype));
        } catch (Exception e) {
            log.error("Failed to get archetype for user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/users/me/archetype/compute
     * Recomputes archetype from latest activity data and persists the result.
     * Call this after a user logs new activities for a fresh classification.
     */
    @PostMapping("/compute")
    public ResponseEntity<?> recompute(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Archetype archetype = archetypeService.compute(userId);
            user.setArchetype(archetype.name());
            userRepository.save(user);

            log.info("Recomputed archetype for userId={}: {}", userId, archetype.name());
            return ResponseEntity.ok(toMap(archetype));
        } catch (Exception e) {
            log.error("Failed to recompute archetype for user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Archetype archetype) {
        return Map.of(
                "archetype", archetype.name(),
                "label", archetype.label,
                "tagline", archetype.tagline
        );
    }
}
