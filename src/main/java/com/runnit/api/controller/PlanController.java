package com.runnit.api.controller;

import com.runnit.api.model.Plan;
import com.runnit.api.model.PlanWorkout;
import com.runnit.api.model.User;
import com.runnit.api.repository.PlanRepository;
import com.runnit.api.repository.PlanWorkoutRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final PlanWorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getPlans(
            @RequestParam(required = false) Long athleteId,
            Authentication auth) {
        try {
            Long targetUserId = athleteId != null ? athleteId : (Long) auth.getPrincipal();
            List<Map<String, Object>> plans = planRepository.findByUserIdOrderByCreatedAtDesc(targetUserId)
                    .stream().map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlan(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Plan plan = planRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));
            if (!plan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            return ResponseEntity.ok(toMap(plan));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createPlan(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Plan plan = Plan.builder()
                    .user(user)
                    .name((String) body.getOrDefault("name", "My Plan"))
                    .sport((String) body.get("sport"))
                    .goal((String) body.get("goal"))
                    .level((String) body.get("level"))
                    .daysPerWeek(body.containsKey("daysPerWeek") ? ((Number) body.get("daysPerWeek")).intValue() : null)
                    .totalWeeks(body.containsKey("totalWeeks") ? ((Number) body.get("totalWeeks")).intValue() : null)
                    .active(false)
                    .build();
            plan = planRepository.save(plan);

            if (body.containsKey("workouts")) {
                List<Map<String, Object>> workoutsData = (List<Map<String, Object>>) body.get("workouts");
                final Plan savedPlan = plan;
                List<PlanWorkout> workouts = workoutsData.stream().map(w -> PlanWorkout.builder()
                        .plan(savedPlan)
                        .day(((Number) w.getOrDefault("day", 1)).intValue())
                        .title((String) w.getOrDefault("title", "Workout"))
                        .description((String) w.get("description"))
                        .durationMinutes(w.containsKey("durationMinutes") ? ((Number) w.get("durationMinutes")).intValue() : null)
                        .distanceMeters(w.containsKey("distanceMeters") ? ((Number) w.get("distanceMeters")).intValue() : null)
                        .build()).collect(Collectors.toList());
                workoutRepository.saveAll(workouts);
                plan.setWorkouts(workouts);
            }

            return ResponseEntity.ok(toMap(plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activate")
    @Transactional
    public ResponseEntity<?> activatePlan(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            planRepository.findByUserIdAndActiveTrue(userId).ifPresent(p -> {
                p.setActive(false);
                planRepository.save(p);
            });
            Plan plan = planRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));
            if (!plan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            plan.setActive(true);
            planRepository.save(plan);
            return ResponseEntity.ok(toMap(plan));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deletePlan(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Plan plan = planRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));
            if (!plan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            planRepository.delete(plan);
            return ResponseEntity.ok(Map.of("message", "Plan deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{planId}/workouts/{workoutId}/complete")
    public ResponseEntity<?> completeWorkout(@PathVariable Long planId, @PathVariable Long workoutId, Authentication auth) {
        return setWorkoutCompleted(planId, workoutId, auth, true);
    }

    @PatchMapping("/{planId}/workouts/{workoutId}/uncomplete")
    public ResponseEntity<?> uncompleteWorkout(@PathVariable Long planId, @PathVariable Long workoutId, Authentication auth) {
        return setWorkoutCompleted(planId, workoutId, auth, false);
    }

    private ResponseEntity<?> setWorkoutCompleted(Long planId, Long workoutId, Authentication auth, boolean completed) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Plan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));
            if (!plan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            PlanWorkout workout = workoutRepository.findById(workoutId)
                    .orElseThrow(() -> new RuntimeException("Workout not found"));
            workout.setCompleted(completed);
            workoutRepository.save(workout);
            return ResponseEntity.ok(Map.of("message", completed ? "Workout completed" : "Workout uncompleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/ai-suggest")
    public ResponseEntity<?> aiSuggest(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String sport = (String) body.getOrDefault("sport", "RUN");
            String goal = (String) body.getOrDefault("goal", "general fitness");
            String level = (String) body.getOrDefault("level", "beginner");

            Plan plan = buildSuggestedPlan(user, sport, goal, level);
            plan = planRepository.save(plan);
            List<PlanWorkout> workouts = buildSuggestedWorkouts(plan, sport, level);
            workoutRepository.saveAll(workouts);
            plan.setWorkouts(workouts);

            return ResponseEntity.ok(toMap(plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Plan buildSuggestedPlan(User user, String sport, String goal, String level) {
        int weeks = "beginner".equalsIgnoreCase(level) ? 4 : "intermediate".equalsIgnoreCase(level) ? 8 : 12;
        int days = "beginner".equalsIgnoreCase(level) ? 3 : 4;
        return Plan.builder()
                .user(user)
                .name(capitalize(level) + " " + capitalize(sport.toLowerCase()) + " Plan")
                .sport(sport)
                .goal(goal)
                .level(level)
                .daysPerWeek(days)
                .totalWeeks(weeks)
                .active(false)
                .build();
    }

    private List<PlanWorkout> buildSuggestedWorkouts(Plan plan, String sport, String level) {
        int weeks = plan.getTotalWeeks() != null ? plan.getTotalWeeks() : ("beginner".equalsIgnoreCase(level) ? 4 : "intermediate".equalsIgnoreCase(level) ? 8 : 12);
        int daysPerWeek = plan.getDaysPerWeek() != null ? plan.getDaysPerWeek() : ("beginner".equalsIgnoreCase(level) ? 3 : 4);
        List<PlanWorkout> workouts = new ArrayList<>();
        for (int week = 0; week < weeks; week++) {
            for (int day = 0; day < daysPerWeek; day++) {
                int globalDay = week * 7 + (day * 2) + 1;
                workouts.add(PlanWorkout.builder()
                        .plan(plan)
                        .day(globalDay)
                        .title("Week " + (week + 1) + " — " + capitalize(sport.toLowerCase()) + " Session " + (day + 1))
                        .description(buildWorkoutDescription(sport, week, day, level))
                        .durationMinutes(30 + (week * 5))
                        .distanceMeters(sport.equalsIgnoreCase("RUN") ? 3000 + (week * 500) : null)
                        .build());
            }
        }
        return workouts;
    }

    private String buildWorkoutDescription(String sport, int week, int day, String level) {
        String intensity = day % 2 == 0 ? "easy" : "moderate";
        return "Week " + (week + 1) + " " + intensity + " " + sport.toLowerCase() + " — " + level + " level.";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private Map<String, Object> toMap(Plan plan) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", plan.getId());
        map.put("name", plan.getName());
        map.put("sport", plan.getSport());
        map.put("goal", plan.getGoal());
        map.put("level", plan.getLevel());
        map.put("isActive", plan.isActive());
        map.put("daysPerWeek", plan.getDaysPerWeek());
        map.put("totalWeeks", plan.getTotalWeeks());
        map.put("createdAt", plan.getCreatedAt());
        if (plan.getWorkouts() != null) {
            map.put("workouts", plan.getWorkouts().stream().map(w -> {
                Map<String, Object> wm = new HashMap<>();
                wm.put("id", w.getId());
                wm.put("day", w.getDay());
                wm.put("title", w.getTitle());
                wm.put("description", w.getDescription());
                wm.put("durationMinutes", w.getDurationMinutes());
                wm.put("distanceMeters", w.getDistanceMeters());
                wm.put("isCompleted", w.isCompleted());
                return wm;
            }).collect(Collectors.toList()));
        }
        return map;
    }
}
