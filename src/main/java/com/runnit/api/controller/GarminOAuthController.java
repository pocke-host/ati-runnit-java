// ========== GarminOAuthController.java ==========
package com.runnit.api.controller;

import com.runnit.api.service.GarminOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/garmin")
@RequiredArgsConstructor
public class GarminOAuthController {

    private final GarminOAuthService garminOAuthService;

    /**
     * Step 1: Initiate Garmin OAuth flow
     * Returns authorization URL for user to visit
     */
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> initiateConnection(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Initiating Garmin OAuth for user: {}", userDetails.getUsername());
        
        String authUrl = garminOAuthService.getAuthorizationUrl(userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "authorizationUrl", authUrl,
            "message", "Redirect user to this URL to authorize Garmin"
        ));
    }

    /**
     * Step 2: Handle OAuth callback from Garmin
     * Garmin redirects here after user authorizes
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam("oauth_token") String oauthToken,
            @RequestParam("oauth_verifier") String oauthVerifier
    ) {
        log.info("Received Garmin OAuth callback with token: {}", oauthToken);
        
        try {
            garminOAuthService.handleCallback(oauthToken, oauthVerifier);
            
            // Redirect to frontend success page
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/dashboard?garmin=connected")
                .body("Garmin connected successfully");
                
        } catch (Exception e) {
            log.error("Failed to handle Garmin callback", e);
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/dashboard?garmin=error")
                .body("Failed to connect Garmin");
        }
    }

    /**
     * Check if user has Garmin connected
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean connected = garminOAuthService.isConnected(userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "connected", connected,
            "provider", "garmin"
        ));
    }

    /**
     * Disconnect Garmin account
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Disconnecting Garmin for user: {}", userDetails.getUsername());
        
        garminOAuthService.disconnect(userDetails.getUsername());
        
        return ResponseEntity.ok(Map.of(
            "message", "Garmin disconnected successfully"
        ));
    }
}