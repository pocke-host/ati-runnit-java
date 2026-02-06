// ========== StravaOAuthController.java ==========
package com.runnit.controller;

import com.runnit.service.StravaOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/strava")
@RequiredArgsConstructor
public class StravaOAuthController {

    private final StravaOAuthService stravaOAuthService;

    /**
     * Step 1: Initiate Strava OAuth flow
     */
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> initiateConnection(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Initiating Strava OAuth for user: {}", userDetails.getUsername());
        
        String authUrl = stravaOAuthService.getAuthorizationUrl(userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "authorizationUrl", authUrl,
            "message", "Redirect user to this URL to authorize Strava"
        ));
    }

    /**
     * Step 2: Handle OAuth callback from Strava
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam("code") String authorizationCode,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state
    ) {
        log.info("Received Strava OAuth callback with code: {}", authorizationCode);
        
        try {
            stravaOAuthService.handleCallback(authorizationCode, state);
            
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/dashboard?strava=connected")
                .body("Strava connected successfully");
                
        } catch (Exception e) {
            log.error("Failed to handle Strava callback", e);
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/dashboard?strava=error")
                .body("Failed to connect Strava");
        }
    }

    /**
     * Check connection status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean connected = stravaOAuthService.isConnected(userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "connected", connected,
            "provider", "strava"
        ));
    }

    /**
     * Disconnect Strava
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Disconnecting Strava for user: {}", userDetails.getUsername());
        
        stravaOAuthService.disconnect(userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "message", "Strava disconnected successfully"
        ));
    }

    /**
     * Manually trigger webhook subscription (for testing)
     */
    @PostMapping("/subscribe-webhook")
    public ResponseEntity<Map<String, String>> subscribeWebhook() {
        log.info("Manually subscribing to Strava webhook");
        
        try {
            stravaOAuthService.subscribeToWebhook();
            return ResponseEntity.ok(Map.of(
                "message", "Webhook subscription created"
            ));
        } catch (Exception e) {
            log.error("Failed to subscribe to webhook", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to subscribe to webhook"
            ));
        }
    }
}