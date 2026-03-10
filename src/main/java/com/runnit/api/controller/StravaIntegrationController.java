package com.runnit.api.controller;

import com.runnit.api.service.StravaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations/strava")
@RequiredArgsConstructor
public class StravaIntegrationController {

    private final StravaService stravaService;

    /** GET /api/integrations/strava/connect
     *  Authenticated — returns the Strava OAuth URL for the frontend to redirect to */
    @GetMapping("/connect")
    public ResponseEntity<?> connect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String authUrl = stravaService.buildAuthorizationUrl(userId);
            return ResponseEntity.ok(Map.of("url", authUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/integrations/strava/callback
     *  Public — Strava redirects here after user authorizes.
     *  Exchanges code for tokens and redirects browser to frontend. */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String redirectUrl;
        if (error != null || code == null || state == null) {
            redirectUrl = stravaService.getFrontendUrl() + "/devices?error=strava_denied";
        } else {
            try {
                redirectUrl = stravaService.handleCallback(code, state);
            } catch (Exception e) {
                redirectUrl = stravaService.getFrontendUrl() + "/devices?error=strava_failed";
            }
        }
        return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }

    /** GET /api/integrations/strava/status */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(stravaService.getStatus(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/integrations/strava/sync — manual activity sync */
    @PostMapping("/sync")
    public ResponseEntity<?> sync(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            int count = stravaService.syncActivities(userId);
            return ResponseEntity.ok(Map.of("imported", count, "message", count + " activities synced"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/integrations/strava/disconnect */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            stravaService.disconnect(userId);
            return ResponseEntity.ok(Map.of("message", "Strava disconnected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
