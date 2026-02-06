// ========== ReactionController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.ReactionRequest;
import com.runnit.api.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/moments/{momentId}/reaction")
@RequiredArgsConstructor
public class ReactionController {
    
    private final ReactionService reactionService;
    
    @PostMapping
    public ResponseEntity<?> addReaction(
            @PathVariable Long momentId,
            @Valid @RequestBody ReactionRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            reactionService.addOrUpdateReaction(userId, momentId, request.getType());
            return ResponseEntity.ok(Map.of("message", "Reaction added"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @DeleteMapping
    public ResponseEntity<?> removeReaction(
            @PathVariable Long momentId,
            Authentication auth) {
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