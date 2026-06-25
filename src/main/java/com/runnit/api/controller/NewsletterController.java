package com.runnit.api.controller;

import com.runnit.api.model.WaitlistEntry;
import com.runnit.api.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final WaitlistRepository waitlistRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        // TODO: integrate with email service (Mailchimp, Resend, etc.)
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }

    @PostMapping("/waitlist")
    public ResponseEntity<?> joinWaitlist(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        if (waitlistRepository.existsByEmail(email.toLowerCase().trim())) {
            return ResponseEntity.ok(Map.of("message", "already_joined"));
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .email(email.toLowerCase().trim())
                .name(body.get("name"))
                .trainingFor(body.get("trainingFor"))
                .trainsWith(body.get("trainsWith"))
                .build();

        waitlistRepository.save(entry);
        log.info("[waitlist] new signup: {} (training: {})", entry.getEmail(), entry.getTrainingFor());

        return ResponseEntity.ok(Map.of("message", "joined"));
    }
}
