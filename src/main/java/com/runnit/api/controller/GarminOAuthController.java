package com.runnit.api.controller;

import com.runnit.api.service.GarminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Handles Garmin Connect's OAuth 2.0 Authorization Code + PKCE callback. */
@Slf4j
@RestController
@RequestMapping("/api/garmin/oauth")
@RequiredArgsConstructor
public class GarminOAuthController {

    private final GarminService garminService;

    /** GET /api/garmin/oauth/callback — Garmin redirects here after authorization */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String redirectUrl;
        if (error != null || code == null || state == null) {
            redirectUrl = garminService.getFrontendUrl() + "/devices?error=garmin_denied";
        } else {
            try {
                redirectUrl = garminService.handleCallback(code, state);
            } catch (Exception e) {
                log.error("Garmin OAuth callback failed: {}", e.getMessage(), e);
                redirectUrl = garminService.getFrontendUrl() + "/devices?error=garmin_failed";
            }
        }
        return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }

}
