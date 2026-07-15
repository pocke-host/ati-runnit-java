package com.runnit.api.controller;

import com.runnit.api.service.CorosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives push notifications from the COROS Open API when a user completes
 * an activity. COROS sends a POST to this endpoint with the user's open_id
 * and the labelId of the new activity.
 *
 * Endpoint must be registered in the COROS developer dashboard as the webhook URL.
 */
@Slf4j
@RestController
@RequestMapping("/api/coros/webhook")
@RequiredArgsConstructor
public class CorosWebhookController {

    private final CorosService corosService;

    @Value("${coros.webhook.secret:}")
    private String webhookSecret;

    /** POST /api/coros/webhook */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Coros-Signature", required = false) String signature,
            @RequestBody Map<String, Object> payload) {

        // Signature verification — skip if secret not yet configured
        if (!webhookSecret.isBlank() && !webhookSecret.equals(signature)) {
            log.warn("COROS webhook: invalid signature");
            return ResponseEntity.status(401).build();
        }

        try {
            String openId  = (String) payload.get("openId");
            String labelId = (String) payload.get("labelId");

            if (openId == null || labelId == null) {
                log.warn("COROS webhook: missing openId or labelId in payload");
                return ResponseEntity.ok().build();
            }

            log.info("COROS webhook: activity {} for user {}", labelId, openId);
            corosService.handleWebhookActivity(openId, labelId);
        } catch (Exception e) {
            log.error("COROS webhook processing failed: {}", e.getMessage(), e);
        }

        // Always return 200 — COROS retries on non-2xx
        return ResponseEntity.ok().build();
    }
}
