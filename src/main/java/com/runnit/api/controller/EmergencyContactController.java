package com.runnit.api.controller;

import com.runnit.api.model.EmergencyContact;
import com.runnit.api.repository.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactRepository repo;

    @GetMapping
    public ResponseEntity<?> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(repo.findByUserId(userId).stream()
                .map(this::toMap).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            EmergencyContact ec = new EmergencyContact();
            ec.setUserId(userId);
            ec.setName((String) body.get("name"));
            ec.setPhone((String) body.get("phone"));
            ec.setEmail((String) body.get("email"));
            return ResponseEntity.ok(toMap(repo.save(ec)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            EmergencyContact ec = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Not found"));
            if (!ec.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            repo.delete(ec);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(EmergencyContact ec) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", ec.getId());
        m.put("name", ec.getName());
        m.put("phone", ec.getPhone());
        m.put("email", ec.getEmail());
        m.put("createdAt", ec.getCreatedAt());
        return m;
    }
}
