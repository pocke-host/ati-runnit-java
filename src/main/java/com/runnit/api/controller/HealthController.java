// ========== HealthController.java ==========
package com.runnit.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import java.sql.Connection;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final DataSource dataSource;
    public HealthController(DataSource dataSource) { this.dataSource = dataSource; }
    
    @GetMapping
    public ResponseEntity<?> health(HttpServletRequest request) {
        try (Connection ignored = dataSource.getConnection()) {
            return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now(),
            "service", "runnit-api",
            "database", "UP",
            "requestId", request.getHeader("X-Request-Id") == null ? "" : request.getHeader("X-Request-Id")
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN", "service", "runnit-api", "database", "DOWN", "timestamp", Instant.now(), "requestId", request.getHeader("X-Request-Id") == null ? "" : request.getHeader("X-Request-Id")));
        }
    }
}
