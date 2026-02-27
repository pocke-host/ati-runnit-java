package com.runnit.api.controller;

import com.runnit.api.dto.MentorshipRequest;
import com.runnit.api.service.MentorshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mentorship")
@RequiredArgsConstructor
public class MentorshipController {

    private final MentorshipService mentorshipService;

    @PostMapping("/request")
    public ResponseEntity<?> requestMentorship(@RequestBody MentorshipRequest request, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(mentorshipService.requestMentorship(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{matchId}/respond")
    public ResponseEntity<?> respond(
            @PathVariable Long matchId,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(mentorshipService.respondToRequest(matchId, userId, status, notes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyMentorships(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(mentorshipService.getMyMentorships(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mentees")
    public ResponseEntity<?> getMyMentees(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(mentorshipService.getMyMentees(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mentors")
    public ResponseEntity<?> findMentors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(mentorshipService.findAvailableMentors(userId, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/availability")
    public ResponseEntity<?> setAvailability(@RequestParam boolean available, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            mentorshipService.setMentorAvailable(userId, available);
            return ResponseEntity.ok(Map.of("mentorAvailable", available));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
