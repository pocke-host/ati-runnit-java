package com.runnit.api.controller;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.dto.CommentResponse;
import com.runnit.api.dto.FeedActivityDTO;
import com.runnit.api.dto.StrengthActivityRequest;
import com.runnit.api.model.Activity;
import com.runnit.api.model.ActivityReaction;
import com.runnit.api.model.Comment;
import com.runnit.api.model.Notification;
import com.runnit.api.model.Reaction;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityReactionRepository;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.CommentRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.WorkoutEventRepository;
import com.runnit.api.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;

@Slf4j
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityRepository activityRepository;
    private final ActivityReactionRepository activityReactionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final WorkoutEventRepository workoutEventRepository;
    private final com.runnit.api.repository.NotificationRepository notificationRepository;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_.-]{2,30})");

    @PostMapping
    public ResponseEntity<?> createActivity(
            @Valid @RequestBody ActivityRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Activity activity = activityService.createActivity(userId, request);
            // Return as DTO so frontend cache has the same shape as GET /api/activities
            return ResponseEntity.ok(FeedActivityDTO.from(activity));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/strength")
    public ResponseEntity<?> createStrengthActivity(
            @Valid @RequestBody StrengthActivityRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Activity activity = activityService.createStrengthActivity(userId, request);
            FeedActivityDTO dto = FeedActivityDTO.from(activity);
            dto.setStrengthExercises(activityService.getStrengthExercises(activity.getId()));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        try {
            Long viewerUserId = (Long) auth.getPrincipal();
            Long targetUserId = userId != null ? userId : viewerUserId;
            Page<FeedActivityDTO> activities = activityService.getUserActivities(targetUserId, page, size, viewerUserId);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<?> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(activityService.getFeed(userId, page, size));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Download the authenticated athlete's deduplicated workouts as CSV. */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<?> exportActivities(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) String anchorDate,
            @RequestParam(defaultValue = "UTC") String timezone,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            ZoneId zone;
            try {
                zone = ZoneId.of(timezone);
            } catch (DateTimeException e) {
                zone = ZoneId.of("UTC");
            }

            LocalDate anchor = anchorDate == null || anchorDate.isBlank()
                    ? LocalDate.now(zone)
                    : LocalDate.parse(anchorDate);
            LocalDate startDate;
            LocalDate endDate;
            String normalizedPeriod = period.trim().toLowerCase();
            if ("month".equals(normalizedPeriod)) {
                startDate = anchor.withDayOfMonth(1);
                endDate = startDate.plusMonths(1);
            } else if ("week".equals(normalizedPeriod)) {
                startDate = anchor.with(DayOfWeek.MONDAY);
                endDate = startDate.plusDays(7);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "period must be week or month"));
            }

            List<Activity> activities = activityRepository.findByUserIdBetween(
                    userId, startDate.atStartOfDay(), endDate.atStartOfDay());
            Map<String, Activity> unique = new LinkedHashMap<>();
            for (Activity activity : activities) {
                String source = activity.getSource() == null ? "MANUAL" : activity.getSource().name();
                String key = activity.getExternalId() == null || activity.getExternalId().isBlank()
                        ? "activity:" + activity.getId()
                        : source + ":" + activity.getExternalId();
                unique.putIfAbsent(key, activity);
            }

            StringBuilder csv = new StringBuilder("date,sport,source,duration_minutes,distance_km,calories,elevation_m,avg_hr,max_hr,avg_speed_mps,avg_cadence,listening_track,listening_artist\n");
            DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Activity activity : unique.values()) {
                LocalDateTime performedAt = activity.getPerformedAt() != null ? activity.getPerformedAt() : activity.getCreatedAt();
                csv.append(csvCell(performedAt == null ? "" : timestamp.format(performedAt))).append(',')
                        .append(csvCell(activity.getSportType() == null ? "" : activity.getSportType().name())).append(',')
                        .append(csvCell(activity.getSource() == null ? "MANUAL" : activity.getSource().name())).append(',')
                        .append(activity.getDurationSeconds() == null ? 0 : Math.max(0, activity.getDurationSeconds()) / 60.0).append(',')
                        .append(activity.getDistanceMeters() == null ? "" : String.format(java.util.Locale.US, "%.3f", activity.getDistanceMeters() / 1000.0)).append(',')
                        .append(csvCell(activity.getCalories())).append(',')
                        .append(csvCell(activity.getElevationGain())).append(',')
                        .append(csvCell(activity.getAverageHeartRate())).append(',')
                        .append(csvCell(activity.getMaxHeartRate())).append(',')
                        .append(csvCell(activity.getAveragePace())).append(',')
                        .append(csvCell(activity.getAverageCadence())).append(',')
                        .append(csvCell(activity.getListeningTrack())).append(',')
                        .append(csvCell(activity.getListeningArtist())).append('\n');
            }

            String filename = "runnit-workouts-" + normalizedPeriod + "-" + startDate + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "anchorDate must be YYYY-MM-DD"));
        } catch (Exception e) {
            log.error("Activity export failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String csvCell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    @GetMapping("/summary/weekly")
    public ResponseEntity<?> getWeeklySummary(
            @RequestParam(required = false) String weekStart,
            @RequestParam(defaultValue = "UTC") String timezone,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            ZoneId zone;
            try {
                zone = ZoneId.of(timezone);
            } catch (DateTimeException e) {
                zone = ZoneId.of("UTC");
            }

            LocalDate requestedDate = weekStart == null || weekStart.isBlank()
                    ? LocalDate.now(zone)
                    : LocalDate.parse(weekStart);
            LocalDate monday = requestedDate.with(DayOfWeek.MONDAY);
            LocalDate nextMonday = monday.plusDays(7);
            List<Activity> activities = activityRepository.findByUserIdBetween(
                    userId, monday.atStartOfDay(), nextMonday.atStartOfDay());

            // Imports are normally idempotent, but protect the summary if an older sync
            // inserted the same provider activity more than once.
            Map<String, Activity> unique = new LinkedHashMap<>();
            for (Activity activity : activities) {
                String source = activity.getSource() == null ? "MANUAL" : activity.getSource().name();
                String key = activity.getExternalId() == null || activity.getExternalId().isBlank()
                        ? "activity:" + activity.getId()
                        : source + ":" + activity.getExternalId();
                unique.putIfAbsent(key, activity);
            }

            Map<String, Integer> sportSeconds = new LinkedHashMap<>();
            Map<String, Integer> sourceSeconds = new LinkedHashMap<>();
            Map<LocalDate, Integer> dailySeconds = new LinkedHashMap<>();
            Map<LocalDate, Integer> dailyCounts = new LinkedHashMap<>();
            Map<LocalDate, Integer> dailyDistance = new LinkedHashMap<>();
            for (int i = 0; i < 7; i++) {
                dailySeconds.put(monday.plusDays(i), 0);
                dailyCounts.put(monday.plusDays(i), 0);
                dailyDistance.put(monday.plusDays(i), 0);
            }

            int totalSeconds = 0;
            int totalDistanceMeters = 0;
            for (Activity activity : unique.values()) {
                int seconds = activity.getDurationSeconds() == null ? 0 : Math.max(0, activity.getDurationSeconds());
                String sport = activity.getSportType() == null ? "OTHER" : activity.getSportType().name();
                String source = activity.getSource() == null ? "MANUAL" : activity.getSource().name();
                totalSeconds += seconds;
                int distance = activity.getDistanceMeters() == null ? 0 : Math.max(0, activity.getDistanceMeters());
                totalDistanceMeters += distance;
                sportSeconds.merge(sport, seconds, Integer::sum);
                sourceSeconds.merge(source, seconds, Integer::sum);
                LocalDate date = (activity.getPerformedAt() != null ? activity.getPerformedAt() : activity.getCreatedAt()).toLocalDate();
                dailySeconds.computeIfPresent(date, (ignored, value) -> value + seconds);
                dailyCounts.computeIfPresent(date, (ignored, value) -> value + 1);
                dailyDistance.computeIfPresent(date, (ignored, value) -> value + distance);
            }

            LocalDate previousMonday = monday.minusDays(7);
            int previousTotalSeconds = activityRepository.findByUserIdBetween(
                    userId, previousMonday.atStartOfDay(), monday.atStartOfDay()).stream()
                    .collect(Collectors.toMap(
                            activity -> {
                                String source = activity.getSource() == null ? "MANUAL" : activity.getSource().name();
                                return activity.getExternalId() == null || activity.getExternalId().isBlank()
                                        ? "activity:" + activity.getId() : source + ":" + activity.getExternalId();
                            },
                            activity -> activity.getDurationSeconds() == null ? 0 : Math.max(0, activity.getDurationSeconds()),
                            Integer::sum
                    )).values().stream().mapToInt(Integer::intValue).sum();
            int changePercent = previousTotalSeconds == 0
                    ? (totalSeconds > 0 ? 100 : 0)
                    : (int) Math.round(((totalSeconds - previousTotalSeconds) * 100.0) / previousTotalSeconds);
            var plannedWorkouts = workoutEventRepository.findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(
                    userId, monday, nextMonday.minusDays(1));
            int plannedMinutes = plannedWorkouts.stream().mapToInt(w -> w.getDurationMinutes() == null ? 0 : Math.max(0, w.getDurationMinutes())).sum();
            int completedPlannedMinutes = plannedWorkouts.stream().filter(com.runnit.api.model.WorkoutEvent::isCompleted)
                    .mapToInt(w -> w.getDurationMinutes() == null ? 0 : Math.max(0, w.getDurationMinutes())).sum();

            List<Map<String, Object>> daily = dailySeconds.keySet().stream().map(date -> {
                Map<String, Object> row = new HashMap<>();
                row.put("date", date.toString());
                row.put("durationSeconds", dailySeconds.get(date));
                row.put("activityCount", dailyCounts.get(date));
                row.put("distanceMeters", dailyDistance.get(date));
                return row;
            }).toList();
            List<Map<String, Object>> bySport = sportSeconds.entrySet().stream().map(entry -> {
                Map<String, Object> row = new HashMap<>();
                row.put("sport", entry.getKey());
                row.put("durationSeconds", entry.getValue());
                return row;
            }).toList();
            List<Map<String, Object>> bySource = sourceSeconds.entrySet().stream().map(entry -> {
                Map<String, Object> row = new HashMap<>();
                row.put("source", entry.getKey());
                row.put("durationSeconds", entry.getValue());
                return row;
            }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("weekStart", monday.toString());
            response.put("weekEnd", nextMonday.minusDays(1).toString());
            response.put("totalDurationSeconds", totalSeconds);
            response.put("totalDistanceMeters", totalDistanceMeters);
            response.put("previousTotalDurationSeconds", previousTotalSeconds);
            response.put("changePercent", changePercent);
            response.put("plannedDurationMinutes", plannedMinutes);
            response.put("completedPlannedDurationMinutes", completedPlannedMinutes);
            response.put("plannedCount", plannedWorkouts.size());
            response.put("completedPlannedCount", plannedWorkouts.stream().filter(com.runnit.api.model.WorkoutEvent::isCompleted).count());
            response.put("activityCount", unique.size());
            response.put("daily", daily);
            response.put("bySport", bySport);
            response.put("bySource", bySource);
            return ResponseEntity.ok(response);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "weekStart must be YYYY-MM-DD"));
        } catch (Exception e) {
            log.error("Weekly activity summary failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getActivity(@PathVariable Long id, Authentication auth) {
        try {
            Long viewerUserId = auth != null ? (Long) auth.getPrincipal() : null;
            Activity activity = activityService.getActivityById(id);
            FeedActivityDTO dto = FeedActivityDTO.from(activity);

            List<Long> ids = List.of(activity.getId());

            Map<String, Long> reactionCounts = new HashMap<>();
            activityReactionRepository.countGroupedByActivityIdsAndType(ids)
                    .forEach(row -> reactionCounts.put(row[1].toString(), (Long) row[2]));
            dto.setReactionCounts(reactionCounts);

            dto.setCommentCount(commentRepository.countByActivityId(id));

            if (activity.getSportType() == Activity.SportType.STRENGTH) {
                dto.setStrengthExercises(activityService.getStrengthExercises(activity.getId()));
            }

            if (viewerUserId != null) {
                java.util.Set<String> reactions = new java.util.HashSet<>();
                for (Object[] row : activityReactionRepository.findUserReactionsByActivityIds(ids, viewerUserId)) {
                    reactions.add(row[1].toString());
                }
                dto.setUserReactions(reactions);
            }

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.warn("Activity not found: id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteActivity(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Activity activity = activityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));
            if (!activity.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            activityRepository.delete(activity);
            return ResponseEntity.ok(Map.of("message", "Activity deleted"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        try {
            List<CommentResponse> comments = commentRepository.findByActivityIdOrderByCreatedAtAsc(id)
                    .stream().map(this::toCommentResponse).collect(Collectors.toList());
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/comments")
    @Transactional
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Activity activity = activityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));

            Comment comment = Comment.builder()
                    .user(user)
                    .activity(activity)
                    .content(body.get("text"))
                    .build();
            if (body.get("parentId") != null && !body.get("parentId").isBlank()) {
                comment.setParentId(Long.valueOf(body.get("parentId")));
            }
            comment.setMediaUrl(body.get("mediaUrl"));
            comment.setMediaType(body.get("mediaType"));
            comment = commentRepository.save(comment);
            notifyMentionedUsers(body.get("text"), user, activity.getId());
            return ResponseEntity.ok(toCommentResponse(comment));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/reactions")
    public ResponseEntity<?> addReaction(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Activity activity = activityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));

            String rawType = body.containsKey("type") ? body.get("type") : body.get("reactionType");
            Reaction.ReactionType type = Reaction.ReactionType.valueOf(rawType.toUpperCase());

            // LIKE and KUDOS are independent — adding one must not touch the other, so this
            // only creates a row if this exact type doesn't already exist (idempotent add,
            // not an upsert-by-user-and-activity like the old single-reaction behavior).
            ActivityReaction reaction = activityReactionRepository
                    .findByActivityIdAndUserIdAndType(id, userId, type)
                    .orElseGet(() -> activityReactionRepository.save(
                            ActivityReaction.builder().user(user).activity(activity).type(type).build()));
            return ResponseEntity.ok(Map.of("reactionType", reaction.getType().name()));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyActivities(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius,
            @RequestParam(required = false) String sport,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            Page<java.util.Map<String, Object>> results = activityRepository.findNearby(
                lat, lng, radius, sport, PageRequest.of(page, size)
            );
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<?> patchActivity(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Activity activity = activityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));
            if (!activity.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            if (body.containsKey("notes")) {
                activity.setNotes(body.get("notes"));
            }
            if (body.containsKey("listeningTrack")) activity.setListeningTrack(body.get("listeningTrack"));
            if (body.containsKey("listeningArtist")) activity.setListeningArtist(body.get("listeningArtist"));
            if (body.containsKey("listeningProvider")) activity.setListeningProvider(body.get("listeningProvider"));
            if (body.containsKey("listeningUrl")) activity.setListeningUrl(body.get("listeningUrl"));
            activityRepository.save(activity);
            return ResponseEntity.ok(FeedActivityDTO.from(activity));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/reactions")
    @Transactional
    public ResponseEntity<?> removeReaction(
            @PathVariable Long id,
            @RequestParam String type,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Reaction.ReactionType reactionType = Reaction.ReactionType.valueOf(type.toUpperCase());
            activityReactionRepository.deleteByActivityIdAndUserIdAndType(id, userId, reactionType);
            return ResponseEntity.ok(Map.of("message", "Reaction removed"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getContent())
                .parentId(comment.getParentId())
                .mediaUrl(comment.getMediaUrl())
                .mediaType(comment.getMediaType())
                .createdAt(comment.getCreatedAt())
                .user(CommentResponse.UserInfo.builder()
                        .id(comment.getUser().getId())
                        .displayName(comment.getUser().getDisplayName())
                        .avatarUrl(comment.getUser().getAvatarUrl())
                        .build())
                .build();
    }

    private void notifyMentionedUsers(String text, User actor, Long activityId) {
        if (text == null) return;
        Matcher matcher = MENTION_PATTERN.matcher(text);
        java.util.Set<Long> notified = new java.util.HashSet<>();
        while (matcher.find()) {
            userRepository.findByUserIgnoreCase(matcher.group(1)).ifPresent(mentioned -> {
                if (!mentioned.getId().equals(actor.getId()) && notified.add(mentioned.getId())) {
                    notificationRepository.save(Notification.builder()
                            .user(mentioned).actor(actor).type("MENTION")
                            .message(actor.getDisplayName() + " mentioned you in a comment")
                            .referenceId(activityId).referenceType("ACTIVITY").build());
                }
            });
        }
    }
}
