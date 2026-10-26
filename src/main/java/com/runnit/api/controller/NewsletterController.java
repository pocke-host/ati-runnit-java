package com.runnit.api.controller;

import com.runnit.api.model.WaitlistEntry;
import com.runnit.api.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
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

        email = email.trim().toLowerCase();
        if (email.length() > 255 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid email"));
        }

        if (waitlistRepository.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("message", "already_joined"));
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .email(email)
                .name(body.get("name"))
                .trainingFor(body.get("trainingFor"))
                .trainsWith(body.get("trainsWith"))
                .build();

        try {
            waitlistRepository.save(entry);
        } catch (DataIntegrityViolationException duplicate) {
            // Two tabs or two people can submit the same address at the same time.
            // The unique database constraint remains the source of truth.
            return ResponseEntity.ok(Map.of("message", "already_joined"));
        }
        log.info("[waitlist] new signup: {} (training: {})", entry.getEmail(), entry.getTrainingFor());

        return ResponseEntity.ok(Map.of("message", "joined"));
    }
}
