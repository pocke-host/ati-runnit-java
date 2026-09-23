package com.runnit.api.controller;

import com.runnit.api.service.GoogleHealthFitbitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/fitbit")
@RequiredArgsConstructor
public class GoogleHealthFitbitController {
    private final GoogleHealthFitbitService fitbit;

    @GetMapping("/connect") public ResponseEntity<?> connect(Authentication auth) { try { return ResponseEntity.ok(Map.of("url", fitbit.buildAuthorizationUrl((Long) auth.getPrincipal()))); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); } }
    @GetMapping("/callback") public ResponseEntity<Void> callback(@RequestParam(required = false) String code, @RequestParam(required = false) String state, @RequestParam(required = false) String error) {
        String target = fitbit.getFrontendUrl();
        if (error != null || code == null || state == null) target += "/devices?error=fitbit_denied";
        else try { target = fitbit.handleCallback(code, state); } catch (Exception e) { log.error("Google Health Fitbit callback failed", e); target += "/devices?error=fitbit_failed"; }
        return ResponseEntity.status(302).location(URI.create(target)).build();
    }
    @PostMapping("/mobile-callback") public ResponseEntity<?> mobileCallback(@RequestBody Map<String, String> body) { try { fitbit.handleCallback(body.get("code"), body.get("state")); return ResponseEntity.ok(Map.of("connected", true)); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "Fitbit connection failed")); } }
    @GetMapping("/status") public ResponseEntity<?> status(Authentication auth) { return ResponseEntity.ok(fitbit.status((Long) auth.getPrincipal())); }
    @PostMapping("/sync") public ResponseEntity<?> sync(Authentication auth) { return ResponseEntity.ok(fitbit.sync((Long) auth.getPrincipal())); }
    @DeleteMapping("/disconnect") public ResponseEntity<?> disconnect(Authentication auth) { fitbit.disconnect((Long) auth.getPrincipal()); return ResponseEntity.ok(Map.of("message", "Fitbit disconnected")); }
}
