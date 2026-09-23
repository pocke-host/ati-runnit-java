package com.runnit.api.controller;

import com.runnit.api.service.OuraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/integrations/oura")
@RequiredArgsConstructor
public class OuraIntegrationController {
    private final OuraService ouraService;

    @GetMapping("/connect") public ResponseEntity<?> connect(Authentication auth) { try { return ResponseEntity.ok(Map.of("url", ouraService.buildAuthorizationUrl((Long) auth.getPrincipal()))); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); } }
    @GetMapping("/callback") public ResponseEntity<Void> callback(@RequestParam(required = false) String code, @RequestParam(required = false) String state, @RequestParam(required = false) String error) {
        String target = ouraService.getFrontendUrl();
        if (error != null || code == null || state == null) target += "/devices?error=oura_denied";
        else try { target = ouraService.handleCallback(code, state); } catch (Exception e) { log.error("Oura callback failed", e); target += "/devices?error=oura_failed"; }
        return ResponseEntity.status(302).location(URI.create(target)).build();
    }
    @PostMapping("/mobile-callback") public ResponseEntity<?> mobileCallback(@RequestBody Map<String, String> body) { try { ouraService.handleCallback(body.get("code"), body.get("state")); return ResponseEntity.ok(Map.of("connected", true)); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "Oura connection failed")); } }
    @GetMapping("/status") public ResponseEntity<?> status(Authentication auth) { try { return ResponseEntity.ok(ouraService.getStatus((Long) auth.getPrincipal())); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); } }
    @PostMapping("/sync") public ResponseEntity<?> sync(Authentication auth) { try { return ResponseEntity.ok(ouraService.sync((Long) auth.getPrincipal())); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); } }
    @DeleteMapping("/disconnect") public ResponseEntity<?> disconnect(Authentication auth) { try { ouraService.disconnect((Long) auth.getPrincipal()); return ResponseEntity.ok(Map.of("message", "Oura disconnected")); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); } }
}
