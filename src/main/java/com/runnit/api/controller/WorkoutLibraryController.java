package com.runnit.api.controller;

import com.runnit.api.model.WorkoutLibrary;
import com.runnit.api.repository.WorkoutLibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Manages the user's personal workout library — saved reusable workout templates.
 * GET    /api/workouts               — list all saved templates
 * POST   /api/workouts               — save a new template
 * POST   /api/workout-templates      — alias for POST /api/workouts (frontend compat)
 * DELETE /api/workouts/{id}          — delete a template
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WorkoutLibraryController {

    private final WorkoutLibraryRepository workoutLibraryRepository;

    @GetMapping("/api/workouts")
    public ResponseEntity<?> listWorkouts(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<WorkoutLibrary> workouts = workoutLibraryRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(workouts.stream().map(this::toMap).toList());
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/workouts")
    public ResponseEntity<?> createWorkout(@RequestBody Map<String, Object> body, Authentication auth) {
        return saveTemplate(body, auth);
    }

    /** Alias — some frontend calls hit /api/workout-templates */
    @PostMapping("/api/workout-templates")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> body, Authentication auth) {
        return saveTemplate(body, auth);
    }

    @DeleteMapping("/api/workouts/{id}")
    public ResponseEntity<?> deleteWorkout(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            WorkoutLibrary workout = workoutLibraryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Workout not found"));
            if (!workout.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            workoutLibraryRepository.delete(workout);
            return ResponseEntity.ok(Map.of("message", "Workout deleted"));
        } catch (RuntimeException e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<?> saveTemplate(Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            if (body.get("title") == null || body.get("title").toString().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "title is required"));
            }
            WorkoutLibrary workout = new WorkoutLibrary();
            workout.setUserId(userId);
            workout.setTitle(body.get("title").toString());
            workout.setDescription(body.containsKey("description") ? (String) body.get("description") : null);
            workout.setWorkoutType(body.containsKey("workoutType") ? (String) body.get("workoutType") : null);
            workout.setSport(body.containsKey("sport") ? (String) body.get("sport") : null);
            if (body.containsKey("distanceMeters") && body.get("distanceMeters") != null) {
                workout.setDistanceMeters(((Number) body.get("distanceMeters")).intValue());
            }
            if (body.containsKey("durationMinutes") && body.get("durationMinutes") != null) {
                workout.setDurationMinutes(((Number) body.get("durationMinutes")).intValue());
            }
            if (body.containsKey("targetPaceSeconds") && body.get("targetPaceSeconds") != null) {
                workout.setTargetPaceSeconds(((Number) body.get("targetPaceSeconds")).intValue());
            }
            workout.setNotes(body.containsKey("notes") ? (String) body.get("notes") : null);
            workoutLibraryRepository.save(workout);
            return ResponseEntity.ok(toMap(workout));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(WorkoutLibrary w) {
        return Map.of(
                "id", w.getId(),
                "title", w.getTitle(),
                "description", w.getDescription() != null ? w.getDescription() : "",
                "workoutType", w.getWorkoutType() != null ? w.getWorkoutType() : "",
                "sport", w.getSport() != null ? w.getSport() : "",
                "distanceMeters", w.getDistanceMeters() != null ? w.getDistanceMeters() : 0,
                "durationMinutes", w.getDurationMinutes() != null ? w.getDurationMinutes() : 0,
                "targetPaceSeconds", w.getTargetPaceSeconds() != null ? w.getTargetPaceSeconds() : 0,
                "createdAt", w.getCreatedAt() != null ? w.getCreatedAt().toString() : ""
        );
    }
}
