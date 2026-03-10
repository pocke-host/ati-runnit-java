package com.runnit.api.controller;

import com.runnit.api.service.GarminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * Handles the OAuth 1.0a callback redirect from Garmin Connect.
 * Garmin redirects here with oauth_token and oauth_verifier after user authorizes.
 */
@RestController
@RequestMapping("/api/garmin/oauth")
@RequiredArgsConstructor
public class GarminOAuthController {

    private final GarminService garminService;

    /** GET /api/garmin/oauth/callback — Garmin redirects here after authorization */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String oauth_token,
            @RequestParam(required = false) String oauth_verifier,
            @RequestParam(required = false) String error) {

        String redirectUrl;
        if (error != null || oauth_token == null || oauth_verifier == null) {
            redirectUrl = garminService.getFrontendUrl() + "/devices?error=garmin_denied";
        } else {
            try {
                redirectUrl = garminService.handleCallback(oauth_token, oauth_verifier);
            } catch (Exception e) {
                redirectUrl = garminService.getFrontendUrl() + "/devices?error=garmin_failed";
            }
        }
        return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }

    /** POST /api/garmin/webhook — Garmin Health API pushes activity events here */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        // Reserved for Garmin Health API push notifications (future use)
        return ResponseEntity.ok().build();
    }
}
