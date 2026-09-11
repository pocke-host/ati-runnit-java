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
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        return ResponseEntity.status(302).location(URI.create(oauth.callback(code, state))).build();
    }

    @GetMapping("/status")
    public Map<String, Object> status(Authentication auth) { return oauth.status((Long) auth.getPrincipal()); }
}
