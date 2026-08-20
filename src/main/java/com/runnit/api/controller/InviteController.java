package com.runnit.api.controller;

import com.runnit.api.model.User;
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

    /** GET /api/invite/{code} — public preview so the join page can show who invited you. */
    @GetMapping("/api/invite/{code}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> previewInvite(@PathVariable String code) {
        return userRepository.findByInviteCode(code)
                .map(inviter -> {
                    Map<String, Object> preview = new HashMap<>();
                    preview.put("displayName", inviter.getDisplayName());
                    preview.put("avatarUrl", inviter.getAvatarUrl());
                    return ResponseEntity.ok((Object) preview);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Invite link not found")));
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
