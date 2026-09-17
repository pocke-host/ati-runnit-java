package com.runnit.api.controller;

import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.MarketplaceEventRepository;
import com.runnit.api.repository.CoachBookingRepository;
import com.runnit.api.model.CoachBooking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Set<String> VALID_ROLES = Set.of("athlete", "coach", "admin");

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final MarketplaceEventRepository marketplaceEventRepository;
    private final CoachBookingRepository coachBookingRepository;

    // ── Admin role guard ─────────────────────────────────────────────────────

    private boolean isAdmin(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .map(u -> "admin".equals(u.getRole()))
                .orElse(false);
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
    }

    // ── Endpoints ────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/stats
     * Returns platform-wide counts.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication auth) {
        if (!isAdmin(auth)) return forbidden();

        long totalUsers      = userRepository.count();
        long totalActivities = activityRepository.count();
        long coaches         = userRepository.findByRole("coach").size();
        long athletes        = userRepository.findByRole("athlete").size();
        long admins          = userRepository.findByRole("admin").size();

        return ResponseEntity.ok(Map.of(
                "totalUsers",      totalUsers,
                "totalActivities", totalActivities,
                "athletes",        athletes,
                "coaches",         coaches,
                "admins",          admins
        ));
    }

    @GetMapping("/marketplace/analytics")
    public ResponseEntity<?> marketplaceAnalytics(Authentication auth) {
        if (!isAdmin(auth)) return forbidden();
        var eventCounts = marketplaceEventRepository.findAll().stream().collect(java.util.stream.Collectors.groupingBy(com.runnit.api.model.MarketplaceEvent::getEventType, java.util.stream.Collectors.counting()));
        var paid = coachBookingRepository.findAll().stream().filter(b -> Set.of("PAID", "COMPLETED").contains(b.getStatus())).toList();
        int gross = paid.stream().mapToInt(CoachBooking::getAmountCents).sum();
        int commission = paid.stream().mapToInt(CoachBooking::getCommissionCents).sum();
        return ResponseEntity.ok(Map.of("events", eventCounts, "grossCents", gross, "commissionCents", commission, "paidBookings", paid.size()));
    }

    /**
     * GET /api/admin/users?page=0&size=25&search=query
     * Returns paginated user list. Optional free-text search on name/email.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            Authentication auth,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false)    String search) {
        if (!isAdmin(auth)) return forbidden();

        size = Math.min(size, 100); // cap to 100 per page

        if (search != null && !search.isBlank()) {
            List<User> results = userRepository.searchByDisplayNameOrEmail(
                    search.trim(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            return ResponseEntity.ok(Map.of(
                    "content",       results.stream().map(this::toAdminUserMap).toList(),
                    "totalElements", results.size(),
                    "page",          page,
                    "size",          size
            ));
        }

        Page<User> userPage = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return ResponseEntity.ok(Map.of(
                "content",       userPage.getContent().stream().map(this::toAdminUserMap).toList(),
                "totalElements", userPage.getTotalElements(),
                "totalPages",    userPage.getTotalPages(),
                "page",          page,
                "size",          size
        ));
    }

    /**
     * PATCH /api/admin/users/{id}/role
     * Body: { "role": "athlete" | "coach" | "admin" }
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!isAdmin(auth)) return forbidden();

        String newRole = body.get("role");
        if (newRole == null || !VALID_ROLES.contains(newRole)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role must be one of: athlete, coach, admin"));
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        Long adminId = (Long) auth.getPrincipal();
        log.info("[admin] userId={} changed role of userId={} from {} to {}", adminId, id, oldRole, newRole);

        return ResponseEntity.ok(toAdminUserMap(user));
    }

    @PatchMapping("/coaches/{id}/verification")
    public ResponseEntity<?> verifyCoach(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        if (!isAdmin(auth)) return forbidden();
        User coach = userRepository.findById(id).orElse(null);
        if (coach == null || !"coach".equalsIgnoreCase(coach.getRole())) return ResponseEntity.notFound().build();
        coach.setCoachVerified(Boolean.TRUE.equals(body.get("verified")));
        userRepository.save(coach);
        return ResponseEntity.ok(Map.of("id", id, "coachVerified", coach.getCoachVerified()));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Map<String, Object> toAdminUserMap(User u) {
        return Map.of(
                "id",                 u.getId(),
                "email",              u.getEmail() != null ? u.getEmail() : "",
                "displayName",        u.getDisplayName() != null ? u.getDisplayName() : "",
                "role",               u.getRole() != null ? u.getRole() : "athlete",
                "coachVerified",      Boolean.TRUE.equals(u.getCoachVerified()),
                "subscriptionStatus", u.getSubscriptionStatus() != null ? u.getSubscriptionStatus() : "none",
                "authProvider",       u.getAuthProvider() != null ? u.getAuthProvider().name() : "EMAIL",
                "createdAt",          u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }
}
