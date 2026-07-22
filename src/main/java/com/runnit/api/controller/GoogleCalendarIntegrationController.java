package com.runnit.api.controller;

import com.runnit.api.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/google-calendar")
@RequiredArgsConstructor
public class GoogleCalendarIntegrationController {

    private final GoogleCalendarService googleCalendarService;

    /** GET /api/integrations/google-calendar/connect — returns the Google OAuth URL to redirect to */
    @GetMapping("/connect")
    public ResponseEntity<?> connect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String authUrl = googleCalendarService.buildAuthorizationUrl(userId);
            return ResponseEntity.ok(Map.of("url", authUrl));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/integrations/google-calendar/callback — public, Google redirects here after consent */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String redirectUrl;
        if (error != null || code == null || state == null) {
            redirectUrl = googleCalendarService.getFrontendUrl() + "/calendar/sync?error=google_calendar_denied";
        } else {
            try {
                redirectUrl = googleCalendarService.handleCallback(code, state);
            } catch (Exception e) {
                log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
                redirectUrl = googleCalendarService.getFrontendUrl() + "/calendar/sync?error=google_calendar_failed";
            }
        }
        return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }

    /** POST /api/integrations/google-calendar/mobile-callback — iOS sends code+state in JSON body */
    @PostMapping("/mobile-callback")
    public ResponseEntity<?> mobileCallback(@RequestBody Map<String, String> body) {
        String code  = body.get("code");
        String state = body.get("state");
        if (code == null || state == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "code and state are required"));
        }
        try {
            googleCalendarService.handleCallback(code, state);
            return ResponseEntity.ok(Map.of("connected", true));
        } catch (Exception e) {
            log.error("Mobile Google Calendar callback failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Google Calendar connection failed"));
        }
    }

    /** GET /api/integrations/google-calendar/status */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(googleCalendarService.getStatus(userId));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/integrations/google-calendar/disconnect */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            googleCalendarService.disconnect(userId);
            return ResponseEntity.ok(Map.of("message", "Google Calendar disconnected"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/integrations/google-calendar/events — create */
    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> workout, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(googleCalendarService.createEvent(userId, workout));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PATCH /api/integrations/google-calendar/events/{googleEventId} — update */
    @PatchMapping("/events/{googleEventId}")
    public ResponseEntity<?> updateEvent(
            @PathVariable String googleEventId,
            @RequestBody Map<String, Object> workout,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(googleCalendarService.updateEvent(userId, googleEventId, workout));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/integrations/google-calendar/events/{googleEventId} */
    @DeleteMapping("/events/{googleEventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable String googleEventId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            googleCalendarService.deleteEvent(userId, googleEventId);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/integrations/google-calendar/events?start=ISO&end=ISO — read */
    @GetMapping("/events")
    public ResponseEntity<?> listEvents(
            @RequestParam String start,
            @RequestParam String end,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(googleCalendarService.listEvents(userId, start, end));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
