package com.runnit.api.controller;

import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/multisport-events")
@RequiredArgsConstructor
public class MultisportEventController {

    private final MultisportEventRepository eventRepo;
    private final ActivityRepository activityRepo;
    private final UserRepository userRepo;

    // ── List user's events ───────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listEvents(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<MultisportEvent> events = eventRepo.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(events.stream().map(this::toSummaryMap).collect(Collectors.toList()));
    }

    // ── Get single event with full segments ──────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable Long id, Authentication auth) {
        return eventRepo.findById(id)
            .map(e -> ResponseEntity.ok(toDetailMap(e)))
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Check if an activity belongs to any event ────────────────────────
    @GetMapping("/by-activity/{activityId}")
    public ResponseEntity<?> getByActivity(@PathVariable Long activityId) {
        return eventRepo.findByActivityId(activityId)
            .map(e -> ResponseEntity.ok(toSummaryMap(e)))
            .orElse(ResponseEntity.noContent().build());
    }

    // ── Create event ─────────────────────────────────────────────────────
    @PostMapping
    @Transactional
    public ResponseEntity<?> createEvent(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            MultisportEvent event = new MultisportEvent();
            event.setUser(user);
            event.setName((String) body.getOrDefault("name", "My Event"));
            event.setEventType((String) body.getOrDefault("eventType", "TRIATHLON"));
            event.setNotes((String) body.get("notes"));
            event.setIsPublic(body.get("isPublic") == null || (Boolean) body.get("isPublic"));

            String dateStr = (String) body.get("eventDate");
            if (dateStr != null && !dateStr.isBlank()) {
                event.setEventDate(LocalDate.parse(dateStr));
            }

            event = eventRepo.save(event);

            // Attach segments
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments = (List<Map<String, Object>>) body.get("segments");
            if (segments != null) {
                attachSegments(event, segments);
            }

            return ResponseEntity.ok(toDetailMap(event));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Update name / notes / segments ───────────────────────────────────
    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return eventRepo.findById(id).map(event -> {
            if (!event.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            if (body.containsKey("name"))      event.setName((String) body.get("name"));
            if (body.containsKey("notes"))     event.setNotes((String) body.get("notes"));
            if (body.containsKey("eventType")) event.setEventType((String) body.get("eventType"));
            if (body.containsKey("isPublic"))  event.setIsPublic((Boolean) body.get("isPublic"));

            String dateStr = (String) body.get("eventDate");
            if (dateStr != null && !dateStr.isBlank()) {
                event.setEventDate(LocalDate.parse(dateStr));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments = (List<Map<String, Object>>) body.get("segments");
            if (segments != null) {
                event.getSegments().clear();
                attachSegments(event, segments);
            }

            eventRepo.save(event);
            return ResponseEntity.ok(toDetailMap(event));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Delete event ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return eventRepo.findById(id).map(event -> {
            if (!event.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            eventRepo.delete(event);
            return ResponseEntity.ok(Map.of("message", "Event deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void attachSegments(MultisportEvent event, List<Map<String, Object>> segments) {
        for (int i = 0; i < segments.size(); i++) {
            Map<String, Object> seg = segments.get(i);
            Long actId = ((Number) seg.get("activityId")).longValue();
            activityRepo.findById(actId).ifPresent(activity -> {
                MultisportEventActivity mea = new MultisportEventActivity();
                mea.setEvent(event);
                mea.setActivity(activity);
                mea.setSequenceOrder(((Number) seg.getOrDefault("order", segments.indexOf(seg))).intValue());
                mea.setSegmentLabel((String) seg.get("label"));
                event.getSegments().add(mea);
            });
        }
    }

    private Map<String, Object> toSummaryMap(MultisportEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         e.getId());
        m.put("name",       e.getName());
        m.put("eventType",  e.getEventType());
        m.put("eventDate",  e.getEventDate());
        m.put("isPublic",   e.getIsPublic());
        m.put("createdAt",  e.getCreatedAt());
        m.put("segmentCount", e.getSegments().size());

        // Combined totals from segments
        int totalSec = 0; long totalM = 0;
        for (MultisportEventActivity seg : e.getSegments()) {
            Activity a = seg.getActivity();
            if (a.getDurationSeconds() != null) totalSec += a.getDurationSeconds();
            if (a.getDistanceMeters()  != null) totalM   += a.getDistanceMeters();
        }
        m.put("totalDurationSeconds", totalSec);
        m.put("totalDistanceMeters",  totalM);
        return m;
    }

    private Map<String, Object> toDetailMap(MultisportEvent e) {
        Map<String, Object> m = toSummaryMap(e);
        m.put("notes", e.getNotes());
        m.put("userId", e.getUser().getId());
        m.put("userDisplayName", e.getUser().getDisplayName());
        m.put("userAvatarUrl",   e.getUser().getAvatarUrl());

        List<Map<String, Object>> segs = new ArrayList<>();
        for (MultisportEventActivity seg : e.getSegments()) {
            Activity a = seg.getActivity();
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id",              seg.getId());
            s.put("activityId",      a.getId());
            s.put("order",           seg.getSequenceOrder());
            s.put("label",           seg.getSegmentLabel());
            s.put("sportType",       a.getSportType());
            s.put("durationSeconds", a.getDurationSeconds());
            s.put("distanceMeters",  a.getDistanceMeters());
            s.put("elevationGain",   a.getElevationGain());
            s.put("averagePace",     a.getAveragePace());
            s.put("averageHeartRate",a.getAverageHeartRate());
            segs.add(s);
        }
        m.put("segments", segs);
        return m;
    }
}
