package com.runnit.api.controller;

import com.runnit.api.model.RaceBookmark;
import com.runnit.api.repository.RaceBookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/race-bookmarks")
@RequiredArgsConstructor
public class RaceBookmarkController {

    private final RaceBookmarkRepository raceBookmarkRepository;

    /** GET /api/race-bookmarks — list current user's bookmarks */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listBookmarks(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<Map<String, Object>> result = raceBookmarkRepository
                    .findByUserIdOrderByRaceDateAsc(userId)
                    .stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/race-bookmarks — save a bookmark */
    @PostMapping
    @Transactional
    public ResponseEntity<?> createBookmark(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String externalRaceId = (String) body.get("externalRaceId");

            // Prevent duplicates
            if (externalRaceId != null) {
                if (raceBookmarkRepository.findByUserIdAndExternalRaceId(userId, externalRaceId).isPresent()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Already bookmarked"));
                }
            }

            RaceBookmark bm = new RaceBookmark();
            bm.setUserId(userId);
            bm.setExternalRaceId(externalRaceId);
            bm.setRaceName((String) body.getOrDefault("raceName", "Unnamed Race"));
            String raceDateStr = (String) body.get("raceDate");
            if (raceDateStr != null && !raceDateStr.isEmpty()) {
                bm.setRaceDate(LocalDate.parse(raceDateStr));
            }
            bm.setRaceType((String) body.get("raceType"));
            bm.setCity((String) body.get("city"));
            bm.setState((String) body.get("state"));
            bm.setRaceUrl((String) body.get("raceUrl"));

            bm = raceBookmarkRepository.save(bm);
            return ResponseEntity.ok(toMap(bm));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/race-bookmarks/{id} — owner-only delete */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteBookmark(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            RaceBookmark bm = raceBookmarkRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Bookmark not found"));
            if (!bm.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            raceBookmarkRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(RaceBookmark bm) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", bm.getId());
        map.put("externalRaceId", bm.getExternalRaceId());
        map.put("raceName", bm.getRaceName());
        map.put("raceDate", bm.getRaceDate() != null ? bm.getRaceDate().toString() : null);
        map.put("raceType", bm.getRaceType());
        map.put("city", bm.getCity());
        map.put("state", bm.getState());
        map.put("raceUrl", bm.getRaceUrl());
        map.put("createdAt", bm.getCreatedAt());
        return map;
    }
}
