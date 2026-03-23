package com.runnit.api.controller;

import com.runnit.api.model.SosEvent;
import com.runnit.api.repository.SosEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sos-events")
@RequiredArgsConstructor
public class SosEventController {

    private final SosEventRepository repo;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            SosEvent ev = new SosEvent();
            ev.setUserId(userId);
            if (body.get("lat") != null) ev.setLat(((Number) body.get("lat")).doubleValue());
            if (body.get("lng") != null) ev.setLng(((Number) body.get("lng")).doubleValue());
            if (body.get("shareUrl") != null) ev.setShareUrl((String) body.get("shareUrl"));
            repo.save(ev);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
