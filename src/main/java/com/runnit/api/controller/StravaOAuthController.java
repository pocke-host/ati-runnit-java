package com.runnit.api.controller;

import com.runnit.api.service.StravaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles Strava webhook subscription verification and event delivery.
 * Strava requires a GET endpoint for verification and POST for events.
 */
@RestController
@RequestMapping("/api/strava/webhook")
@RequiredArgsConstructor
public class StravaOAuthController {

    private final StravaService stravaService;

    @Value("${strava.webhook.verify.token:runnit_strava_verify}")
    private String verifyToken;

    /** GET /api/strava/webhook — Strava challenge verification */
    @GetMapping
    public ResponseEntity<?> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(Map.of("hub.challenge", challenge));
        }
        return ResponseEntity.status(403).body(Map.of("error", "Invalid verify token"));
    }

    /** POST /api/strava/webhook — Strava pushes activity events here */
    @PostMapping
    public ResponseEntity<Void> event(@RequestBody Map<String, Object> event) {
        try {
            stravaService.handleWebhookEvent(event);
        } catch (Exception ignored) {
            // Always return 200 to Strava so it doesn't retry
        }
        return ResponseEntity.ok().build();
    }
}
