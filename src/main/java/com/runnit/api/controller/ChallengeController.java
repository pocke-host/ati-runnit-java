package com.runnit.api.controller;

import com.runnit.api.model.Challenge;
import com.runnit.api.model.ChallengeParticipant;
import com.runnit.api.model.User;
import com.runnit.api.repository.ChallengeParticipantRepository;
import com.runnit.api.repository.ChallengeRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllChallenges() {
        List<Map<String, Object>> challenges = challengeRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyChallenges(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<Long> challengeIds = participantRepository.findByUserId(userId)
                    .stream().map(ChallengeParticipant::getChallengeId).collect(Collectors.toList());
            List<Map<String, Object>> challenges = challengeRepository.findAllById(challengeIds)
                    .stream().map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(challenges);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to fetch user challenges: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/enter")
    @Transactional
    public ResponseEntity<?> enter(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            if (!challengeRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of("error", "Challenge not found"));
            }
            if (participantRepository.existsByChallengeIdAndUserId(id, userId)) {
                return ResponseEntity.status(409).body(Map.of("error", "Already entered this challenge"));
            }
            participantRepository.save(ChallengeParticipant.builder()
                    .challengeId(id).userId(userId).value(0).build());
            challengeRepository.findById(id).ifPresent(c -> {
                c.setParticipantCount(c.getParticipantCount() + 1);
                challengeRepository.save(c);
            });
            return ResponseEntity.ok(Map.of("message", "Entered challenge"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to enter challenge id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/leave")
    @Transactional
    public ResponseEntity<?> leave(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            participantRepository.deleteById(new com.runnit.api.model.ChallengeParticipantId(id, userId));
            challengeRepository.findById(id).ifPresent(c -> {
                c.setParticipantCount(Math.max(0, c.getParticipantCount() - 1));
                challengeRepository.save(c);
            });
            return ResponseEntity.ok(Map.of("message", "Left challenge"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to leave challenge id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns the leaderboard for a challenge.
     * Users are batch-loaded in a single query to avoid N+1 queries.
     */
    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<?> getLeaderboard(@PathVariable Long id) {
        try {
            List<ChallengeParticipant> participants = participantRepository.findByChallengeIdOrderByValueDesc(id);

            // Batch-load all users in one query instead of per-participant lookups
            List<Long> userIds = participants.stream()
                    .map(ChallengeParticipant::getUserId)
                    .collect(Collectors.toList());
            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            AtomicInteger rank = new AtomicInteger(1);
            List<Map<String, Object>> entries = participants.stream().map(p -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("userId", p.getUserId());
                entry.put("rank", rank.getAndIncrement());
                entry.put("value", p.getValue());
                User u = userMap.get(p.getUserId());
                if (u != null) {
                    entry.put("displayName", u.getDisplayName());
                    entry.put("avatarUrl", u.getAvatarUrl());
                }
                return entry;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("entries", entries));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to fetch leaderboard for challenge id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Challenge c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("description", c.getDescription());
        map.put("sport", c.getSport());
        map.put("imageUrl", c.getImageUrl());
        map.put("endDate", c.getEndDate());
        map.put("prize", c.getPrize());
        map.put("participantCount", c.getParticipantCount());
        return map;
    }
}
