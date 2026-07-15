package com.runnit.api.controller;

import com.runnit.api.service.CorosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/coros")
@RequiredArgsConstructor
public class CorosIntegrationController {

    private final CorosService corosService;

    /** GET /api/integrations/coros/connect — returns COROS authorization URL */
    @GetMapping("/connect")
    public ResponseEntity<?> connect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String authUrl = corosService.buildAuthorizationUrl(userId);
            return ResponseEntity.ok(Map.of("url", authUrl));
        } catch (Exception e) {
            log.error("COROS connect failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/integrations/coros/callback — OAuth 2.0 redirect from COROS */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String redirectUrl;
        if (error != null || code == null || state == null) {
            redirectUrl = corosService.getFrontendUrl() + "/devices?error=coros_denied";
        } else {
            try {
                redirectUrl = corosService.handleCallback(code, state);
            } catch (Exception e) {
                log.error("COROS callback failed: {}", e.getMessage(), e);
                redirectUrl = corosService.getFrontendUrl() + "/devices?error=coros_failed";
            }
        }
        return ResponseEntity.status(302)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }

    /** GET /api/integrations/coros/status */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(corosService.getStatus(userId));
        } catch (Exception e) {
            log.error("COROS status failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/integrations/coros/sync — manual sync trigger */
    @PostMapping("/sync")
    public ResponseEntity<?> sync(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            int count = corosService.syncActivities(userId);
            return ResponseEntity.ok(Map.of("imported", count, "message", count + " activities synced"));
        } catch (Exception e) {
            log.error("COROS sync failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/integrations/coros/disconnect */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            corosService.disconnect(userId);
            return ResponseEntity.ok(Map.of("message", "COROS disconnected"));
        } catch (Exception e) {
            log.error("COROS disconnect failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
