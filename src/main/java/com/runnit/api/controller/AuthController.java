package com.runnit.api.controller;

import com.runnit.api.dto.LoginRequest;
import com.runnit.api.dto.RegisterRequest;
import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.security.AppleTokenValidator;
import com.runnit.api.security.GoogleTokenValidator;
import com.runnit.api.service.AuthService;
import com.runnit.api.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final FollowRepository followRepository;
    private final ActivityRepository activityRepository;
    private final GoogleTokenValidator googleTokenValidator;
    private final AppleTokenValidator appleTokenValidator;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:2592000000}")
    private Long refreshExpiration;

    // -------------------------------------------------------------------------
    // Email auth
    // -------------------------------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = authService.registerWithEmail(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                request.getRole()
        );
        Long userId = extractUserId(result);
        result.put("user", buildUserResponse(authService.getUserById(userId), userId));

        String refreshToken = refreshTokenService.createRefreshToken(userId);
        result.put("refreshToken", refreshToken);

        setJwtCookie(response, (String) result.get("token"));
        setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = authService.loginWithEmail(request.getEmail(), request.getPassword());
        Long userId = extractUserId(result);
        result.put("user", buildUserResponse(authService.getUserById(userId), userId));

        String refreshToken = refreshTokenService.createRefreshToken(userId);
        result.put("refreshToken", refreshToken);

        setJwtCookie(response, (String) result.get("token"));
        setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Revoke refresh token — prefer body, fall back to cookie
        String refreshToken = resolveRefreshToken(body, request);
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenService.revoke(refreshToken);
        }
        clearCookie(response, "jwt");
        clearCookie(response, "refreshToken");
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(buildUserResponse(user, userId));
    }

    // -------------------------------------------------------------------------
    // Refresh token
    // -------------------------------------------------------------------------

    /**
     * POST /api/auth/refresh
     *
     * Accepts { "refreshToken": "..." } in the body OR a refreshToken httpOnly cookie.
     * Returns a new access JWT + rotated refresh token.
     * Each refresh token is single-use (rotation) — reuse of a revoked token
     * immediately invalidates all sessions for that user.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String incomingToken = resolveRefreshToken(body, request);
        if (!StringUtils.hasText(incomingToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token is required"));
        }

        Map<String, Object> result = refreshTokenService.rotate(incomingToken);

        setJwtCookie(response, (String) result.get("token"));
        setRefreshTokenCookie(response, (String) result.get("refreshToken"));

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Google Sign-In
    // -------------------------------------------------------------------------

    /**
     * POST /api/auth/google
     * Body: { "idToken": "<Google ID token from client>" }
     *
     * Validates the Google ID token, finds or creates the RUNNIT account,
     * and returns a RUNNIT JWT + refresh token.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleSignIn(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        String idToken = body.get("idToken");
        if (!StringUtils.hasText(idToken)) {
            return ResponseEntity.badRequest().body(Map.of("error", "idToken is required"));
        }

        GoogleTokenValidator.GoogleClaims claims = googleTokenValidator.validate(idToken);

        Map<String, Object> result = authService.handleOAuthLogin(
                User.AuthProvider.GOOGLE,
                claims.sub(),
                claims.email(),
                claims.name(),
                claims.picture()
        );

        Long userId = extractUserId(result);
        result.put("user", buildUserResponse(authService.getUserById(userId), userId));

        String refreshToken = refreshTokenService.createRefreshToken(userId);
        result.put("refreshToken", refreshToken);

        setJwtCookie(response, (String) result.get("token"));
        setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Apple Sign-In
    // -------------------------------------------------------------------------

    /**
     * POST /api/auth/apple
     * Body: { "identityToken": "<Apple identity token>", "fullName": "<optional>" }
     *
     * Validates the Apple identity token, finds or creates the RUNNIT account,
     * and returns a RUNNIT JWT + refresh token.
     *
     * Note: Apple only sends the user's name on the very first sign-in.
     * The client should pass `fullName` from the Apple credential when available.
     */
    @PostMapping("/apple")
    public ResponseEntity<?> appleSignIn(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        String identityToken = body.get("identityToken");
        if (!StringUtils.hasText(identityToken)) {
            return ResponseEntity.badRequest().body(Map.of("error", "identityToken is required"));
        }

        AppleTokenValidator.AppleClaims claims = appleTokenValidator.validate(identityToken);

        // Apple may only provide name on first sign-in — use body fallback
        String displayName = StringUtils.hasText(body.get("fullName"))
                ? body.get("fullName")
                : claims.email();

        Map<String, Object> result = authService.handleOAuthLogin(
                User.AuthProvider.APPLE,
                claims.sub(),
                claims.email(),
                displayName,
                null  // Apple doesn't provide a profile picture
        );

        Long userId = extractUserId(result);
        result.put("user", buildUserResponse(authService.getUserById(userId), userId));

        String refreshToken = refreshTokenService.createRefreshToken(userId);
        result.put("refreshToken", refreshToken);

        setJwtCookie(response, (String) result.get("token"));
        setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private UserResponse buildUserResponse(User user, Long userId) {
        String status = user.getSubscriptionStatus();
        String tier = ("active".equals(status) || "trialing".equals(status)) ? "pro" : "free";
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .location(user.getLocation())
                .sport(user.getSport())
                .primarySport(user.getSport())
                .bio(user.getBio())
                .isPublic(user.getIsPublic())
                .role(user.getRole())
                .onboardingComplete(user.getOnboardingComplete())
                .followerCount(followRepository.countByFollowingUserId(userId))
                .followingCount(followRepository.countByFollowerUserId(userId))
                .activityCount(activityRepository.countByUserId(userId))
                .createdAt(user.getCreatedAt())
                .unitSystem(user.getUnitSystem())
                .archetype(user.getArchetype())
                .subscriptionStatus(status)
                .subscriptionTier(tier)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Long extractUserId(Map<String, Object> authResult) {
        Map<String, Object> userMap = (Map<String, Object>) authResult.get("user");
        return ((Number) userMap.get("id")).longValue();
    }

    private String resolveRefreshToken(Map<String, String> body, HttpServletRequest request) {
        // 1. Request body
        if (body != null && StringUtils.hasText(body.get("refreshToken"))) {
            return body.get("refreshToken");
        }
        // 2. httpOnly cookie
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000));
        cookie.setAttribute("SameSite", "None"); // required for cross-origin cookie sharing
        response.addCookie(cookie);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth/refresh"); // scope refresh cookie to the refresh endpoint only
        cookie.setMaxAge((int) (refreshExpiration / 1000));
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
    }
}
