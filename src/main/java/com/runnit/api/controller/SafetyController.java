package com.runnit.api.controller;

import com.runnit.api.dto.EmergencyContactDTO;
import com.runnit.api.dto.SosAlertRequest;
import com.runnit.api.service.SafetyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    // ─── Emergency Contacts ───────────────────────────────────────────────────

    @PostMapping("/contacts")
    public ResponseEntity<?> addContact(@RequestBody EmergencyContactDTO dto, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(safetyService.addContact(userId, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getContacts(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(safetyService.getContacts(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/contacts/{contactId}")
    public ResponseEntity<?> updateContact(
            @PathVariable Long contactId,
            @RequestBody EmergencyContactDTO dto,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(safetyService.updateContact(contactId, userId, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<?> deleteContact(@PathVariable Long contactId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            safetyService.deleteContact(contactId, userId);
            return ResponseEntity.ok(Map.of("message", "Contact deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Live Location Sharing ────────────────────────────────────────────────

    @PostMapping("/live-location/start")
    public ResponseEntity<?> startLocationShare(
            @RequestParam(required = false) Long activityId,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(safetyService.startLocationShare(userId, activityId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/live-location/{shareToken}")
    public ResponseEntity<?> updateLocation(
            @PathVariable String shareToken,
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            Authentication auth) {
        try {
            return ResponseEntity.ok(safetyService.updateLocation(shareToken, lat, lng));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/live-location/{shareToken}")
    public ResponseEntity<?> stopLocationShare(@PathVariable String shareToken, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            safetyService.stopLocationShare(userId, shareToken);
            return ResponseEntity.ok(Map.of("message", "Location sharing stopped"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Public endpoint - no auth required (anyone with the token can view)
    @GetMapping("/live-location/{shareToken}")
    public ResponseEntity<?> getLiveLocation(@PathVariable String shareToken) {
        try {
            return ResponseEntity.ok(safetyService.getLiveLocation(shareToken));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─── SOS / Panic Button ───────────────────────────────────────────────────

    @PostMapping("/sos")
    public ResponseEntity<?> triggerSOS(@RequestBody SosAlertRequest request, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(safetyService.triggerSOS(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/sos/{alertId}/resolve")
    public ResponseEntity<?> resolveSOS(@PathVariable Long alertId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(safetyService.resolveSOS(alertId, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sos")
    public ResponseEntity<?> getMySosAlerts(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(safetyService.getMySosAlerts(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
