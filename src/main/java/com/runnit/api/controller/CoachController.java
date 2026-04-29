package com.runnit.api.controller;

import com.runnit.api.model.Activity;
import com.runnit.api.model.CoachRequest;
import com.runnit.api.model.RaceBookmark;
import com.runnit.api.model.User;
import com.runnit.api.model.WorkoutEvent;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.CoachRequestRepository;
import com.runnit.api.repository.RaceBookmarkRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.WorkoutEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles coach-side endpoints (/api/coach/*) and
 * athlete-side endpoints (/api/coaches/*, /api/athlete/*).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CoachController {

    private final CoachRequestRepository coachRequestRepository;
    private final UserRepository userRepository;
    private final WorkoutEventRepository workoutEventRepository;
    private final ActivityRepository activityRepository;
    private final RaceBookmarkRepository raceBookmarkRepository;

    // ─── Coach perspective ───────────────────────────────────────────────────

    /** GET /api/coach/athletes — athletes with APPROVED status */
    @GetMapping("/api/coach/athletes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyAthletes(Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            List<Long> athleteIds = coachRequestRepository
                    .findByCoachIdAndStatus(coachId, "APPROVED").stream()
                    .map(CoachRequest::getAthleteId)
                    .collect(Collectors.toList());
            List<Map<String, Object>> athletes = userRepository.findAllById(athleteIds).stream()
                    .map(this::toUserMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(athletes);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/coach/athletes/requests — pending coaching requests */
    @GetMapping("/api/coach/athletes/requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPendingRequests(Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            List<CoachRequest> requests = coachRequestRepository
                    .findByCoachIdAndStatus(coachId, "PENDING");
            List<Long> athleteIds = requests.stream()
                    .map(CoachRequest::getAthleteId).collect(Collectors.toList());
            Map<Long, User> userMap = userRepository.findAllById(athleteIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            List<Map<String, Object>> result = requests.stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getId());
                map.put("status", r.getStatus());
                map.put("createdAt", r.getCreatedAt());
                User athlete = userMap.get(r.getAthleteId());
                if (athlete != null) map.put("athlete", toUserMap(athlete));
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PATCH /api/coach/athletes/requests/{reqId}/approve */
    @PatchMapping("/api/coach/athletes/requests/{reqId}/approve")
    @Transactional
    public ResponseEntity<?> approveRequest(@PathVariable Long reqId, Authentication auth) {
        return updateRequestStatus(reqId, "APPROVED", (Long) auth.getPrincipal());
    }

    /** PATCH /api/coach/athletes/requests/{reqId}/reject */
    @PatchMapping("/api/coach/athletes/requests/{reqId}/reject")
    @Transactional
    public ResponseEntity<?> rejectRequest(@PathVariable Long reqId, Authentication auth) {
        return updateRequestStatus(reqId, "REJECTED", (Long) auth.getPrincipal());
    }

    /** DELETE /api/coach/athletes/{athleteId} — remove athlete from coaching */
    @DeleteMapping("/api/coach/athletes/{athleteId}")
    @Transactional
    public ResponseEntity<?> removeAthlete(@PathVariable Long athleteId, Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            coachRequestRepository.deleteByCoachIdAndAthleteId(coachId, athleteId);
            return ResponseEntity.ok(Map.of("message", "Athlete removed"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Athlete perspective ─────────────────────────────────────────────────

    /** GET /api/coaches — list all users with role=coach */
    @GetMapping("/api/coaches")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listCoaches(Authentication auth) {
        try {
            List<Map<String, Object>> coaches = userRepository.findByRole("coach").stream()
                    .map(this::toUserMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(coaches);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/coaches/{coachId}/requests — send a coaching request */
    @PostMapping("/api/coaches/{coachId}/requests")
    @Transactional
    public ResponseEntity<?> sendRequest(@PathVariable Long coachId, Authentication auth) {
        try {
            Long athleteId = (Long) auth.getPrincipal();
            if (!userRepository.existsById(coachId)) {
                return ResponseEntity.status(404).body(Map.of("error", "Coach not found"));
            }
            if (coachRequestRepository.existsByCoachIdAndAthleteIdAndStatus(coachId, athleteId, "PENDING")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Request already sent"));
            }
            CoachRequest req = new CoachRequest();
            req.setCoachId(coachId);
            req.setAthleteId(athleteId);
            req.setStatus("PENDING");
            req = coachRequestRepository.save(req);
            return ResponseEntity.ok(Map.of("id", req.getId(), "status", req.getStatus()));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/coaches/{coachId}/requests — cancel pending request */
    @DeleteMapping("/api/coaches/{coachId}/requests")
    @Transactional
    public ResponseEntity<?> cancelRequest(@PathVariable Long coachId, Authentication auth) {
        try {
            Long athleteId = (Long) auth.getPrincipal();
            coachRequestRepository.deleteByCoachIdAndAthleteId(coachId, athleteId);
            return ResponseEntity.ok(Map.of("message", "Request cancelled"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/athlete/coach — get current athlete's approved coach */
    @GetMapping("/api/athlete/coach")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyCoach(Authentication auth) {
        try {
            Long athleteId = (Long) auth.getPrincipal();
            return coachRequestRepository.findByAthleteIdAndStatus(athleteId, "APPROVED")
                    .flatMap(r -> userRepository.findById(r.getCoachId()))
                    .map(coach -> ResponseEntity.ok((Object) toUserMap(coach)))
                    .orElse(ResponseEntity.ok(null));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Coach-athlete calendar ───────────────────────────────────────────────

    /**
     * GET /api/coach/athletes/{athleteId}/calendar?start=YYYY-MM-DD&end=YYYY-MM-DD
     * Returns an athlete's workout events within the date range.
     */
    @GetMapping("/api/coach/athletes/{athleteId}/calendar")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAthleteCalendar(
            @PathVariable Long athleteId,
            @RequestParam String start,
            @RequestParam String end,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            List<WorkoutEvent> events = workoutEventRepository
                    .findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(athleteId, startDate, endDate);
            List<Map<String, Object>> result = events.stream().map(this::toEventMap).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/coach/athletes/{athleteId}/calendar
     * Coach creates a workout event on the athlete's calendar.
     */
    @PostMapping("/api/coach/athletes/{athleteId}/calendar")
    @Transactional
    public ResponseEntity<?> createAthleteCalendarEvent(
            @PathVariable Long athleteId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            User athlete = userRepository.findById(athleteId)
                    .orElseThrow(() -> new RuntimeException("Athlete not found"));
            WorkoutEvent event = new WorkoutEvent();
            event.setUser(athlete);
            event.setPlannedDate(LocalDate.parse((String) body.get("plannedDate")));
            event.setTitle((String) body.getOrDefault("title", "Workout"));
            event.setDescription((String) body.get("description"));
            event.setWorkoutType((String) body.get("workoutType"));
            if (body.containsKey("distanceMeters") && body.get("distanceMeters") != null) {
                event.setDistanceMeters(((Number) body.get("distanceMeters")).intValue());
            }
            if (body.containsKey("durationMinutes") && body.get("durationMinutes") != null) {
                event.setDurationMinutes(((Number) body.get("durationMinutes")).intValue());
            }
            if (body.containsKey("targetPaceSeconds") && body.get("targetPaceSeconds") != null) {
                event.setTargetPaceSeconds(((Number) body.get("targetPaceSeconds")).intValue());
            }
            event.setNotes((String) body.get("notes"));
            event.setSource("COACH");
            workoutEventRepository.save(event);
            return ResponseEntity.ok(toEventMap(event));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/coach/athletes/{athleteId}/calendar/{eventId}
     * Coach updates a workout event on the athlete's calendar.
     */
    @PatchMapping("/api/coach/athletes/{athleteId}/calendar/{eventId}")
    @Transactional
    public ResponseEntity<?> updateAthleteCalendarEvent(
            @PathVariable Long athleteId,
            @PathVariable Long eventId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            WorkoutEvent event = workoutEventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            if (!event.getUser().getId().equals(athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Event does not belong to this athlete"));
            }
            if (body.containsKey("plannedDate") && body.get("plannedDate") != null) {
                event.setPlannedDate(LocalDate.parse((String) body.get("plannedDate")));
            }
            if (body.containsKey("title") && body.get("title") != null) {
                event.setTitle((String) body.get("title"));
            }
            if (body.containsKey("description")) {
                event.setDescription((String) body.get("description"));
            }
            if (body.containsKey("workoutType")) {
                event.setWorkoutType((String) body.get("workoutType"));
            }
            if (body.containsKey("distanceMeters") && body.get("distanceMeters") != null) {
                event.setDistanceMeters(((Number) body.get("distanceMeters")).intValue());
            }
            if (body.containsKey("durationMinutes") && body.get("durationMinutes") != null) {
                event.setDurationMinutes(((Number) body.get("durationMinutes")).intValue());
            }
            if (body.containsKey("targetPaceSeconds") && body.get("targetPaceSeconds") != null) {
                event.setTargetPaceSeconds(((Number) body.get("targetPaceSeconds")).intValue());
            }
            if (body.containsKey("notes")) {
                event.setNotes((String) body.get("notes"));
            }
            if (body.containsKey("completed")) {
                event.setCompleted(Boolean.TRUE.equals(body.get("completed")));
            }
            workoutEventRepository.save(event);
            return ResponseEntity.ok(toEventMap(event));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/coach/athletes/{athleteId}/calendar/{eventId}
     * Coach removes a workout event from the athlete's calendar.
     */
    @DeleteMapping("/api/coach/athletes/{athleteId}/calendar/{eventId}")
    @Transactional
    public ResponseEntity<?> deleteAthleteCalendarEvent(
            @PathVariable Long athleteId,
            @PathVariable Long eventId,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            WorkoutEvent event = workoutEventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            if (!event.getUser().getId().equals(athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Event does not belong to this athlete"));
            }
            workoutEventRepository.delete(event);
            return ResponseEntity.ok(Map.of("message", "Event deleted"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Coach-athlete compliance ─────────────────────────────────────────────

    /**
     * GET /api/coach/athletes/{athleteId}/compliance?weeks=4
     * Returns week-by-week compliance: planned vs completed workout events.
     */
    @GetMapping("/api/coach/athletes/{athleteId}/compliance")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAthleteCompliance(
            @PathVariable Long athleteId,
            @RequestParam(defaultValue = "4") int weeks,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            LocalDate today = LocalDate.now();
            LocalDate rangeStart = today.minusWeeks(weeks);

            List<WorkoutEvent> events = workoutEventRepository
                    .findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(athleteId, rangeStart, today);

            // Group by ISO week number and calculate completion rate per week
            WeekFields weekFields = WeekFields.ISO;
            Map<String, long[]> weekStats = new LinkedHashMap<>();
            for (WorkoutEvent e : events) {
                String weekKey = e.getPlannedDate().getYear() + "-W"
                        + String.format("%02d", e.getPlannedDate().get(weekFields.weekOfWeekBasedYear()));
                weekStats.computeIfAbsent(weekKey, k -> new long[]{0, 0});
                weekStats.get(weekKey)[0]++; // total
                if (e.isCompleted()) weekStats.get(weekKey)[1]++; // completed
            }

            List<Map<String, Object>> weeklyBreakdown = weekStats.entrySet().stream().map(entry -> {
                long total = entry.getValue()[0];
                long completed = entry.getValue()[1];
                Map<String, Object> row = new HashMap<>();
                row.put("week", entry.getKey());
                row.put("planned", total);
                row.put("completed", completed);
                row.put("rate", total > 0 ? Math.round((completed * 100.0) / total) : 0);
                return row;
            }).toList();

            long totalPlanned = weeklyBreakdown.stream().mapToLong(r -> ((Number) r.get("planned")).longValue()).sum();
            long totalCompleted = weeklyBreakdown.stream().mapToLong(r -> ((Number) r.get("completed")).longValue()).sum();

            Map<String, Object> result = new HashMap<>();
            result.put("weeks", weeklyBreakdown);
            result.put("totalPlanned", totalPlanned);
            result.put("totalCompleted", totalCompleted);
            result.put("overallRate", totalPlanned > 0 ? Math.round((totalCompleted * 100.0) / totalPlanned) : 0);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Coach-athlete HR zones ───────────────────────────────────────────────

    /**
     * GET /api/coach/athletes/{athleteId}/zones
     * Returns the athlete's HR zones (stored as JSON on the user record).
     */
    @GetMapping("/api/coach/athletes/{athleteId}/zones")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAthleteZones(@PathVariable Long athleteId, Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            User athlete = userRepository.findById(athleteId)
                    .orElseThrow(() -> new RuntimeException("Athlete not found"));
            // Return raw JSON string — frontend parses it
            String zonesJson = athlete.getHrZonesJson();
            return ResponseEntity.ok(Map.of("zones", zonesJson != null ? zonesJson : "[]"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/coach/athletes/{athleteId}/zones
     * Coach saves HR zones for an athlete (stored as raw JSON string).
     * Body: { "zones": "[{...},{...}]" } or { "zones": [...] }
     */
    @PutMapping("/api/coach/athletes/{athleteId}/zones")
    @Transactional
    public ResponseEntity<?> updateAthleteZones(
            @PathVariable Long athleteId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            User athlete = userRepository.findById(athleteId)
                    .orElseThrow(() -> new RuntimeException("Athlete not found"));
            Object zonesValue = body.get("zones");
            if (zonesValue == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "zones is required"));
            }
            // Accept either a pre-serialized JSON string or a raw object (serialize to string)
            String zonesJson = zonesValue instanceof String s ? s : zonesValue.toString();
            athlete.setHrZonesJson(zonesJson);
            userRepository.save(athlete);
            return ResponseEntity.ok(Map.of("zones", zonesJson));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Coach-athlete races ──────────────────────────────────────────────────

    /**
     * GET /api/coach/athletes/{athleteId}/races
     * Returns the athlete's bookmarked / target races.
     */
    @GetMapping("/api/coach/athletes/{athleteId}/races")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAthleteRaces(@PathVariable Long athleteId, Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            List<RaceBookmark> races = raceBookmarkRepository.findByUserIdOrderByRaceDateAsc(athleteId);
            List<Map<String, Object>> result = races.stream().map(this::toRaceMap).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/coach/athletes/{athleteId}/races
     * Coach adds a target race for the athlete.
     */
    @PostMapping("/api/coach/athletes/{athleteId}/races")
    @Transactional
    public ResponseEntity<?> addAthleteRace(
            @PathVariable Long athleteId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            if (body.get("raceName") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "raceName is required"));
            }
            RaceBookmark race = new RaceBookmark();
            race.setUserId(athleteId);
            race.setRaceName((String) body.get("raceName"));
            if (body.containsKey("raceDate") && body.get("raceDate") != null) {
                race.setRaceDate(LocalDate.parse((String) body.get("raceDate")));
            }
            race.setRaceType((String) body.get("raceType"));
            race.setCity((String) body.get("city"));
            race.setState((String) body.get("state"));
            race.setRaceUrl((String) body.get("raceUrl"));
            race.setExternalRaceId((String) body.get("externalRaceId"));
            raceBookmarkRepository.save(race);
            return ResponseEntity.ok(toRaceMap(race));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/coach/athletes/{athleteId}/races/{raceId}
     * Coach removes a target race for the athlete.
     */
    @DeleteMapping("/api/coach/athletes/{athleteId}/races/{raceId}")
    @Transactional
    public ResponseEntity<?> deleteAthleteRace(
            @PathVariable Long athleteId,
            @PathVariable Long raceId,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            RaceBookmark race = raceBookmarkRepository.findById(raceId)
                    .orElseThrow(() -> new RuntimeException("Race not found"));
            if (!race.getUserId().equals(athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Race does not belong to this athlete"));
            }
            raceBookmarkRepository.delete(race);
            return ResponseEntity.ok(Map.of("message", "Race deleted"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Coach activity annotation ────────────────────────────────────────────

    /**
     * POST /api/coach/activities/{activityId}/annotation
     * Coach adds or replaces a written annotation on an athlete's activity.
     * Body: { "annotation": "Great pacing, work on cadence next time." }
     */
    @PostMapping("/api/coach/activities/{activityId}/annotation")
    @Transactional
    public ResponseEntity<?> annotateActivity(
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long coachId = (Long) auth.getPrincipal();
            Activity activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));
            // Verify the coach is approved for this athlete
            Long athleteId = activity.getUser().getId();
            if (!isApprovedCoachAthlete(coachId, athleteId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized for this athlete"));
            }
            String annotation = body.containsKey("annotation") ? (String) body.get("annotation") : "";
            activity.setCoachAnnotation(annotation);
            activityRepository.save(activity);
            return ResponseEntity.ok(Map.of("activityId", activityId, "annotation", annotation));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ResponseEntity<?> updateRequestStatus(Long reqId, String status, Long coachId) {
        try {
            CoachRequest req = coachRequestRepository.findById(reqId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));
            if (!req.getCoachId().equals(coachId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            req.setStatus(status);
            coachRequestRepository.save(req);
            return ResponseEntity.ok(Map.of("id", req.getId(), "status", req.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Returns true if coachId has an APPROVED relationship with athleteId. */
    private boolean isApprovedCoachAthlete(Long coachId, Long athleteId) {
        return coachRequestRepository.existsByCoachIdAndAthleteIdAndStatus(coachId, athleteId, "APPROVED");
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("displayName", user.getDisplayName());
        map.put("email", user.getEmail());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("sport", user.getSport());
        map.put("role", user.getRole());
        return map;
    }

    private Map<String, Object> toEventMap(WorkoutEvent e) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", e.getId());
        map.put("plannedDate", e.getPlannedDate() != null ? e.getPlannedDate().toString() : null);
        map.put("title", e.getTitle());
        map.put("description", e.getDescription());
        map.put("workoutType", e.getWorkoutType());
        map.put("distanceMeters", e.getDistanceMeters());
        map.put("durationMinutes", e.getDurationMinutes());
        map.put("targetPaceSeconds", e.getTargetPaceSeconds());
        map.put("notes", e.getNotes());
        map.put("source", e.getSource());
        map.put("completed", e.isCompleted());
        return map;
    }

    private Map<String, Object> toRaceMap(RaceBookmark r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("raceName", r.getRaceName());
        map.put("raceDate", r.getRaceDate() != null ? r.getRaceDate().toString() : null);
        map.put("raceType", r.getRaceType());
        map.put("city", r.getCity());
        map.put("state", r.getState());
        map.put("raceUrl", r.getRaceUrl());
        map.put("externalRaceId", r.getExternalRaceId());
        map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return map;
    }
}
