package com.runnit.api.controller;

import com.runnit.api.model.Reaction;
import com.runnit.api.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class ReactionAltController {

    private final ReactionService reactionService;

    @PostMapping("/{momentId}")
    public ResponseEntity<?> addReaction(
            @PathVariable Long momentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Reaction.ReactionType type = Reaction.ReactionType.valueOf(body.get("reactionType").toUpperCase());
            reactionService.addOrUpdateReaction(userId, momentId, type);
            return ResponseEntity.ok(Map.of("message", "Reaction added"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{momentId}")
    public ResponseEntity<?> removeReaction(@PathVariable Long momentId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            reactionService.removeReaction(userId, momentId);
            return ResponseEntity.ok(Map.of("message", "Reaction removed"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
