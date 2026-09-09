package com.runnit.api.controller;

import com.runnit.api.model.InviteEvent;
import com.runnit.api.model.User;
import com.runnit.api.repository.InviteEventRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Personal "invite a friend" link — reuses the same User.inviteCode field the coach
 * roster invite (CoachController) already uses. The code is just an opaque lookup key;
 * which flow it drives depends on which frontend route/accept-endpoint consumes it
 * (/join-coach/{code} vs /join/{code} here), so one code can safely serve both.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InviteController {

    private final UserRepository userRepository;
    private final FollowService followService;
    private final InviteEventRepository inviteEventRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * GET /api/invite-link — returns (generating on first call) this user's shareable
     * invite link. Deliberately NOT under /api/invite/* — that prefix is permitAll'd
     * for the public code preview below, and this endpoint must stay authenticated.
     */
    @GetMapping("/api/invite-link")
    @Transactional
    public ResponseEntity<?> getInviteLink(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getInviteCode() == null) {
                String code;
                do {
                    code = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
                } while (userRepository.findByInviteCode(code).isPresent());
                user.setInviteCode(code);
                userRepository.save(user);
            }

            return ResponseEntity.ok(Map.of(
                    "code", user.getInviteCode(),
                    "url", frontendUrl + "/join/" + user.getInviteCode()
            ));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/invite/{code} — public preview so the join page can show who invited you.
     * Also logs a VISITED event: visitor is null when the caller isn't authenticated,
     * which is the proxy for "a new person clicked this" vs. an existing user reopening
     * a link they'd already used.
     */
    @GetMapping("/api/invite/{code}")
    @Transactional
    public ResponseEntity<?> previewInvite(@PathVariable String code, Authentication auth) {
        return userRepository.findByInviteCode(code)
                .map(inviter -> {
                    User visitor = (auth != null && auth.getPrincipal() instanceof Long visitorId)
                            ? userRepository.findById(visitorId).orElse(null)
                            : null;
                    inviteEventRepository.save(InviteEvent.builder()
                            .inviter(inviter)
                            .visitor(visitor)
                            .eventType("VISITED")
                            .build());

                    Map<String, Object> preview = new HashMap<>();
                    preview.put("displayName", inviter.getDisplayName());
                    preview.put("avatarUrl", inviter.getAvatarUrl());
                    return ResponseEntity.ok((Object) preview);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Invite link not found")));
    }

    /** POST /api/invite/copied — authenticated user recorded copying their own invite link. */
    @PostMapping("/api/invite/copied")
    @Transactional
    public ResponseEntity<?> logInviteCopied(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User inviter = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            inviteEventRepository.save(InviteEvent.builder()
                    .inviter(inviter)
                    .eventType("COPIED")
                    .build());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/invite-stats — copy/visit counts for the current user's own invite link.
     * Deliberately NOT under /api/invite/* — same reason as /api/invite-link above: that
     * prefix is permitAll'd (GET) for the public code preview, and "stats" is a single path
     * segment that would otherwise match it and slip past authentication.
     */
    @GetMapping("/api/invite-stats")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getInviteStats(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            long copied = inviteEventRepository.countByInviter_IdAndEventType(userId, "COPIED");
            long visited = inviteEventRepository.countByInviter_IdAndEventType(userId, "VISITED");
            long visitedByNewUsers = inviteEventRepository.countByInviter_IdAndEventTypeAndVisitorIsNull(userId, "VISITED");

            Map<String, Object> stats = new HashMap<>();
            stats.put("copied", copied);
            stats.put("visited", visited);
            stats.put("visitedByNewUsers", visitedByNewUsers);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/invite/{code}/accept — authenticated user follows whoever sent the link. */
    @PostMapping("/api/invite/{code}/accept")
    @Transactional
    public ResponseEntity<?> acceptInvite(@PathVariable String code, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User inviter = userRepository.findByInviteCode(code)
                    .orElseThrow(() -> new RuntimeException("Invite link not found"));

            if (inviter.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "That's your own invite link"));
            }
            if (!followService.isFollowing(userId, inviter.getId())) {
                followService.followUser(userId, inviter.getId());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("followed", true);
            result.put("displayName", inviter.getDisplayName());
            result.put("avatarUrl", inviter.getAvatarUrl());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
