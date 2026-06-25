package com.runnit.api.controller;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.model.Club;
import com.runnit.api.model.ClubMember;
import com.runnit.api.model.ClubMessage;
import com.runnit.api.model.User;
import com.runnit.api.repository.ClubMemberRepository;
import com.runnit.api.repository.ClubMessageRepository;
import com.runnit.api.repository.ClubRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository memberRepository;
    private final ClubMessageRepository messageRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllClubs() {
        return ResponseEntity.ok(clubRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toMap).collect(Collectors.toList()));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyClubs(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(clubRepository.findByMemberUserId(userId)
                    .stream().map(this::toMap).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createClub(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Club club = Club.builder()
                    .name((String) body.get("name"))
                    .sport((String) body.get("sport"))
                    .description((String) body.get("description"))
                    .imageUrl((String) body.get("imageUrl"))
                    .privateClub(Boolean.TRUE.equals(body.get("isPrivate")))
                    .owner(owner)
                    .memberCount(1)
                    .build();
            club = clubRepository.save(club);

            memberRepository.save(ClubMember.builder().clubId(club.getId()).userId(userId).build());
            return ResponseEntity.ok(toMap(club));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/join")
    @Transactional
    public ResponseEntity<?> join(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Club club = clubRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Club not found"));
            if (memberRepository.existsByClubIdAndUserId(id, userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Already a member"));
            }
            memberRepository.save(ClubMember.builder().clubId(id).userId(userId).build());
            club.setMemberCount(club.getMemberCount() + 1);
            clubRepository.save(club);
            return ResponseEntity.ok(Map.of("message", "Joined club"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/leave")
    @Transactional
    public ResponseEntity<?> leave(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            memberRepository.deleteByClubIdAndUserId(id, userId);
            clubRepository.findById(id).ifPresent(c -> {
                c.setMemberCount(Math.max(0, c.getMemberCount() - 1));
                clubRepository.save(c);
            });
            return ResponseEntity.ok(Map.of("message", "Left club"));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getMembers(@PathVariable Long id) {
        try {
            List<Long> userIds = memberRepository.findByClubId(id)
                    .stream().map(ClubMember::getUserId).collect(Collectors.toList());
            List<Map<String, Object>> members = userRepository.findAllById(userIds).stream().map(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("displayName", u.getDisplayName());
                m.put("avatarUrl", u.getAvatarUrl());
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id) {
        try {
            List<Map<String, Object>> messages = messageRepository.findByClubIdOrderByCreatedAtAsc(id)
                    .stream().map(this::toMessageMap).collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/messages")
    @Transactional
    public ResponseEntity<?> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Club club = clubRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Club not found"));
            ClubMessage message = ClubMessage.builder()
                    .club(club).user(user).content(body.get("text")).build();
            message = messageRepository.save(message);
            return ResponseEntity.ok(toMessageMap(message));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/clubs/search?q=QUERY&city=CITY&sport=SPORT
     * At least one param is required. When city alone is provided, returns clubs ordered by
     * member count. Otherwise falls back to a full-text contains search across name/city/sport.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchClubs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sport) {
        try {
            boolean hasQ = q != null && !q.isBlank();
            boolean hasCity = city != null && !city.isBlank();
            boolean hasSport = sport != null && !sport.isBlank();

            if (!hasQ && !hasCity && !hasSport) {
                throw new BadRequestException("At least one search parameter (q, city, sport) is required");
            }

            List<Club> results;
            if (hasCity && !hasQ && !hasSport) {
                // City-only search: return sorted by member count descending
                results = clubRepository.findByCityIgnoreCaseOrderByMemberCountDesc(city);
            } else {
                // General search: use the query text or fall back to city/sport as search terms
                String term = hasQ ? q : (hasCity ? city : sport);
                String cityTerm = hasCity ? city : term;
                String sportTerm = hasSport ? sport : term;
                results = clubRepository
                        .findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrSportContainingIgnoreCase(
                                term, cityTerm, sportTerm);
            }

            return ResponseEntity.ok(results.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/clubs/nearby?lat=LAT&lng=LNG&radiusKm=RADIUS
     * Returns clubs within radiusKm (default 25 km) sorted by distance ascending.
     */
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyClubs(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "25") double radiusKm) {
        try {
            if (lat == null || lng == null) {
                throw new BadRequestException("lat and lng are required");
            }
            List<Club> nearby = clubRepository.findNearby(lat, lng, radiusKm);
            return ResponseEntity.ok(nearby.stream().map(this::toMap).collect(Collectors.toList()));
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/with-location")
    public ResponseEntity<?> getWithLocation() {
        return ResponseEntity.ok(clubRepository.findAll().stream()
                .filter(c -> c.getLatitude() != null)
                .map(this::toMap).collect(Collectors.toList()));
    }

    @PatchMapping("/{id}/location")
    @Transactional
    public ResponseEntity<?> setLocation(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Club club = clubRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Club not found"));
            if (!club.getOwner().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not owner"));
            }
            if (body.get("latitude") != null)
                club.setLatitude(((Number) body.get("latitude")).doubleValue());
            if (body.get("longitude") != null)
                club.setLongitude(((Number) body.get("longitude")).doubleValue());
            if (body.get("city") != null)
                club.setCity((String) body.get("city"));
            clubRepository.save(club);
            return ResponseEntity.ok(toMap(club));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Club c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("sport", c.getSport());
        map.put("description", c.getDescription());
        map.put("imageUrl", c.getImageUrl());
        map.put("memberCount", c.getMemberCount());
        map.put("isPrivate", c.isPrivateClub());
        map.put("createdAt", c.getCreatedAt());
        map.put("ownerId",   c.getOwner().getId());
        map.put("latitude",  c.getLatitude());
        map.put("longitude", c.getLongitude());
        map.put("city",      c.getCity());
        return map;
    }

    private Map<String, Object> toMessageMap(ClubMessage m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("text", m.getContent());
        map.put("content", m.getContent());
        map.put("senderId", m.getUser().getId());
        map.put("userId", m.getUser().getId());
        map.put("senderName", m.getUser().getDisplayName());
        map.put("displayName", m.getUser().getDisplayName());
        map.put("createdAt", m.getCreatedAt());
        map.put("timestamp", m.getCreatedAt());
        return map;
    }
}
