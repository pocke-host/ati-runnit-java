// ========== GarminWebhookController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.GarminActivityDTO;
import com.runnit.api.service.GarminWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/garmin")
@RequiredArgsConstructor
public class GarminWebhookController {

    private final GarminWebhookService garminWebhookService;

    @PostMapping
    public ResponseEntity<String> handleGarminWebhook(
            @RequestBody GarminActivityDTO activityDTO,
            @RequestHeader(value = "X-Garmin-Signature", required = false) String signature
    ) {
        log.info("Received Garmin webhook for user: {}", activityDTO.getUserAccessToken());
        
        try {
            // TODO: Verify signature for security
            // verifyGarminSignature(activityDTO, signature);
            
            garminWebhookService.processGarminActivity(activityDTO);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Failed to process Garmin webhook", e);
            return ResponseEntity.status(500).body("Failed to process webhook");
        }
    }

    @GetMapping("/verification")
    public ResponseEntity<String> verifyWebhook(@RequestParam String challenge) {
        // Garmin webhook verification
        return ResponseEntity.ok(challenge);
    }
}