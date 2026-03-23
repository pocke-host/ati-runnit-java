package com.runnit.api.controller;

import com.runnit.api.model.User;
import com.runnit.api.model.WorkoutEvent;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.WorkoutEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/workout-events")
@RequiredArgsConstructor
public class WorkoutEventController {

    private final WorkoutEventRepository eventRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getEvents(
            @RequestParam String start,
            @RequestParam String end,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate   = LocalDate.parse(end);
            List<Map<String, Object>> events = eventRepository
                    .findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(userId, startDate, endDate)
                    .stream().map(this::toMap).toList();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createEvent(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            WorkoutEvent ev = new WorkoutEvent();
            ev.setUser(user);
            ev.setPlannedDate(LocalDate.parse((String) body.get("plannedDate")));
            ev.setTitle((String) body.getOrDefault("title", "Workout"));
            ev.setDescription((String) body.get("description"));
            ev.setWorkoutType((String) body.get("workoutType"));
            ev.setNotes((String) body.get("notes"));
            ev.setSource((String) body.getOrDefault("source", "MANUAL"));
            if (body.get("distanceMeters")   != null) ev.setDistanceMeters(((Number) body.get("distanceMeters")).intValue());
            if (body.get("durationMinutes")  != null) ev.setDurationMinutes(((Number) body.get("durationMinutes")).intValue());
            if (body.get("targetPaceSeconds")!= null) ev.setTargetPaceSeconds(((Number) body.get("targetPaceSeconds")).intValue());

            return ResponseEntity.ok(toMap(eventRepository.save(ev)));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            WorkoutEvent ev = eventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            if (!ev.getUser().getId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));

            if (body.get("title")       != null) ev.setTitle((String) body.get("title"));
            if (body.get("description") != null) ev.setDescription((String) body.get("description"));
            if (body.get("workoutType") != null) ev.setWorkoutType((String) body.get("workoutType"));
            if (body.get("notes")       != null) ev.setNotes((String) body.get("notes"));
            if (body.get("completed")   != null) ev.setCompleted((Boolean) body.get("completed"));
            if (body.get("distanceMeters")    != null) ev.setDistanceMeters(((Number) body.get("distanceMeters")).intValue());
            if (body.get("durationMinutes")   != null) ev.setDurationMinutes(((Number) body.get("durationMinutes")).intValue());
            if (body.get("targetPaceSeconds") != null) ev.setTargetPaceSeconds(((Number) body.get("targetPaceSeconds")).intValue());

            return ResponseEntity.ok(toMap(eventRepository.save(ev)));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            WorkoutEvent ev = eventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            if (!ev.getUser().getId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            eventRepository.delete(ev);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(WorkoutEvent ev) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                ev.getId());
        m.put("plannedDate",       ev.getPlannedDate().toString());
        m.put("title",             ev.getTitle());
        m.put("description",       ev.getDescription());
        m.put("workoutType",       ev.getWorkoutType());
        m.put("distanceMeters",    ev.getDistanceMeters());
        m.put("durationMinutes",   ev.getDurationMinutes());
        m.put("targetPaceSeconds", ev.getTargetPaceSeconds());
        m.put("notes",             ev.getNotes());
        m.put("source",            ev.getSource());
        m.put("completed",         ev.isCompleted());
        m.put("createdAt",         ev.getCreatedAt() != null ? ev.getCreatedAt().toString() : null);
        return m;
    }
}
