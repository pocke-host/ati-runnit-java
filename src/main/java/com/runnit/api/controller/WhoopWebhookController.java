package com.runnit.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.service.WhoopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Receives WHOOP v2 webhook events (workout.updated, sleep.updated,
 * recovery.updated, plus .deleted variants). Register
 * https://ati-runnit-java.onrender.com/api/integrations/whoop/webhook
 * as the Webhook URL in the WHOOP Developer Dashboard app settings.
 * See https://developer.whoop.com/docs/developing/webhooks/.
 */
@Slf4j
@RestController
@RequestMapping("/api/integrations/whoop")
@RequiredArgsConstructor
public class WhoopWebhookController {

    private final WhoopService whoopService;
    private final ObjectMapper objectMapper;

    /**
     * POST /api/integrations/whoop/webhook
     * Body must be read as a raw String (not auto-deserialized to a Map) so the
     * exact bytes WHOOP signed are what we verify — re-serializing a parsed Map
     * could reorder fields or change whitespace and break the HMAC comparison.
     * Must return 2XX quickly; WHOOP retries non-2XX/timeouts up to 5x over ~1hr.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-WHOOP-Signature", required = false) String signature,
            @RequestHeader(value = "X-WHOOP-Signature-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody) {
        try {
            if (!whoopService.verifyWebhookSignature(timestamp, rawBody, signature)) {
                log.warn("WHOOP webhook: signature verification failed, rejecting");
                return ResponseEntity.status(401).build();
            }

            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            Object userIdRaw = payload.get("user_id");
            String type = (String) payload.get("type");
            if (userIdRaw instanceof Number userId) {
                whoopService.handleWebhookEvent(userId.longValue(), type);
            }
        } catch (Exception e) {
            log.warn("WHOOP webhook: failed to process payload — {}", e.getMessage());
            // Still ack 2XX — a malformed payload from WHOOP's side will never succeed
            // on retry, so returning an error just burns WHOOP's 5-retry budget for nothing.
        }
        return ResponseEntity.ok().build();
    }
}
