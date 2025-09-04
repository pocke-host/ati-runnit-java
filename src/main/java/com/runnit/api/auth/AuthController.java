// src/main/java/com/runnit/api/auth/AuthController.java
package com.runnit.api.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Fake "check" — later we’ll look up in DB
        if ("taken@example.com".equalsIgnoreCase(request.getEmail())) {
            return ResponseEntity.status(409).body(Map.of(
                "ok", false,
                "message", "Email already in use"
            ));
        }

        // Fake "create user" response
        return ResponseEntity.status(201).body(Map.of(
            "ok", true,
            "message", "User registered successfully",
            "user", Map.of(
                "id", 1,
                "name", request.getName(),
                "email", request.getEmail()
            )
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", false,
                "message", "Email and password required"
            ));
        }

        // fake success
        return ResponseEntity.ok(Map.of(
            "ok", true,
            "token", "fake-jwt-token",
            "email", email
        ));
    }

    @GetMapping("/me")
    public Map<String,Object> me() {
        return Map.of("id", 1, "name", "Quinn", "email", "quinn@example.com");
    }
}
