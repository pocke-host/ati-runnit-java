package com.runnit.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        // TODO: integrate with email service (Mailchimp, Resend, etc.)
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }
}
