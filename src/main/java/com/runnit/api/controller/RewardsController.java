package com.runnit.api.controller;

import com.runnit.api.service.RewardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardsController {
    private final RewardsService rewards;

    @GetMapping
    public ResponseEntity<?> dashboard(Authentication auth) {
        try { return ResponseEntity.ok(rewards.dashboard((Long) auth.getPrincipal())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{rewardId}/redeem")
    public ResponseEntity<?> redeem(@PathVariable Long rewardId, @RequestBody(required = false) Map<String, String> body, Authentication auth) {
        try { return ResponseEntity.ok(rewards.redeem((Long) auth.getPrincipal(), rewardId, body)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{rewardId}/checkout")
    public ResponseEntity<?> checkout(@PathVariable Long rewardId, @RequestBody(required = false) Map<String, String> body, Authentication auth) {
        try { return ResponseEntity.ok(rewards.createPaidCheckout((Long) auth.getPrincipal(), rewardId, body)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/redemptions/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, Authentication auth) {
        try { return ResponseEntity.ok(rewards.cancel((Long) auth.getPrincipal(), id, body == null ? null : body.get("reason"))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
