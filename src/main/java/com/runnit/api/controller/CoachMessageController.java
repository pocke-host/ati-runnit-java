package com.runnit.api.controller;

import com.runnit.api.model.CoachMessage;
import com.runnit.api.repository.CoachMessageRepository;
import com.runnit.api.repository.CoachRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coach/messages")
@RequiredArgsConstructor
public class CoachMessageController {

    private final CoachMessageRepository coachMessageRepository;
    private final CoachRequestRepository coachRequestRepository;

    /**
     * GET /api/coach/messages/{athleteId}
     * Fetch thread — accessible by both coach and athlete.
     * Coach calls with their athlete's id; athlete calls with their own id.
     */
    @GetMapping("/{athleteId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getThread(@PathVariable Long athleteId, Authentication auth) {
        try {
            Long callerId = (Long) auth.getPrincipal();
            Long coachId = resolveCoachId(callerId, athleteId);
            if (coachId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "No approved coaching relationship"));
            }
            List<Map<String, Object>> messages = coachMessageRepository
                    .findByCoachIdAndAthleteIdOrderByCreatedAtAsc(coachId, athleteId)
                    .stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/coach/messages/{athleteId}
     * Send a message in the thread.
     */
    @PostMapping("/{athleteId}")
    @Transactional
    public ResponseEntity<?> sendMessage(
            @PathVariable Long athleteId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long callerId = (Long) auth.getPrincipal();
            Long coachId = resolveCoachId(callerId, athleteId);
            if (coachId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "No approved coaching relationship"));
            }
            String msgBody = (String) body.get("body");
            if (msgBody == null || msgBody.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message body required"));
            }
            CoachMessage msg = new CoachMessage();
            msg.setCoachId(coachId);
            msg.setAthleteId(athleteId);
            msg.setSenderId(callerId);
            msg.setBody(msgBody.trim());
            msg = coachMessageRepository.save(msg);
            return ResponseEntity.ok(toMap(msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/coach/messages/{athleteId}/read
     * Mark all unread messages (not sent by caller) as read.
     */
    @PatchMapping("/{athleteId}/read")
    @Transactional
    public ResponseEntity<?> markRead(@PathVariable Long athleteId, Authentication auth) {
        try {
            Long callerId = (Long) auth.getPrincipal();
            Long coachId = resolveCoachId(callerId, athleteId);
            if (coachId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "No approved coaching relationship"));
            }
            List<CoachMessage> unread = coachMessageRepository
                    .findByCoachIdAndAthleteIdAndSenderIdNotAndIsReadFalse(coachId, athleteId, callerId);
            for (CoachMessage m : unread) m.setRead(true);
            coachMessageRepository.saveAll(unread);
            return ResponseEntity.ok(Map.of("marked", unread.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/coach/messages/unread-count
     * Total unread messages for the caller across all threads.
     */
    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public ResponseEntity<?> unreadCount(Authentication auth) {
        try {
            Long callerId = (Long) auth.getPrincipal();
            // Find all threads where caller is involved and count unread (not sent by caller)
            long count = coachMessageRepository.findAll().stream()
                    .filter(m -> (m.getCoachId().equals(callerId) || m.getAthleteId().equals(callerId))
                            && !m.getSenderId().equals(callerId)
                            && !m.isRead())
                    .count();
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * Resolve the coachId for the thread.
     * - If caller is the coach for athleteId → return callerId
     * - If caller is the athlete (callerId == athleteId) → look up their approved coach
     */
    private Long resolveCoachId(Long callerId, Long athleteId) {
        if (callerId.equals(athleteId)) {
            // Caller is the athlete — find their approved coach
            return coachRequestRepository.findByAthleteIdAndStatus(athleteId, "APPROVED")
                    .map(r -> r.getCoachId())
                    .orElse(null);
        }
        // Caller might be the coach — verify approved relationship exists
        boolean isCoach = coachRequestRepository
                .findByCoachIdAndAthleteId(callerId, athleteId)
                .map(r -> "APPROVED".equals(r.getStatus()))
                .orElse(false);
        if (isCoach) return callerId;

        // Caller might be the athlete (different from athleteId path param not matching)
        // Check if caller is the athlete in the relationship
        return coachRequestRepository.findByAthleteIdAndStatus(callerId, "APPROVED")
                .filter(r -> r.getAthleteId().equals(callerId))
                .map(r -> r.getCoachId())
                .orElse(null);
    }

    private Map<String, Object> toMap(CoachMessage msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", msg.getId());
        map.put("coachId", msg.getCoachId());
        map.put("athleteId", msg.getAthleteId());
        map.put("senderId", msg.getSenderId());
        map.put("body", msg.getBody());
        map.put("isRead", msg.isRead());
        map.put("createdAt", msg.getCreatedAt());
        return map;
    }
}
