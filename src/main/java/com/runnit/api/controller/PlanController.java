package com.runnit.api.controller;

import com.runnit.api.dto.TrainingBlockResponse;
import com.runnit.api.model.Plan;
import com.runnit.api.model.PlanWorkout;
import com.runnit.api.model.User;
import com.runnit.api.repository.PlanRepository;
import com.runnit.api.repository.PlanWorkoutRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.service.TrainingBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final PlanWorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final TrainingBlockService trainingBlockService;

    /**
     * GET /api/plans/active/block
     * Returns a full training-block summary for the currently active plan:
     * current phase/theme, week progress, phase boundaries, per-week volume
     * trend, and this week's workouts.
     * Returns 404 if no active plan exists.
     */
    @GetMapping("/active/block")
    public ResponseEntity<?> getActiveBlock(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            TrainingBlockResponse block = trainingBlockService.getBlockSummary(userId);
            if (block == null) {
                return ResponseEntity.status(404).body(Map.of("error", "No active plan found"));
            }
            return ResponseEntity.ok(block);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
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

            Plan.Builder builder = Plan.builder()
                    .user(user)
                    .name((String) body.getOrDefault("name", "My Plan"))
                    .sport((String) body.get("sport"))
                    .goal((String) body.get("goal"))
                    .level((String) body.get("level"))
                    .daysPerWeek(body.containsKey("daysPerWeek") ? ((Number) body.get("daysPerWeek")).intValue() : null)
                    .totalWeeks(body.containsKey("totalWeeks") ? ((Number) body.get("totalWeeks")).intValue() : null)
                    .active(false);

            // Parse new date/fitness fields
            if (body.get("startDate") instanceof String s && !s.isBlank()) {
                try { builder.startDate(LocalDate.parse(s)); } catch (Exception ignored) {}
            }
            if (body.get("targetRaceDate") instanceof String s && !s.isBlank()) {
                try { builder.targetRaceDate(LocalDate.parse(s)); } catch (Exception ignored) {}
            }
            if (body.containsKey("currentWeeklyMeters") && body.get("currentWeeklyMeters") != null) {
                builder.currentWeeklyMeters(((Number) body.get("currentWeeklyMeters")).intValue());
            }
            if (body.containsKey("targetSeconds") && body.get("targetSeconds") != null) {
                builder.targetSeconds(((Number) body.get("targetSeconds")).intValue());
            }

            Plan plan = planRepository.save(builder.build());

            if (body.containsKey("workouts")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> workoutsData = (List<Map<String, Object>>) body.get("workouts");
                final Plan savedPlan = plan;
                List<PlanWorkout> workouts = workoutsData.stream().map(w -> PlanWorkout.builder()
                        .plan(savedPlan)
                        .day(((Number) w.getOrDefault("day", 1)).intValue())
                        .title((String) w.getOrDefault("title", "Workout"))
                        .description((String) w.get("description"))
                        .durationMinutes(w.containsKey("durationMinutes") ? ((Number) w.get("durationMinutes")).intValue() : null)
                        .distanceMeters(w.containsKey("distanceMeters") ? ((Number) w.get("distanceMeters")).intValue() : null)
                        .workoutType(w.containsKey("workoutType") ? (String) w.get("workoutType") : null)
                        .weekNumber(w.containsKey("weekNumber") ? ((Number) w.get("weekNumber")).intValue() : null)
                        .targetPaceSeconds(w.containsKey("targetPaceSeconds") && w.get("targetPaceSeconds") != null
                                ? ((Number) w.get("targetPaceSeconds")).intValue() : null)
                        .build()).collect(Collectors.toList());
                workoutRepository.saveAll(workouts);
                plan.setWorkouts(workouts);
            }

            return ResponseEntity.ok(toMap(plan));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
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

    /** POST /api/plans/{planId}/weeks/{weekNum}/workouts — add a workout to a specific week */
    @PostMapping("/{planId}/weeks/{weekNum}/workouts")
    @Transactional
    public ResponseEntity<?> addWorkoutToWeek(
            @PathVariable Long planId,
            @PathVariable Integer weekNum,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Plan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));
            if (!plan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            PlanWorkout workout = PlanWorkout.builder()
                    .plan(plan)
                    .weekNumber(weekNum)
                    .day(body.containsKey("day") ? ((Number) body.get("day")).intValue() : 1)
                    .title((String) body.getOrDefault("title", "Workout"))
                    .description((String) body.get("description"))
                    .workoutType(body.containsKey("workoutType") ? (String) body.get("workoutType") : null)
                    .durationMinutes(body.containsKey("durationMinutes") && body.get("durationMinutes") != null
                            ? ((Number) body.get("durationMinutes")).intValue() : null)
                    .distanceMeters(body.containsKey("distanceMeters") && body.get("distanceMeters") != null
                            ? ((Number) body.get("distanceMeters")).intValue() : null)
                    .targetPaceSeconds(body.containsKey("targetPaceSeconds") && body.get("targetPaceSeconds") != null
                            ? ((Number) body.get("targetPaceSeconds")).intValue() : null)
                    .build();
            workoutRepository.save(workout);
            Map<String, Object> result = new HashMap<>();
            result.put("id", workout.getId());
            result.put("weekNumber", workout.getWeekNumber());
            result.put("day", workout.getDay());
            result.put("title", workout.getTitle());
            result.put("description", workout.getDescription());
            result.put("workoutType", workout.getWorkoutType());
            result.put("durationMinutes", workout.getDurationMinutes());
            result.put("distanceMeters", workout.getDistanceMeters());
            result.put("targetPaceSeconds", workout.getTargetPaceSeconds());
            result.put("completed", workout.isCompleted());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PATCH /api/plans/{planId}/workouts/{workoutId} — update any fields on a plan workout */
    @PatchMapping("/{planId}/workouts/{workoutId}")
    @Transactional
    public ResponseEntity<?> updatePlanWorkout(
            @PathVariable Long planId,
            @PathVariable Long workoutId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Plan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));
            if (!plan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            PlanWorkout workout = workoutRepository.findById(workoutId)
                    .orElseThrow(() -> new RuntimeException("Workout not found"));
            if (body.containsKey("title") && body.get("title") != null) {
                workout.setTitle((String) body.get("title"));
            }
            if (body.containsKey("description")) {
                workout.setDescription((String) body.get("description"));
            }
            if (body.containsKey("workoutType")) {
                workout.setWorkoutType((String) body.get("workoutType"));
            }
            if (body.containsKey("durationMinutes") && body.get("durationMinutes") != null) {
                workout.setDurationMinutes(((Number) body.get("durationMinutes")).intValue());
            }
            if (body.containsKey("distanceMeters") && body.get("distanceMeters") != null) {
                workout.setDistanceMeters(((Number) body.get("distanceMeters")).intValue());
            }
            if (body.containsKey("targetPaceSeconds") && body.get("targetPaceSeconds") != null) {
                workout.setTargetPaceSeconds(((Number) body.get("targetPaceSeconds")).intValue());
            }
            if (body.containsKey("day") && body.get("day") != null) {
                workout.setDay(((Number) body.get("day")).intValue());
            }
            if (body.containsKey("completed")) {
                workout.setCompleted(Boolean.TRUE.equals(body.get("completed")));
            }
            workoutRepository.save(workout);
            Map<String, Object> result = new HashMap<>();
            result.put("id", workout.getId());
            result.put("weekNumber", workout.getWeekNumber());
            result.put("day", workout.getDay());
            result.put("title", workout.getTitle());
            result.put("description", workout.getDescription());
            result.put("workoutType", workout.getWorkoutType());
            result.put("durationMinutes", workout.getDurationMinutes());
            result.put("distanceMeters", workout.getDistanceMeters());
            result.put("targetPaceSeconds", workout.getTargetPaceSeconds());
            result.put("completed", workout.isCompleted());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/plans/{planId}/workouts/{workoutId} — remove a single workout from a plan */
    @DeleteMapping("/{planId}/workouts/{workoutId}")
    @Transactional
    public ResponseEntity<?> deletePlanWorkout(
            @PathVariable Long planId,
            @PathVariable Long workoutId,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Plan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found"));
            if (!plan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            PlanWorkout workout = workoutRepository.findById(workoutId)
                    .orElseThrow(() -> new RuntimeException("Workout not found"));
            workoutRepository.delete(workout);
            return ResponseEntity.ok(Map.of("message", "Workout deleted"));
        } catch (RuntimeException e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
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
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
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
        int weeks = plan.getTotalWeeks() != null ? plan.getTotalWeeks() : 8;
        int daysPerWeek = plan.getDaysPerWeek() != null ? plan.getDaysPerWeek() : 3;
        List<PlanWorkout> workouts = new ArrayList<>();
        String[] dayLabels = {"Monday", "Wednesday", "Friday", "Sunday"};
        for (int week = 0; week < weeks; week++) {
            for (int day = 0; day < daysPerWeek; day++) {
                workouts.add(PlanWorkout.builder()
                        .plan(plan)
                        .day(day + 1)
                        .weekNumber(week + 1)
                        .title(dayLabels[day % dayLabels.length])
                        .description(buildWorkoutDescription(sport, week, day, level))
                        .workoutType(day == daysPerWeek - 1 ? "LONG_RUN" : day % 2 == 0 ? "EASY" : "TEMPO")
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

    // ── Phase & Theme helpers ─────────────────────────

    private String computePhase(int weekNumber, int totalWeeks) {
        int baseWeeks = (int) Math.ceil(totalWeeks * 0.40);
        int buildWeeks = (int) Math.ceil(totalWeeks * 0.35);
        int peakWeeks = (int) Math.ceil(totalWeeks * 0.15);
        if (weekNumber <= baseWeeks) return "BASE";
        if (weekNumber <= baseWeeks + buildWeeks) return "BUILD";
        if (weekNumber <= baseWeeks + buildWeeks + peakWeeks) return "PEAK";
        return "TAPER";
    }

    private String computeTheme(int weekNumber, int totalWeeks) {
        String phase = computePhase(weekNumber, totalWeeks);
        return switch (phase) {
            case "BASE"  -> weekNumber % 4 == 0 ? "Recovery" : "Base Building";
            case "BUILD" -> weekNumber % 4 == 0 ? "Recovery" : "Build Phase";
            case "PEAK"  -> "Peak Week";
            case "TAPER" -> weekNumber == totalWeeks ? "Race Week" : "Taper";
            default      -> "Training Week";
        };
    }

    private String mapWorkoutType(String workoutType) {
        if (workoutType == null) return "Easy Run";
        return switch (workoutType) {
            case "EASY"     -> "Easy Run";
            case "TEMPO"    -> "Tempo Run";
            case "INTERVAL" -> "Interval";
            case "LONG_RUN" -> "Long Run";
            case "RECOVERY" -> "Recovery Run";
            case "REST"     -> "Rest";
            default         -> workoutType;
        };
    }

    // ── toMap ────────────────────────────────────────

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
        map.put("startDate", plan.getStartDate() != null ? plan.getStartDate().toString() : null);
        map.put("targetRaceDate", plan.getTargetRaceDate() != null ? plan.getTargetRaceDate().toString() : null);
        map.put("currentWeeklyMeters", plan.getCurrentWeeklyMeters());
        map.put("targetSeconds", plan.getTargetSeconds());
        map.put("createdAt", plan.getCreatedAt());

        if (plan.getWorkouts() != null && !plan.getWorkouts().isEmpty()) {
            int totalWeeks = plan.getTotalWeeks() != null ? plan.getTotalWeeks() : 1;

            // Group by weekNumber (fall back to day-based grouping for legacy data)
            Map<Integer, List<PlanWorkout>> byWeek = new LinkedHashMap<>();
            for (PlanWorkout w : plan.getWorkouts()) {
                int wn = w.getWeekNumber() != null ? w.getWeekNumber()
                        : (w.getDay() != null ? ((w.getDay() - 1) / 7) + 1 : 1);
                byWeek.computeIfAbsent(wn, k -> new ArrayList<>()).add(w);
            }

            List<Map<String, Object>> weeks = byWeek.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        int wn = entry.getKey();
                        Map<String, Object> weekMap = new HashMap<>();
                        weekMap.put("weekNumber", wn);
                        weekMap.put("theme", computeTheme(wn, totalWeeks));
                        weekMap.put("phase", computePhase(wn, totalWeeks));

                        List<Map<String, Object>> wos = entry.getValue().stream()
                                .sorted(Comparator.comparingInt(w -> w.getDay() != null ? w.getDay() : 0))
                                .map(w -> {
                                    Map<String, Object> wm = new HashMap<>();
                                    wm.put("id", w.getId());
                                    wm.put("dayLabel", w.getTitle()); // title stores dayLabel
                                    wm.put("type", mapWorkoutType(w.getWorkoutType()));
                                    wm.put("workoutType", w.getWorkoutType());
                                    wm.put("description", w.getDescription());
                                    wm.put("durationMinutes", w.getDurationMinutes());
                                    wm.put("distanceMeters", w.getDistanceMeters());
                                    wm.put("targetPaceSeconds", w.getTargetPaceSeconds());
                                    wm.put("completed", w.isCompleted());
                                    return wm;
                                }).collect(Collectors.toList());

                        weekMap.put("workouts", wos);
                        return weekMap;
                    }).collect(Collectors.toList());

            map.put("weeks", weeks);
        } else {
            map.put("weeks", List.of());
        }

        return map;
    }
}
