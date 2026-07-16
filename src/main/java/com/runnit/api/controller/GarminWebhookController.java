package com.runnit.api.controller;

import com.runnit.api.service.GarminWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Receives Garmin Health API activity push events.
 * Register https://ati-runnit-java.onrender.com/api/garmin/oauth/webhook
 * as the callback URL in the Garmin Health API developer portal.
 */
@Slf4j
@RestController
@RequestMapping("/api/garmin/oauth")
@RequiredArgsConstructor
public class GarminWebhookController {

    private final GarminWebhookService garminWebhookService;

    /**
     * POST /api/garmin/oauth/webhook
     * Garmin pushes activity summaries here immediately after device sync.
     * Payload: { "activities": [ { "userAccessToken": "...", "activityId": ..., ... } ] }
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> activities =
                    (List<Map<String, Object>>) payload.get("activities");

            if (activities != null && !activities.isEmpty()) {
                garminWebhookService.processActivities(activities);
            }
        } catch (Exception e) {
            log.error("Garmin webhook processing failed: {}", e.getMessage(), e);
        }

        // Always return 200 — Garmin retries on non-2xx
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/garmin/oauth/webhook
     * Garmin sends a verification ping during webhook registration — respond with 200.
     */
    @GetMapping("/webhook")
    public ResponseEntity<Void> verify() {
        return ResponseEntity.ok().build();
    }
}
