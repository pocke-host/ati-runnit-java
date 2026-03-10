package com.runnit.api.controller;

import com.runnit.api.model.CoachRequest;
import com.runnit.api.model.User;
import com.runnit.api.repository.CoachRequestRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles coach-side endpoints (/api/coach/*) and
 * athlete-side endpoints (/api/coaches/*, /api/athlete/*).
 */
@RestController
@RequiredArgsConstructor
public class CoachController {

    private final CoachRequestRepository coachRequestRepository;
    private final UserRepository userRepository;

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
}
