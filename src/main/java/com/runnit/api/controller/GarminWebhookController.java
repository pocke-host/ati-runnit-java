package com.runnit.api.controller;

import com.runnit.api.service.GarminWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives Garmin Health API activity push events.
 * Register https://ati-runnit-java.onrender.com/api/garmin/webhook
 * as the callback URL in the Garmin Health API developer portal.
 */
@Slf4j
@RestController
@RequestMapping({"/api/garmin", "/api/garmin/oauth"})
@RequiredArgsConstructor
public class GarminWebhookController {

    private final GarminWebhookService garminWebhookService;

    /**
     * POST /api/garmin/webhook
     * Garmin pushes activity summaries here immediately after device sync.
     * Payload may include activities, dailies, sleeps, and other enabled Garmin datasets.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload) {
        try {
            garminWebhookService.processPayload(payload);
        } catch (Exception e) {
            log.error("Garmin webhook processing failed: {}", e.getMessage(), e);
            // Do not acknowledge an envelope that could not be processed. A
            // non-2xx response lets Garmin retry the delivery instead of
            // silently losing the user's activity.
            return ResponseEntity.internalServerError().build();
        }

        // Acknowledge only after the payload has been accepted for processing.
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/garmin/webhook
     * Garmin sends a verification ping during webhook registration — respond with 200.
     */
    @GetMapping("/webhook")
    public ResponseEntity<Void> verify() {
        return ResponseEntity.ok().build();
    }
}
