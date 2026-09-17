package com.runnit.api.controller;

import com.runnit.api.service.RunSignupOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations/runsignup/oauth")
@RequiredArgsConstructor
public class RunSignupOAuthController {
    private final RunSignupOAuthService oauth;

    @GetMapping("/connect")
    public Map<String, String> connect(Authentication auth) { return Map.of("url", oauth.connect((Long) auth.getPrincipal())); }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null || code == null || state == null) {
            return redirect(oauth.failureRedirect(error == null ? "missing_code" : error));
        }
        try {
            return redirect(oauth.callback(code, state));
        } catch (Exception e) {
            return redirect(oauth.failureRedirect("exchange_failed"));
        }
    }

    @PostMapping("/mobile-callback")
    public ResponseEntity<?> mobileCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String state = body.get("state");
        if (code == null || state == null) return ResponseEntity.badRequest().body(Map.of("error", "code and state are required"));
        try { oauth.callback(code, state); return ResponseEntity.ok(Map.of("connected", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "RunSignup connection failed")); }
    }

    @GetMapping("/status")
    public Map<String, Object> status(Authentication auth) { return oauth.status((Long) auth.getPrincipal()); }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(302).location(URI.create(location)).build();
    }
}
