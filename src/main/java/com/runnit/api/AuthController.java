// src/main/java/com/runnit/api/auth/AuthController.java
package com.runnit.api;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public Map<String,Object> register(@RequestBody Map<String,String> body) {
        return Map.of(
            "ok", true,
            "message", "Fake register success",
            "data", body
        );
    }

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> body) {
        return Map.of(
            "ok", true,
            "token", "fake-jwt-token",
            "email", body.get("email")
        );
    }

    @GetMapping("/me")
    public Map<String,Object> me() {
        return Map.of("id", 1, "name", "Quinn", "email", "quinn@example.com");
    }
}
