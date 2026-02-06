// ========== MomentController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.MomentRequest;
import com.runnit.api.dto.MomentResponse;
import com.runnit.api.model.Moment;
import com.runnit.api.service.MomentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/moments")
@RequiredArgsConstructor
public class MomentController {
    
    private final MomentService momentService;
    
    @PostMapping
    public ResponseEntity<?> createMoment(
            @Valid @RequestBody MomentRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Moment moment = momentService.createMoment(userId, request);
            return ResponseEntity.ok(moment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/feed")
    public ResponseEntity<?> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Page<MomentResponse> feed = momentService.getFeed(userId, page, size);
            return ResponseEntity.ok(feed);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getMoment(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            MomentResponse moment = momentService.getMomentById(id, userId);
            return ResponseEntity.ok(moment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserMoments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long currentUserId = (Long) auth.getPrincipal();
            Page<MomentResponse> moments = momentService.getUserMoments(userId, currentUserId, page, size);
            return ResponseEntity.ok(moments);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}